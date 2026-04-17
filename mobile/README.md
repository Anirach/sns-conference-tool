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

## Bridge stubs (pass 1)

| Bridge message | Stub behaviour |
|---|---|
| `gps.start/stop` | logs only |
| `qr.scan` | returns `{ eventCode: "NEURIPS2026" }` after a 400 ms dialog |
| `file.pickArticle` | returns a fake path under `assets/sample.pdf` |
| `storage.get/set/delete` | real `flutter_secure_storage` — persists across app restarts |
| `sns.login` | returns a fake OAuth token |
| `push.*` | returns granted + fake FCM token |

Pass 2 will replace these with real plugins (`geolocator`, `mobile_scanner`, `file_picker`, `firebase_messaging`, `flutter_facebook_auth`).

## Regenerating platform folders

This repo contains only `lib/`, `pubspec.yaml`, and `assets/`. Run:

```bash
flutter create --platforms=android,ios --project-name=sns_mobile .
```

from inside `mobile/` once to generate `android/` and `ios/` directories. The custom `lib/` files will not be overwritten.
