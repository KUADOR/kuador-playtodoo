import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_theme.dart';
import 'package:playtodoo/features/tenant/data/in_memory_tenant_api.dart';
import 'package:playtodoo/features/tenant/domain/tenant_api.dart';
import 'package:playtodoo/features/tenant/presentation/tenant_text.dart';
import 'package:playtodoo/features/tenant/tenant_entry_screen.dart';

import '../booking/booking_test_support.dart' show phoneSize;

const String _complexName = 'Canchas El Sol';

void main() {
  late List<String> requestedSubdomains;
  late TenantAccess answer;

  Future<void> pumpEntry(WidgetTester tester) async {
    tester.view
      ..devicePixelRatio = 1
      ..physicalSize = phoneSize;
    addTearDown(tester.view.reset);
    requestedSubdomains = [];
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: TenantEntryScreen(
          apiForSubdomain: (subdomain) {
            requestedSubdomains.add(subdomain);
            return InMemoryTenantApi(access: answer);
          },
        ),
      ),
    );
  }

  Future<void> enter(WidgetTester tester, String subdomain) async {
    await tester.enterText(find.byType(TextField), subdomain);
    await tester.tap(find.text(TenantEntryScreen.enter));
    await tester.pumpAndSettle();
  }

  testWidgets('enters an active complex and shows its name', (tester) async {
    answer = const ActiveTenant(displayName: _complexName);
    await pumpEntry(tester);

    await enter(tester, '  CanchasElSol ');

    expect(requestedSubdomains, ['canchaselsol']);
    expect(find.text(_complexName), findsOneWidget);
  });

  testWidgets('shows the state screen the api answers for the complex', (
    tester,
  ) async {
    answer = const UnavailableTenant();
    await pumpEntry(tester);

    await enter(tester, 'canchaselsol');

    expect(find.text(TenantText.unavailableTitle), findsOneWidget);
  });

  testWidgets('goes back to the entry from «Ir a Playtodoo»', (tester) async {
    answer = const TenantNotFound();
    await pumpEntry(tester);
    await enter(tester, 'otro');

    await tester.tap(find.text(TenantText.goToPlatform));
    await tester.pumpAndSettle();

    expect(find.text(TenantEntryScreen.subdomainLabel), findsOneWidget);
  });

  testWidgets('asks for a valid subdomain before calling the api', (
    tester,
  ) async {
    answer = const ActiveTenant(displayName: _complexName);
    await pumpEntry(tester);

    await enter(tester, 'el sol');

    expect(find.text(TenantEntryScreen.subdomainInvalid), findsOneWidget);
    expect(requestedSubdomains, isEmpty);
  });
}
