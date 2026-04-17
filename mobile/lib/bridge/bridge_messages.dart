class BridgeMessageTypes {
  BridgeMessageTypes._();

  // Web → Native
  static const String gpsStart = 'gps.start';
  static const String gpsStop = 'gps.stop';
  static const String qrScan = 'qr.scan';
  static const String filePickArticle = 'file.pickArticle';
  static const String storageGet = 'storage.get';
  static const String storageSet = 'storage.set';
  static const String storageDelete = 'storage.delete';
  static const String localdbMatchesList = 'localdb.matches.list';
  static const String localdbMatchesSave = 'localdb.matches.save';
  static const String snsLogin = 'sns.login';
  static const String pushRequestPermission = 'push.requestPermission';
  static const String pushToken = 'push.token';
  static const String appInfo = 'app.info';

  // Native → Web
  static const String pushReceived = 'push.received';
  static const String gpsError = 'gps.error';
  static const String connectivityChange = 'connectivity.change';
  static const String appResume = 'app.resume';
}
