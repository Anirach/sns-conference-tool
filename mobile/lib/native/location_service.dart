import 'dart:developer' as developer;

/// Pass 1 stub. Real implementation in pass 2 uses `geolocator`
/// per docs/SNS-system.md §4.6.
class LocationService {
  bool _active = false;

  Future<Map<String, Object?>> start({int intervalSec = 30, double minMoveMeters = 10}) async {
    _active = true;
    developer.log(
      'gps.start (stub) intervalSec=$intervalSec minMoveMeters=$minMoveMeters',
      name: 'LocationService',
    );
    return {'started': true};
  }

  Future<Map<String, Object?>> stop() async {
    _active = false;
    developer.log('gps.stop (stub)', name: 'LocationService');
    return {'stopped': true};
  }

  bool get isActive => _active;
}
