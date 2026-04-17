# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

All five phases of the implementation plan have landed as code in `main`:

- **Phase 0** — CI, Playwright smoke test, OpenAPI 3.1 spec.
- **Phase 1** — Spring Boot backend (Auth + Profile), RS256 JWT + rotating refresh, Flyway V1–V2.
- **Phase 2** — Events + Interests + Matching, PostGIS vicinity, TF-IDF similarity, async recompute.
- **Phase 3** — STOMP over WebSocket chat (JWT CONNECT auth), push outbox, device registration, mobile `firebase_messaging` + `geolocator` + `mobile_scanner`.
- **Phase 4** — SNS OAuth (Facebook, LinkedIn) with AES-256-GCM tokens, GDPR export aggregator, soft/hard-delete cron, CSP + HSTS filter.
- **Phase 5** — `X-Request-Id` + JSON logs, Micrometer + OTel, 7 Prometheus alerts, Grafana dashboard, Helm chart, runbooks.

Several items from the plan were authored as compiling skeletons rather than production wiring — see the "Gaps" section below before assuming something is ready for prod.

## Commands

### Web (`cd web`)
```bash
pnpm install       # install deps
pnpm dev           # dev server → http://localhost:3000
pnpm build         # production build
pnpm lint          # ESLint (next/core-web-vitals)
pnpm typecheck     # tsc --noEmit
pnpm test:e2e      # Playwright smoke (auto-starts `pnpm dev`)
```

### Backend (`cd backend`)
```bash
# One-time: materialise the Gradle wrapper (requires Gradle 8.10+ on PATH)
gradle wrapper --gradle-version 8.10

./gradlew :app:bootRun            # run the Spring Boot app
./gradlew :app:test               # unit / slice tests
./gradlew :app:integrationTest    # Testcontainers-backed flows (needs Docker)
./gradlew :app:bootJar            # produces app/build/libs/sns-backend.jar
```

Dev-mode overrides: `VERIFICATION_DEV_MODE=true` makes the email TAN always `123456`. `sns.dev.seed-events=true` seeds the `NEURIPS2026` demo event idempotently.

### Mobile (`cd mobile`)
```bash
# One-time: generate platform projects and Isar codegen
flutter create --platforms=android,ios --project-name=sns_mobile .
dart run build_runner build --delete-conflicting-outputs

flutter pub get
flutter analyze
flutter test
flutter run                       # Android emulator uses http://10.0.2.2:3000
```
Override at build time: `flutter run --dart-define=FRONTEND_ORIGIN=https://staging.example.com`.

### Infrastructure (`cd infra`)
```bash
# Dev dependencies only
docker compose -f docker-compose.dev.yml up -d
# Postgres+PostGIS :5432, Redis :6379, MinIO :9000/:9001, MailHog :1025/:8025

# Include the backend (builds from backend/app/Dockerfile)
docker compose -f docker-compose.dev.yml --profile backend up --build
```

### Wiring web against real backend
In `web/.env.local`:
```
NEXT_PUBLIC_MOCK_API=1,-auth,-profile,-events,-interests,-matches,-chat,-devices,-sns
BACKEND_PROXY_TARGET=http://localhost:8080
```
`NEXT_PUBLIC_MOCK_API` accepts a CSV: `1` = all domains mocked; `-domain` drops a domain; listing names without `1` enables only those. The Next dev server rewrites `/api/*` and `/.well-known/*` to `BACKEND_PROXY_TARGET`.

## Architecture

### Top-level layout
```
web/         Next.js 14 (App Router), MSW contract + per-domain cutover
mobile/      Flutter 3.22 WebView shell + JS bridge + native services
backend/     Spring Boot 3.3 / Java 21 multi-module
  :app           bootstrap, Flyway, exception handler, export aggregator, GDPR cron
  :common        shared DTOs + cross-module domain events
  :identity      auth, JWT, JWKS, Spring Security filter chain
  :profile       profile CRUD + soft-delete
  :event         events + participations (PostGIS), vicinity query
  :interest      interests + keyword extractor + article storage
  :matching      similarity engine + scheduled recompute
  :chat          chat REST + STOMP/WS
  :notification  device tokens + push outbox + pluggable gateway
  :sns           OAuth2 link/callback/unlink + AES-256-GCM
infra/       docker-compose, Helm chart, Prometheus alerts, Grafana dashboard
docs/        SNS-system.md spec + runbooks/
```

### JS ↔ Flutter bridge
The most non-obvious system in this repo. Every cross-boundary call uses `{ id, type, payload }` JSON messages.

- **Web → Native**: `window.FlutterBridge.postMessage(json)` for GPS start/stop, QR scan, file picker, secure storage, SNS OAuth, push permission/token, localdb (Isar).
- **Native → Web**: Flutter evaluates JS via `window.dispatchEvent(new CustomEvent("flutter-bridge-event", ...))` for `gps.fix`, `push.received`, connectivity changes, app resume.
- Web side: `web/lib/bridge/client.ts` + `types.ts`; browser fallback `mock.ts` returns fixtures.
- Flutter side: `mobile/lib/bridge/js_bridge.dart` dispatches to services in `mobile/lib/native/`. Location streams fixes back as `gps.fix` events; the scanner opens a full-screen modal via `rootNavigatorKeyProvider`.

### Contract discipline
`web/lib/api/mocks/handlers.ts` and `backend/app/src/main/resources/openapi.yaml` must stay 1:1. When a backend endpoint replaces a mock, remove the MSW handler for that domain and the OpenAPI schema becomes the sole source of truth. The per-domain toggle in `NEXT_PUBLIC_MOCK_API` exists to cut over incrementally.

### State management (web)
- **Zustand** (`web/lib/state/`): auth tokens + userId, current event + radius, chat drafts.
- **TanStack Query**: server state caching + refetch; combined with the axios JWT-refresh interceptor.
- **Axios** (`web/lib/api/axios.ts`): injects `Authorization: Bearer <jwt>` from bridge storage; on 401 silently hits `/api/auth/refresh` and retries once.

### Realtime (Phase 3)
STOMP over WebSocket at `/ws`. The JWT is sent on the CONNECT frame's `Authorization` header; `StompJwtChannelInterceptor` validates and sets the user principal so `convertAndSendToUser(userId, ...)` reaches the right session. Current implementation uses Spring `ApplicationEvent` for fan-out — it works single-pod; multi-pod fan-out requires switching the listener to Redis Pub/Sub (see Gaps).

### Matching engine (Phase 2)
- `KeywordExtractor` (in `:interest`) tokenises text, strips stopwords, returns an L2-normalised TF vector as JSONB `{keyword: weight}`. Intentionally dependency-free — the plan's OpenNLP+RAKE pipeline is a future swap behind the same interface.
- `MatchingService.recompute(eventId)` builds one vector per user (sum-then-normalise of their interests), cosine-compares every canonical pair (`user_id_a < user_id_b`), and upserts rows with similarity ≥ 0.05.
- Triggers: a `@Scheduled` sweep every ~3 min (configurable), plus `@TransactionalEventListener(AFTER_COMMIT)` on participation and interest changes. A new match publishes `MatchFound` which `NotificationService` turns into push outbox rows.

### Push pipeline (Phase 3)
Domain event → `NotificationService.enqueue` → `push_outbox` row (`PENDING`) → `@Scheduled` drain (5 s default) → `PushGateway.deliver`. Default gateway is `LoggingPushGateway`; real FCM/APNs is wired by providing a bean that implements `PushGateway`. At-least-once: failures stay `PENDING` up to 5 attempts, then move to `FAILED`.

### GDPR (Phase 4)
- `GET /api/users/me/export` streams a ZIP with profile.json, interests, matches, chat-threads + chat-messages, sns-links, manifest. The aggregator lives in `:app` so it can reach every module.
- `DELETE /api/users/me` sets `deleted_at`. `HardDeleteJob` (cron `0 0 3 * * *`, configurable) removes users whose `deleted_at` is older than 30 days; FK cascades wipe profile, interests, participations, matches, chat, devices, refresh tokens, sns_links.

### Observability (Phase 5)
- `RequestIdFilter` mints/echoes `X-Request-Id`, binds to MDC key `requestId`.
- `logback-spring.xml`: `CONSOLE_PLAIN` in dev, `CONSOLE_JSON` (logstash-encoder) under `prod`/`staging` profiles.
- Micrometer + OTel OTLP exporter. Pods carry `OTEL_EXPORTER_OTLP_ENDPOINT`.
- `infra/prometheus/alert-rules.yaml` has the 7 alerts from the spec; each has a runbook under `docs/runbooks/`.

## Key conventions

- Path alias `@/*` maps to `web/` root.
- Tailwind custom colors: `brand-*` and `brass-*` palettes; keyframes `laser-scan`, `pulse-halo`, `typing-bounce`.
- Fixture data: `web/lib/fixtures/` — types in `fixtures/types.ts`.
- `web/public/mockServiceWorker.js` is MSW-generated; do not edit.
- shadcn/ui components in `web/components/ui/` are owned source, not node_modules.
- Java modules do not have circular deps: `:matching` depends on `:event` + `:interest`; cross-module hooks go through `com.sns.common.events.*` publish/subscribe. Do not add reverse deps — use a new domain event instead.

## Gaps — closed in the code-completion round

Previously-deferred items that have now been authored (behind interfaces or config flags; see
[`docs/SECURITY.md`](docs/SECURITY.md) and [`backend/README.md`](backend/README.md) for rotation
and wiring notes):

- WS fan-out: `ChatRelay` interface with `RedisChatRelay` (default, multi-pod) and
  `InProcessChatRelay` (single-pod / test). Toggle via `sns.chat.relay`.
- Rate limiter: `RateLimiter` interface with `RedissonRateLimiter` (prod) and
  `InMemoryRateLimiter` (default). Toggle via `sns.rate-limit.backend`.
- Server-side location throttle: `EventService.ingestLocation` rejects fixes within 30 s AND < 10 m
  of the previous; emits `sns_location_throttled` counter.
- Redis-cached vicinity: `@Cacheable(cacheNames="vicinity")` with a 10-s TTL, evicted on
  `LocationUpdated` and `MatchRecomputeRequested`.
- `audit_log` writes: `AuditLogger` service wired through auth / profile / sns / export paths; IPs
  SHA-256-salted before persistence; payloads run through `PiiScrubber`.
- PII-masking log encoder: `PiiMaskingMessageJsonProvider` (JSON mode) + `%piiMsg` pattern
  converter (plain mode) redact emails, bearer tokens, JWTs, high-precision lat/lon.
- Idempotent chat send: Flyway V8 adds `client_message_id`; `ChatService.send` dedupes replays.
- Real FCM + APNs: `FcmPushGateway` (firebase-admin) + `ApnsPushGateway` (pushy) registered
  conditionally; `PushGatewayRouter` dispatches by `DeviceTokenEntity.Platform`. Logging fallback
  remains when no creds are configured.
- OpenNLP/RAKE: `KeywordExtractor` is now an interface. `OpenNlpKeywordExtractor` is `@Primary`
  when `sns.nlp.models-dir` is set; otherwise `TfKeywordExtractor` stays.
- HMAC-signed QR tokens: `QrCodeService.issue(code, expiresAt)` + `verify(token)`.
  `EventService.join` accepts both the signed form and the legacy hash form.
- SNS enrichment: `SnsEnrichmentJob` runs every 6h when `sns.enrichment.enabled=true`, pulls
  provider userinfo, writes to `sns_links.imported_data`.
- Helm: `redis-statefulset.yaml` (Sentinel, 3 replicas, PVCs) and `networkpolicy.yaml`
  (egress allow-list for DNS / Postgres / Redis / OTel / FCM / APNs / OAuth).
- Terraform: `infra/terraform/modules/{vpc, rds-postgis, elasticache-redis, s3, kms, route53,
  acm}` + `environments/{staging, prod}` composing them.
- CI: `contract` job runs `tools/openapi-diff.sh`; `security` job runs `pnpm audit --prod`,
  Gradle `dependencyCheckAggregate`, and OWASP ZAP baseline. `deploy-backend.yml`,
  `deploy-web.yml`, `deploy-mobile.yml` workflows land the build output.
- k6 scenarios: `infra/load/k6-vicinity.js` + `k6-chat.js`.
- Unit tests: `SimilarityEngineTest`, `TfKeywordExtractorTest`, `QrCodeServiceTest`,
  `AesGcmCipherTest`, `PiiScrubberTest`.
- Integration tests: `AuthIntegrationTest`, `EventAndMatchingIntegrationTest`,
  `ChatIntegrationTest`, `AuditLogIntegrationTest` all share a Postgres + Redis Testcontainers
  base. Multi-pod WS round-trip is wired by the base class (Redis container present), asserted by
  the existing chat test set.
- Mobile: `sns_auth_service.dart` wraps `flutter_facebook_auth` and a `url_launcher` custom tab
  for LinkedIn.

## Environment gaps (cannot be done from code alone)

- `flutter create` to materialise `mobile/ios/` + `mobile/android/`.
- `gradle wrapper` to commit the wrapper jar.
- `dart run build_runner build` to materialise `mobile/lib/storage/isar_db.g.dart`.
- Firebase project config (`google-services.json`, `GoogleService-Info.plist`) + APNs
  entitlements + `.p8` signing key uploaded as a repo secret.
- Facebook + LinkedIn OAuth app registration (client IDs, secrets, redirect URIs, app review).
- AWS account + Terraform state bucket + external-secrets operator + PagerDuty / Sentry wiring.
- Running k6 load scenarios, OWASP ZAP scan, and accessibility audit against a real staging
  deployment. Store submission (App Store + Play Store).
