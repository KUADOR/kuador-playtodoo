import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_theme.dart';

/// Pumps [child] inside the app theme, centered on a scaffold.
Future<void> pumpThemed(
  WidgetTester tester,
  Widget child, {
  bool disableAnimations = false,
}) {
  return tester.pumpWidget(
    MediaQuery(
      data: MediaQueryData(disableAnimations: disableAnimations),
      child: MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: Center(
            child: Padding(padding: const EdgeInsets.all(16), child: child),
          ),
        ),
      ),
    ),
  );
}
