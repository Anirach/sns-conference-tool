# SNS Conference Tool

On-site conference networking app: discover nearby researchers with overlapping interests, chat in real time.

**Full specification**: [docs/SNS-system.md](docs/SNS-system.md)

---

## Status

**Pass 1 — UI Mockups (current)**
Fully navigable Next.js web frontend + Flutter WebView shell, driven by mock data. No backend required.

**Pass 2 — Backend wiring (next)**
Spring Boot 3.3 / Java 21 implementation per spec §6. Replaces MSW mocks with real REST + WebSocket.

---

## Repository layout

```
.
├── web/          Next.js 14 (App Router). Primary pass-1 deliverable.
├── mobile/       Flutter 3.22 WebView shell with bridge stubs.
├── backend/      Reserved for pass 2.
├── infra/        docker-compose.dev.yml (Postgres+PostGIS, Redis, MinIO, MailHog).
└── docs/         SNS-system.md — full spec.
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
