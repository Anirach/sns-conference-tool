# SNS Conference Tool

On-site conference networking app: discover nearby researchers with overlapping interests, chat in real time.

**Full specification**: [docs/SNS-system.md](docs/SNS-system.md)
**Implementation plan**: [.claude/plans/lively-dreaming-thimble.md](.claude/plans/lively-dreaming-thimble.md)
**Contributor guide**: [CLAUDE.md](CLAUDE.md)
**Security & rotation**: [docs/SECURITY.md](docs/SECURITY.md)

---

## Status

All five phases of the original roadmap plus the follow-up code-completion round are in `main`. Every gap the Phase 5 review flagged has been closed in code behind the interfaces it already shipped.

| Area | What shipped |
|---|---|
| **Auth** | RS256 JWT with `iss` + `aud` validation, rotating refresh with reuse-detection family revoke, BCrypt(12) + phantom-hash for unknown emails (timing-safe), constant-time TAN compare, `PasswordPolicy` blocklist + email-equal check, JWKS endpoint |
| **Events + Matching** | PostGIS `ST_DWithin` vicinity (Redis-cached 10 s TTL, event-evicted), TF or OpenNLP keyword extraction, incremental + scheduled async recompute, HMAC-signed QR tokens alongside the legacy hash form |
| **Chat** | STOMP over WebSocket with JWT CONNECT auth, multi-pod fan-out via `RedisChatRelay` (Pub/Sub bucketed channels), idempotent send with client-supplied message IDs, `@Valid` body validation on REST + WS handlers |
| **Push** | DB-backed outbox with `SELECT … FOR UPDATE SKIP LOCKED` claim, `PushGatewayRouter` dispatches by platform to `FcmPushGateway` (firebase-admin) / `ApnsPushGateway` (pushy); logging fallback when no creds |
| **SNS OAuth** | Facebook + LinkedIn link/callback/unlink with AES-256-GCM encrypted tokens, scheduled enrichment job, mobile flows via `flutter_facebook_auth` + LinkedIn custom tab |
| **GDPR** | `/api/users/me/export` aggregates profile/interests/matches/chats/SNS, soft-delete + 30-day hard-delete cron, `audit_log` writes on every actionable path with DB-trigger immutability + 180-day retention prune |
| **Rate limiting** | Buckets for register / login (per-IP + per-email) / refresh; Redisson (`RedissonRateLimiter`) or in-memory backend toggle |
| **Transport security** | HSTS, CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy, `Cache-Control: no-store` on `/api/auth/**`; CORS allowlist driven by `sns.security.cors.allowed-origins` (HTTP + STOMP read the same property; default same-origin only) |
| **Upload safety** | MIME allowlist + magic-byte sniff on `/api/interests` multipart, hard 10 MB cap from `spring.servlet.multipart.max-file-size` |
| **Actuator** | `/actuator/prometheus` gated by `sns.actuator.scrape-token` (constant-time bearer compare) or JWT |
| **Boot-time gate** | `ProductionSecretsCheck` halts startup under `prod` profile if any of `sns.qr.hmac-key`, `sns.crypto.master-key`, `sns.audit.ip-salt`, or the JWT keypair is missing / dev-default |
| **Observability** | JSON logs with `X-Request-Id` correlation + `PiiScrubber` masking, Micrometer + OTel OTLP, 7 Prometheus alerts, Grafana dashboard |
| **Deployment** | Helm chart with HPA, PDB, Ingress, CronJob, optional Redis Sentinel StatefulSet, NetworkPolicy; Terraform modules (VPC, PostGIS RDS, ElastiCache Redis, S3, KMS, Route53, ACM) with staging + prod compositions |
| **Tests** | Unit + integration + contract diff gate + Flutter widget + k6 load scenarios |
| **CI** | `contract` (OpenAPI ⊇ MSW), `security` (pnpm audit, Gradle dep-check, ZAP baseline), `web`, `mobile`, `backend`, plus `deploy-{backend,web,mobile}` workflows |

Environment-only tasks remaining: `flutter create`, `gradle wrapper`, `dart run build_runner build`, Firebase/APNs credentials, OAuth app registration, real cluster provisioning, store submissions. See [CLAUDE.md](CLAUDE.md#environment-gaps-cannot-be-done-from-code-alone).

---

## Repository layout

```
.
├── web/           Next.js 14 (App Router). MSW contract + per-domain cutover toggle.
├── mobile/        Flutter 3.22 WebView shell. Bridge + native services (geolocator, mobile_scanner,
│                  file_picker, firebase_messaging, flutter_facebook_auth, Isar).
├── backend/       Spring Boot 3.3 / Java 21 multi-module:
│                    :app :common :identity :profile :event :interest :matching :chat :notification :sns
├── infra/
│    ├── docker-compose.dev.yml   Postgres+PostGIS, Redis, MinIO, MailHog (+ optional backend).
│    ├── helm/sns/                Deployment, HPA, PDB, Ingress, CronJob, Redis StatefulSet, NetworkPolicy.
│    ├── terraform/               modules/ + environments/{staging,prod}.
│    ├── prometheus/              alert-rules.yaml — 7 alerts from the spec.
│    ├── grafana/                 sns-overview.json dashboard.
│    └── load/                    k6 scenarios — vicinity 500 RPS, chat 1000 WS.
├── docs/
│    ├── SNS-system.md            Full spec.
│    ├── SECURITY.md              Key rotation and secrets guide.
│    └── runbooks/                7 alert runbooks + deploy-rollback + db-restore.
└── tools/
     └── openapi-diff.sh          CI-enforced MSW ⊆ OpenAPI contract gate.
```

---

## Quick start

### 1. Dev dependencies

```bash
cd infra && docker compose -f docker-compose.dev.yml up -d
```
Brings up Postgres + PostGIS `:5432`, Redis `:6379`, MinIO `:9000/:9001`, MailHog `:1025/:8025`.

### 2. Backend

First-time only (materialise the Gradle wrapper with system Gradle 8.10+ and Java 21):
```bash
cd backend && gradle wrapper --gradle-version 8.10
```
Then:
```bash
./gradlew :app:bootRun
```
Backend at `http://localhost:8080`. Dev-mode TAN is `123456`; demo event `NEURIPS2026` is auto-seeded. Default config keeps rate-limit in memory and chat fan-out in-process — flip to Redis in prod via `RATE_LIMIT_BACKEND=redis` + `CHAT_RELAY=redis`.

Or build + run the container:
```bash
cd infra && docker compose -f docker-compose.dev.yml --profile backend up --build
```

### 3. Web frontend

```bash
cd web
pnpm install
pnpm dev
```
Opens at `http://localhost:3000`. By default MSW intercepts every `/api/*` call with fixtures from `lib/fixtures/`.

To hit the real backend for a subset of domains, in `web/.env.local`:
```
NEXT_PUBLIC_MOCK_API=1,-auth,-profile,-events,-interests,-matches,-chat,-devices,-sns
BACKEND_PROXY_TARGET=http://localhost:8080
```
Next's dev server rewrites `/api/*` and `/.well-known/*` to the backend.

### 4. Mobile

Prerequisites: Flutter 3.22+, Xcode (iOS), Android SDK (Android).

First-time only:
```bash
cd mobile
flutter create --platforms=android,ios --project-name=sns_mobile .
dart run build_runner build --delete-conflicting-outputs
```
Then:
```bash
flutter pub get
flutter run
```
Android emulator loads `http://10.0.2.2:3000`; iOS simulator loads `http://localhost:3000`. Override with `--dart-define=FRONTEND_ORIGIN=...`.

---

## Demo flow (against real backend)

1. `curl` the [auth flow end-to-end](backend/README.md#end-to-end-smoke-curl) to obtain an access token.
2. `POST /api/events/join` with `{"eventCode":"NEURIPS2026"}` — seeded at boot in dev.
3. `POST /api/interests` with a TEXT payload — keywords are extracted inline.
4. `POST /api/events/{id}/location` with lat/lon — rejected silently if < 30 s / < 10 m since the last fix.
5. `GET /api/events/{id}/vicinity?radius=100` — returns peers within radius joined with pre-computed match rows (Redis-cached 10 s).
6. Connect STOMP client to `ws://localhost:8080/ws` with `Authorization: Bearer <jwt>` on the CONNECT frame.
7. Send to `/app/chat.send` with `{eventId, toUserId, content, clientMessageId}`; the other user's session receives it on `/user/queue/chat`. Replaying the same `clientMessageId` returns the original row without duplicating.
8. `GET /api/users/me/export` to download the GDPR ZIP; `DELETE /api/users/me` to soft-delete (hard-deleted after 30 days).

---

## Load testing

```bash
BASE_URL=http://localhost:8080 JWT=... EVENT_ID=... k6 run infra/load/k6-vicinity.js
BASE_WS=ws://localhost:8080/ws JWT=... EVENT_ID=... PEER_USER_ID=... k6 run infra/load/k6-chat.js
```
Targets: 500 RPS on vicinity (p95 ≤ 300 ms), 1000 WS at 10 msg/s on chat (p95 round-trip ≤ 200 ms). See [`infra/load/README.md`](infra/load/README.md).

---

## CI

`.github/workflows/ci.yml` gates merges with:

- **contract** — `tools/openapi-diff.sh` asserts every MSW route has a matching OpenAPI path.
- **security** — `pnpm audit --prod`, Gradle `dependencyCheckAggregate`, OWASP ZAP baseline.
- **web** — lint, typecheck, Playwright smoke.
- **mobile** — `flutter analyze` + `flutter test`.
- **backend** — Gradle build + `integrationTest` (Testcontainers) once the wrapper jar is committed.

Deploy pipelines (`deploy-backend.yml`, `deploy-web.yml`, `deploy-mobile.yml`) build + push on `main` and gate production promotion on manual `workflow_dispatch` with an environment review.

---

## Commands reference

See [CLAUDE.md](CLAUDE.md#commands) for the full matrix (web, backend, mobile, infra).

## On-call

Seven Prometheus alerts are defined in [`infra/prometheus/alert-rules.yaml`](infra/prometheus/alert-rules.yaml); per-alert runbooks live under [`docs/runbooks/`](docs/runbooks/), alongside operational procedures for [deploy-rollback](docs/runbooks/deploy-rollback.md) and [db-restore](docs/runbooks/db-restore.md).
