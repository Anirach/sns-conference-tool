import 'dart:async';
import 'dart:developer' as developer;

import 'package:flutter_facebook_auth/flutter_facebook_auth.dart';
import 'package:url_launcher/url_launcher.dart';

/// OAuth2 login flows for Facebook and LinkedIn.
///
/// Facebook uses the native SDK via `flutter_facebook_auth` — the session token that comes back
/// is handed off to the backend `/api/sns/callback` as the {@code code} parameter (the backend
/// accepts either a real authorization code or an already-exchanged access token in dev-mode).
///
/// LinkedIn doesn't expose a native SDK; we open the authorization URL in a Chrome Custom Tab /
/// SFSafariViewController via `url_launcher` and rely on a deep-link redirect back into the app.
/// The host app is expected to register `sns://linkedin/callback` and forward the received
/// `?code=...&state=...` query into [onLinkedInRedirect].
class SnsAuthService {
  final Completer<_LinkedInResult>? _pendingLinkedIn;

  SnsAuthService() : _pendingLinkedIn = null;

  Future<Map<String, Object?>> login(String provider) async {
    final lower = provider.toLowerCase();
    if (lower == 'facebook') return _facebook();
    if (lower == 'linkedin') return _linkedIn();
    return {'error': 'unknown-provider'};
  }

  Future<Map<String, Object?>> _facebook() async {
    try {
      final LoginResult result = await FacebookAuth.instance.login(
        permissions: const ['public_profile', 'email'],
      );
      if (result.status != LoginStatus.success || result.accessToken == null) {
        return {'cancelled': true};
      }
      final token = result.accessToken!;
      return {
        'provider': 'FACEBOOK',
        'accessToken': token.tokenString,
        'providerUserId': token.userId,
      };
    } catch (e) {
      developer.log('Facebook login failed: $e', name: 'SnsAuthService');
      return {'error': e.toString()};
    }
  }

  Future<Map<String, Object?>> _linkedIn() async {
    // Host app wires the actual URL; this stub returns an error when the caller hasn't populated
    // `sns.oauth.linkedin.auth-url` into `LinkedInConfig`. Kept minimal — the real redirect
    // handler belongs in the app shell, not this service.
    final url = Uri.tryParse(LinkedInConfig.authorizeUrl);
    if (url == null) return {'error': 'linkedin-not-configured'};
    final launched = await launchUrl(url, mode: LaunchMode.externalApplication);
    if (!launched) return {'error': 'launch-failed'};
    return {'pending': true, 'provider': 'LINKEDIN'};
  }
}

/// LinkedIn authorize URL is built by the backend (`POST /api/sns/link`) and exposed back to the
/// host app through the bridge. The app sets it via [LinkedInConfig.authorizeUrl] before calling
/// {@link SnsAuthService.login}.
class LinkedInConfig {
  static String authorizeUrl = '';
}

class _LinkedInResult {
  _LinkedInResult(this.code, this.state);
  final String code;
  final String state;
}
