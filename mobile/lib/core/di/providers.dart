import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:webview_flutter/webview_flutter.dart';

import '../../bridge/js_bridge.dart';
import '../../native/file_picker_service.dart';
import '../../native/location_service.dart';
import '../../native/push_service.dart';
import '../../native/qr_scanner_service.dart';
import '../../native/secure_storage_service.dart';
import '../../native/sns_auth_service.dart';
import '../../storage/isar_db.dart';

final rootNavigatorKeyProvider =
    Provider<GlobalKey<NavigatorState>>((_) => GlobalKey<NavigatorState>());

final secureStorageProvider =
    Provider<FlutterSecureStorage>((_) => const FlutterSecureStorage());

final secureStorageServiceProvider =
    Provider<SecureStorageService>((ref) => SecureStorageService(ref.read(secureStorageProvider)));

final locationServiceProvider =
    Provider<LocationService>((_) => LocationService());

final qrScannerServiceProvider =
    Provider<QrScannerService>((ref) => QrScannerService(ref.read(rootNavigatorKeyProvider)));

final filePickerServiceProvider =
    Provider<FilePickerService>((_) => FilePickerService());

final snsAuthServiceProvider =
    Provider<SnsAuthService>((_) => SnsAuthService());

final pushServiceProvider = Provider<PushService>((_) => PushService());

final isarDbProvider = Provider<IsarDb>((_) => IsarDb());

final webviewControllerProvider =
    StateProvider<WebViewController?>((_) => null);

final jsBridgeProvider = Provider<JsBridge>((ref) {
  return JsBridge(
    storage: ref.read(secureStorageServiceProvider),
    location: ref.read(locationServiceProvider),
    qr: ref.read(qrScannerServiceProvider),
    files: ref.read(filePickerServiceProvider),
    sns: ref.read(snsAuthServiceProvider),
    push: ref.read(pushServiceProvider),
    localDb: ref.read(isarDbProvider),
  );
});
