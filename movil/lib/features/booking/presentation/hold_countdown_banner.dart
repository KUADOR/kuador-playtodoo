import 'package:material_ui/material_ui.dart';

import '../../../core/design/app_colors.dart';
import '../../../core/design/app_icon.dart';
import '../../../core/design/tokens.g.dart';
import '../domain/hold_countdown.dart';
import 'booking_text.dart';

/// Countdown of a pending booking (Pantallas · componente 2): clock icon,
/// «Tienes 4:32 para confirmar» and a progress bar. In the last minute it
/// turns to the warning color, with an alert icon and bold text.
///
/// The text is not a live region on purpose: the screen announces only the
/// milestones, never every second. With reduced motion
/// the bar is hidden and only the text remains.
class HoldCountdownBanner extends StatelessWidget {
  const HoldCountdownBanner({
    super.key,
    required this.remaining,
    required this.fractionLeft,
  });

  static const double _barHeight = AppTokens.space2;

  final Duration remaining;
  final double fractionLeft;

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    final scheme = Theme.of(context).colorScheme;
    final isWarning = HoldCountdown.isWarning(remaining);
    final color = isWarning ? colors.warning : scheme.primary;
    final textStyle = Theme.of(context).textTheme.bodyLarge?.copyWith(
      color: isWarning ? colors.warning : null,
      fontWeight: isWarning
          ? AppTokens.fontWeightExtrabold
          : AppTokens.fontWeightSemibold,
    );
    final reduceMotion = MediaQuery.disableAnimationsOf(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            AppIcon(
              isWarning ? AppIcons.warning : AppIcons.clock,
              color: color,
            ),
            const SizedBox(width: AppTokens.space2),
            Expanded(
              child: Text(formatHoldRemaining(remaining), style: textStyle),
            ),
          ],
        ),
        if (!reduceMotion) ...[
          const SizedBox(height: AppTokens.space3),
          ExcludeSemantics(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(AppTokens.radiusPill),
              child: LinearProgressIndicator(
                value: fractionLeft,
                minHeight: _barHeight,
                color: color,
                backgroundColor: colors.surfaceAlt,
              ),
            ),
          ),
        ],
      ],
    );
  }
}
