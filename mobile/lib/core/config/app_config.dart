import 'dart:io' show Platform;

class AppConfig {
  const AppConfig._();

  static const String _overrideOrigin = String.fromEnvironment('FRONTEND_ORIGIN');

  static String get frontendOrigin {
    if (_overrideOrigin.isNotEmpty) return _overrideOrigin;
    if (Platform.isAndroid) return 'http://10.0.2.2:3000';
    return 'http://localhost:3000';
  }

  static const String appName = 'SNS Conference Tool';
  static const String appVersion = '0.1.0-dev';

  static bool isAllowedOrigin(String url) => url.startsWith(frontendOrigin);
}
