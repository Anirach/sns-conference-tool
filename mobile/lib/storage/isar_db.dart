import 'dart:async';

import 'package:isar/isar.dart';
import 'package:path_provider/path_provider.dart';

part 'isar_db.g.dart';

/// Local cache of match cards + user settings. Opt-in: the web layer only writes here when the
/// user has enabled "Keep matches offline" in Settings. Read via bridge calls
/// `localdb.matches.list` / `localdb.matches.save`.
class IsarDb {
  Isar? _isar;

  Future<Isar> open() async {
    if (_isar != null) return _isar!;
    final dir = await getApplicationDocumentsDirectory();
    _isar = await Isar.open(
      [MatchEntitySchema, SettingEntitySchema],
      directory: dir.path,
    );
    return _isar!;
  }

  Future<List<Map<String, Object?>>> listMatches() async {
    final db = await open();
    final all = await db.matchEntitys.where().sortByCreatedAtDesc().findAll();
    return all.map((m) => m.toJson()).toList();
  }

  Future<void> saveMatch(Map<String, Object?> json) async {
    final db = await open();
    final entity = MatchEntity.fromJson(json);
    await db.writeTxn(() async {
      await db.matchEntitys.put(entity);
    });
  }

  Future<String?> getSetting(String key) async {
    final db = await open();
    final e = await db.settingEntitys.where().keyEqualTo(key).findFirst();
    return e?.value;
  }

  Future<void> setSetting(String key, String value) async {
    final db = await open();
    await db.writeTxn(() async {
      final existing = await db.settingEntitys.where().keyEqualTo(key).findFirst();
      final row = existing ?? SettingEntity()
        ..key = key;
      row.value = value;
      await db.settingEntitys.put(row);
    });
  }
}

@collection
class MatchEntity {
  Id id = Isar.autoIncrement;

  @Index(unique: true, replace: true)
  late String matchId;

  late String eventId;
  late String otherUserId;
  late String name;
  String? title;
  String? institution;
  String? profilePictureUrl;
  List<String> commonKeywords = [];
  double similarity = 0;
  bool mutual = false;
  double distanceMeters = 0;
  late DateTime createdAt;

  Map<String, Object?> toJson() => {
        'matchId': matchId,
        'eventId': eventId,
        'otherUserId': otherUserId,
        'name': name,
        'title': title,
        'institution': institution,
        'profilePictureUrl': profilePictureUrl,
        'commonKeywords': commonKeywords,
        'similarity': similarity,
        'mutual': mutual,
        'distanceMeters': distanceMeters,
      };

  static MatchEntity fromJson(Map<String, Object?> m) {
    return MatchEntity()
      ..matchId = m['matchId'] as String
      ..eventId = m['eventId'] as String
      ..otherUserId = m['otherUserId'] as String
      ..name = m['name'] as String? ?? ''
      ..title = m['title'] as String?
      ..institution = m['institution'] as String?
      ..profilePictureUrl = m['profilePictureUrl'] as String?
      ..commonKeywords = ((m['commonKeywords'] as List?) ?? const []).cast<String>()
      ..similarity = (m['similarity'] as num?)?.toDouble() ?? 0
      ..mutual = m['mutual'] as bool? ?? false
      ..distanceMeters = (m['distanceMeters'] as num?)?.toDouble() ?? 0
      ..createdAt = DateTime.now();
  }
}

@collection
class SettingEntity {
  Id id = Isar.autoIncrement;

  @Index(unique: true, replace: true)
  late String key;

  String? value;
}
