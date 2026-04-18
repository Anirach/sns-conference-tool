# backend/

Spring Boot 3.3 / Java 21 multi-module implementation per [docs/SNS-system.md §6](../docs/SNS-system.md).

## Status

All five phases plus the follow-up code-completion round are in `main`. Remaining work is environment-only — see [`/CLAUDE.md`](../CLAUDE.md#environment-gaps-cannot-be-done-from-code-alone).

| Phase | Highlights |
|---|---|
| 1 | Auth (register/verify/complete/login/refresh/logout), Profile, RS256 JWT + rotating refresh, JWKS, BCrypt(12), Flyway V1–V2, RFC 7807 handler, `AuditLogger` on every auth path |
| 2 | Events + Interests + Matching, PostGIS vicinity (`ST_DWithin` + 5-min freshness, Redis-cached 10s), TF or OpenNLP keyword extraction, async recompute, HMAC-signed QR tokens, server-side location throttle, Flyway V3–V5 |
| 3 | STOMP over WebSocket with JWT CONNECT interceptor, multi-pod `RedisChatRelay` (Pub/Sub) or `InProcessChatRelay`, idempotent chat send, push outbox routed by `PushGatewayRouter` to `FcmPushGateway` / `ApnsPushGateway` / `LoggingPushGateway`, Flyway V6 + V8 |
| 4 | SNS OAuth (Facebook, LinkedIn) with AES-256-GCM token crypto + scheduled `SnsEnrichmentJob`, GDPR export aggregator, soft/hard-delete cron, CSP + HSTS filter, Flyway V7 |
| 5 | `X-Request-Id` filter, JSON logs with `PiiScrubber` masking, Micrometer + OTel OTLP, 7 Prometheus alert rules, Grafana dashboard, Helm chart (Deployment/HPA/PDB/Ingress/CronJob/Redis StatefulSet/NetworkPolicy), Terraform modules, runbooks, k6 load scenarios |

## Module layout

```
backend/
├── app/            Spring Boot bootstrap, Flyway, Docker, global exception handler,
│                   GDPR export aggregator, HardDeleteJob, RequestIdFilter,
│                   SecurityHeadersFilter, RateLimitFilter (+ InMemory / Redisson impls),
│                   CacheConfig (Redis vicinity cache), DevSeedRunner,
│                   PiiMaskingMessageJsonProvider + %piiMsg converter.
├── common/         Shared DTOs (RFC 7807 Problem) + domain events
│                   (MatchRecomputeRequested, UserInterestsChanged, MatchFound,
│                    ChatMessageSent, LocationUpdated).
├── identity/       Auth endpoints, JWT (RS256), JWKS, Spring Security filter chain,
│                   RefreshTokenService, VerificationService + MailHog, AuditLogger,
│                   PiiScrubber, AuditLogEntity / repo.
├── profile/        Profile CRUD + soft-delete, registration-time ProfileWriter hook.
├── event/          Events + participations (PostGIS geography),
│                   QrCodeService (SHA-256 hash + HMAC-signed token issue/verify),
│                   VicinityService (@Cacheable, event-evicted), EventService with
│                   server-side location throttle.
├── interest/       KeywordExtractor interface + TfKeywordExtractor (default)
│                   + OpenNlpKeywordExtractor (@Primary when sns.nlp.models-dir is set),
│                   ArticleStorageService (S3/MinIO with in-memory fallback).
├── matching/       SimilarityEngine (cosine), MatchingService
│                   (scheduled sweep + event-triggered recompute), MatchFound publisher.
├── chat/           ChatService (idempotent send), REST controller, STOMP WebSocket
│                   config, StompJwtChannelInterceptor, ChatRelay iface +
│                   RedisChatRelay (Pub/Sub default) / InProcessChatRelay,
│                   ChatWsController.
├── notification/   DeviceToken + PushOutbox entities, NotificationService drain,
│                   PushGateway interface, PushGatewayRouter (@Primary),
│                   FcmPushGateway (firebase-admin) + ApnsPushGateway (pushy)
│                   + LoggingPushGateway fallback.
└── sns/            SnsController (link/callback/unlink), SnsService (OAuth exchange
                    with dev-stub fallback), SnsEnrichmentJob (@Scheduled),
                    AesGcmCipher (AES-256-GCM).

openapi.yaml at app/src/main/resources/openapi.yaml — single source of truth.
```

Inter-module dependencies: `:app` depends on everything; `:matching` on `:event` + `:interest`; `:profile` and `:chat` and `:notification` and `:sns` on `:identity` + `:common`. There are no reverse or circular deps — cross-module coupling goes through `com.sns.common.events.*` publish/subscribe.

## First-time setup

Nothing required — the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) is committed. Just need Java 21 on PATH (or run via Docker, which doesn't need a host JDK at all).

## Build & run

Two paths — pick one.

**Path A — All-in-Docker (no host JDK):**

```bash
cd infra
docker compose -f docker-compose.dev.yml --profile backend up -d --build
```

This brings up the data plane and the backend container. Add `--profile web` to also boot the Next.js dev server in a container (see top-level [README.md](../README.md#option-a--all-in-docker-5-minutes-zero-host-tooling-beyond-docker)).

**Path B — Host JDK (sub-second restart cycles via Spring DevTools):**

```bash
# Bring up the data plane in containers
cd infra && docker compose -f docker-compose.dev.yml up -d

# Run the app on the host JDK
cd ../backend
./gradlew :app:bootRun
```

Default endpoints:
- `http://localhost:8080/actuator/health` — liveness + readiness
- `http://localhost:8080/actuator/prometheus` — Micrometer scrape
- `http://localhost:8080/.well-known/jwks.json` — JWT verification key
- `http://localhost:8080/swagger-ui.html` — Springdoc UI
- `http://localhost:8080/ws` — STOMP endpoint (auth via `Authorization: Bearer <jwt>` on CONNECT frame)
- `http://localhost:8080/api/auth/**` — public
- Everything else — JWT-gated

Dev-mode overrides:
- `sns.verification.dev-mode=true` (default): TAN is always `123456`; real SMTP send still attempted.
- `sns.dev.seed-events=true` (default): three demo events (NeurIPS Bangkok, ACL Vienna, ICML Montreal expired) are seeded at boot.
- `sns.dev.seed-demo-data=true` (default off; on in `infra/docker-compose.dev.yml`): on the first fresh boot also seeds 20 fixture users, profiles with `/avatars/*` portraits, NeurIPS+ACL participations with PostGIS positions clustered around each venue, real interests with `KeywordExtractor`-derived vectors, recomputed similarity matches, 19 chat messages mirroring `web/lib/fixtures/chats.ts`, and promotes Alex Chen to `SUPER_ADMIN` (Lukas Svensson + Rajesh Iyer get `ADMIN`). Idempotent via the sentinel `you@example.com`. Login: `you@example.com` / `Demo!2026`. Refused under the `prod` profile by `ProductionSecretsCheck`.
- The same flag activates `DemoDataKeepalive` — a `@Scheduled(fixedDelay=60s)` job that refreshes any `participations.last_update` older than 4 minutes back to `now()` and clears the `vicinity` cache, so the seeded fellows stay inside `VicinityService`'s 5-minute freshness filter indefinitely (the Fellows screen never goes blank).
- `SNS_ADMIN_EMAIL` (`sns.admin.bootstrap-email`): when set, `AdminBootstrap` promotes the matching user to `SUPER_ADMIN` on every boot (idempotent). `ProductionSecretsCheck` refuses to start under `prod` when unset.

## End-to-end smoke (curl)

```bash
# 1. Register
curl -sS -X POST localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com"}'

# 2. Verify with the dev TAN (production: check MailHog at http://localhost:8025)
TOKEN=$(curl -sS -X POST localhost:8080/api/auth/verify \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","tan":"123456"}' | jq -r .verificationToken)

# 3. Complete registration
TOKENS=$(curl -sS -X POST localhost:8080/api/auth/complete \
  -H 'Content-Type: application/json' \
  -d "{\"verificationToken\":\"$TOKEN\",\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"password\":\"analytical-engine\"}")
ACCESS=$(echo "$TOKENS" | jq -r .accessToken)

# 4. Join the seeded event
curl -sS -X POST localhost:8080/api/events/join \
  -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d '{"eventCode":"NEURIPS2026"}'

# 5. Add an interest
curl -sS -X POST localhost:8080/api/interests \
  -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d '{"type":"TEXT","content":"We study graph neural networks with attention"}'

# 6. Post a location fix (lat/lon in Bangkok)
EID=$(curl -sS -H "Authorization: Bearer $ACCESS" localhost:8080/api/events/joined | jq -r '.[0].eventId')
curl -sS -X POST "localhost:8080/api/events/$EID/location" \
  -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
  -d '{"lat":13.7,"lon":100.55,"accuracyMeters":5}'

# 7. Read vicinity
curl -sS -H "Authorization: Bearer $ACCESS" "localhost:8080/api/events/$EID/vicinity?radius=100"

# 8. GDPR export
curl -sS -H "Authorization: Bearer $ACCESS" localhost:8080/api/users/me/export -o export.zip
```

## Tests

```bash
./gradlew :app:test               # unit + slice tests
./gradlew :app:integrationTest    # Testcontainers flows (Docker required)
```

Integration tests all extend [`IntegrationTestBase`](app/src/test/java/com/sns/app/support/IntegrationTestBase.java) (Postgres + Redis Testcontainers via `@ServiceConnection`):

- [`AuthIntegrationTest`](app/src/test/java/com/sns/app/AuthIntegrationTest.java) — full register → verify → complete → profile loop.
- [`EventAndMatchingIntegrationTest`](app/src/test/java/com/sns/app/EventAndMatchingIntegrationTest.java) — two users join, submit overlapping interests, post GPS fixes, vicinity returns the peer with similarity (awaits async recompute).
- [`ChatIntegrationTest`](app/src/test/java/com/sns/app/ChatIntegrationTest.java) — chat send → history round-trip over REST + WebSocket.
- [`AuditLogIntegrationTest`](app/src/test/java/com/sns/app/AuditLogIntegrationTest.java) — canonical auth lifecycle emits `audit_log` rows with expected action names.

Unit tests per module cover the pure-logic pieces: `SimilarityEngineTest`, `TfKeywordExtractorTest`, `QrCodeServiceTest`, `AesGcmCipherTest`, `PiiScrubberTest`.

## Running in Docker

```bash
cd infra
docker compose -f docker-compose.dev.yml --profile backend up -d --build
```
Builds from [`backend/app/Dockerfile`](app/Dockerfile) and wires to Postgres, Redis, MailHog, and MinIO via the compose network. Add `--profile web` to also bring up the Next.js dev server in a sibling container (see [`infra/docker-compose.dev.yml`](../infra/docker-compose.dev.yml)).

Container env (set in compose, override via `.env`):

| Var | Value | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/conf` | Compose-internal DNS name of Postgres |
| `SPRING_DATA_REDIS_HOST` | `redis` | Spring Data Redis target |
| `REDISSON_ADDRESS` | `redis://redis:6379` | Redisson rate limiter target |
| `SMTP_HOST` / `SMTP_PORT` | `mailhog` / `1025` | MailHog catches verification emails |
| `CHAT_RELAY` | `redis` | Multi-pod chat fan-out via `RedisChatRelay` |
| `RATE_LIMIT_BACKEND` | `memory` | Switch to `redis` (Redisson) for multi-pod prod |
| `VERIFICATION_DEV_MODE` | `true` | TAN is `123456` |

## Configuration reference

| Property / env | Default | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/conf` | |
| `SPRING_DATA_REDIS_URL` | `redis://localhost:6379` | Feeds `StringRedisTemplate`, `RedisMessageListenerContainer`, and the Redisson client |
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | unset (dev ephemeral) | PEM-encoded PKCS#8 + X.509 |
| `sns.jwt.access-token-ttl` | `PT15M` | |
| `sns.jwt.refresh-token-ttl` | `P30D` | |
| `sns.verification.dev-mode` | `true` | TAN is `123456`; set false in prod |
| `sns.dev.seed-events` | `true` | Seed the three demo events; set false in prod |
| `sns.dev.seed-demo-data` | `false` | Seed 20 fixture users + interests + matches + chats — login `you@example.com` / `Demo!2026`. Refused in `prod`. |
| `sns.admin.bootstrap-email` (`SNS_ADMIN_EMAIL`) | unset | Email to promote to `SUPER_ADMIN` at boot. Required in `prod`. |
| `sns.matching.sweep-interval-ms` | `180000` | Scheduled recompute cadence |
| `sns.push.drain-interval-ms` | `5000` | Outbox drain cadence |
| `sns.push.fcm.credentials-json` | unset | Firebase service-account JSON; when set, registers `FcmPushGateway` |
| `sns.push.fcm.project-id` | unset | Optional override |
| `sns.push.apns.team-id` / `.key-id` / `.bundle-id` / `.signing-key-pem` / `.sandbox` | unset | When team-id set, registers `ApnsPushGateway` |
| `sns.chat.relay` | `redis` | `redis` (Pub/Sub fan-out) or `inproc` (single-pod / tests) |
| `sns.chat.relay-buckets` | `64` | Number of bucketed channels `RedisChatRelay` subscribes to |
| `sns.rate-limit.backend` | `memory` | `memory` (in-process fixed-window) or `redis` (Redisson) |
| `sns.rate-limit.register-per-ip-per-hour` | `5` | Budget for `POST /api/auth/register` |
| `sns.rate-limit.login-per-ip-per-hour` | `30` | Budget for `POST /api/auth/login`, per IP |
| `sns.rate-limit.login-per-email-per-hour` | `10` | Budget for `POST /api/auth/login`, per email |
| `sns.rate-limit.refresh-per-ip-per-hour` | `60` | Budget for `POST /api/auth/refresh` |
| `sns.security.cors.allowed-origins` | unset | CSV. Empty = same-origin only. Same source for HTTP CORS + STOMP `/ws` |
| `sns.actuator.scrape-token` | unset | Bearer token Prometheus scrapes with. When unset, `/actuator/prometheus` falls back to JWT auth |
| `sns.location.throttle-seconds` | `30` | Min seconds between accepted GPS fixes |
| `sns.location.throttle-min-move-meters` | `10` | Min distance between accepted fixes |
| `sns.cache.vicinity-ttl-seconds` | `10` | Redis cache TTL for vicinity responses |
| `sns.nlp.models-dir` | unset | Path to OpenNLP `en-token.bin` + `en-pos-maxent.bin` + `en-lemmatizer.dict`; when set, `OpenNlpKeywordExtractor` becomes `@Primary` |
| `sns.enrichment.enabled` | `false` | Enable the 6-hourly SNS profile enrichment sweep |
| `sns.enrichment.cron` | `0 15 */6 * * *` | Cron for the enrichment sweep |
| `sns.enrichment.stale-hours` | `24` | Skip links refreshed within this window |
| `sns.audit.ip-salt` | `dev-audit-ip-salt-change-me` | Salt applied before SHA-256 hashing of request IPs |
| `sns.audit.retention-days` | `180` | Rows older than this are pruned by `AuditLogPruneJob` |
| `sns.audit.prune-cron` | `0 30 3 * * *` | When `AuditLogPruneJob` runs |
| `sns.gdpr.hard-delete-grace-days` | `30` | Soft→hard delete age |
| `sns.gdpr.hard-delete-cron` | `0 0 3 * * *` | Nightly sweep |
| `sns.qr.hmac-key` | `dev-hmac-key-change-me` | Rotate in prod; see [docs/SECURITY.md](../docs/SECURITY.md) |
| `sns.crypto.master-key` | `dev-sns-crypto-key-change-me` | AES-256-GCM key seed for SNS tokens; rotate via staged deploy |
| `sns.jwt.audience` | `sns-conf` | Required `aud` claim. Tokens with a different audience are rejected |
| `sns.oauth.{facebook,linkedin}.client-id` / `-secret` / `-redirect-uri` / `-scopes` | unset | When unset, SNS callback returns a dev-stub link |
| `spring.servlet.multipart.max-file-size` | `10MB` | Hard cap per upload |
| `spring.servlet.multipart.max-request-size` | `10MB` | Hard cap per multipart request |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | unset | OTLP target for traces |

> **`prod` profile boots only when secrets are real.** `ProductionSecretsCheck` halts startup
> if `sns.qr.hmac-key`, `sns.crypto.master-key`, `sns.audit.ip-salt`, or the JWT keypair are
> missing / dev-default. Setting all of these (plus `sns.security.cors.allowed-origins`) is
> the minimum prod-deploy checklist.

## Wiring the web frontend

In `web/.env.local`:
```
NEXT_PUBLIC_MOCK_API=1,-auth,-profile,-events,-interests,-matches,-chat,-devices,-sns
BACKEND_PROXY_TARGET=http://localhost:8080
```
Drop `-domain` entries from the CSV to roll individual domains back to MSW. Next's dev server rewrites `/api/*` and `/.well-known/*` to `BACKEND_PROXY_TARGET`.
