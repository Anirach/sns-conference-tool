# SNS Mobile — Flutter shell

Thin native shell around the Next.js web frontend per [docs/SNS-system.md §4](../docs/SNS-system.md).

**Pass 1 scope**: bootable WebView + JS bridge dispatcher + stub native services. The UI lives entirely in the web layer.

## Setup

```bash
flutter pub get
```

### iOS-specific

```bash
cd ios
pod install
cd ..
```

## Run

Start the web dev server first (`cd ../web && pnpm dev`), then:

```bash
flutter run
```

- **Android emulator** loads `http://10.0.2.2:3000` (the host machine's localhost seen from the emulator).
- **iOS simulator** loads `http://localhost:3000`.

Override at build time with: `flutter run --dart-define=FRONTEND_ORIGIN=https://staging.example.com`.

## Bridge services

| Bridge message | Status | Backing plugin |
|---|---|---|
| `gps.start/stop` | Phase 2 — real | `geolocator` — streams fixes via `gps.fix` events |
| `qr.scan` | Phase 2 — real | `mobile_scanner` — full-screen modal scanner |
| `file.pickArticle` | Phase 2 — real | `file_picker` — PDF/TXT/MD |
| `storage.get/set/delete` | Phase 1 — real | `flutter_secure_storage` |
| `sns.login` | Phase 4 — real (needs app IDs) | `flutter_facebook_auth`; LinkedIn via `url_launcher` custom-tab |
| `push.*` | Phase 3 — real (needs Firebase config) | `firebase_messaging` — `Firebase.initializeApp` returns `configured: false` until `google-services.json` / `GoogleService-Info.plist` is provisioned |
| `localdb.matches.*` | Phase 4 — real (needs codegen) | `isar` — run `dart run build_runner build` once to generate `isar_db.g.dart` |

Location fixes arrive on the web side as `window.addEventListener("flutter-bridge-event", ...)` with `detail.type === "gps.fix"`.

## Regenerating platform folders

This repo contains only `lib/`, `pubspec.yaml`, and `assets/`. Run:

```bash
flutter create --platforms=android,ios --project-name=sns_mobile .
```

from inside `mobile/` once to generate `android/` and `ios/` directories. The custom `lib/` files will not be overwritten.
