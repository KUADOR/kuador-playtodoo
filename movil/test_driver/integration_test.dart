import 'dart:io';

import 'package:integration_test/integration_test_driver_extended.dart';

/// Runs on the host: saves the screenshots of the integration tests in
/// `CAPTURE_DIR` (default `build/screenshots`).
Future<void> main() {
  final directory = Directory(
    Platform.environment['CAPTURE_DIR'] ?? 'build/screenshots',
  )..createSync(recursive: true);
  return integrationDriver(
    onScreenshot: (name, bytes, [args]) async {
      await File('${directory.path}/$name.png').writeAsBytes(bytes);
      return true;
    },
  );
}
