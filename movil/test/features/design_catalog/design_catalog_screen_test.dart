import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/app/app_routes.dart';
import 'package:playtodoo/features/design_catalog/design_catalog_screen.dart';
import 'package:playtodoo/main.dart';
import 'package:playtodoo/shared/ui/app_button.dart';
import 'package:playtodoo/shared/ui/app_card.dart';
import 'package:playtodoo/shared/ui/app_field.dart';

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
    for (final section in ['Botones', 'Campos', 'Tarjetas', 'Iconos']) {
      await tester.scrollUntilVisible(
        find.text(section),
        200,
        scrollable: catalogScroll,
      );
      expect(find.text(section), findsOneWidget);
    }
    expect(find.byType(AppButton, skipOffstage: false), findsWidgets);
    expect(
      find.byWidgetPredicate((w) => w is AppField, skipOffstage: false),
      findsWidgets,
    );
    expect(find.byType(AppCard, skipOffstage: false), findsWidgets);
  });
}
