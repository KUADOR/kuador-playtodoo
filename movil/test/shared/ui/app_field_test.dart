import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_icon.dart';
import 'package:playtodoo/core/design/tokens.g.dart';
import 'package:playtodoo/shared/ui/app_field.dart';

import '../../support/themed.dart';

void main() {
  const label = 'Correo';
  const error = 'Ese correo no es válido. Revisa que tenga @.';
  const options = [
    AppSelectOption(value: 'futbol', label: 'Fútbol'),
    AppSelectOption(value: 'padel', label: 'Pádel'),
  ];

  OutlineInputBorder? activeBorder(WidgetTester tester) {
    final decorator = tester.widget<InputDecorator>(
      find.byType(InputDecorator),
    );
    final theme = Theme.of(tester.element(find.byType(InputDecorator)))
        .inputDecorationTheme;
    final hasError = decorator.decoration.error != null;
    return (hasError ? theme.errorBorder : theme.enabledBorder)
        as OutlineInputBorder?;
  }

  group('AppField.text', () {
    testWidgets('shows the label above the input and accepts text', (
      tester,
    ) async {
      String? changed;
      await pumpThemed(
        tester,
        AppField.text(label: label, onChanged: (text) => changed = text),
      );

      expect(find.text(label), findsOneWidget);
      final labelBottom = tester.getBottomLeft(find.text(label)).dy;
      expect(
        labelBottom,
        lessThanOrEqualTo(tester.getTopLeft(find.byType(TextField)).dy),
      );

      await tester.enterText(find.byType(TextField), 'ana@correo.com');
      expect(changed, 'ana@correo.com');
    });

    testWidgets('error: danger border of 2, alert icon and message', (
      tester,
    ) async {
      await pumpThemed(
        tester,
        const AppField.text(label: label, errorText: error),
      );

      expect(find.text(error), findsOneWidget);
      expect(
        find.byWidgetPredicate((w) => w is AppIcon && w.icon == AppIcons.alert),
        findsOneWidget,
      );
      final border = activeBorder(tester)!;
      expect(border.borderSide.color, AppTokens.colorDanger);
      expect(border.borderSide.width, AppTokens.borderWidthLg);
    });

    testWidgets('error and label are part of the field semantics', (
      tester,
    ) async {
      final handle = tester.ensureSemantics();
      await pumpThemed(
        tester,
        const AppField.text(label: label, errorText: error),
      );

      final semantics = tester.getSemantics(find.byType(TextField));
      final description =
          '${semantics.label} ${semantics.hint} ${semantics.value}';
      expect(semantics, isSemantics(isTextField: true));
      expect(description, contains(label));
      expect(description, contains(error));
      handle.dispose();
    });

    testWidgets('disabled: not editable', (tester) async {
      await pumpThemed(
        tester,
        const AppField.text(label: label, enabled: false),
      );

      expect(tester.widget<TextField>(find.byType(TextField)).enabled, isFalse);
    });

    testWidgets('meets the minimum touch size', (tester) async {
      await pumpThemed(tester, const AppField.text(label: label));

      expect(
        tester.getSize(find.byType(TextField)).height,
        greaterThanOrEqualTo(AppTokens.sizeTouchMin),
      );
    });
  });

  group('AppField.select', () {
    testWidgets('opens the options and emits the chosen value', (tester) async {
      String? chosen;
      await pumpThemed(
        tester,
        AppField<String>.select(
          label: 'Deporte',
          options: options,
          onChanged: (value) => chosen = value,
        ),
      );

      await tester.tap(find.byType(DropdownButtonFormField<String>));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Pádel').last);
      await tester.pumpAndSettle();

      expect(chosen, 'padel');
      expect(find.text('Pádel'), findsOneWidget);
    });

    testWidgets('disabled: does not open the options', (tester) async {
      await pumpThemed(
        tester,
        AppField<String>.select(
          label: 'Deporte',
          options: options,
          enabled: false,
          onChanged: (_) {},
        ),
      );

      await tester.tap(
        find.byType(DropdownButtonFormField<String>),
        warnIfMissed: false,
      );
      await tester.pumpAndSettle();

      expect(find.text('Fútbol'), findsNothing);
    });

    testWidgets('error: shows icon and message', (tester) async {
      await pumpThemed(
        tester,
        AppField<String>.select(
          label: 'Deporte',
          options: options,
          errorText: 'Elige un deporte para continuar.',
          onChanged: (_) {},
        ),
      );

      expect(find.text('Elige un deporte para continuar.'), findsOneWidget);
      expect(
        find.byWidgetPredicate((w) => w is AppIcon && w.icon == AppIcons.alert),
        findsOneWidget,
      );
    });

    testWidgets('meets the minimum touch size', (tester) async {
      await pumpThemed(
        tester,
        AppField<String>.select(
          label: 'Deporte',
          options: options,
          onChanged: (_) {},
        ),
      );

      expect(
        tester.getSize(find.byType(DropdownButtonFormField<String>)).height,
        greaterThanOrEqualTo(AppTokens.sizeTouchMin),
      );
    });
  });
}
