import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';

import 'package:sns_mobile/bridge/js_bridge.dart';
import 'package:sns_mobile/bridge/bridge_messages.dart';

void main() {
  group('JsBridge dispatch', () {
    test('rejects unknown bridge type', () async {
      final bridge = JsBridge(
        storage: _NullStorage(),
        location: _NullLocation(),
        qr: _NullQr(),
        files: _NullFiles(),
        sns: _NullSns(),
        push: _NullPush(),
        localDb: _NullDb(),
      );

      String? receivedReply;
      bridge.attach(_NullController(reply: (s) => receivedReply = s));

      await bridge.handleIncoming(jsonEncode({
        'id': 'req-1',
        'type': 'unknown.type',
        'payload': const <String, Object?>{},
      }));

      expect(receivedReply, contains('"ok":false'));
      expect(receivedReply, contains('Unknown bridge type'));
    });

    test('storage.set returns ok envelope', () async {
      final bridge = JsBridge(
        storage: _RecordingStorage(),
        location: _NullLocation(),
        qr: _NullQr(),
        files: _NullFiles(),
        sns: _NullSns(),
        push: _NullPush(),
        localDb: _NullDb(),
      );

      String? reply;
      bridge.attach(_NullController(reply: (s) => reply = s));

      await bridge.handleIncoming(jsonEncode({
        'id': 'req-2',
        'type': BridgeMessageTypes.storageSet,
        'payload': {'key': 'jwt', 'value': 'abc'},
      }));

      expect(reply, contains('"ok":true'));
    });
  });
}

// ── Test doubles ──────────────────────────────────────────────────────────────

class _NullController implements WebViewControllerLike {
  _NullController({required this.reply});
  final void Function(String script) reply;
  @override
  Future<void> runJavaScript(String script) async => reply(script);
}

abstract class WebViewControllerLike {
  Future<void> runJavaScript(String script);
}

// The test doubles below intentionally throw on any unexpected call so the bridge
// router behaviour is exercised in isolation rather than depending on real plugins.

class _NullStorage extends NoSuchMethod_ implements dynamic {}
class _NullLocation extends NoSuchMethod_ implements dynamic {}
class _NullQr extends NoSuchMethod_ implements dynamic {}
class _NullFiles extends NoSuchMethod_ implements dynamic {}
class _NullSns extends NoSuchMethod_ implements dynamic {}
class _NullPush extends NoSuchMethod_ implements dynamic {
  void onForegroundMessage(void Function(dynamic) _) {}
}
class _NullDb extends NoSuchMethod_ implements dynamic {}

class _RecordingStorage implements dynamic {
  Future<void> write(String key, String value) async {}
  Future<String?> read(String key) async => null;
  Future<void> delete(String key) async {}
}

class NoSuchMethod_ {
  noSuchMethod(Invocation invocation) {
    throw UnimplementedError('${invocation.memberName} not stubbed in test');
  }
}
