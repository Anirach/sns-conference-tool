import 'dart:convert';
import 'dart:developer' as developer;
import 'dart:io' show Platform;

import 'package:webview_flutter/webview_flutter.dart';

import '../core/config/app_config.dart';
import '../native/file_picker_service.dart';
import '../native/location_service.dart';
import '../native/push_service.dart';
import '../native/qr_scanner_service.dart';
import '../native/secure_storage_service.dart';
import '../native/sns_auth_service.dart';
import '../storage/isar_db.dart';
import 'bridge_messages.dart';

class JsBridge {
  JsBridge({
    required SecureStorageService storage,
    required LocationService location,
    required QrScannerService qr,
    required FilePickerService files,
    required SnsAuthService sns,
    required PushService push,
    required IsarDb localDb,
  })  : _storage = storage,
        _location = location,
        _qr = qr,
        _files = files,
        _sns = sns,
        _push = push,
        _localDb = localDb;

  final SecureStorageService _storage;
  final LocationService _location;
  final QrScannerService _qr;
  final FilePickerService _files;
  final SnsAuthService _sns;
  final PushService _push;
  final IsarDb _localDb;

  WebViewController? _controller;

  void attach(WebViewController controller) {
    _controller = controller;
    _push.onForegroundMessage((msg) {
      sendEvent(BridgeMessageTypes.pushReceived, {
        'data': msg.data,
        'title': msg.notification?.title,
        'body': msg.notification?.body,
      });
    });
  }

  Future<void> injectBootstrap(WebViewController controller) async {
    await controller.runJavaScript('''
      window.__bridgeResolve = window.__bridgeResolve || {};
      window.dispatchEvent(new CustomEvent('flutter-bridge-ready'));
    ''');
  }

  Future<void> handleIncoming(String raw) async {
    String id = '';
    try {
      final decoded = jsonDecode(raw) as Map<String, dynamic>;
      id = decoded['id'] as String;
      final type = decoded['type'] as String;
      final payload = (decoded['payload'] as Map?)?.cast<String, Object?>() ?? const {};
      final result = await _dispatch(type, payload);
      _reply(id, {'ok': true, 'data': result});
    } catch (e, st) {
      developer.log('Bridge error: $e', name: 'JsBridge', error: e, stackTrace: st);
      if (id.isNotEmpty) {
        _reply(id, {'ok': false, 'error': e.toString()});
      }
    }
  }

  Future<Object?> _dispatch(String type, Map<String, Object?> p) async {
    switch (type) {
      case BridgeMessageTypes.gpsStart:
        return _location.start(
          intervalSec: (p['intervalSec'] as num?)?.toInt() ?? 30,
          minMoveMeters: (p['minMoveMeters'] as num?)?.toDouble() ?? 10,
          onFix: (fix) => sendEvent('gps.fix', {
            'lat': fix.latitude,
            'lon': fix.longitude,
            'accuracyMeters': fix.accuracy,
            'timestampMs': fix.timestamp.millisecondsSinceEpoch,
          }),
        );
      case BridgeMessageTypes.gpsStop:
        return _location.stop();
      case BridgeMessageTypes.qrScan:
        return _qr.scanOnce();
      case BridgeMessageTypes.filePickArticle:
        return _files.pickArticle();
      case BridgeMessageTypes.storageGet:
        return _storage.read(p['key'] as String);
      case BridgeMessageTypes.storageSet:
        await _storage.write(p['key'] as String, p['value'] as String);
        return {'ok': true};
      case BridgeMessageTypes.storageDelete:
        await _storage.delete(p['key'] as String);
        return {'ok': true};
      case BridgeMessageTypes.localdbMatchesList:
        return _localDb.listMatches();
      case BridgeMessageTypes.localdbMatchesSave:
        await _localDb.saveMatch(p);
        return {'ok': true};
      case BridgeMessageTypes.snsLogin:
        return _sns.login((p['provider'] as String?) ?? 'facebook');
      case BridgeMessageTypes.pushRequestPermission:
        return _push.requestPermission();
      case BridgeMessageTypes.pushToken:
        return _push.token();
      case BridgeMessageTypes.appInfo:
        return {
          'version': AppConfig.appVersion,
          'platform': Platform.isAndroid ? 'android' : (Platform.isIOS ? 'ios' : 'web'),
          'deviceModel': Platform.operatingSystemVersion,
        };
      default:
        throw UnsupportedError('Unknown bridge type: $type');
    }
  }

  void _reply(String id, Map<String, Object?> body) {
    final js = 'window.__bridgeResolve[${jsonEncode(id)}] && window.__bridgeResolve[${jsonEncode(id)}](${jsonEncode(body)});';
    _controller?.runJavaScript(js);
  }

  /// Server-initiated message to the web layer (e.g. push arrived, connectivity change).
  void sendEvent(String type, Map<String, Object?> payload) {
    final body = jsonEncode({'type': type, 'payload': payload});
    _controller?.runJavaScript(
      'window.dispatchEvent(new CustomEvent("flutter-bridge-event", { detail: $body }));',
    );
  }
}
