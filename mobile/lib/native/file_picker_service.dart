import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';

/// Opens the OS file picker and returns metadata for the selected article.
/// The web layer streams the bytes to `POST /api/interests` as multipart/form-data.
class FilePickerService {
  Future<Map<String, Object?>> pickArticle() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: const ['pdf', 'txt', 'md'],
      withData: false,
    );
    if (result == null || result.files.isEmpty) {
      return {'cancelled': true};
    }

    final file = result.files.single;
    final path = file.path;
    if (path == null) {
      return {'error': 'no-path'};
    }

    String? previewBase64;
    try {
      final raw = await File(path).openRead(0, 64 * 1024).toList();
      final flat = raw.expand((chunk) => chunk).toList();
      previewBase64 = base64Encode(flat);
    } catch (_) {
      previewBase64 = null;
    }

    return {
      'path': path,
      'name': file.name,
      'sizeBytes': file.size,
      'previewBase64': previewBase64,
    };
  }
}
