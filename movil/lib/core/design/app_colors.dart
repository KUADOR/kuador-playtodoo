import 'package:material_ui/material_ui.dart';

import 'tokens.g.dart';

/// Brand colors that Material's [ColorScheme] does not cover.
///
/// Read them with `context.appColors`.
@immutable
class AppColors extends ThemeExtension<AppColors> {
  const AppColors({
    required this.brandNavy,
    required this.primaryHover,
    required this.primaryTint,
    required this.accent,
    required this.accentStrong,
    required this.accentTint,
    required this.surfaceAlt,
    required this.success,
    required this.warning,
    required this.danger,
    required this.focus,
    required this.focusHalo,
    required this.occupiedBackground,
    required this.occupiedForeground,
    required this.disabledBackground,
    required this.disabledForeground,
    required this.disabledBorder,
  });

  /// Light palette taken from the design tokens.
  static const AppColors light = AppColors(
    brandNavy: AppTokens.colorBrandNavy,
    primaryHover: AppTokens.colorPrimaryHover,
    primaryTint: AppTokens.colorPrimaryTint,
    accent: AppTokens.colorAccent,
    accentStrong: AppTokens.colorAccentStrong,
    accentTint: AppTokens.colorAccentTint,
    surfaceAlt: AppTokens.colorSurfaceAlt,
    success: AppTokens.colorSuccess,
    warning: AppTokens.colorWarning,
    danger: AppTokens.colorDanger,
    focus: AppTokens.colorFocus,
    focusHalo: AppTokens.colorFocusHalo,
    occupiedBackground: AppTokens.colorOccupiedBg,
    // The calendar contract labels occupied slots with fg-muted text.
    occupiedForeground: AppTokens.colorFgMuted,
    disabledBackground: AppTokens.colorDisabledBg,
    disabledForeground: AppTokens.colorDisabledFg,
    disabledBorder: AppTokens.colorDisabledBorder,
  );

  final Color brandNavy;
  final Color primaryHover;
  final Color primaryTint;
  final Color accent;
  final Color accentStrong;
  final Color accentTint;
  final Color surfaceAlt;
  final Color success;
  final Color warning;
  final Color danger;
  final Color focus;
  final Color focusHalo;
  final Color occupiedBackground;
  final Color occupiedForeground;
  final Color disabledBackground;
  final Color disabledForeground;
  final Color disabledBorder;

  @override
  AppColors copyWith({
    Color? brandNavy,
    Color? primaryHover,
    Color? primaryTint,
    Color? accent,
    Color? accentStrong,
    Color? accentTint,
    Color? surfaceAlt,
    Color? success,
    Color? warning,
    Color? danger,
    Color? focus,
    Color? focusHalo,
    Color? occupiedBackground,
    Color? occupiedForeground,
    Color? disabledBackground,
    Color? disabledForeground,
    Color? disabledBorder,
  }) {
    return AppColors(
      brandNavy: brandNavy ?? this.brandNavy,
      primaryHover: primaryHover ?? this.primaryHover,
      primaryTint: primaryTint ?? this.primaryTint,
      accent: accent ?? this.accent,
      accentStrong: accentStrong ?? this.accentStrong,
      accentTint: accentTint ?? this.accentTint,
      surfaceAlt: surfaceAlt ?? this.surfaceAlt,
      success: success ?? this.success,
      warning: warning ?? this.warning,
      danger: danger ?? this.danger,
      focus: focus ?? this.focus,
      focusHalo: focusHalo ?? this.focusHalo,
      occupiedBackground: occupiedBackground ?? this.occupiedBackground,
      occupiedForeground: occupiedForeground ?? this.occupiedForeground,
      disabledBackground: disabledBackground ?? this.disabledBackground,
      disabledForeground: disabledForeground ?? this.disabledForeground,
      disabledBorder: disabledBorder ?? this.disabledBorder,
    );
  }

  @override
  AppColors lerp(covariant ThemeExtension<AppColors>? other, double t) {
    if (other is! AppColors) {
      return this;
    }
    Color mix(Color a, Color b) => Color.lerp(a, b, t)!;
    return AppColors(
      brandNavy: mix(brandNavy, other.brandNavy),
      primaryHover: mix(primaryHover, other.primaryHover),
      primaryTint: mix(primaryTint, other.primaryTint),
      accent: mix(accent, other.accent),
      accentStrong: mix(accentStrong, other.accentStrong),
      accentTint: mix(accentTint, other.accentTint),
      surfaceAlt: mix(surfaceAlt, other.surfaceAlt),
      success: mix(success, other.success),
      warning: mix(warning, other.warning),
      danger: mix(danger, other.danger),
      focus: mix(focus, other.focus),
      focusHalo: mix(focusHalo, other.focusHalo),
      occupiedBackground: mix(occupiedBackground, other.occupiedBackground),
      occupiedForeground: mix(occupiedForeground, other.occupiedForeground),
      disabledBackground: mix(disabledBackground, other.disabledBackground),
      disabledForeground: mix(disabledForeground, other.disabledForeground),
      disabledBorder: mix(disabledBorder, other.disabledBorder),
    );
  }
}

extension AppColorsContext on BuildContext {
  /// Brand colors of the current theme. Requires a theme built by `AppTheme`.
  AppColors get appColors {
    final colors = Theme.of(this).extension<AppColors>();
    assert(
      colors != null,
      'AppColors missing: build the theme with AppTheme.light().',
    );
    return colors ?? AppColors.light;
  }
}
