import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/app/app_routes.dart';
import 'package:playtodoo/core/design/app_theme.dart';
import 'package:playtodoo/features/design_catalog/design_catalog_screen.dart';
import 'package:playtodoo/main.dart';
import 'package:playtodoo/shared/ui/app_button.dart';
import 'package:playtodoo/shared/ui/app_card.dart';
import 'package:playtodoo/shared/ui/app_field.dart';
import 'package:playtodoo/shared/ui/booking_calendar/booking_calendar.dart';

void main() {
  test('the catalog route does not exist when the catalog is disabled', () {
    expect(
      buildAppRoutes(enableDesignCatalog: false),
      isNot(contains(AppRoutes.designCatalog)),
    );
    expect(
      buildAppRoutes(enableDesignCatalog: true),
      contains(AppRoutes.designCatalog),
    );
  });

  testWidgets('release build: the home screen has no catalog link', (
    tester,
  ) async {
    await tester.pumpWidget(const PlaytodooApp(enableDesignCatalog: false));

    expect(find.text('Ver catálogo de diseño'), findsNothing);
  });

  testWidgets(
    'shows every button variant in normal, loading and disabled states, '
    'and every card variant plus selected and loading, like the web catalogs',
    (tester) async {
      tester.view.physicalSize = const Size(390, 6000);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        MaterialApp(theme: AppTheme.light(), home: const DesignCatalogScreen()),
      );

      for (final variant in AppButtonVariant.values) {
        final buttons = tester
            .widgetList<AppButton>(find.byType(AppButton, skipOffstage: false))
            .where((button) => button.variant == variant)
            // The calendar error state adds its own «Reintentar» button.
            .where((button) => button.label != 'Reintentar');
        expect(
          buttons.where((b) => !b.isLoading && b.onPressed != null),
          hasLength(1),
          reason: '$variant normal',
        );
        expect(
          buttons.where((b) => b.isLoading),
          hasLength(1),
          reason: '$variant loading',
        );
        expect(
          buttons.where((b) => b.onPressed == null),
          hasLength(1),
          reason: '$variant disabled',
        );
      }

      final cards = tester.widgetList<AppCard>(
        find.byType(AppCard, skipOffstage: false),
      );
      expect(cards.where((c) => !c.selected && !c.isLoading), hasLength(5));
      expect(cards.where((c) => c.selected), hasLength(1));
      expect(cards.where((c) => c.isLoading), hasLength(1));
    },
  );

  testWidgets('debug build: opens the catalog with every component', (
    tester,
  ) async {
    await tester.pumpWidget(const PlaytodooApp(enableDesignCatalog: true));

    await tester.tap(find.text('Ver catálogo de diseño'));
    // The catalog shows endless loading animations, so it never settles.
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));

    expect(find.byType(DesignCatalogScreen), findsOneWidget);
    final catalogScroll = find
        .descendant(
          of: find.byType(ListView),
          matching: find.byType(Scrollable),
        )
        .first;
    // The list is lazy: each component is checked while its section is
    // built.
    final componentsBySection = {
      'Botones': find.byType(AppButton, skipOffstage: false),
      'Campos': find.byWidgetPredicate(
        (w) => w is AppField,
        skipOffstage: false,
      ),
      'Tarjetas': find.byType(AppCard, skipOffstage: false),
      'Calendario': find.byType(BookingCalendar, skipOffstage: false),
      'Iconos': find.text('Iconos'),
    };
    for (final MapEntry(key: section, value: components)
        in componentsBySection.entries) {
      await tester.scrollUntilVisible(
        find.text(section),
        200,
        scrollable: catalogScroll,
      );
      expect(find.text(section), findsOneWidget);
      expect(components, findsWidgets, reason: section);
    }
  });
}
