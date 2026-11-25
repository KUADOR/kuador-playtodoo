import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_colors.dart';
import 'package:playtodoo/core/design/app_theme.dart';
import 'package:playtodoo/core/design/tokens.g.dart';

void main() {
  final theme = AppTheme.light();

  group('ColorScheme', () {
    test('maps the brand tokens explicitly', () {
      final scheme = theme.colorScheme;

      expect(scheme.primary, AppTokens.colorPrimary);
      expect(scheme.onPrimary, AppTokens.colorOnPrimary);
      expect(scheme.secondary, AppTokens.colorAccent);
      expect(scheme.onSecondary, AppTokens.colorBrandNavy);
      expect(scheme.error, AppTokens.colorDanger);
      expect(scheme.surface, AppTokens.colorSurface);
      expect(scheme.onSurface, AppTokens.colorFg);
      expect(scheme.outline, AppTokens.colorFgMuted);
      expect(scheme.outlineVariant, AppTokens.colorBorder);
    });
  });

  group('TextTheme', () {
    test('uses Nunito extrabold for headings', () {
      for (final style in [
        theme.textTheme.displaySmall,
        theme.textTheme.headlineSmall,
        theme.textTheme.titleMedium,
      ]) {
        expect(style?.fontFamily, AppTokens.fontFamilyHeading);
        expect(style?.fontWeight, AppTokens.fontWeightExtrabold);
      }
    });

    test('uses Inter for body and labels with 16 as main mobile size', () {
      expect(theme.textTheme.bodyLarge?.fontFamily, AppTokens.fontFamilyBody);
      expect(
        theme.textTheme.bodyLarge?.fontWeight,
        AppTokens.fontWeightRegular,
      );
      expect(theme.textTheme.bodyMedium?.fontSize, AppTokens.fontSizeMd);
      expect(
        theme.textTheme.labelLarge?.fontWeight,
        AppTokens.fontWeightSemibold,
      );
    });
  });

  group('AppColors', () {
    test('is registered in the theme with the token values', () {
      final colors = theme.extension<AppColors>();

      expect(colors, isNotNull);
      expect(colors!.primaryTint, AppTokens.colorPrimaryTint);
      expect(colors.accentTint, AppTokens.colorAccentTint);
      expect(colors.occupiedBackground, AppTokens.colorOccupiedBg);
      expect(colors.occupiedForeground, AppTokens.colorFgMuted);
      expect(colors.disabledBackground, AppTokens.colorDisabledBg);
      expect(colors.disabledForeground, AppTokens.colorDisabledFg);
      expect(colors.focusHalo, AppTokens.colorFocusHalo);
    });

    test('copyWith replaces only the given colors', () {
      final copy = AppColors.light.copyWith(danger: AppTokens.colorFg);

      expect(copy.danger, AppTokens.colorFg);
      expect(copy.primaryTint, AppColors.light.primaryTint);
    });

    test('lerp interpolates every color and ignores foreign extensions', () {
      final target = AppColors.light.copyWith(
        brandNavy: AppTokens.colorSurface,
      );

      expect(
        AppColors.light.lerp(target, 0).brandNavy,
        AppTokens.colorBrandNavy,
      );
      expect(AppColors.light.lerp(target, 1).brandNavy, AppTokens.colorSurface);
      expect(AppColors.light.lerp(null, 0.5), same(AppColors.light));
    });

    testWidgets('is reachable through context.appColors', (tester) async {
      late AppColors colors;
      await tester.pumpWidget(
        MaterialApp(
          theme: theme,
          home: Builder(
            builder: (context) {
              colors = context.appColors;
              return const SizedBox.shrink();
            },
          ),
        ),
      );

      expect(colors.primaryTint, AppTokens.colorPrimaryTint);
    });
  });

  test('widgets use no literal colors outside the generated tokens', () {
    final literalColor = RegExp(r'Color\(0x|Color\.fromRGBO|\bColors\.');
    final offenders = Directory('lib')
        .listSync(recursive: true)
        .whereType<File>()
        .where(
          (file) =>
              file.path.endsWith('.dart') &&
              !file.path.endsWith('tokens.g.dart'),
        )
        .where((file) => literalColor.hasMatch(file.readAsStringSync()))
        .map((file) => file.path)
        .toList();

    expect(offenders, isEmpty);
  });
}
