# SNS Conference Tool

On-site conference networking app: discover nearby researchers with overlapping interests, chat in real time.

**Full specification**: [docs/SNS-system.md](docs/SNS-system.md)

---

## Status

All five phases of [the implementation plan](.claude/plans/lively-dreaming-thimble.md) have been implemented:

- **Phase 0**: CI, Playwright smoke test, OpenAPI 3.1 spec derived from the MSW contract.
- **Phase 1**: Spring Boot backend — Auth (register/verify/complete/login/refresh/logout) + Profile, RS256 JWT with rotating refresh tokens, Flyway V1–V2.
- **Phase 2**: Events + Interests + Matching — PostGIS `ST_DWithin` vicinity queries, TF-IDF similarity, async match recompute. Real mobile `geolocator` / `mobile_scanner` / `file_picker`.
- **Phase 3**: Real-time chat (STOMP/WebSocket with JWT CONNECT auth), push-notification outbox with at-least-once delivery, device-token registration, `firebase_messaging` wired on mobile.
- **Phase 4**: SNS OAuth2 (Facebook + LinkedIn) with AES-256-GCM token encryption, GDPR export aggregator + soft/hard-delete cron, CSP + HSTS security headers, Isar local store on mobile.
- **Phase 5**: JSON logs with `X-Request-Id` correlation, Micrometer + OpenTelemetry, 7 Prometheus alert rules, Grafana dashboard, Helm chart with HPA + PDB + CronJob, on-call runbooks.

Web-side per-domain MSW toggle enables incremental cutover per phase.

---

## Repository layout

```
.
├── web/          Next.js 14 (App Router) — MSW contract + per-domain cutover toggle.
├── mobile/       Flutter 3.22 WebView shell — geolocator / mobile_scanner / firebase_messaging / Isar.
├── backend/      Spring Boot 3.3 / Java 21 multi-module (:app :common :identity :profile :event :interest :matching :chat :notification :sns).
├── infra/        docker-compose.dev.yml + Helm chart + Prometheus alerts + Grafana dashboard.
└── docs/         SNS-system.md — full spec; runbooks/ — on-call procedures.
```

---

## Quick start

### 1. Web frontend

```bash
cd web
pnpm install
pnpm dev
```

Opens at `http://localhost:3000`. MSW intercepts all `/api/*` calls and returns fixtures from `lib/fixtures/`.

### 2. Flutter mobile shell

Prerequisite: Flutter 3.22+, Xcode (iOS), Android SDK (Android).

```bash
cd mobile
flutter pub get
flutter run
```

On Android emulator, the WebView loads `http://10.0.2.2:3000`. On iOS simulator, `http://localhost:3000`. Start the web dev server first.

### 3. Local infrastructure (for pass 2)

```bash
cd infra
docker compose -f docker-compose.dev.yml up -d
```

Brings up:
- **Postgres + PostGIS** — `localhost:5432` (`conf`/`conf`/`conf`)
- **Redis** — `localhost:6379`
- **MinIO** — `localhost:9000` (API), `localhost:9001` (console, `minio`/`miniosecret`)
- **MailHog** — `localhost:1025` (SMTP), `localhost:8025` (web UI)

---

## Demo walk-through

1. `http://localhost:3000` → welcome screen → Register.
2. Email `you@example.com` → any 6-digit TAN (e.g. `123456`) → complete profile.
3. Interests → add text, upload PDF (mock), or paste an arXiv URL.
4. Events → Join → "Scan QR" (Flutter returns demo code; browser shows manual entry: `NEURIPS2026`).
5. Vicinity → radius slider 20/50/100m → tap a `MatchCard`.
6. Chat → type a message (appears instantly) → wait 8 s (mock incoming arrives).
7. Profile → SNS → "Link Facebook" → mock success.
8. Settings → toggle push preferences → persisted via bridge mock.
