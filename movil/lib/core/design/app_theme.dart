import 'package:material_ui/material_ui.dart';

import 'app_colors.dart';
import 'tokens.g.dart';

/// Maps the design tokens to Material 3 [ThemeData].
///
/// Widgets read colors from the theme; they never use literal values.
abstract final class AppTheme {
  static const Size _minTouchTarget = Size(
    AppTokens.sizeTouchMin,
    AppTokens.sizeTouchMin,
  );
  static const EdgeInsets _controlPadding = EdgeInsets.symmetric(
    horizontal: AppTokens.space4,
    vertical: AppTokens.space3,
  );
  static final BorderRadius _controlRadius = BorderRadius.circular(
    AppTokens.radiusMd,
  );

  static ThemeData light() {
    const colors = AppColors.light;
    const colorScheme = ColorScheme(
      brightness: Brightness.light,
      primary: AppTokens.colorPrimary,
      onPrimary: AppTokens.colorOnPrimary,
      primaryContainer: AppTokens.colorPrimaryTint,
      onPrimaryContainer: AppTokens.colorBrandNavy,
      secondary: AppTokens.colorAccent,
      onSecondary: AppTokens.colorBrandNavy,
      secondaryContainer: AppTokens.colorAccentTint,
      onSecondaryContainer: AppTokens.colorFg,
      error: AppTokens.colorDanger,
      onError: AppTokens.colorOnPrimary,
      surface: AppTokens.colorSurface,
      onSurface: AppTokens.colorFg,
      onSurfaceVariant: AppTokens.colorFgMuted,
      surfaceContainerHighest: AppTokens.colorSurfaceAlt,
      outline: AppTokens.colorFgMuted,
      outlineVariant: AppTokens.colorBorder,
      shadow: AppTokens.colorBrandNavy,
    );
    final textTheme = _textTheme();

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppTokens.colorSurface,
      fontFamily: AppTokens.fontFamilyBody,
      textTheme: textTheme,
      materialTapTargetSize: MaterialTapTargetSize.padded,
      extensions: const [colors],
      filledButtonTheme: FilledButtonThemeData(
        style: _filledButtonStyle(textTheme),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: _outlinedButtonStyle(textTheme),
      ),
      textButtonTheme: TextButtonThemeData(style: _textButtonStyle(textTheme)),
      inputDecorationTheme: _inputDecorationTheme(textTheme),
      cardTheme: CardThemeData(
        color: AppTokens.colorSurface,
        margin: EdgeInsets.zero,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppTokens.radiusLg),
          side: const BorderSide(
            color: AppTokens.colorBorder,
            width: AppTokens.borderWidthSm,
          ),
        ),
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: AppTokens.colorPrimary,
      ),
    );
  }

  static TextTheme _textTheme() {
    TextStyle heading(double size) => TextStyle(
      fontFamily: AppTokens.fontFamilyHeading,
      fontWeight: AppTokens.fontWeightExtrabold,
      fontSize: size,
      height: AppTokens.lineHeightHeading,
      color: AppTokens.colorFg,
    );
    TextStyle body(double size, FontWeight weight) => TextStyle(
      fontFamily: AppTokens.fontFamilyBody,
      fontWeight: weight,
      fontSize: size,
      height: AppTokens.lineHeightBody,
      color: AppTokens.colorFg,
    );

    return TextTheme(
      displayLarge: heading(AppTokens.fontSize2xl),
      displayMedium: heading(AppTokens.fontSize2xl),
      displaySmall: heading(AppTokens.fontSizeXl),
      headlineLarge: heading(AppTokens.fontSize2xl),
      headlineMedium: heading(AppTokens.fontSizeXl),
      headlineSmall: heading(AppTokens.fontSizeLg),
      titleLarge: heading(AppTokens.fontSizeLg),
      titleMedium: heading(AppTokens.fontSizeMd),
      titleSmall: heading(AppTokens.fontSizeSm),
      bodyLarge: body(AppTokens.fontSizeMd, AppTokens.fontWeightRegular),
      bodyMedium: body(AppTokens.fontSizeMd, AppTokens.fontWeightRegular),
      bodySmall: body(AppTokens.fontSizeSm, AppTokens.fontWeightRegular),
      labelLarge: body(AppTokens.fontSizeMd, AppTokens.fontWeightSemibold),
      labelMedium: body(AppTokens.fontSizeSm, AppTokens.fontWeightSemibold),
      labelSmall: body(AppTokens.fontSizeXs, AppTokens.fontWeightSemibold),
    );
  }

  static ButtonStyle _baseButtonStyle(TextTheme textTheme) {
    return ButtonStyle(
      minimumSize: const WidgetStatePropertyAll(_minTouchTarget),
      padding: const WidgetStatePropertyAll(_controlPadding),
      textStyle: WidgetStatePropertyAll(textTheme.labelLarge),
      shape: WidgetStatePropertyAll(
        RoundedRectangleBorder(borderRadius: _controlRadius),
      ),
      elevation: const WidgetStatePropertyAll(0),
      tapTargetSize: MaterialTapTargetSize.padded,
    );
  }

  static ButtonStyle _filledButtonStyle(TextTheme textTheme) {
    return _baseButtonStyle(textTheme).copyWith(
      backgroundColor: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.disabled)) {
          return AppTokens.colorDisabledBg;
        }
        if (states.contains(WidgetState.pressed) ||
            states.contains(WidgetState.hovered)) {
          return AppTokens.colorPrimaryHover;
        }
        return AppTokens.colorPrimary;
      }),
      foregroundColor: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.disabled)) {
          return AppTokens.colorDisabledFg;
        }
        return AppTokens.colorOnPrimary;
      }),
    );
  }

  static ButtonStyle _outlinedButtonStyle(TextTheme textTheme) {
    return _baseButtonStyle(textTheme).copyWith(
      backgroundColor: WidgetStateProperty.resolveWith((states) {
        final isActive =
            !states.contains(WidgetState.disabled) &&
            (states.contains(WidgetState.pressed) ||
                states.contains(WidgetState.hovered));
        return isActive ? AppTokens.colorPrimaryTint : AppTokens.colorSurface;
      }),
      foregroundColor: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.disabled)) {
          return AppTokens.colorDisabledBorder;
        }
        return AppTokens.colorPrimary;
      }),
      side: WidgetStateProperty.resolveWith((states) {
        final color = states.contains(WidgetState.disabled)
            ? AppTokens.colorDisabledBorder
            : AppTokens.colorPrimary;
        return BorderSide(color: color, width: AppTokens.borderWidthMd);
      }),
    );
  }

  static ButtonStyle _textButtonStyle(TextTheme textTheme) {
    return _baseButtonStyle(textTheme).copyWith(
      foregroundColor: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.disabled)) {
          return AppTokens.colorDisabledFg;
        }
        return AppTokens.colorPrimary;
      }),
      textStyle: WidgetStateProperty.resolveWith((states) {
        final underline = states.contains(WidgetState.hovered)
            ? TextDecoration.underline
            : null;
        return textTheme.labelLarge?.copyWith(decoration: underline);
      }),
    );
  }

  static InputDecorationTheme _inputDecorationTheme(TextTheme textTheme) {
    OutlineInputBorder border(Color color, double width) => OutlineInputBorder(
      borderRadius: _controlRadius,
      borderSide: BorderSide(color: color, width: width),
    );

    return InputDecorationTheme(
      filled: true,
      fillColor: WidgetStateColor.resolveWith((states) {
        return states.contains(WidgetState.disabled)
            ? AppTokens.colorSurfaceAlt
            : AppTokens.colorSurface;
      }),
      contentPadding: _controlPadding,
      constraints: const BoxConstraints(minHeight: AppTokens.sizeTouchMin),
      hintStyle: textTheme.bodyLarge?.copyWith(color: AppTokens.colorFgMuted),
      helperStyle: textTheme.bodySmall?.copyWith(color: AppTokens.colorFgMuted),
      helperMaxLines: 3,
      errorStyle: textTheme.bodySmall?.copyWith(color: AppTokens.colorDanger),
      errorMaxLines: 3,
      border: border(AppTokens.colorFgMuted, AppTokens.borderWidthSm),
      enabledBorder: border(AppTokens.colorFgMuted, AppTokens.borderWidthSm),
      focusedBorder: border(AppTokens.colorPrimary, AppTokens.borderWidthLg),
      errorBorder: border(AppTokens.colorDanger, AppTokens.borderWidthLg),
      focusedErrorBorder: border(
        AppTokens.colorDanger,
        AppTokens.borderWidthLg,
      ),
      disabledBorder: border(
        AppTokens.colorDisabledBorder,
        AppTokens.borderWidthSm,
      ),
    );
  }
}
