import 'package:material_ui/material_ui.dart';

import '../../core/design/app_colors.dart';
import '../../core/design/app_icon.dart';
import '../../core/design/tokens.g.dart';
import 'app_focus_ring.dart';

enum AppButtonVariant { primary, secondary, danger, text }

/// Brand button built on Material buttons, which keep the native ripple,
/// keyboard activation and semantics.
///
/// A null [onPressed] disables it. While [isLoading] it ignores taps, keeps
/// the colors of its variant, shows a spinner with [loadingLabel] and
/// announces it.
class AppButton extends StatelessWidget {
  const AppButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.variant = AppButtonVariant.primary,
    this.isLoading = false,
    this.loadingLabel = defaultLoadingLabel,
    this.leadingIcon,
  });

  static const String defaultLoadingLabel = 'Guardando…';

  /// With reduced motion the spinner stops as a three-quarter arc, like the
  /// web `LoaderCircle` icon.
  static const double reducedMotionSpinnerValue = 0.75;

  final String label;
  final VoidCallback? onPressed;
  final AppButtonVariant variant;
  final bool isLoading;
  final String loadingLabel;
  final AppIcons? leadingIcon;

  @override
  Widget build(BuildContext context) {
    final action = isLoading ? null : onPressed;
    final content = _ButtonContent(
      label: isLoading ? loadingLabel : label,
      isLoading: isLoading,
      leadingIcon: leadingIcon,
    );

    final style = _variantStyle(context);
    final effectiveStyle = isLoading && onPressed != null
        ? _keepEnabledLook(style)
        : style;

    final Widget button = switch (variant) {
      AppButtonVariant.primary || AppButtonVariant.danger => FilledButton(
        onPressed: action,
        style: effectiveStyle,
        child: content,
      ),
      AppButtonVariant.secondary => OutlinedButton(
        onPressed: action,
        style: effectiveStyle,
        child: content,
      ),
      AppButtonVariant.text => TextButton(
        onPressed: action,
        style: effectiveStyle,
        child: content,
      ),
    };

    return Semantics(
      liveRegion: isLoading,
      child: AppFocusRing(
        borderRadius: BorderRadius.circular(AppTokens.radiusMd),
        child: button,
      ),
    );
  }

  /// The variant style resolved against the app theme, so loading can reuse
  /// its enabled colors.
  ButtonStyle? _variantStyle(BuildContext context) {
    final theme = Theme.of(context);
    return switch (variant) {
      AppButtonVariant.primary => theme.filledButtonTheme.style,
      AppButtonVariant.danger => _dangerStyle(
        context.appColors,
      ).merge(theme.filledButtonTheme.style),
      AppButtonVariant.secondary => theme.outlinedButtonTheme.style,
      AppButtonVariant.text => theme.textButtonTheme.style,
    };
  }

  /// While loading, the Material button is disabled to ignore taps, but it
  /// keeps the colors of its variant as on the web (only `disabled` greys out).
  static ButtonStyle? _keepEnabledLook(ButtonStyle? style) {
    if (style == null) return null;
    return ButtonStyle(
      backgroundColor: _enabledValue(style.backgroundColor),
      foregroundColor: _enabledValue(style.foregroundColor),
      side: _enabledValue(style.side),
    ).merge(style);
  }

  static WidgetStateProperty<T>? _enabledValue<T>(
    WidgetStateProperty<T>? property,
  ) {
    if (property == null) return null;
    return WidgetStatePropertyAll(property.resolve(const <WidgetState>{}));
  }

  static ButtonStyle _dangerStyle(AppColors colors) {
    return ButtonStyle(
      backgroundColor: WidgetStateProperty.resolveWith((states) {
        return states.contains(WidgetState.disabled)
            ? colors.disabledBackground
            : colors.danger;
      }),
    );
  }
}

class _ButtonContent extends StatelessWidget {
  const _ButtonContent({
    required this.label,
    required this.isLoading,
    required this.leadingIcon,
  });

  static const double _indicatorSize = AppTokens.fontSizeMd;

  final String label;
  final bool isLoading;
  final AppIcons? leadingIcon;

  @override
  Widget build(BuildContext context) {
    final foreground = DefaultTextStyle.of(context).style.color;
    final Widget? leading = isLoading
        ? SizedBox.square(
            dimension: _indicatorSize,
            child: CircularProgressIndicator(
              value: MediaQuery.disableAnimationsOf(context)
                  ? AppButton.reducedMotionSpinnerValue
                  : null,
              strokeWidth: AppTokens.borderWidthLg,
              color: foreground,
            ),
          )
        : leadingIcon == null
        ? null
        : AppIcon(leadingIcon!, size: _indicatorSize, color: foreground);

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (leading != null) ...[
          leading,
          const SizedBox(width: AppTokens.space2),
        ],
        Flexible(child: Text(label)),
      ],
    );
  }
}
