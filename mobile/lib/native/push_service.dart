import 'dart:async';
import 'dart:developer' as developer;
import 'dart:io' show Platform;

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

/// Handles FCM permission prompts, token acquisition, and foreground message delivery.
///
/// Project-level Firebase config (`google-services.json` on Android, `GoogleService-Info.plist`
/// on iOS) must be provisioned by DevOps before first run. Until it is, [initialize] returns
/// `configured: false` and the rest of the methods return `null`.
class PushService {
  static bool _firebaseReady = false;
  void Function(RemoteMessage message)? _onForegroundMessage;

  Future<Map<String, Object?>> initialize() async {
    if (_firebaseReady) return {'configured': true};
    try {
      await Firebase.initializeApp();
      _firebaseReady = true;
      FirebaseMessaging.onMessage.listen((msg) {
        developer.log('fcm.onMessage ${msg.messageId}', name: 'PushService');
        _onForegroundMessage?.call(msg);
      });
      return {'configured': true};
    } catch (e) {
      developer.log('Firebase init failed (missing config?): $e', name: 'PushService');
      return {'configured': false, 'error': e.toString()};
    }
  }

  Future<Map<String, Object?>> requestPermission() async {
    await initialize();
    if (!_firebaseReady) return {'granted': false};
    final settings = await FirebaseMessaging.instance.requestPermission();
    final granted = settings.authorizationStatus == AuthorizationStatus.authorized ||
        settings.authorizationStatus == AuthorizationStatus.provisional;
    return {'granted': granted, 'status': settings.authorizationStatus.name};
  }

  Future<Map<String, Object?>> token() async {
    await initialize();
    if (!_firebaseReady) return {'token': null, 'platform': _platform()};
    String? fcmToken;
    try {
      if (Platform.isIOS) {
        await FirebaseMessaging.instance.getAPNSToken();
      }
      fcmToken = await FirebaseMessaging.instance.getToken();
    } catch (e) {
      developer.log('getToken failed: $e', name: 'PushService');
    }
    return {'token': fcmToken, 'platform': _platform()};
  }

  void onForegroundMessage(void Function(RemoteMessage) handler) {
    _onForegroundMessage = handler;
  }

  String _platform() {
    if (Platform.isAndroid) return 'ANDROID';
    if (Platform.isIOS) return 'IOS';
    return 'WEB';
  }
}
