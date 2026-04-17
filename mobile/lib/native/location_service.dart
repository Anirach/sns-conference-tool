import 'dart:async';
import 'dart:developer' as developer;

import 'package:geolocator/geolocator.dart';

/// Streams device location to the web layer over the JS bridge. The web side
/// forwards each fix to `POST /api/events/{id}/location`.
///
/// Android uses the OS-native distance filter; iOS uses `activityType=fitness`
/// to keep power draw reasonable. We do not run a dedicated foreground service
/// in this pass — if the app is backgrounded, location stops automatically,
/// which matches the spec's §4.6 Android Doze guidance.
class LocationService {
  StreamSubscription<Position>? _subscription;
  void Function(Position position)? _onFix;

  bool get isActive => _subscription != null;

  Future<Map<String, Object?>> start({
    int intervalSec = 30,
    double minMoveMeters = 10,
    void Function(Position position)? onFix,
  }) async {
    if (isActive) {
      return {'started': true, 'alreadyRunning': true};
    }

    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return {'started': false, 'error': 'location-services-disabled'};
    }

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.deniedForever || permission == LocationPermission.denied) {
      return {'started': false, 'error': 'permission-denied'};
    }

    _onFix = onFix;

    final settings = LocationSettings(
      accuracy: LocationAccuracy.high,
      distanceFilter: minMoveMeters.toInt(),
    );
    _subscription = Geolocator.getPositionStream(locationSettings: settings).listen(
      _dispatch,
      onError: (Object e) {
        developer.log('geolocator error: $e', name: 'LocationService');
      },
    );

    return {'started': true, 'intervalSec': intervalSec, 'minMoveMeters': minMoveMeters};
  }

  void _dispatch(Position p) {
    _onFix?.call(p);
  }

  Future<Map<String, Object?>> stop() async {
    await _subscription?.cancel();
    _subscription = null;
    _onFix = null;
    return {'stopped': true};
  }

  Future<Position?> currentFix() async {
    try {
      return await Geolocator.getCurrentPosition();
    } catch (e) {
      developer.log('currentFix failed: $e', name: 'LocationService');
      return null;
    }
  }
}
