# Assets

Bundled resources are declared in `../pubspec.yaml` under `flutter.assets`.

Historically this directory held a `sample.pdf` used by the Phase 1 file-picker stub. From Phase 2 onward `FilePickerService` uses the real `file_picker` plugin to surface an OS-selected file, so the bundled PDF is no longer consulted — it's kept as a placeholder in case a future demo needs a canned article.
