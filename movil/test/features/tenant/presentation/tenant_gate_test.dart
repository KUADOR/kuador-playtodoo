import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_icon.dart';
import 'package:playtodoo/core/design/app_theme.dart';
import 'package:playtodoo/core/design/tokens.g.dart';
import 'package:playtodoo/features/tenant/data/in_memory_tenant_api.dart';
import 'package:playtodoo/features/tenant/domain/tenant_api.dart';
import 'package:playtodoo/features/tenant/presentation/tenant_gate.dart';
import 'package:playtodoo/features/tenant/presentation/tenant_text.dart';
import 'package:playtodoo/shared/ui/app_card.dart';
import 'package:playtodoo/shared/ui/app_notice.dart';

import '../../booking/booking_test_support.dart'
    show phoneSize, sendAppToBackgroundAndBack;

const String _complexName = 'Canchas El Sol';
const ActiveTenant _activeTenant = ActiveTenant(displayName: _complexName);
const String _openBookingLabel = 'Abrir reserva';
const String _openBookingTitle = 'Reserva abierta';
const String _platformHome = 'Inicio de Playtodoo';
const String _enterComplexLabel = 'Entrar al complejo';

/// Longer than the page transition, so the loading state can be seen.
const Duration _latency = Duration(seconds: 1);
const Duration _pageTransition = Duration(milliseconds: 500);
const Size _tabletSize = Size(1024, 768);

/// Pumps the app start with a button that opens the complex behind a
/// [TenantGate], as a player does from the app. «Ir a Playtodoo» goes back to
/// that start.
Future<void> _pumpApp(
  WidgetTester tester,
  TenantApi api, {
  Size size = phoneSize,
}) async {
  tester.view
    ..devicePixelRatio = 1
    ..physicalSize = size;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Builder(
        builder: (context) => Scaffold(
          body: Column(
            children: [
              const Text(_platformHome),
              TextButton(
                onPressed: () => _enterComplex(context, api),
                child: const Text(_enterComplexLabel),
              ),
            ],
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text(_enterComplexLabel));
  await tester.pump();
}

void _enterComplex(BuildContext context, TenantApi api) {
  final navigator = Navigator.of(context);
  navigator.push(
    MaterialPageRoute<void>(
      builder: (_) => TenantGate(
        api: api,
        onGoToPlatform: () => navigator.popUntil((route) => route.isFirst),
        complexBuilder: (_) => const _ComplexHome(),
      ),
    ),
  );
}

class _ComplexHome extends StatelessWidget {
  const _ComplexHome();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(TenantGate.of(context).tenant.displayName)),
      body: TextButton(
        onPressed: () => Navigator.of(context).push(
          MaterialPageRoute<void>(
            builder: (_) => const Scaffold(body: Text(_openBookingTitle)),
          ),
        ),
        child: const Text(_openBookingLabel),
      ),
    );
  }
}

Finder _icon(AppIcons icon) => find.byWidgetPredicate(
  (widget) => widget is AppIcon && widget.icon == icon,
);

Finder _button(String label) => find.ancestor(
  of: find.text(label),
  matching: find.byWidgetPredicate((widget) => widget is ButtonStyleButton),
);

void main() {
  group('loading', () {
    testWidgets('shows the card skeleton until the complex answers', (
      tester,
    ) async {
      final semantics = tester.ensureSemantics();
      await _pumpApp(
        tester,
        InMemoryTenantApi(access: _activeTenant, latency: _latency),
      );
      await tester.pump(_pageTransition);

      expect(
        find.bySemanticsLabel(RegExp(TenantText.checking)),
        findsOneWidget,
      );
      expect(find.text(_complexName), findsNothing);

      await tester.pumpAndSettle();

      expect(find.bySemanticsLabel(RegExp(TenantText.checking)), findsNothing);
      expect(find.text(_complexName), findsOneWidget);
      semantics.dispose();
    });
  });

  group('complex not available', () {
    testWidgets('shows the state without the name nor the reason', (
      tester,
    ) async {
      await _pumpApp(
        tester,
        InMemoryTenantApi(access: const UnavailableTenant()),
      );
      await tester.pumpAndSettle();

      expect(find.text(TenantText.unavailableTitle), findsOneWidget);
      expect(find.text(TenantText.unavailableText), findsOneWidget);
      expect(_icon(AppIcons.lock), findsOneWidget);
      expect(find.text(_complexName), findsNothing);
      expect(
        find.widgetWithText(OutlinedButton, TenantText.goToPlatform),
        findsOneWidget,
        reason: '«Ir a Playtodoo» is the secondary action here.',
      );
    });

    testWidgets('the title is a header and the icon is decorative', (
      tester,
    ) async {
      final semantics = tester.ensureSemantics();
      await _pumpApp(
        tester,
        InMemoryTenantApi(access: const UnavailableTenant()),
      );
      await tester.pumpAndSettle();

      expect(
        tester.getSemantics(find.text(TenantText.unavailableTitle)),
        matchesSemantics(label: TenantText.unavailableTitle, isHeader: true),
      );
      expect(
        find.descendant(
          of: _icon(AppIcons.lock),
          matching: find.byType(ExcludeSemantics),
        ),
        findsWidgets,
      );
      semantics.dispose();
    });

    testWidgets('«Ir a Playtodoo» goes back to the start of the app', (
      tester,
    ) async {
      await _pumpApp(
        tester,
        InMemoryTenantApi(access: const UnavailableTenant()),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text(TenantText.goToPlatform));
      await tester.pumpAndSettle();

      expect(find.text(_platformHome), findsOneWidget);
      expect(find.text(TenantText.unavailableTitle), findsNothing);
    });

    testWidgets('replaces the whole navigation of the complex when it is '
        'suspended while the player has a booking open', (tester) async {
      final api = InMemoryTenantApi(access: _activeTenant);
      await _pumpApp(tester, api);
      await tester.pumpAndSettle();
      await tester.tap(find.text(_openBookingLabel));
      await tester.pumpAndSettle();
      expect(find.text(_openBookingTitle), findsOneWidget);

      api.access = const UnavailableTenant();
      sendAppToBackgroundAndBack(tester);
      await tester.pumpAndSettle();

      expect(find.text(TenantText.unavailableTitle), findsOneWidget);
      expect(find.text(_openBookingTitle), findsNothing);
      expect(find.text(_complexName), findsNothing);

      await tester.tap(find.text(TenantText.goToPlatform));
      await tester.pumpAndSettle();
      expect(find.text(_platformHome), findsOneWidget);
    });

    testWidgets('a screen of the complex can ask to check it again', (
      tester,
    ) async {
      final api = InMemoryTenantApi(access: _activeTenant);
      await _pumpApp(tester, api);
      await tester.pumpAndSettle();

      api.access = const UnavailableTenant();
      await TenantGate.recheck(tester.element(find.byType(_ComplexHome)));
      await tester.pumpAndSettle();

      expect(find.text(TenantText.unavailableTitle), findsOneWidget);
    });
  });

  group('complex not found', () {
    testWidgets('shows the state with «Ir a Playtodoo» as primary action', (
      tester,
    ) async {
      final semantics = tester.ensureSemantics();
      await _pumpApp(tester, InMemoryTenantApi(access: const TenantNotFound()));
      await tester.pumpAndSettle();

      expect(find.text(TenantText.notFoundTitle), findsOneWidget);
      expect(find.text(TenantText.notFoundText), findsOneWidget);
      expect(_icon(AppIcons.search), findsOneWidget);
      expect(
        find.widgetWithText(FilledButton, TenantText.goToPlatform),
        findsOneWidget,
      );
      expect(
        tester.getSemantics(find.text(TenantText.notFoundTitle)),
        matchesSemantics(label: TenantText.notFoundTitle, isHeader: true),
      );
      semantics.dispose();
    });

    testWidgets('meets the touch target and contrast guidelines', (
      tester,
    ) async {
      final semantics = tester.ensureSemantics();
      await _pumpApp(tester, InMemoryTenantApi(access: const TenantNotFound()));
      await tester.pumpAndSettle();

      final buttonSize = tester.getSize(_button(TenantText.goToPlatform));
      expect(buttonSize.height, greaterThanOrEqualTo(AppTokens.sizeTouchMin));
      final cardContentWidth =
          tester.getSize(find.byType(AppCard)).width -
          2 * (AppTokens.space4 + AppTokens.borderWidthSm);
      expect(
        tester.getSize(find.byType(AppCard)).width,
        phoneSize.width - 2 * AppTokens.space4,
        reason: 'Full-width card with a 16 margin on phones.',
      );
      expect(buttonSize.width, cardContentWidth, reason: 'Full-width button.');
      await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
      semantics.dispose();
    });

    testWidgets('the card is at most 480 wide on large screens', (
      tester,
    ) async {
      await _pumpApp(
        tester,
        InMemoryTenantApi(access: const TenantNotFound()),
        size: _tabletSize,
      );
      await tester.pumpAndSettle();

      final buttonWidth = tester
          .getSize(_button(TenantText.goToPlatform))
          .width;
      expect(buttonWidth, lessThanOrEqualTo(480));
    });
  });

  group('network error', () {
    testWidgets('announces that the complex could not be checked and retries', (
      tester,
    ) async {
      final semantics = tester.ensureSemantics();
      final api = InMemoryTenantApi(access: _activeTenant, latency: _latency)
        ..isOffline = true;
      await _pumpApp(tester, api);
      await tester.pumpAndSettle();

      expect(find.text(TenantText.checkFailed), findsOneWidget);
      expect(find.text(TenantText.unavailableTitle), findsNothing);
      expect(find.text(TenantText.notFoundTitle), findsNothing);
      final notice = tester.getSemantics(find.byType(AppNotice));
      expect(notice.flagsCollection.isLiveRegion, isTrue);
      expect(
        tester.getSize(_button(TenantText.retry)).height,
        greaterThanOrEqualTo(AppTokens.sizeTouchMin),
      );

      api.isOffline = false;
      await tester.tap(find.text(TenantText.retry));
      await tester.pump();
      expect(
        find.bySemanticsLabel(RegExp(TenantText.checking)),
        findsOneWidget,
      );

      await tester.pumpAndSettle();
      expect(find.text(_complexName), findsOneWidget);
      semantics.dispose();
    });

    testWidgets('a failed check while inside keeps the player in the complex', (
      tester,
    ) async {
      final api = InMemoryTenantApi(access: _activeTenant);
      await _pumpApp(tester, api);
      await tester.pumpAndSettle();
      await tester.tap(find.text(_openBookingLabel));
      await tester.pumpAndSettle();

      api.isOffline = true;
      sendAppToBackgroundAndBack(tester);
      await tester.pumpAndSettle();

      expect(find.text(_openBookingTitle), findsOneWidget);
      expect(find.text(TenantText.checkFailed), findsNothing);
    });
  });

  testWidgets('the system back pops the screens of the complex first', (
    tester,
  ) async {
    await _pumpApp(tester, InMemoryTenantApi(access: _activeTenant));
    await tester.pumpAndSettle();
    await tester.tap(find.text(_openBookingLabel));
    await tester.pumpAndSettle();

    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();

    expect(find.text(_openBookingTitle), findsNothing);
    expect(find.text(_complexName), findsOneWidget);
  });
}
