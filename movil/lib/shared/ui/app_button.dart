import 'package:material_ui/material_ui.dart';

import '../../core/design/app_colors.dart';
import '../../core/design/app_icon.dart';
import '../../core/design/tokens.g.dart';
import 'app_focus_ring.dart';

enum AppButtonVariant { primary, secondary, danger, text }

/// Brand button built on Material buttons, which keep the native ripple,
/// keyboard activation and semantics.
///
/// A null [onPressed] disables it. While [isLoading] it ignores taps, shows a
/// spinner with [loadingLabel] and announces it.
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

    final Widget button = switch (variant) {
      AppButtonVariant.primary => FilledButton(
        onPressed: action,
        child: content,
      ),
      AppButtonVariant.secondary => OutlinedButton(
        onPressed: action,
        child: content,
      ),
      AppButtonVariant.danger => FilledButton(
        onPressed: action,
        style: _dangerStyle(context.appColors),
        child: content,
      ),
      AppButtonVariant.text => TextButton(onPressed: action, child: content),
    };

    return Semantics(
      liveRegion: isLoading,
      child: AppFocusRing(
        borderRadius: BorderRadius.circular(AppTokens.radiusMd),
        child: button,
      ),
    );
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
