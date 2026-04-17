import 'dart:async';

/// Pass 1 stub. Real implementation in pass 2 uses `mobile_scanner`.
class QrScannerService {
  Future<Map<String, Object?>> scanOnce() async {
    // Simulate a brief scan delay for realism.
    await Future<void>.delayed(const Duration(milliseconds: 400));
    return {
      'eventCode': 'NEURIPS2026',
    };
  }
}
