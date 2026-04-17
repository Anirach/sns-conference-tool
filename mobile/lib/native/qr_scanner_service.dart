import 'dart:async';
import 'dart:developer' as developer;

import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

/// Opens a full-screen QR scanner and returns the decoded event code. The
/// scanner is a modal over the WebView — when a barcode is detected it closes
/// and the returned value is delivered to the web bridge caller.
class QrScannerService {
  QrScannerService(this._navigatorKey);

  final GlobalKey<NavigatorState> _navigatorKey;

  Future<Map<String, Object?>> scanOnce({Duration timeout = const Duration(seconds: 30)}) async {
    final navigator = _navigatorKey.currentState;
    if (navigator == null) {
      return {'error': 'no-navigator'};
    }

    final completer = Completer<String?>();
    final timer = Timer(timeout, () {
      if (!completer.isCompleted) completer.complete(null);
    });

    await navigator.push<void>(
      MaterialPageRoute<void>(
        fullscreenDialog: true,
        builder: (context) => _ScannerPage(
          onScan: (value) {
            if (!completer.isCompleted) completer.complete(value);
            Navigator.of(context).pop();
          },
        ),
      ),
    );
    timer.cancel();

    final code = await completer.future;
    if (code == null) {
      developer.log('qr.scan cancelled or timed out', name: 'QrScannerService');
      return {'eventCode': null, 'cancelled': true};
    }
    return {'eventCode': code.trim()};
  }
}

class _ScannerPage extends StatelessWidget {
  const _ScannerPage({required this.onScan});

  final void Function(String code) onScan;

  @override
  Widget build(BuildContext context) {
    final controller = MobileScannerController(
      detectionSpeed: DetectionSpeed.normal,
      facing: CameraFacing.back,
    );

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(title: const Text('Scan cipher')),
      body: MobileScanner(
        controller: controller,
        onDetect: (capture) {
          final value = capture.barcodes.firstOrNull?.rawValue;
          if (value != null && value.isNotEmpty) {
            onScan(value);
          }
        },
      ),
    );
  }
}

extension on List<Barcode> {
  Barcode? get firstOrNull => isEmpty ? null : first;
}
