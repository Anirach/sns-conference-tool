# SNS Mobile — Flutter shell

Thin native shell around the Next.js web frontend per [docs/SNS-system.md §4](../docs/SNS-system.md).

The UI lives entirely in the web layer; this module exists to give the web frontend access to device capabilities that browsers can't reach (foreground GPS, camera-based QR scanning, native file picker, secure storage, push notifications, OS-provided OAuth flows, on-device encrypted DB) through a single JSON message bridge.

## First-time setup

This repo keeps only `lib/`, `pubspec.yaml`, and `assets/` in git. Platform projects and code-generated files must be produced locally on first checkout:

```bash
# 1. Generate ios/ + android/ projects (custom lib/ is preserved)
flutter create --platforms=android,ios --project-name=sns_mobile .

# 2. Code-gen for Isar schemas (produces lib/storage/isar_db.g.dart)
dart run build_runner build --delete-conflicting-outputs

# 3. Install dependencies
flutter pub get

# 4. iOS only — CocoaPods
cd ios && pod install && cd ..
```

Provisioning (not checked in):
- **Firebase** — drop `google-services.json` into `android/app/` and `GoogleService-Info.plist` into `ios/Runner/`. Without these, `PushService.initialize()` returns `configured: false` and bridge push calls become no-ops (by design).
- **APNs entitlement** — add `aps-environment` to `ios/Runner/Runner.entitlements`.
- **OAuth client IDs** — set Facebook App ID in `android/app/src/main/AndroidManifest.xml` meta-data and iOS `Info.plist`. LinkedIn uses the custom-tab path via `url_launcher` against a redirect URI whose handler you configure on the backend (`sns.oauth.linkedin.redirect-uri`).

## Run

Start the web dev server first (`cd ../web && pnpm dev`), then:

```bash
flutter run
```

- **Android emulator** loads `http://10.0.2.2:3000` (host's localhost as seen from the emulator).
- **iOS simulator** loads `http://localhost:3000`.

Override at build time: `flutter run --dart-define=FRONTEND_ORIGIN=https://staging.example.com`.

## Bridge services

All Web ↔ Native calls go through `lib/bridge/js_bridge.dart`. Message schema is `{ id, type, payload }`.

| Bridge message | Status | Implementation |
|---|---|---|
| `gps.start` / `gps.stop` | real | `geolocator` streams fixes; each fix is pushed back to web as `gps.fix` events (no request/response) |
| `qr.scan` | real | `mobile_scanner` in a full-screen modal via `rootNavigatorKeyProvider` |
| `file.pickArticle` | real | `file_picker` filtered to PDF/TXT/MD, returns `{path, name, sizeBytes, previewBase64}` |
| `storage.get` / `set` / `delete` | real | `flutter_secure_storage` — JWT access + refresh, push preferences |
| `sns.login` | real (needs app IDs) | Facebook via `flutter_facebook_auth` native SDK; LinkedIn via `url_launcher` custom tab — host app forwards the deep-link callback back into the bridge reply |
| `push.requestPermission` / `push.token` | real (needs Firebase config) | `firebase_messaging` — graceful no-op when config is missing |
| `localdb.matches.list` / `save` | real (needs codegen) | Isar schemas in `lib/storage/isar_db.dart`; generated `isar_db.g.dart` must be produced by `build_runner` |
| `app.info` | real | `Platform` + `AppConfig.appVersion` |

### Web → Native

```ts
import { bridge } from "@/lib/bridge/client";
await bridge.call<QrScanResult>("qr.scan");
```

### Native → Web

Server-initiated events are dispatched as `CustomEvent`s on `window`:

```ts
window.addEventListener("flutter-bridge-event", (e) => {
  const { type, payload } = (e as CustomEvent).detail;
  if (type === "gps.fix") sendLocationToApi(payload);
  if (type === "push.received") handleInboundNotification(payload);
});
```

## Directory layout

```
lib/
  app.dart, main.dart          app bootstrap + root navigator
  core/
    config/app_config.dart     env + version
    di/providers.dart          Riverpod providers for every service
    logging/                   talker wiring
  bridge/
    js_bridge.dart             JSON dispatcher; webview_flutter channel handler
    bridge_messages.dart       message type constants
  features/
    splash/                    cold-start screen
    webview/                   WebView host + controller
  native/
    location_service.dart      geolocator
    qr_scanner_service.dart    mobile_scanner + full-screen modal
    file_picker_service.dart   file_picker
    secure_storage_service.dart  flutter_secure_storage
    push_service.dart          firebase_messaging with graceful no-config fallback
    sns_auth_service.dart      flutter_facebook_auth + LinkedIn custom-tab via url_launcher
  storage/
    isar_db.dart               Isar schemas + accessors; requires build_runner codegen
assets/                        bundled demo article for stub file picker
```

## Tests

```bash
flutter analyze
flutter test
```

Widget-level coverage lives under `test/` — [`test/bridge/js_bridge_test.dart`](test/bridge/js_bridge_test.dart) exercises the dispatch table (unknown type, storage.set) with hand-rolled test doubles so no plugin platform channels are touched. Integration tests per real native service (geolocator, mobile_scanner, firebase_messaging) are on the backlog and require a device runner.
