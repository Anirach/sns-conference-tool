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

The Gradle wrapper jar is intentionally not committed yet. Bootstrap once with a system Gradle:

```bash
# Requires Gradle 8.10+ and Java 21 on PATH
gradle wrapper --gradle-version 8.10
```

This writes `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`. Commit them on first use.

## Build & run

```bash
# From repo root: start dependencies (Postgres+PostGIS, Redis, MinIO, MailHog)
cd infra && docker compose -f docker-compose.dev.yml up -d

# Run the app
cd backend
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
- `sns.dev.seed-events=true` (default): demo event `NEURIPS2026` is seeded at boot.

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
docker compose -f docker-compose.dev.yml --profile backend up --build
```
Builds from `backend/app/Dockerfile` and wires to Postgres + MailHog + Redis + MinIO.

## Configuration reference

| Property / env | Default | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/conf` | |
| `SPRING_DATA_REDIS_URL` | `redis://localhost:6379` | Feeds `StringRedisTemplate`, `RedisMessageListenerContainer`, and the Redisson client |
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | unset (dev ephemeral) | PEM-encoded PKCS#8 + X.509 |
| `sns.jwt.access-token-ttl` | `PT15M` | |
| `sns.jwt.refresh-token-ttl` | `P30D` | |
| `sns.verification.dev-mode` | `true` | TAN is `123456`; set false in prod |
| `sns.dev.seed-events` | `true` | Seed demo event; set false in prod |
| `sns.matching.sweep-interval-ms` | `180000` | Scheduled recompute cadence |
| `sns.push.drain-interval-ms` | `5000` | Outbox drain cadence |
| `sns.push.fcm.credentials-json` | unset | Firebase service-account JSON; when set, registers `FcmPushGateway` |
| `sns.push.fcm.project-id` | unset | Optional override |
| `sns.push.apns.team-id` / `.key-id` / `.bundle-id` / `.signing-key-pem` / `.sandbox` | unset | When team-id set, registers `ApnsPushGateway` |
| `sns.chat.relay` | `redis` | `redis` (Pub/Sub fan-out) or `inproc` (single-pod / tests) |
| `sns.rate-limit.backend` | `memory` | `memory` (in-process fixed-window) or `redis` (Redisson) |
| `sns.rate-limit.register-per-ip-per-hour` | `5` | Budget for `POST /api/auth/register` |
| `sns.location.throttle-seconds` | `30` | Min seconds between accepted GPS fixes |
| `sns.location.throttle-min-move-meters` | `10` | Min distance between accepted fixes |
| `sns.cache.vicinity-ttl-seconds` | `10` | Redis cache TTL for vicinity responses |
| `sns.nlp.models-dir` | unset | Path to OpenNLP `en-token.bin` + `en-pos-maxent.bin` + `en-lemmatizer.dict`; when set, `OpenNlpKeywordExtractor` becomes `@Primary` |
| `sns.enrichment.enabled` | `false` | Enable the 6-hourly SNS profile enrichment sweep |
| `sns.enrichment.cron` | `0 15 */6 * * *` | Cron for the enrichment sweep |
| `sns.enrichment.stale-hours` | `24` | Skip links refreshed within this window |
| `sns.audit.ip-salt` | `dev-audit-ip-salt-change-me` | Salt applied before SHA-256 hashing of request IPs |
| `sns.gdpr.hard-delete-grace-days` | `30` | Soft→hard delete age |
| `sns.gdpr.hard-delete-cron` | `0 0 3 * * *` | Nightly sweep |
| `sns.qr.hmac-key` | `dev-hmac-key-change-me` | Rotate in prod; see [docs/SECURITY.md](../docs/SECURITY.md) |
| `sns.crypto.master-key` | `dev-sns-crypto-key-change-me` | AES-256-GCM key seed for SNS tokens; rotate via staged deploy |
| `sns.oauth.{facebook,linkedin}.client-id` / `-secret` / `-redirect-uri` / `-scopes` | unset | When unset, SNS callback returns a dev-stub link |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | unset | OTLP target for traces |

## Wiring the web frontend

In `web/.env.local`:
```
NEXT_PUBLIC_MOCK_API=1,-auth,-profile,-events,-interests,-matches,-chat,-devices,-sns
BACKEND_PROXY_TARGET=http://localhost:8080
```
Drop `-domain` entries from the CSV to roll individual domains back to MSW. Next's dev server rewrites `/api/*` and `/.well-known/*` to `BACKEND_PROXY_TARGET`.
