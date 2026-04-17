# SNS Conference Tool

On-site conference networking app: discover nearby researchers with overlapping interests, chat in real time.

**Full specification**: [docs/SNS-system.md](docs/SNS-system.md)
**Implementation plan**: [.claude/plans/lively-dreaming-thimble.md](.claude/plans/lively-dreaming-thimble.md)
**Contributor guide**: [CLAUDE.md](CLAUDE.md)

---

## Status

All five phases of the plan have been authored in `main`:

| Phase | Scope | Runs locally? |
|---|---|---|
| 0 | CI, Playwright smoke, OpenAPI 3.1 seed | ✅ with pnpm + Docker |
| 1 | Spring Boot Auth + Profile, RS256 JWT + rotating refresh, Flyway V1–V2, per-domain MSW cutover | ✅ with Postgres + MailHog |
| 2 | Events + Interests + Matching, PostGIS `ST_DWithin`, TF-IDF similarity, async recompute; real mobile `geolocator` / `mobile_scanner` / `file_picker` | ✅ |
| 3 | Chat REST + STOMP over WebSocket (JWT CONNECT auth), push outbox with at-least-once drain, device-token registration, mobile `firebase_messaging` scaffold | ✅ WS single-pod; FCM needs Firebase config |
| 4 | SNS OAuth2 (Facebook + LinkedIn) with AES-256-GCM tokens, GDPR export + soft/hard-delete cron, CSP + HSTS filter, Isar local store | ✅ OAuth needs real app IDs |
| 5 | `X-Request-Id` + JSON logs, Micrometer + OTel, 7 Prometheus alerts, Grafana dashboard, Helm chart, runbooks | ✅ Helm needs a target cluster |

Known deferrals (WS Redis fan-out, real FCM/APNs SDK wiring, Redisson rate limiter, OpenNLP/RAKE pipeline, audit-log writes, Redis-cached vicinity) are listed in [CLAUDE.md](CLAUDE.md#gaps-known-intentional).

---

## Repository layout

```
.
├── web/          Next.js 14 (App Router). MSW contract + per-domain cutover toggle.
├── mobile/       Flutter 3.22 WebView shell. Bridge + native services (geolocator, mobile_scanner,
│                 file_picker, firebase_messaging, flutter_facebook_auth, Isar).
├── backend/      Spring Boot 3.3 / Java 21 multi-module:
│                   :app :common :identity :profile :event :interest :matching :chat :notification :sns
├── infra/        docker-compose.dev.yml, Helm chart, Prometheus alerts, Grafana dashboard.
└── docs/         SNS-system.md — full spec. runbooks/ — on-call procedures per alert.
```

---

## Quick start

### 1. Dev dependencies

```bash
cd infra && docker compose -f docker-compose.dev.yml up -d
```
Brings up Postgres + PostGIS `:5432`, Redis `:6379`, MinIO `:9000/:9001`, MailHog `:1025/:8025`.

### 2. Backend

First-time only: materialise the Gradle wrapper (needs Gradle 8.10+ and Java 21 installed):
```bash
cd backend && gradle wrapper --gradle-version 8.10
```
Then:
```bash
./gradlew :app:bootRun
```
Available at `http://localhost:8080`. Dev-mode TAN is `123456`; demo event `NEURIPS2026` is auto-seeded.

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
4. `POST /api/events/{id}/location` with lat/lon.
5. `GET /api/events/{id}/vicinity?radius=100` — returns peers within radius joined with pre-computed match rows.
6. Connect STOMP client to `ws://localhost:8080/ws` with `Authorization: Bearer <jwt>` on the CONNECT frame.
7. Send to `/app/chat.send` with `{eventId, toUserId, content}`; the other user's session receives it on `/user/queue/chat`.
8. `GET /api/users/me/export` to download the GDPR ZIP; `DELETE /api/users/me` to soft-delete (hard-deleted after 30 days).

---

## CI

`.github/workflows/ci.yml` runs three jobs:

- **web** — lint, typecheck, `playwright` smoke.
- **mobile** — `flutter analyze` + `flutter test`.
- **backend** — Gradle build + `integrationTest` (Testcontainers) when the wrapper jar is committed.

---

## Commands reference

See [CLAUDE.md](CLAUDE.md#commands) for the full matrix (web, backend, mobile, infra).

## On-call

Seven Prometheus alerts are defined in [`infra/prometheus/alert-rules.yaml`](infra/prometheus/alert-rules.yaml); per-alert runbooks live under [`docs/runbooks/`](docs/runbooks/).
