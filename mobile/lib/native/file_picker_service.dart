/// Pass 1 stub. Real implementation in pass 2 uses `file_picker`.
class FilePickerService {
  Future<Map<String, Object?>> pickArticle() async {
    return {
      'path': 'asset:///assets/sample.pdf',
      'name': 'sample-article.pdf',
      'sizeBytes': 245760,
      'previewBase64': null,
    };
  }
}
