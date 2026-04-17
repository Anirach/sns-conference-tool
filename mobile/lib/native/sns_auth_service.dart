/// Pass 1 stub. Real implementation in pass 2 uses `flutter_facebook_auth`
/// and a LinkedIn OAuth WebView, per docs/SNS-system.md §12.
class SnsAuthService {
  Future<Map<String, Object?>> login(String provider) async {
    await Future<void>.delayed(const Duration(milliseconds: 600));
    return {
      'provider': provider,
      'accessToken':
          'mock_oauth_token_${provider}_${DateTime.now().millisecondsSinceEpoch}',
      'providerUserId': 'mock_${provider}_uid',
    };
  }
}
