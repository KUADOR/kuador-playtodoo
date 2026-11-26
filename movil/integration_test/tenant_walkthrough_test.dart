import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:material_ui/material_ui.dart' show TextField;
import 'package:playtodoo/features/home/home_screen.dart';
import 'package:playtodoo/features/tenant/presentation/tenant_text.dart';
import 'package:playtodoo/features/tenant/tenant_entry_screen.dart';
import 'package:playtodoo/main.dart';

// One state of the walkthrough in infra/local/e2e, against the real api:
//   flutter drive --driver=test_driver/integration_test.dart \
//     --target=integration_test/tenant_walkthrough_test.dart \
//     --dart-define=PLAYTODOO_TENANT_URL_PATTERN=http://{subdomain}.localhost:8000 \
//     --dart-define=E2E_SUBDOMAIN=canchaselsol --dart-define=E2E_EXPECTED=active \
//     --dart-define="E2E_DISPLAY_NAME=Canchas El Sol" --dart-define=E2E_STATE=activo

const String _subdomain = String.fromEnvironment('E2E_SUBDOMAIN');

/// `active`, `unavailable` or `not-found`.
const String _expected = String.fromEnvironment('E2E_EXPECTED');

/// Name the api answers for an active complex.
const String _displayName = String.fromEnvironment('E2E_DISPLAY_NAME');

/// Name of the screenshot.
const String _state = String.fromEnvironment('E2E_STATE');

const Duration _answerTimeout = Duration(seconds: 15);
const Duration _pollInterval = Duration(milliseconds: 100);

void main() {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('enters the complex on $_subdomain and sees «$_expected»', (
    tester,
  ) async {
    await tester.pumpWidget(const PlaytodooApp(enableDesignCatalog: true));
    await tester.tap(find.text(HomeScreen.tenantEntryLabel));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), _subdomain);
    await tester.tap(find.text(TenantEntryScreen.enter));

    final screen = switch (_expected) {
      'active' => find.text(_displayName),
      'unavailable' => find.text(TenantText.unavailableTitle),
      'not-found' => find.text(TenantText.notFoundTitle),
      _ => throw ArgumentError.value(_expected, 'E2E_EXPECTED'),
    };
    await _pumpUntilFound(tester, screen);

    expect(screen, findsWidgets);
    if (defaultTargetPlatform == TargetPlatform.android) {
      await binding.convertFlutterSurfaceToImage();
      await tester.pumpAndSettle();
    }
    await binding.takeScreenshot(_state);
  });
}

/// The answer comes from the real api, so the test waits for it with real
/// time instead of a fixed number of frames.
Future<void> _pumpUntilFound(WidgetTester tester, Finder finder) async {
  final deadline = DateTime.now().add(_answerTimeout);
  while (finder.evaluate().isEmpty && DateTime.now().isBefore(deadline)) {
    await tester.runAsync(() => Future<void>.delayed(_pollInterval));
    await tester.pump();
  }
  await tester.pumpAndSettle();
}
