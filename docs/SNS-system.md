# SNS Conference Tool — System Implementation & Detailed Design

**Version**: 1.0
**Date**: 2026-04-17
**Status**: Implementation Specification
**Related document**: Requirements Specification v1.3

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Technology Stack & Rationale](#3-technology-stack--rationale)
4. [Mobile Client App — Detailed Design (Flutter)](#4-mobile-client-app--detailed-design-flutter)
5. [Web Frontend — Detailed Design (React / Next.js)](#5-web-frontend--detailed-design-react--nextjs)
6. [Backend Server — Detailed Design (Java / Spring Boot)](#6-backend-server--detailed-design-java--spring-boot)
7. [Database Design (PostgreSQL + PostGIS)](#7-database-design-postgresql--postgis)
8. [Cache & Job Queue Design (Redis)](#8-cache--job-queue-design-redis)
9. [Real-time Communication (WebSocket)](#9-real-time-communication-websocket)
10. [Similarity Engine — Detailed Algorithm](#10-similarity-engine--detailed-algorithm)
11. [Authentication, Authorization & Security](#11-authentication-authorization--security)
12. [SNS Integration (Optional Module)](#12-sns-integration-optional-module)
13. [Push Notifications](#13-push-notifications)
14. [API Contracts](#14-api-contracts)
15. [Deployment, Infrastructure & DevOps](#15-deployment-infrastructure--devops)
16. [Observability](#16-observability)
17. [Testing Strategy](#17-testing-strategy)
18. [Non-Functional Engineering](#18-non-functional-engineering)
19. [Project Structure](#19-project-structure)
20. [Implementation Roadmap](#20-implementation-roadmap)

---

## 1. Executive Summary

This document describes the detailed system implementation of the Conference Tool (SNS Tool). The system enables on-site participants at scientific conferences to discover each other based on research-interest similarity and spatial proximity, and to chat in real time. It consists of:

- A cross-platform **Flutter** mobile app (Android 11+ / iOS 14+) that embeds a WebView and provides native capabilities (GPS, QR, file, push, optional SNS OAuth).
- A **React / Next.js** web frontend rendered inside the WebView, providing the main UI and business flows.
- A **Java 21 / Spring Boot 3.x** backend exposing REST and WebSocket APIs, handling authentication, similarity computation, event lifecycle, and chat.
- **PostgreSQL 15+** with **PostGIS** for persistent data and geospatial queries.
- **Redis 7+** for session cache, hot GPS buffers, Pub/Sub fan-out across backend instances, rate limiting, and asynchronous job queue.
- Optional integration with Facebook and LinkedIn via OAuth 2.0.

The system is designed to scale horizontally (stateless backend, Redis-coordinated WebSocket fan-out), to respect GDPR (automatic deletion of expired events, encrypted tokens, opt-in SNS), and to achieve sub-second chat latency and sub-5-second match-list updates.

---

## 2. Architecture Overview

### 2.1 High-Level Component Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Mobile Device (iOS/Android)                  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                      Flutter App (Dart)                        │  │
│  │  ┌──────────────────────────────────────────────────────────┐  │  │
│  │  │            webview_flutter (WebView)                     │  │  │
│  │  │  ┌────────────────────────────────────────────────────┐  │  │  │
│  │  │  │       Next.js / React UI (TypeScript)              │  │  │  │
│  │  │  │   • Login / Profile / Interests                    │  │  │  │
│  │  │  │   • Event join / Vicinity list / Chat              │  │  │  │
│  │  │  └────────────────────────────────────────────────────┘  │  │  │
│  │  └───────────────▲──────────────────────┬───────────────────┘  │  │
│  │          JS Bridge (JavascriptChannel)  │                      │  │
│  │  ┌───────────────┴──────────────────────▼───────────────────┐  │  │
│  │  │ Native Modules: GPS | QR Scanner | File Picker           │  │  │
│  │  │                 Push | Secure Storage | Local DB (Isar)  │  │  │
│  │  │                 Facebook SDK | LinkedIn OAuth WebView    │  │  │
│  │  └──────────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ HTTPS (REST) + WSS (STOMP)
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   Load Balancer (TLS 1.3 termination)                │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          ▼                     ▼                     ▼
  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
  │ Next.js SSR  │      │ Spring Boot  │      │ Spring Boot  │
  │  (Node.js)   │      │ Instance #1  │      │ Instance #N  │
  └──────┬───────┘      └──────┬───────┘      └──────┬───────┘
         │                     │                     │
         └─────────────┬───────┴─────────────────────┘
                       │
        ┌──────────────┼──────────────────┐
        ▼              ▼                  ▼
  ┌──────────┐  ┌────────────┐    ┌──────────────┐
  │PostgreSQL│  │   Redis    │    │ Object Store │
  │ + PostGIS│  │ Cache/Jobs │    │ (S3/MinIO)   │
  └──────────┘  │ Pub/Sub    │    │ profile pics │
                └────────────┘    │ articles     │
                                  └──────────────┘
                       │
        ┌──────────────┼─────────────────────────────┐
        ▼              ▼                             ▼
   ┌────────┐    ┌──────────┐              ┌────────────────┐
   │ SMTP   │    │ FCM/APNs │              │ SNS APIs       │
   │ Mailer │    │   Push   │              │ FB / LinkedIn  │
   └────────┘    └──────────┘              └────────────────┘
```

### 2.2 Logical Layers

| Layer | Responsibility | Technology |
|-------|----------------|------------|
| Presentation (Native) | Device-specific capabilities, WebView host | Flutter / Dart |
| Presentation (Web) | UI, forms, lists, chat, state management | Next.js / React |
| API Gateway | Routing, TLS, rate limiting | Nginx / Traefik / cloud LB |
| Application | Business logic, auth, similarity, chat orchestration | Spring Boot |
| Integration | SNS, SMTP, push, arXiv | Spring Security OAuth2, HTTP clients |
| Async Processing | Background jobs, scheduled tasks | Redisson / Quartz |
| Data | Persistent storage, geospatial queries | PostgreSQL + PostGIS |
| Cache / Bus | Session cache, Pub/Sub, queues | Redis |
| Object Storage | Profile pictures, uploaded PDFs | S3 / MinIO |

### 2.3 Request Lifecycle Example — "Join Event"

1. User scans QR → `mobile_scanner` (Flutter) decodes it → Dart posts a JS bridge message to the WebView.
2. Next.js (in WebView) receives `{eventId, expirationCode}` and calls `POST /api/events/join` with the JWT.
3. Nginx terminates TLS, forwards to one of the Spring Boot instances.
4. Spring Security validates the JWT, `EventController` receives the call.
5. `EventService` checks expiration via PostgreSQL; if valid, writes `Participation` row, adds `userId` to `event:{id}:participants` Set in Redis.
6. Response returned. The Next.js UI transitions to the vicinity-list screen and subscribes to `/user/queue/matches` via STOMP.

---

## 3. Technology Stack & Rationale

| Concern | Choice | Rationale |
|---|---|---|
| Mobile framework | Flutter 3.22+ (Dart 3) | Single codebase for iOS & Android; mature WebView plugin; strong ecosystem for GPS/QR/local DB. |
| Web framework | Next.js 14+ (App Router, React 18) | SSR for fast first paint in WebView; TypeScript; Server Components reduce client JS; mature SWR/React Query ecosystem. |
| Backend | Spring Boot 3.3 on Java 21 | Enterprise-grade, virtual threads (Project Loom) for WebSockets, first-class Spring Data JPA, Spring Security, Spring WebSocket. |
| DB | PostgreSQL 15 + PostGIS 3.4 | ACID, mature JSONB, PostGIS for `ST_DWithin` radius queries, full-text search for keyword fallback. |
| Cache / bus | Redis 7 (single-node → Sentinel → Cluster) | Low-latency cache, Pub/Sub for WebSocket fan-out, stream-based job queue (Redis Streams) or Redisson. |
| Object storage | S3-compatible (AWS S3 or MinIO on-prem) | Offloads binary assets from PG; presigned URLs. |
| Push | FCM (Android) + APNs (iOS) | Standard mobile push. |
| NLP / keywords | Apache OpenNLP + RAKE / TF-IDF | Pure-Java, no external GPU; good enough for keyword extraction. |
| Build | Gradle (backend), pnpm (frontend), Flutter CLI | Standard per ecosystem. |
| CI/CD | GitHub Actions + Docker | Build → test → container image → deploy via Helm. |
| Infra | Kubernetes (EKS/GKE/self-managed) | Horizontal pod autoscaling for Spring Boot; Redis Sentinel; managed PG. |

---

## 4. Mobile Client App — Detailed Design (Flutter)

### 4.1 App Responsibilities

The Flutter app is a **thin native shell** around the Next.js WebView, extended with device-native capabilities that the web layer cannot access directly. The web layer owns navigation, UI, and business flows; Flutter owns device integration and secure token storage.

### 4.2 Module Breakdown

```
lib/
├── main.dart
├── app.dart                       # Root widget, theme, routing
├── core/
│   ├── config/                    # Environment config (dev/stage/prod URLs)
│   ├── di/                        # get_it / riverpod providers
│   └── logging/                   # Talker / dart:developer
├── bridge/
│   ├── js_bridge.dart             # JavascriptChannel handler
│   ├── bridge_messages.dart       # Sealed classes for incoming/outgoing messages
│   └── bridge_dispatcher.dart     # Routes bridge calls to native modules
├── native/
│   ├── location_service.dart      # geolocator wrapper
│   ├── qr_scanner_service.dart    # mobile_scanner wrapper
│   ├── file_picker_service.dart   # file_picker wrapper
│   ├── push_service.dart          # firebase_messaging + APNs
│   ├── secure_storage_service.dart# flutter_secure_storage (JWT, refresh)
│   └── sns_auth_service.dart      # flutter_facebook_auth + LinkedIn webview
├── storage/
│   ├── isar_db.dart               # Database instance
│   ├── schemas/
│   │   ├── match_entity.dart      # @Collection class MatchEntity
│   │   └── setting_entity.dart
│   └── repositories/
│       ├── match_repo.dart
│       └── settings_repo.dart
├── features/
│   ├── webview/
│   │   ├── webview_screen.dart    # Main WebView host screen
│   │   └── webview_controller.dart
│   └── splash/
│       └── splash_screen.dart
└── utils/
    ├── permissions.dart           # permission_handler wrappers
    └── error_boundary.dart
```

### 4.3 Key Flutter Dependencies

```yaml
# pubspec.yaml (excerpt)
dependencies:
  flutter:
    sdk: flutter
  webview_flutter: ^4.7.0
  webview_flutter_android: ^3.16.0
  webview_flutter_wkwebview: ^3.13.0
  geolocator: ^12.0.0
  mobile_scanner: ^5.0.0
  file_picker: ^8.0.0
  permission_handler: ^11.3.0
  flutter_secure_storage: ^9.2.0
  isar: ^3.1.0
  isar_flutter_libs: ^3.1.0
  firebase_core: ^3.0.0
  firebase_messaging: ^15.0.0
  flutter_facebook_auth: ^7.0.0
  flutter_riverpod: ^2.5.0
  dio: ^5.4.0                 # for direct Flutter-side API calls (file upload, etc.)
  path_provider: ^2.1.0
  connectivity_plus: ^6.0.0
  talker_flutter: ^4.4.0
```

### 4.4 WebView Setup

```dart
// features/webview/webview_screen.dart (simplified)
class WebViewScreen extends ConsumerStatefulWidget {
  const WebViewScreen({super.key});
  @override
  ConsumerState<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends ConsumerState<WebViewScreen> {
  late final WebViewController _controller;

  @override
  void initState() {
    super.initState();
    final bridge = ref.read(jsBridgeProvider);

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(const Color(0x00000000))
      ..addJavaScriptChannel(
        'FlutterBridge',
        onMessageReceived: (msg) => bridge.handleIncoming(msg.message),
      )
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageFinished: (_) => bridge.injectBootstrap(_controller),
          onNavigationRequest: (req) {
            // Block navigation away from trusted host
            if (!req.url.startsWith(AppConfig.frontendOrigin)) {
              launchUrl(Uri.parse(req.url), mode: LaunchMode.externalApplication);
              return NavigationDecision.prevent;
            }
            return NavigationDecision.navigate;
          },
        ),
      )
      ..loadRequest(Uri.parse(AppConfig.frontendOrigin));
  }
  ...
}
```

### 4.5 JavaScript Bridge Protocol

Bi-directional, JSON-based. Every message has `{ id, type, payload }`. The web side posts via `window.FlutterBridge.postMessage(JSON.stringify(...))`. Flutter replies by evaluating a JS callback registered under `window.__bridgeResolve[id]`.

#### 4.5.1 Message Types (Web → Native)

| Type | Payload | Native Action |
|---|---|---|
| `gps.start` | `{intervalSec, minMoveMeters}` | Begin streaming locations to backend |
| `gps.stop` | `{}` | Stop location stream |
| `qr.scan` | `{}` | Opens scanner, returns decoded string |
| `file.pickArticle` | `{allowedExt: ["pdf","txt"]}` | Returns file path + base64 preview |
| `storage.get` | `{key}` | Secure-storage read (tokens) |
| `storage.set` | `{key, value}` | Secure-storage write |
| `localdb.matches.list` | `{limit, offset}` | Returns cached matches |
| `localdb.matches.save` | `{match}` | Persist match in Isar |
| `sns.login` | `{provider: "facebook" \| "linkedin"}` | OAuth flow, returns token |
| `push.requestPermission` | `{}` | Request notification permission |
| `push.token` | `{}` | Return current FCM/APNs token |
| `app.info` | `{}` | Version, OS, device model |

#### 4.5.2 Message Types (Native → Web)

| Type | Payload | Web Handler |
|---|---|---|
| `push.received` | `{title, body, data}` | Show in-app toast / navigate |
| `gps.error` | `{code, message}` | Toggle off GPS indicator |
| `connectivity.change` | `{online: bool}` | Offline banner |
| `app.resume` | `{}` | Refresh matches list |

#### 4.5.3 Bridge Implementation (Dart)

```dart
class JsBridge {
  final LocationService _loc;
  final QrScannerService _qr;
  final FilePickerService _file;
  final SecureStorageService _store;
  final MatchRepo _matchRepo;
  final SnsAuthService _sns;
  WebViewController? _controller;

  Future<void> handleIncoming(String raw) async {
    final msg = jsonDecode(raw) as Map<String, dynamic>;
    final id = msg['id'] as String;
    final type = msg['type'] as String;
    final payload = msg['payload'] ?? {};
    try {
      final result = await _dispatch(type, payload);
      _reply(id, {'ok': true, 'data': result});
    } catch (e, st) {
      _reply(id, {'ok': false, 'error': e.toString()});
      log('Bridge error', error: e, stackTrace: st);
    }
  }

  Future<Object?> _dispatch(String type, Map p) async {
    switch (type) {
      case 'gps.start':      return _loc.start(intervalSec: p['intervalSec'] ?? 30);
      case 'qr.scan':        return _qr.scanOnce();
      case 'file.pickArticle': return _file.pickArticle();
      case 'storage.get':    return _store.read(p['key']);
      case 'storage.set':    return _store.write(p['key'], p['value']);
      case 'localdb.matches.list': return _matchRepo.list(limit: p['limit'], offset: p['offset']);
      case 'localdb.matches.save': return _matchRepo.save(MatchEntity.fromJson(p['match']));
      case 'sns.login':      return _sns.login(p['provider']);
      default: throw UnsupportedError('Unknown bridge type: $type');
    }
  }

  void _reply(String id, Map<String, dynamic> body) {
    final js = 'window.__bridgeResolve["$id"](${jsonEncode(body)});';
    _controller?.runJavaScript(js);
  }
}
```

### 4.6 GPS Service

```dart
class LocationService {
  StreamSubscription<Position>? _sub;
  final Dio _dio;
  final SecureStorageService _store;

  Future<void> start({int intervalSec = 30, double minMoveMeters = 10}) async {
    if (!await Geolocator.isLocationServiceEnabled()) {
      throw StateError('Location service disabled');
    }
    var perm = await Geolocator.checkPermission();
    if (perm == LocationPermission.denied) {
      perm = await Geolocator.requestPermission();
    }
    if (perm != LocationPermission.always && perm != LocationPermission.whileInUse) {
      throw StateError('Permission not granted');
    }

    final settings = Platform.isAndroid
      ? AndroidSettings(
          accuracy: LocationAccuracy.high,
          distanceFilter: minMoveMeters.toInt(),
          intervalDuration: Duration(seconds: intervalSec),
          foregroundNotificationConfig: const ForegroundNotificationConfig(
            notificationTitle: 'Conference Tool', notificationText: 'Sharing your location during the event',
            enableWakeLock: false),
        )
      : AppleSettings(accuracy: LocationAccuracy.high, distanceFilter: minMoveMeters.toInt(), pauseLocationUpdatesAutomatically: true);

    await _sub?.cancel();
    _sub = Geolocator.getPositionStream(locationSettings: settings).listen(_send);
  }

  Future<void> stop() async { await _sub?.cancel(); _sub = null; }

  Future<void> _send(Position p) async {
    final jwt = await _store.read('jwt');
    final eventId = await _store.read('activeEventId');
    if (jwt == null || eventId == null) return;
    await _dio.post('/api/events/$eventId/location',
      data: {'lat': p.latitude, 'lon': p.longitude, 'accuracy': p.accuracy, 'ts': DateTime.now().toUtc().toIso8601String()},
      options: Options(headers: {'Authorization': 'Bearer $jwt'}));
  }
}
```

### 4.7 Local Database (Isar)

```dart
@collection
class MatchEntity {
  Id id = Isar.autoIncrement;
  @Index(unique: true) late String matchId;         // server-provided UUID
  late String eventId;
  late String otherUserId;
  late String otherName;
  late String? otherTitle;
  late String? otherInstitution;
  late String? profilePictureUrl;
  late List<String> commonKeywords;
  late double similarity;
  late DateTime matchedAt;
}

class MatchRepo {
  final Isar _isar;
  MatchRepo(this._isar);

  Future<void> save(MatchEntity m) => _isar.writeTxn(() => _isar.matchEntitys.put(m));

  Future<List<MatchEntity>> list({int limit = 50, int offset = 0}) =>
    _isar.matchEntitys.where().sortByMatchedAtDesc().offset(offset).limit(limit).findAll();

  Future<void> clearAll() => _isar.writeTxn(() => _isar.matchEntitys.clear());
}
```

### 4.8 Push Notifications

- **Firebase Core** initialised on app start.
- On cold start, fetch FCM token (Android) / APNs token (iOS), send to backend via `POST /api/devices/register` tied to the current `userId`.
- Handle three message states: `onMessage` (foreground → toast), `onMessageOpenedApp` (tap from background → deep link into WebView route), `onBackgroundMessage` (system tray).
- Deep-link payload schema: `{route: "/chat/:userId" | "/matches", eventId}`. Flutter navigates the WebView to `${frontendOrigin}${route}`.

### 4.9 App Lifecycle & Background Behavior

| State | Behavior |
|---|---|
| Foreground | GPS at configured interval; WebView active; WebSocket via web layer. |
| Background (event active) | Continue GPS updates (Android foreground service; iOS significant-change mode if battery tight). |
| Background (no event) | GPS off. Push still received via FCM/APNs. |
| App killed | Push received via system. Tap reopens app and routes to the deep link. |

### 4.10 Error Handling & Resilience

- All bridge failures bubble to the web layer as `{ok:false,error}` responses — the web UI displays user-facing toasts.
- Dio retries 3× with exponential backoff for 5xx and network errors on idempotent endpoints (GET, PUT `/location`).
- Connectivity changes via `connectivity_plus` pause the GPS transmitter; buffered positions (max 20 in memory) flush when online.

---

## 5. Web Frontend — Detailed Design (React / Next.js)

### 5.1 Responsibilities

The Next.js app renders inside the Flutter WebView and owns:
- All user-visible screens (login, profile, interests, event join, vicinity list, chat, settings).
- Client-state management, forms, validation.
- Calls to the Java backend via REST and STOMP/WebSocket.
- Delegating device operations to Flutter via the JS bridge.

### 5.2 Project Structure (App Router)

```
web/
├── app/
│   ├── layout.tsx                  # Root layout, theme, providers
│   ├── page.tsx                    # Start / welcome
│   ├── (auth)/
│   │   ├── register/page.tsx
│   │   ├── verify/page.tsx
│   │   └── login/page.tsx
│   ├── profile/
│   │   ├── page.tsx                # View/edit profile
│   │   └── sns/page.tsx            # SNS link management
│   ├── interests/
│   │   └── page.tsx
│   ├── events/
│   │   ├── join/page.tsx           # QR scan trigger + manual entry
│   │   └── [eventId]/
│   │       ├── page.tsx            # Event home
│   │       ├── vicinity/page.tsx   # Match list
│   │       └── chat/[otherId]/page.tsx
│   ├── settings/page.tsx
│   └── api/                        # (Only lightweight BFF helpers if any)
├── components/
│   ├── ui/                         # shadcn/ui re-exports
│   ├── chat/
│   │   ├── ChatWindow.tsx
│   │   ├── MessageBubble.tsx
│   │   └── MessageInput.tsx
│   ├── match/
│   │   ├── MatchCard.tsx
│   │   └── VicinityRadiusSelector.tsx
│   ├── bridge/
│   │   └── BridgeProvider.tsx
│   └── layout/
│       └── AppShell.tsx
├── lib/
│   ├── bridge/                     # JS bridge client
│   │   ├── client.ts
│   │   └── types.ts
│   ├── api/                        # REST client (fetch + React Query hooks)
│   │   ├── axios.ts
│   │   ├── auth.ts
│   │   ├── events.ts
│   │   └── chat.ts
│   ├── ws/                         # STOMP client
│   │   ├── client.ts
│   │   └── hooks.ts
│   ├── auth/                       # Token storage (delegates to bridge)
│   ├── state/                      # Zustand stores
│   │   ├── authStore.ts
│   │   ├── eventStore.ts
│   │   └── chatStore.ts
│   └── utils/
├── styles/
│   └── globals.css
├── public/
├── next.config.mjs
├── package.json
└── tsconfig.json
```

### 5.3 Key Dependencies

```json
{
  "dependencies": {
    "next": "14.2.x",
    "react": "18.3.x",
    "react-dom": "18.3.x",
    "typescript": "5.4.x",
    "@tanstack/react-query": "5.x",
    "zustand": "4.5.x",
    "zod": "3.23.x",
    "react-hook-form": "7.51.x",
    "@hookform/resolvers": "3.3.x",
    "axios": "1.7.x",
    "@stomp/stompjs": "7.x",
    "sockjs-client": "1.6.x",
    "tailwindcss": "3.4.x",
    "lucide-react": "0.383.x",
    "date-fns": "3.6.x",
    "clsx": "2.1.x"
  }
}
```

### 5.4 Bridge Client (TypeScript)

```ts
// lib/bridge/client.ts
type BridgePayload = Record<string, unknown>;

declare global {
  interface Window {
    FlutterBridge?: { postMessage: (msg: string) => void };
    __bridgeResolve?: Record<string, (r: unknown) => void>;
  }
}

class BridgeClient {
  private idCounter = 0;
  constructor() {
    if (typeof window !== 'undefined') {
      window.__bridgeResolve = window.__bridgeResolve || {};
    }
  }

  private nextId() { return `b_${Date.now()}_${++this.idCounter}`; }

  get available() { return typeof window !== 'undefined' && !!window.FlutterBridge; }

  call<T = unknown>(type: string, payload: BridgePayload = {}, timeoutMs = 15000): Promise<T> {
    if (!this.available) return Promise.reject(new Error('Bridge unavailable (browser mode)'));
    const id = this.nextId();
    return new Promise<T>((resolve, reject) => {
      const timer = setTimeout(() => {
        delete window.__bridgeResolve![id];
        reject(new Error(`Bridge timeout: ${type}`));
      }, timeoutMs);
      window.__bridgeResolve![id] = (raw) => {
        clearTimeout(timer);
        delete window.__bridgeResolve![id];
        const r = raw as { ok: boolean; data?: T; error?: string };
        r.ok ? resolve(r.data as T) : reject(new Error(r.error));
      };
      window.FlutterBridge!.postMessage(JSON.stringify({ id, type, payload }));
    });
  }
}
export const bridge = new BridgeClient();
```

### 5.5 Auth Strategy in WebView

- Backend issues a short-lived **access JWT** (15 min) + long-lived **refresh token** (30 days).
- Tokens are **never stored in localStorage** (XSS risk inside WebView). Instead, `bridge.call('storage.set', {key:'jwt', value})` persists them in Flutter `flutter_secure_storage` (Android Keystore / iOS Keychain).
- Each React Query / Axios request pulls the JWT via `bridge.call('storage.get', {key:'jwt'})` (cached per-render in memory).
- 401 interceptor calls `/api/auth/refresh`, updates storage, retries original request.

```ts
// lib/api/axios.ts
const api = axios.create({ baseURL: '/api', timeout: 10000 });

api.interceptors.request.use(async (cfg) => {
  const jwt = await bridge.call<string|null>('storage.get', {key: 'jwt'}).catch(() => null);
  if (jwt) cfg.headers.Authorization = `Bearer ${jwt}`;
  return cfg;
});

api.interceptors.response.use(undefined, async (err) => {
  if (err.response?.status !== 401 || err.config.__retried) throw err;
  const refresh = await bridge.call<string|null>('storage.get', {key: 'refresh'}).catch(() => null);
  if (!refresh) throw err;
  const { data } = await axios.post('/api/auth/refresh', { refresh });
  await bridge.call('storage.set', { key: 'jwt', value: data.accessToken });
  err.config.__retried = true;
  err.config.headers.Authorization = `Bearer ${data.accessToken}`;
  return api(err.config);
});
```

### 5.6 State Management

- **React Query** for server-state (matches list, profile, chat history): caches and revalidates.
- **Zustand** for cross-cutting UI state (current event, online status, unread chat count).
- **React Hook Form + Zod** for all forms (registration, profile, interests) with client-side validation mirroring backend rules.

### 5.7 Real-Time Chat (STOMP client)

```ts
// lib/ws/client.ts
import { Client } from '@stomp/stompjs';

export function createStompClient(jwt: string) {
  return new Client({
    brokerURL: `${WS_ORIGIN}/ws`,                   // wss://api.example.com/ws
    connectHeaders: { Authorization: `Bearer ${jwt}` },
    reconnectDelay: 3000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,
    debug: () => {},
  });
}

// Hook usage
export function useChat(eventId: string, otherId: string) {
  const [messages, setMessages] = useState<Msg[]>([]);
  const clientRef = useRef<Client>();

  useEffect(() => {
    (async () => {
      const jwt = await bridge.call<string>('storage.get', { key: 'jwt' });
      const c = createStompClient(jwt);
      c.onConnect = () => {
        c.subscribe(`/user/queue/chat.${eventId}`, (f) => {
          const msg = JSON.parse(f.body) as Msg;
          if (msg.fromUserId === otherId || msg.toUserId === otherId) {
            setMessages((prev) => [...prev, msg]);
          }
        });
      };
      c.activate();
      clientRef.current = c;
    })();
    return () => { clientRef.current?.deactivate(); };
  }, [eventId, otherId]);

  const send = (content: string) =>
    clientRef.current?.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ eventId, toUserId: otherId, content }),
    });

  return { messages, send };
}
```

### 5.8 WebView-Specific UX

- Disable text-selection long-press menus on non-content areas via CSS `user-select: none`.
- Hide browser scroll-bounce with `overscroll-behavior: none`.
- Use `viewport-fit=cover` + safe-area insets to respect iOS notch.
- Forms use native input types (`type="email"`, `inputmode="numeric"` for TAN) so Flutter shows the correct soft keyboard.
- Offline indicator driven by `connectivity.change` bridge event — React Query pauses retries when offline.

### 5.9 Accessibility

- All interactive elements have `aria-label` or visible text.
- Focus rings preserved (`focus-visible:ring-2`).
- Colour contrast ≥ 4.5:1 (checked with axe in CI).
- Screen-reader announcements for new chat messages via `aria-live="polite"` region.

---

## 6. Backend Server — Detailed Design (Java / Spring Boot)

### 6.1 Service Boundary

The backend is a **modular monolith** (single deployable) organised into bounded contexts. If scale demands, contexts can later be extracted into separate services without data-model disruption.

| Bounded Context | Responsibilities |
|---|---|
| `identity` | Registration, email verification, JWT issuance, refresh, device token registration. |
| `profile` | User profile CRUD, profile picture upload (presigned S3 URL). |
| `interest` | Interest capture (text/article/link), keyword extraction orchestration. |
| `event` | Event CRUD, QR generation & validation, participation join/leave. |
| `location` | GPS position ingestion, vicinity queries. |
| `matching` | Similarity calculation, match list generation, mutual-match notifications. |
| `chat` | WebSocket endpoints, chat persistence. |
| `sns` | OAuth2 linking with Facebook/LinkedIn, data enrichment jobs. |
| `notification` | FCM/APNs dispatch, email dispatch. |
| `admin` | Event cleanup job, retention enforcement, metrics. |

### 6.2 Project Layout (Gradle Multi-module)

```
backend/
├── build.gradle.kts
├── settings.gradle.kts
├── app/                             # Spring Boot main module
│   └── src/main/java/com/conf/app/
│       ├── ConfApplication.java
│       └── config/
│           ├── SecurityConfig.java
│           ├── WebSocketConfig.java
│           ├── RedisConfig.java
│           ├── JpaConfig.java
│           ├── OpenApiConfig.java
│           └── SchedulingConfig.java
├── modules/
│   ├── identity/
│   ├── profile/
│   ├── interest/
│   ├── event/
│   ├── location/
│   ├── matching/
│   ├── chat/
│   ├── sns/
│   └── notification/
├── common/                          # shared kernel: DTOs, exceptions, geo utils
└── infrastructure/
    ├── persistence-jpa/
    ├── redis/
    ├── storage-s3/
    └── mail/
```

Each module follows a hexagonal layout:

```
modules/event/src/main/java/com/conf/event/
├── api/                    # REST controllers
│   ├── EventController.java
│   └── dto/
├── domain/                 # Entities, value objects, domain services
│   ├── Event.java
│   ├── Participation.java
│   └── EventLifecycle.java
├── application/            # Use cases
│   ├── JoinEventUseCase.java
│   ├── CreateEventUseCase.java
│   └── CleanupExpiredEventsUseCase.java
├── infrastructure/
│   ├── EventRepository.java     # Spring Data JPA interface
│   ├── EventRedisCache.java
│   └── QrCodeEncoder.java
└── events/                 # Domain events published to app event bus
    └── ParticipantJoinedEvent.java
```

### 6.3 Core Dependencies

```kotlin
// build.gradle.kts (excerpt)
dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-websocket")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-mail")

  implementation("org.postgresql:postgresql")
  implementation("net.postgis:postgis-jdbc:2024.1.0")
  implementation("org.hibernate.orm:hibernate-spatial")

  implementation("org.redisson:redisson-spring-boot-starter:3.31.0")

  implementation("com.auth0:java-jwt:4.4.0")
  implementation("org.apache.opennlp:opennlp-tools:2.3.2")
  implementation("org.apache.pdfbox:pdfbox:3.0.2")

  implementation("com.google.firebase:firebase-admin:9.3.0")

  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

  runtimeOnly("org.postgresql:postgresql")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.testcontainers:postgresql:1.19.8")
  testImplementation("org.testcontainers:junit-jupiter:1.19.8")
  testImplementation("it.ozimov:embedded-redis:0.7.3")
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
```

### 6.4 Application Configuration

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/conf
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    hibernate.ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect
      hibernate.jdbc.time_zone: UTC
  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      password: ${REDIS_PASSWORD}
  mail:
    host: ${SMTP_HOST}
    port: 587
    username: ${SMTP_USER}
    password: ${SMTP_PASSWORD}
    properties.mail.smtp.starttls.enable: true

app:
  jwt:
    issuer: https://api.conference-tool.example
    access-ttl: 15m
    refresh-ttl: 30d
    secret: ${JWT_SECRET}
  cors:
    allowed-origins: https://app.conference-tool.example
  similarity:
    recompute-interval: 5m
    top-n: 10
    threshold: 0.35
  vicinity:
    allowed-radii: [20, 50, 100]
  storage:
    s3:
      endpoint: ${S3_ENDPOINT}
      bucket: conf-assets
      region: eu-central-1
  fcm:
    credentials-path: ${FCM_CRED_PATH}
  sns:
    facebook.client-id: ${FB_CLIENT_ID}
    facebook.client-secret: ${FB_CLIENT_SECRET}
    linkedin.client-id: ${LI_CLIENT_ID}
    linkedin.client-secret: ${LI_CLIENT_SECRET}

logging.level.root: INFO
management.endpoints.web.exposure.include: health, info, metrics, prometheus
```

### 6.5 Representative Controller — Event Join

```java
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Validated
public class EventController {

  private final JoinEventUseCase joinEvent;
  private final VicinityQueryUseCase vicinityQuery;

  @PostMapping("/join")
  public ResponseEntity<JoinEventResponse> join(
      @AuthenticationPrincipal AuthUser user,
      @Valid @RequestBody JoinEventRequest req) {
    var result = joinEvent.execute(user.id(), req.eventId(), req.expirationCode());
    return ResponseEntity.ok(JoinEventResponse.from(result));
  }

  @GetMapping("/{eventId}/vicinity")
  public VicinityResponse vicinity(
      @AuthenticationPrincipal AuthUser user,
      @PathVariable UUID eventId,
      @RequestParam @Min(20) @Max(100) int radius) {
    return vicinityQuery.execute(user.id(), eventId, radius);
  }
}
```

### 6.6 Domain Use-Case Example — JoinEvent

```java
@Service
@RequiredArgsConstructor
@Transactional
public class JoinEventUseCase {

  private final EventRepository events;
  private final ParticipationRepository participations;
  private final EventRedisCache redis;
  private final Clock clock;

  public JoinEventResult execute(UUID userId, UUID eventId, Instant expirationCode) {
    Event event = events.findById(eventId)
        .orElseThrow(() -> new NotFoundException("Event not found"));

    if (!event.getExpirationCode().equals(expirationCode)) {
      throw new ValidationException("QR code tampered");
    }
    if (event.getExpirationCode().isBefore(clock.instant())) {
      throw new ValidationException("Event expired");
    }

    Participation p = participations
        .findByUserIdAndEventId(userId, eventId)
        .orElseGet(() -> Participation.create(userId, eventId));
    p.markActive(clock.instant());
    participations.save(p);

    redis.addActiveParticipant(eventId, userId);     // SADD event:{id}:participants
    return new JoinEventResult(event, p);
  }
}
```

### 6.7 REST API Conventions

- All URLs under `/api/`.
- JSON only. `application/json; charset=utf-8`.
- Problem Details (RFC 9457) for errors:
  ```json
  { "type": "/problems/validation", "title": "Validation failed", "status": 400, "detail": "...", "errors": [...] }
  ```
- Pagination: `?page=0&size=20`, response includes `{content, page, size, totalElements, totalPages}`.
- Versioning via header `X-API-Version: 1` (future-proofing); current version implicit.
- Rate-limited globally (100 req/min/user) and per-endpoint where needed (registration: 5 req/hour/IP).

### 6.8 Background Jobs

Implemented with **Redisson** delayed-queues and **Spring `@Scheduled`** for cron-like tasks.

| Job | Trigger | Purpose |
|---|---|---|
| `SimilarityRecomputeJob` | Every 5 min per active event | Compute top-N matches per participant; cache in Redis; publish `MatchDetected` events. |
| `EventCleanupJob` | Every 15 min | Delete events past `expirationCode` + cascade data. |
| `EmailDispatchJob` | Redis queue, on-demand | Send verification / password reset emails. |
| `SnsRefreshJob` | Daily per linked user | Refresh SNS profile data (respecting rate limits). |
| `KeywordExtractionJob` | On interest upload | Parse PDF, extract keywords, persist. |
| `PushDispatchJob` | Redis queue, on-demand | FCM/APNs send with retry. |

All jobs: idempotent, with Redisson distributed locks to prevent duplicate execution across instances.

```java
@Component
@RequiredArgsConstructor
public class SimilarityRecomputeJob {

  private final RedissonClient redisson;
  private final MatchingService matching;

  @Scheduled(fixedDelayString = "${app.similarity.recompute-interval}")
  public void run() {
    RLock lock = redisson.getLock("job:similarity-recompute");
    if (!lock.tryLock()) return;
    try {
      matching.recomputeAllActiveEvents();
    } finally {
      lock.unlock();
    }
  }
}
```

### 6.9 Transactional Boundaries

- Use-case services are `@Transactional` at the method level.
- Long-running operations (keyword extraction, SNS fetches) run **outside** transactions as async jobs.
- Domain events (`ParticipantJoinedEvent`, `MatchDetectedEvent`) are published via `ApplicationEventPublisher` **after commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)`).

### 6.10 Error Handling

Global `@ControllerAdvice` translates:

| Exception | HTTP | Problem type |
|---|---|---|
| `NotFoundException` | 404 | `/problems/not-found` |
| `ValidationException`, `MethodArgumentNotValidException` | 400 | `/problems/validation` |
| `ForbiddenException` | 403 | `/problems/forbidden` |
| `AuthenticationException` | 401 | `/problems/unauthorized` |
| `RateLimitExceededException` | 429 | `/problems/rate-limited` |
| `ConflictException` | 409 | `/problems/conflict` |
| Any other | 500 | `/problems/server-error` (no stack trace leaked) |

---

## 7. Database Design (PostgreSQL + PostGIS)

### 7.1 Schema — Core Tables

```sql
-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- users (identity)
CREATE TABLE users (
  user_id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  email              CITEXT UNIQUE NOT NULL,
  email_verified     BOOLEAN NOT NULL DEFAULT FALSE,
  password_hash      VARCHAR(255),                 -- nullable if SSO-only
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at         TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON users (email);

-- profiles (one-to-one with users)
CREATE TABLE profiles (
  user_id               UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
  last_name             VARCHAR(100) NOT NULL,
  first_name            VARCHAR(100) NOT NULL,
  academic_title        VARCHAR(50),
  institution           VARCHAR(200),
  profile_picture_url   TEXT,
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- interests
CREATE TYPE interest_type AS ENUM ('TEXT', 'ARTICLE_LOCAL', 'ARTICLE_LINK');

CREATE TABLE interests (
  interest_id       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id           UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  type              interest_type NOT NULL,
  content           TEXT NOT NULL,        -- free text OR S3 URL OR external URL
  extracted_keywords TEXT[] NOT NULL DEFAULT '{}',
  keyword_vector    JSONB,                -- sparse TF-IDF vector
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_interests_user ON interests (user_id);
CREATE INDEX idx_interests_keywords ON interests USING GIN (extracted_keywords);

-- events
CREATE TABLE events (
  event_id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  event_name        VARCHAR(200) NOT NULL,
  venue             VARCHAR(300) NOT NULL,
  expiration_code   TIMESTAMPTZ NOT NULL,
  qr_code_hash      VARCHAR(128) NOT NULL UNIQUE,
  centroid          GEOGRAPHY(POINT, 4326),       -- optional event centre
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_expiration ON events (expiration_code);

-- participations
CREATE TABLE participations (
  user_id             UUID NOT NULL REFERENCES users(user_id)  ON DELETE CASCADE,
  event_id            UUID NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
  selected_radius     SMALLINT NOT NULL DEFAULT 50 CHECK (selected_radius IN (20, 50, 100)),
  last_position       GEOGRAPHY(POINT, 4326),
  last_position_acc_m REAL,
  last_update         TIMESTAMPTZ,
  joined_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, event_id)
);

CREATE INDEX idx_participations_event ON participations (event_id);
CREATE INDEX idx_participations_position ON participations USING GIST (last_position);
CREATE INDEX idx_participations_lastupd ON participations (event_id, last_update DESC);

-- similarity matches
CREATE TABLE similarity_matches (
  match_id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  event_id         UUID NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
  user_id_a        UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  user_id_b        UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  similarity       REAL NOT NULL,
  common_keywords  TEXT[] NOT NULL DEFAULT '{}',
  mutual           BOOLEAN NOT NULL DEFAULT FALSE,
  notified_at      TIMESTAMPTZ,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (user_id_a < user_id_b)                 -- canonical order
);

CREATE UNIQUE INDEX idx_matches_pair ON similarity_matches (event_id, user_id_a, user_id_b);
CREATE INDEX idx_matches_a ON similarity_matches (user_id_a, event_id);
CREATE INDEX idx_matches_b ON similarity_matches (user_id_b, event_id);

-- chat messages
CREATE TABLE chat_messages (
  message_id    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  event_id      UUID NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
  from_user_id  UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  to_user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  content       TEXT NOT NULL CHECK (char_length(content) <= 4000),
  read_flag     BOOLEAN NOT NULL DEFAULT FALSE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_event_pair_time ON chat_messages
  (event_id, LEAST(from_user_id, to_user_id), GREATEST(from_user_id, to_user_id), created_at DESC);

-- SNS links (optional)
CREATE TYPE sns_provider AS ENUM ('FACEBOOK', 'LINKEDIN');

CREATE TABLE sns_links (
  sns_id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id            UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  provider           sns_provider NOT NULL,
  provider_user_id   VARCHAR(200) NOT NULL,
  access_token_enc   BYTEA NOT NULL,              -- encrypted at app level
  refresh_token_enc  BYTEA,
  token_expires_at   TIMESTAMPTZ,
  last_fetch         TIMESTAMPTZ,
  imported_data      JSONB,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, provider)
);

-- device tokens (push)
CREATE TABLE device_tokens (
  token_id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id         UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  platform        VARCHAR(10) NOT NULL CHECK (platform IN ('ANDROID','IOS')),
  token           TEXT NOT NULL,
  app_version     VARCHAR(30),
  last_seen       TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, token)
);

-- refresh tokens
CREATE TABLE refresh_tokens (
  jti             UUID PRIMARY KEY,
  user_id         UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  expires_at      TIMESTAMPTZ NOT NULL,
  revoked         BOOLEAN NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- audit log (append-only)
CREATE TABLE audit_log (
  id              BIGSERIAL PRIMARY KEY,
  actor_user_id   UUID,
  action          VARCHAR(80) NOT NULL,
  resource_type   VARCHAR(80),
  resource_id     TEXT,
  ip_hash         VARCHAR(128),
  payload         JSONB,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_actor_time ON audit_log (actor_user_id, created_at DESC);
```

### 7.2 Key Geospatial Query

```sql
-- Find candidates within radius for a given user in an event.
-- Called from MatchingService for similarity computation.
SELECT p.user_id,
       ST_Distance(p.last_position, me.last_position) AS distance_m
FROM participations p
JOIN participations me
  ON me.event_id = p.event_id
 AND me.user_id  = :meId
WHERE p.event_id = :eventId
  AND p.user_id <> me.user_id
  AND p.last_update > now() - INTERVAL '5 minutes'
  AND ST_DWithin(p.last_position, me.last_position, :radiusMeters)
ORDER BY distance_m
LIMIT 200;
```

`ST_DWithin` uses the GiST index — performance is logarithmic even at thousands of participants.

### 7.3 Migrations

Managed via **Flyway**:
```
backend/app/src/main/resources/db/migration/
├── V1__init_extensions.sql
├── V2__users_and_profiles.sql
├── V3__interests.sql
├── V4__events_and_participations.sql
├── V5__matches_and_chat.sql
├── V6__sns_and_devices.sql
└── V7__audit_log.sql
```

### 7.4 Data Retention

- Expired events cascade-delete participations, matches, chat messages.
- Audit log retained 180 days (partition by month; drop old partitions).
- GPS positions are not stored separately — only `last_position` in `participations` (overwritten on update).

### 7.5 Backup Strategy

- Continuous WAL archiving to object storage (PITR RPO ≤ 5 min).
- Daily full `pg_basebackup` retained 14 days.
- Monthly backup retained 12 months (GDPR-compatible retention).

---

## 8. Cache & Job Queue Design (Redis)

### 8.1 Key Namespace

```
event:{eventId}:participants         SET   userIds currently active in event
user:{userId}:position               HASH  {lat, lon, accuracy, updatedAt}  TTL 120s
user:{userId}:matches:{eventId}      STRING JSON list of top-N matches    TTL 5m
session:access:{jti}                 STRING userId                         TTL = access-ttl
session:refresh:{jti}                STRING userId                         TTL = refresh-ttl
ratelimit:{userId}:{bucket}          COUNTER                                TTL 60s
rlimit:ip:{ip}:register              COUNTER                                TTL 3600s
lock:job:{name}                      STRING (Redisson RLock)
queue:email                          STREAM
queue:push                           STREAM
queue:keywords                       STREAM
queue:sns-refresh                    STREAM
chat:online:{userId}                 STRING                                 TTL 30s (heartbeat)
```

### 8.2 Pub/Sub Channels

```
ws:chat:{userId}       → WebSocket fan-out; any backend instance holding the user's session subscribes.
ws:push:{userId}       → server-initiated (match notifications).
event:active:{eventId} → management events (user joined/left).
```

Each Spring Boot instance runs one Redis subscriber per connected WebSocket session, delivering messages down the socket.

### 8.3 Job Queue (Redis Streams)

```java
// Enqueue
redis.opsForStream().add(
    MapRecord.create("queue:email", Map.of(
        "to", "alice@example.com",
        "template", "verify",
        "tan", "123456")));

// Consumer group (one per worker pool)
redis.opsForStream().createGroup("queue:email", "email-workers");

// Workers consume with XREADGROUP, XACK on success, dead-letter on repeated failure.
```

### 8.4 Distributed Locking

Redisson `RLock` used for:
- Scheduled jobs (prevent duplicate execution).
- User-level serialisation of account operations (registration race conditions).

### 8.5 Cache Coherence

Rule: PostgreSQL is source of truth. Cache entries are **read-through with short TTL**. Writes update the database first, then invalidate / update the cache (`repo.save` → `cache.evict`).

---

## 9. Real-time Communication (WebSocket)

### 9.1 Protocol Stack

- Transport: WSS at `/ws` (SockJS fallback not required inside WebView).
- Framing: STOMP 1.2.
- Auth: `Authorization: Bearer <jwt>` in CONNECT frame; validated by `ChannelInterceptor` on each message.

### 9.2 Spring Configuration

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtChannelInterceptor authInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry c) {
    // Use Redis relay via StompBrokerRelay if external; here we use simple broker + Redis Pub/Sub adapter
    c.enableSimpleBroker("/topic", "/queue");
    c.setApplicationDestinationPrefixes("/app");
    c.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry r) {
    r.addEndpoint("/ws").setAllowedOriginPatterns("*");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration r) {
    r.interceptors(authInterceptor);
  }
}
```

### 9.3 Chat Controller

```java
@Controller
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;
  private final SimpMessagingTemplate broker;

  @MessageMapping("/chat.send")
  public void send(@AuthenticationPrincipal AuthUser user, @Payload ChatSendRequest req) {
    ChatMessage saved = chatService.send(user.id(), req.toUserId(), req.eventId(), req.content());
    // Deliver to recipient queue (per-user destination) and echo back to sender
    broker.convertAndSendToUser(req.toUserId().toString(),
        "/queue/chat." + req.eventId(), ChatMessageDto.from(saved));
    broker.convertAndSendToUser(user.id().toString(),
        "/queue/chat." + req.eventId(), ChatMessageDto.from(saved));
  }
}
```

### 9.4 Scaling Across Instances

Default Spring simple broker is in-memory. For multi-instance, option A: **StompBrokerRelay** to an external STOMP broker (RabbitMQ). Option B (used here): wrap outbound messages with a Redis Pub/Sub adapter that each instance subscribes to, then routes to locally connected sessions.

```java
@Service
@RequiredArgsConstructor
public class RedisRelay {
  private final StringRedisTemplate redis;
  private final SimpMessagingTemplate local;

  public void send(UUID userId, String destination, Object body) {
    redis.convertAndSend("ws:" + userId, new Envelope(destination, body).serialize());
  }

  @EventListener
  public void onEnvelope(RedisEnvelope e) {                    // received via MessageListener
    if (isUserConnectedLocally(e.userId())) {
      local.convertAndSendToUser(e.userId().toString(), e.destination(), e.body());
    }
  }
}
```

### 9.5 Heartbeats

STOMP `heart-beat: 10000,10000`. Mobile WebSockets are killed aggressively by OS on background — clients reconnect on resume with replay from last-seen message via `GET /api/chat?since=<timestamp>`.

---

## 10. Similarity Engine — Detailed Algorithm

### 10.1 Input

For each user: set of extracted keywords from all interests, represented as a **sparse TF-IDF vector** over a global vocabulary maintained per-event (to keep IDF locally meaningful).

### 10.2 Keyword Extraction Pipeline

1. **Ingest**: Text (raw), PDF (via PDFBox), URL (fetch; arXiv API returns abstract + categories).
2. **Normalise**: Lowercase, Unicode NFC, strip punctuation, tokenise (OpenNLP English tokeniser).
3. **Stop-word removal**: Standard English + domain-specific list.
4. **Lemmatise**: OpenNLP lemmatizer with a pre-trained model.
5. **Candidate extraction**: RAKE or YAKE — both work on frequency × co-occurrence without needing training.
6. **Filter**: Keep top 30 candidates per document; dedupe across user's interests.
7. **Persist**: `interests.extracted_keywords` (array) + `interests.keyword_vector` (JSONB TF map).

### 10.3 Similarity Computation

```
For each active event E with recomputation due:
  1. Build event vocabulary V_E  =  union of keywords from participants in E.
  2. Compute IDF_E(k) = log(N / df_E(k))  where N = |participants|.
  3. For each participant u, form vector v_u where
        v_u[k] = tf_u(k) * IDF_E(k)
  4. Pair candidates via geospatial filter:
        SQL ST_DWithin(..., selected_radius)
     (only candidates within each user's selected radius).
  5. For each candidate pair (u, v):
        sim(u, v) = cosine(v_u, v_v)
     Store common keywords = top-5 dimensions contributing to the dot product.
  6. For each user u, keep top-N (config N=10) candidates with sim >= THRESHOLD.
  7. Persist to similarity_matches (upsert).
  8. For any new pair where both appear in each other's top-N:
        mark mutual = true
        publish MatchDetectedEvent(u, v, commonKeywords)
  9. Cache per-user top-N in Redis with 5 min TTL.
```

### 10.4 Complexity

For `P` participants per event, naïve all-pairs is `O(P²)`. With spatial pre-filter, expected candidates per user is `O(k)` where `k` ≪ `P` at small radii. Dominant cost becomes the database round-trip — mitigated by batching: one SQL call returns all pairs for the event, then similarity runs in-memory.

For 1000 participants per event at 50m radius (req N-04): typical `k ~ 30-50`, total pair work `~30,000`, < 500 ms on a single core.

### 10.5 Implementation Sketch

```java
@Service
@RequiredArgsConstructor
public class MatchingService {

  private final ParticipationRepository participations;
  private final InterestRepository interests;
  private final SimilarityMatchRepository matches;
  private final RedisTemplate<String, String> redis;
  private final ApplicationEventPublisher publisher;

  public void recomputeEvent(UUID eventId) {
    var activeUsers = participations.findActiveWithPosition(eventId, Duration.ofMinutes(5));
    if (activeUsers.size() < 2) return;

    var vocab = buildVocabulary(eventId, activeUsers);
    var vectors = buildTfIdfVectors(eventId, activeUsers, vocab);
    var pairs = participations.findPairsWithinRadius(eventId);   // single SQL call

    Map<UUID, PriorityQueue<Candidate>> topN = new HashMap<>();
    for (var pair : pairs) {
      double sim = cosine(vectors.get(pair.a()), vectors.get(pair.b()));
      if (sim < threshold) continue;
      List<String> common = topContributingKeywords(vectors, pair, 5);
      record(topN, pair.a(), new Candidate(pair.b(), sim, common));
      record(topN, pair.b(), new Candidate(pair.a(), sim, common));
    }

    persistAndNotify(eventId, topN);
  }
  // ...
}
```

### 10.6 Alternative / Future: Embeddings

When scale or quality demands it, replace TF-IDF with sentence embeddings (e.g., `all-MiniLM-L6-v2` via DJL or an external inference service). Store 384-dim vectors per user; use `pgvector` for approximate nearest-neighbour search. This swap is interface-compatible with the current `SimilarityEngine` port.

---

## 11. Authentication, Authorization & Security

### 11.1 Authentication Flow

```
Registration:
  POST /api/auth/register {email}
       → user row (email_verified=false) + verification token (stored hashed)
       → send email with /verify?token=...
  GET  /api/auth/verify?token=...    → email_verified=true
  POST /api/auth/complete {firstName, lastName, title, institution, password}
       → password_hash=BCrypt(12)
       → returns {accessToken, refreshToken, userId}

Login:
  POST /api/auth/login {email, password}
       → {accessToken, refreshToken}

Refresh:
  POST /api/auth/refresh {refresh}
       → {accessToken (new), refreshToken (rotated)}

Logout:
  POST /api/auth/logout {refresh}
       → refresh token revoked
```

### 11.2 JWT Details

- Algorithm: RS256 (asymmetric) — public key exposed via JWKS (`/.well-known/jwks.json`) for future microservice expansion.
- Claims: `sub = userId`, `iss`, `aud`, `iat`, `exp`, `jti`, `scope`.
- Access TTL: 15 minutes. Refresh TTL: 30 days. Refresh rotated on every use; old `jti` blacklisted in `refresh_tokens.revoked = true`.
- Clock skew tolerance: 30 s.

### 11.3 Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationConverter jwtConverter;

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    return http
      .csrf(csrf -> csrf.disable())                               // stateless JWT
      .cors(Customizer.withDefaults())
      .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
      .authorizeHttpRequests(a -> a
        .requestMatchers("/api/auth/**", "/actuator/health", "/.well-known/**").permitAll()
        .requestMatchers("/ws/**").permitAll()                    // auth handled in STOMP interceptor
        .anyRequest().authenticated())
      .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)))
      .headers(h -> h
        .contentSecurityPolicy(c -> c.policyDirectives(
          "default-src 'self'; frame-ancestors 'self'; object-src 'none'"))
        .referrerPolicy(r -> r.policy(SAME_ORIGIN)))
      .build();
  }

  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
}
```

### 11.4 Transport Security

- TLS 1.3 only (config at LB).
- HSTS: `max-age=31536000; includeSubDomains; preload`.
- Certificate pinning in Flutter via `dio`'s `HttpClient` with a custom `badCertificateCallback` (dev) or `CertificatePinner`-style check (prod) against the production leaf + issuer SPKI.

### 11.5 Secrets & Key Management

- JWT signing key: stored in KMS (AWS KMS / Vault); loaded at boot into memory only.
- SNS credentials: environment variables injected by Kubernetes Secret.
- Database credentials: rotated monthly via Vault dynamic credentials.
- Encryption-at-rest for SNS tokens: AES-256-GCM with key from KMS; IV per token; stored alongside ciphertext.

### 11.6 Input Validation

- Bean Validation annotations on all DTOs (`@NotBlank`, `@Email`, `@Size`, `@Pattern`).
- File uploads: max 10 MB, MIME sniffing with Apache Tika, reject anything not `application/pdf`, `text/plain`.
- QR payload signed with HMAC (`qr_code_hash` = HMAC-SHA256(eventName|venue|expirationCode, serverSecret)); verified on join.

### 11.7 Privacy / GDPR

- **Right to be forgotten**: `DELETE /api/users/me` soft-deletes (sets `deleted_at`), then a scheduled job hard-deletes after 30 days. Chat messages: on hard delete, `from_user_id`/`to_user_id` set to NULL and content cleared, but metadata preserved for audit.
- **Export**: `GET /api/users/me/export` returns a ZIP with all user data (profile, interests, matches, chats as JSON) — generated asynchronously.
- **Data minimisation**: GPS only transmitted during active event with user permission; older than 5 min is discarded.
- **Consent log**: `audit_log` records every consent event (SNS opt-in, local-storage opt-in).

---

## 12. SNS Integration (Optional Module)

### 12.1 OAuth2 Flow (Facebook & LinkedIn)

```
1. User taps "Link Facebook" in React UI.
2. React calls bridge.call('sns.login', {provider:'facebook'}).
3. Flutter uses flutter_facebook_auth (Facebook) or opens a WebView to LinkedIn's /authorization endpoint
   with redirect_uri = https://api.conference-tool.example/api/sns/callback/linkedin.
4. User authorises → provider redirects with authorization code.
5. Flutter captures the code and calls POST /api/sns/callback {provider, code, state}.
6. Backend exchanges code for access_token + refresh_token (Spring Security OAuth2 Client).
7. Backend fetches profile (/me for FB; /v2/me + /v2/emailAddress for LinkedIn), encrypts tokens, stores in sns_links.
8. Backend enqueues SnsEnrichJob to fetch additional data (profile picture, positions, skills for LinkedIn).
9. Enriched fields are merged into the user's profile (only fields the user has left blank, or user confirms override).
```

### 12.2 Backend Components

```java
public interface SnsClient {
  SnsProfile fetchProfile(String accessToken);
  String refreshAccessToken(String refreshToken);
  SnsProvider provider();
}

@Component class FacebookSnsClient implements SnsClient { ... }
@Component class LinkedInSnsClient implements SnsClient { ... }

@Service
@RequiredArgsConstructor
public class SnsLinkService {
  private final SnsLinkRepository repo;
  private final List<SnsClient> clients;
  private final TokenCrypto crypto;

  public void linkFromCode(UUID userId, SnsProvider provider, String code, String state) {
    TokenBundle tokens = clients.stream().filter(c -> c.provider() == provider).findFirst()
        .orElseThrow().exchange(code);
    SnsLink link = SnsLink.create(userId, provider,
        crypto.encrypt(tokens.accessToken()),
        crypto.encrypt(tokens.refreshToken()),
        tokens.expiresAt());
    repo.save(link);
    enrichmentQueue.publish(new SnsEnrichmentMessage(link.id()));
  }
}
```

### 12.3 Scopes

| Provider | Scope | Reason |
|---|---|---|
| Facebook | `public_profile`, `email` | Profile picture, display name, email (verification). |
| LinkedIn | `r_liteprofile`, `r_emailaddress` | Name, headline, profile picture. Extended `r_fullprofile` **not** requested (requires partnership). |

All scopes are documented in the consent screen shown before OAuth is initiated.

### 12.4 Token Encryption

AES-256-GCM. Format stored in `access_token_enc`: `[12-byte IV][ciphertext][16-byte GCM tag]`. Key from KMS, cached in memory for 1 h, then re-fetched. Rotating keys supported: each ciphertext prefixed with a 4-byte key-version id.

### 12.5 Unlinking

`DELETE /api/sns/{provider}`:
1. Revoke token at provider (best-effort — LinkedIn/FB revocation endpoint).
2. Hard-delete `sns_links` row.
3. Audit log entry.
4. Imported data (fields in profile) is kept unless user requests removal.

---

## 13. Push Notifications

### 13.1 Dispatch Pipeline

```
MatchDetectedEvent → PushDispatcher → queue:push (Redis Stream)
                                       ↓
                                 PushWorker (3 instances)
                                       ↓
                   ┌───────────────────┴──────────────────┐
                   ▼                                      ▼
                FCM Admin SDK                      APNs HTTP/2 (h2)
```

### 13.2 Payload Contract

```json
{
  "title": "New match: Dr. Alice Smith",
  "body": "You share: nlp, graph-neural-networks, transformers",
  "data": {
    "type": "MATCH",
    "eventId": "c5a8…",
    "otherUserId": "f3e2…",
    "route": "/events/c5a8…/vicinity"
  }
}
```

### 13.3 Delivery Guarantees

- At-least-once delivery via Redis Streams consumer group + `XACK` on success.
- Dead-letter after 5 failures.
- On APNs 410 (unregistered), mark `device_tokens` row for deletion.

### 13.4 Opt-out

User preference `push.matches`, `push.chat` in profile. Workers skip dispatch if user has opted out for the given type.

---

## 14. API Contracts

### 14.1 OpenAPI Snapshot (Core Endpoints)

```yaml
openapi: 3.1.0
info: { title: Conference Tool API, version: 1.0 }
paths:
  /api/auth/register:
    post:
      summary: Start registration (email only)
      requestBody:
        content: { application/json: { schema: { $ref: '#/components/schemas/RegisterRequest' } } }
      responses:
        '202': { description: Verification email sent }
        '409': { description: Email already registered }

  /api/auth/complete:
    post:
      summary: Complete registration after verification
      security: [ bearerAuth: [] ]
      requestBody: { content: { application/json: { schema: { $ref: '#/components/schemas/CompleteRegistrationRequest' } } } }
      responses:
        '200': { content: { application/json: { schema: { $ref: '#/components/schemas/AuthTokens' } } } }

  /api/profile:
    get:
      security: [ bearerAuth: [] ]
      responses: { '200': { content: { application/json: { schema: { $ref: '#/components/schemas/Profile' } } } } }
    put:
      security: [ bearerAuth: [] ]
      requestBody: { content: { application/json: { schema: { $ref: '#/components/schemas/ProfileUpdate' } } } }
      responses: { '200': { content: { application/json: { schema: { $ref: '#/components/schemas/Profile' } } } } }

  /api/interests:
    post:
      security: [ bearerAuth: [] ]
      requestBody:
        content:
          application/json: { schema: { $ref: '#/components/schemas/InterestCreate' } }
          multipart/form-data: { schema: { $ref: '#/components/schemas/InterestArticleUpload' } }
      responses: { '201': { content: { application/json: { schema: { $ref: '#/components/schemas/Interest' } } } } }

  /api/events/join:
    post:
      security: [ bearerAuth: [] ]
      requestBody: { content: { application/json: { schema: { $ref: '#/components/schemas/JoinEventRequest' } } } }
      responses:
        '200': { content: { application/json: { schema: { $ref: '#/components/schemas/JoinEventResponse' } } } }
        '400': { description: Event expired or tampered }

  /api/events/{eventId}/vicinity:
    get:
      security: [ bearerAuth: [] ]
      parameters:
        - { name: eventId, in: path, required: true, schema: { type: string, format: uuid } }
        - { name: radius, in: query, required: true, schema: { type: integer, enum: [20,50,100] } }
      responses: { '200': { content: { application/json: { schema: { $ref: '#/components/schemas/VicinityResponse' } } } } }

  /api/events/{eventId}/location:
    post:
      security: [ bearerAuth: [] ]
      requestBody: { content: { application/json: { schema: { $ref: '#/components/schemas/LocationUpdate' } } } }
      responses: { '204': { description: Recorded } }

  /api/chat/{eventId}/{otherUserId}:
    get:
      security: [ bearerAuth: [] ]
      parameters:
        - { name: since, in: query, schema: { type: string, format: date-time } }
      responses: { '200': { content: { application/json: { schema: { $ref: '#/components/schemas/ChatHistory' } } } } }

  /api/sns/link:
    post: { summary: Begin OAuth flow, returns auth URL }
  /api/sns/callback:
    post: { summary: Exchange code, store link }
  /api/sns/{provider}:
    delete: { summary: Remove SNS link }

  /api/devices/register:
    post: { summary: Register FCM/APNs device token }

components:
  securitySchemes:
    bearerAuth: { type: http, scheme: bearer, bearerFormat: JWT }
  schemas:
    JoinEventRequest:
      type: object
      required: [eventId, expirationCode]
      properties:
        eventId:         { type: string, format: uuid }
        expirationCode:  { type: string, format: date-time }
    VicinityResponse:
      type: object
      properties:
        radius: { type: integer }
        matches:
          type: array
          items:
            type: object
            properties:
              matchId:          { type: string, format: uuid }
              otherUserId:      { type: string, format: uuid }
              name:             { type: string }
              title:            { type: string, nullable: true }
              institution:      { type: string, nullable: true }
              profilePictureUrl: { type: string, nullable: true }
              commonKeywords:   { type: array, items: { type: string } }
              similarity:       { type: number, format: float }
              mutual:           { type: boolean }
    # ... other schemas omitted for brevity
```

### 14.2 WebSocket Destinations

| Destination (client → server) | Payload |
|---|---|
| `/app/chat.send` | `{eventId, toUserId, content}` |
| `/app/chat.markRead` | `{eventId, messageId}` |

| Destination (server → client user queue) | Payload |
|---|---|
| `/user/queue/chat.{eventId}` | `{messageId, fromUserId, toUserId, content, createdAt}` |
| `/user/queue/matches` | `{eventId, matchId, otherUserId, commonKeywords, similarity}` |

---

## 15. Deployment, Infrastructure & DevOps

### 15.1 Runtime Topology

```
                   Internet
                      │
        ┌─────────────┴────────────┐
        │   Cloud LB (TLS 1.3)     │
        └─────┬────────────────┬───┘
              │                │
  ┌───────────▼────┐  ┌────────▼────────┐
  │  Ingress (nginx)│  │ WAF (optional) │
  └───────────┬────┘  └─────────────────┘
              │
     ┌────────┴────────────────────────────┐
     │               K8s Cluster           │
     │                                     │
     │  ns: conf                           │
     │    Deployment: web-frontend (Next.js SSR) x2
     │    Deployment: backend (Spring Boot) x3 (HPA 3..15, CPU 60%)
     │    Deployment: push-worker x2
     │    Deployment: matching-worker x2
     │    Deployment: nlp-worker x2
     │    StatefulSet: redis x3 (Sentinel)  -- or managed Redis
     │    PVC: postgres-primary             -- or managed RDS/CloudSQL
     │    CronJob: event-cleanup
     │    CronJob: sns-refresh
     └─────────────────────────────────────┘
```

### 15.2 Containerisation

Backend `Dockerfile` (multi-stage):

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew --no-daemon :app:bootJar

FROM eclipse-temurin:21-jre
RUN useradd -r -u 1000 app
WORKDIR /app
COPY --from=build /src/app/build/libs/*.jar app.jar
USER 1000
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseZGC"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
```

Web frontend `Dockerfile`:

```dockerfile
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN corepack enable && pnpm install --frozen-lockfile

FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN corepack enable && pnpm build

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public
COPY --from=build /app/package.json ./
RUN corepack enable && pnpm install --prod --frozen-lockfile
USER node
EXPOSE 3000
CMD ["pnpm","start"]
```

### 15.3 Kubernetes Essentials

```yaml
# backend-deploy.yaml (excerpt)
apiVersion: apps/v1
kind: Deployment
metadata: { name: backend, namespace: conf }
spec:
  replicas: 3
  selector: { matchLabels: { app: backend } }
  template:
    metadata: { labels: { app: backend } }
    spec:
      containers:
      - name: backend
        image: registry.example/conf-backend:1.0.0
        ports: [{containerPort: 8080}]
        env:
          - { name: DB_HOST, value: postgres.db.svc.cluster.local }
          - { name: REDIS_HOST, value: redis.cache.svc.cluster.local }
          - { name: JWT_SECRET, valueFrom: { secretKeyRef: { name: backend-secrets, key: jwt }}}
        readinessProbe: { httpGet: { path: /actuator/health/readiness, port: 8080 }, periodSeconds: 5 }
        livenessProbe:  { httpGet: { path: /actuator/health/liveness,  port: 8080 }, periodSeconds: 10 }
        resources:
          requests: { cpu: "500m", memory: "1Gi" }
          limits:   { cpu: "2",    memory: "2Gi" }
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata: { name: backend-hpa, namespace: conf }
spec:
  scaleTargetRef: { apiVersion: apps/v1, kind: Deployment, name: backend }
  minReplicas: 3
  maxReplicas: 15
  metrics:
    - type: Resource
      resource: { name: cpu, target: { type: Utilization, averageUtilization: 60 } }
```

### 15.4 CI/CD Pipelines (GitHub Actions)

Per-component pipeline:

| Stage | Backend | Web | Flutter |
|---|---|---|---|
| Lint | Spotless + Checkstyle | ESLint + Prettier | `flutter analyze` |
| Test | JUnit + Testcontainers | Vitest + Playwright | `flutter test` + integration tests |
| Security scan | OWASP dep-check, Trivy | npm audit, Trivy | Dart audit |
| Build artefact | Docker image | Docker image | APK/IPA/AAB |
| Push | GHCR | GHCR | App Store / Play Store (fastlane) |
| Deploy | Helm upgrade on `main` to staging; tag `v*` to prod | Same | TestFlight / internal track |

### 15.5 Environments

| Env | Purpose | Data |
|---|---|---|
| local | Developer laptops | docker-compose (PG + Redis + MinIO + MailHog) |
| dev | Integration | ephemeral, reset nightly |
| staging | Pre-prod | sanitised prod-like data |
| prod | Live | real users |

### 15.6 Feature Flags

Unleash (self-hosted) or LaunchDarkly. Flags: `feature.sns.enabled`, `feature.sns.linkedin`, `feature.embedding-similarity` — allow rolling out the SNS integration and future improvements without redeploy.

---

## 16. Observability

### 16.1 Metrics

- Micrometer → Prometheus.
- Dashboards (Grafana):
  - REST latency p50/p95/p99 per endpoint.
  - WebSocket connected sessions, messages/s.
  - Match recomputation duration per event.
  - Redis command latency.
  - Postgres query latency (via `pg_stat_statements`).
  - JVM heap, GC pauses (ZGC).

### 16.2 Tracing

OpenTelemetry SDK, W3C trace context propagated through HTTP and STOMP headers. Exported to Tempo/Jaeger. Each `MessageMapping` and each background job creates a span.

### 16.3 Logging

- Structured JSON via Logback `logstash-logback-encoder`.
- Correlation ID (`X-Request-Id`) added by Nginx, echoed in all logs and response headers.
- PII-scrubbing filter: emails masked (`a***@example.com`), GPS redacted.
- Shipped to Loki or ELK.

### 16.4 Alerts

| Alert | Condition | Severity |
|---|---|---|
| API error rate | 5xx > 2% for 5 min | P1 |
| WS reconnect storm | reconnects/s > 10× baseline | P2 |
| Redis memory > 80% | sustained 10 min | P2 |
| PG replication lag > 30 s | | P2 |
| Match recompute > 30 s | per-event | P3 |
| JWT signing errors | any | P1 |
| FCM/APNs error rate > 5% | | P3 |

---

## 17. Testing Strategy

### 17.1 Test Pyramid

| Layer | Tool | Target |
|---|---|---|
| Unit | JUnit 5, Mockito (BE); Vitest (Web); Dart test (Mobile) | 80% line coverage on domain/services |
| Integration | Testcontainers (PostGIS + Redis), Spring Boot slice tests | Repository + use-case paths |
| Contract | Spring Cloud Contract / Pact | REST + WS contracts between web and BE |
| E2E (web) | Playwright against staging | Critical flows |
| E2E (mobile) | Flutter integration_test on simulators/devices | Cold start → join event → chat |
| Load | k6, Gatling | Chat (1k concurrent), matching recompute |
| Security | OWASP ZAP, dep-check | Pre-release |

### 17.2 Example Integration Test

```java
@SpringBootTest
@Testcontainers
class JoinEventUseCaseIT {
  @Container static PostgreSQLContainer<?> pg =
      new PostgreSQLContainer<>("postgis/postgis:15-3.4");
  @Container static GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", pg::getJdbcUrl);
    r.add("spring.datasource.username", pg::getUsername);
    r.add("spring.datasource.password", pg::getPassword);
    r.add("spring.data.redis.host", redis::getHost);
    r.add("spring.data.redis.port", redis::getFirstMappedPort);
  }

  @Autowired JoinEventUseCase useCase;
  @Autowired EventRepository events;

  @Test
  void joinsValidFutureEvent() {
    var e = events.save(Event.of("Conf 2026", "Venue", Instant.now().plus(1, DAYS)));
    var r = useCase.execute(UUID.randomUUID(), e.getEventId(), e.getExpirationCode());
    assertThat(r.participation().getEventId()).isEqualTo(e.getEventId());
  }
}
```

### 17.3 Load Targets

- 1000 concurrent WebSocket sessions per backend pod, 10 msg/s each → chat median latency < 200 ms.
- Matching recomputation for event with 1000 participants → < 5 s end-to-end.
- 500 RPS on `/api/events/{id}/vicinity` p95 < 300 ms.

---

## 18. Non-Functional Engineering

### 18.1 Scalability

- **Backend**: stateless, HPA on CPU + request rate. Redis handles session stickiness — no affinity required at LB.
- **Database**: primary + read replica. Read-heavy queries (vicinity, chat history) route to replica via Spring `@Transactional(readOnly = true)` + `LazyConnectionDataSourceProxy`.
- **Hot event problem**: one event with 1000 participants could hammer a single PG row set. Mitigate by sharding cache keys per event on Redis Cluster.

### 18.2 Availability (99.5%)

- Multi-AZ deployment for backend pods, Redis Sentinel, PG primary+standby.
- Graceful shutdown: SIGTERM → stop accepting new requests → drain WebSockets over 30 s → exit.
- Zero-downtime deploys via rolling updates with `maxUnavailable: 0`.

### 18.3 Latency Budget

| Segment | Budget |
|---|---|
| Client → LB | 50 ms |
| LB → backend | 5 ms |
| Backend processing | 100 ms (REST) / 20 ms (WS) |
| DB query | 30 ms |
| Redis op | 2 ms |
| Network back | 50 ms |
| Total (REST) | ~240 ms |

### 18.4 Energy Efficiency (mobile)

- GPS at 30 s / 10 m minimum (requirement N-07).
- Doze-mode compliance on Android via `ForegroundService` only during active event.
- Suspend GPS when battery < 15% (Flutter `battery_plus`).

### 18.5 Offline Capability

- Flutter stores last vicinity list in Isar; React app reads via bridge on load if REST fails.
- Outgoing chat messages buffered locally; replayed on reconnect with client-generated `messageId` (UUID) to dedupe on server.

### 18.6 Accessibility Audit

- Lighthouse a11y score target ≥ 95 on all web pages.
- Flutter: `flutter_test`'s `SemanticsTester` covers critical widgets.

---

## 19. Project Structure (Monorepo)

```
conference-tool/
├── backend/                  # Java Spring Boot
├── web/                      # Next.js
├── mobile/                   # Flutter
├── infra/
│   ├── helm/
│   │   ├── backend/
│   │   ├── web/
│   │   └── workers/
│   ├── terraform/            # cloud resources (VPC, RDS, Redis, S3, DNS)
│   └── docker-compose.dev.yml
├── docs/
│   ├── requirements-v1.3.md
│   ├── SNS-system.md         # this document
│   └── architecture/
│       └── decisions/        # ADRs
├── .github/
│   └── workflows/
└── README.md
```

---

## 20. Implementation Roadmap

### Milestone 1 — Foundations (Weeks 1–3)
- Repos, CI/CD skeletons for all three apps.
- Backend: Spring Boot bootstrap, Flyway schema V1-V4, JWT auth, `/api/auth/**`, `/api/profile`.
- Web: Next.js scaffold, bridge client, auth screens.
- Mobile: Flutter shell, WebView, bridge, secure storage.
- Local docker-compose environment runs end-to-end.

### Milestone 2 — Event & Matching Core (Weeks 4–7)
- Events (create, QR generation, join with HMAC validation).
- Participations, location ingest, PostGIS queries.
- Interest capture (text, file upload to S3, arXiv link).
- Keyword extraction pipeline (OpenNLP + RAKE).
- Matching service + Redis caching + `@Scheduled` job.
- Web: interests UI, event join, vicinity list.
- Mobile: QR scanner, GPS service.

### Milestone 3 — Real-time & Push (Weeks 8–10)
- Spring WebSocket + STOMP + Redis Pub/Sub fan-out.
- Chat REST history + WS live.
- FCM + APNs integration; device token registration; match notifications.
- Web: chat UI.
- Mobile: push handling, deep links.

### Milestone 4 — Optional SNS + Hardening (Weeks 11–13)
- OAuth2 Client config for FB + LinkedIn.
- SNS link / unlink / enrichment jobs.
- Local storage of matches in Isar (opt-in).
- GDPR endpoints (export, delete).
- Security scans (ZAP, dep-check).
- Load tests to req N-03, N-04.

### Milestone 5 — Pilot & GA (Weeks 14–16)
- Staging pilot with real conference (50 users).
- Observability dashboards & alerts finalised.
- On-call runbooks.
- Accessibility & a11y audit.
- Store submission (Play Store + App Store).
- Documentation handover.

---

## Appendix A — Glossary (Implementation Terms)

| Term | Meaning |
|---|---|
| BFF | Backend for Frontend — thin Next.js server routes if needed. |
| HPA | Kubernetes Horizontal Pod Autoscaler. |
| JWKS | JSON Web Key Set. |
| PII | Personally identifiable information. |
| RAKE | Rapid Automatic Keyword Extraction. |
| STOMP | Simple Text Oriented Messaging Protocol. |
| TF-IDF | Term Frequency – Inverse Document Frequency. |
| ZGC | Z Garbage Collector (Java). |

---

## Appendix B — Key Architectural Decisions (ADR Summary)

| # | Decision | Why |
|---|---|---|
| ADR-001 | Modular monolith over microservices | Single team, simpler ops, later extraction possible. |
| ADR-002 | PostgreSQL + PostGIS over MongoDB + geo | Spatial correctness, joins with participations, mature stack. |
| ADR-003 | Redis for both cache and queue | One infra component; Streams + Redisson sufficient for job scale. |
| ADR-004 | JWT (RS256) over session cookies | Stateless backend, easy WS auth, mobile-friendly. |
| ADR-005 | Next.js inside Flutter WebView | Re-use web talent, single UI codebase, Flutter for native bridge only. |
| ADR-006 | TF-IDF now, embeddings later | Zero-infra keyword similarity; swap via interface when justified. |
| ADR-007 | Redis Pub/Sub relay instead of RabbitMQ STOMP broker | Avoid operating a second broker at current scale. |

---

**End of Document**
