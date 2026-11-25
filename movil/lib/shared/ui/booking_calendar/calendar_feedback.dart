import 'package:material_ui/material_ui.dart';

import '../../../core/design/app_colors.dart';
import '../../../core/design/app_icon.dart';
import '../../../core/design/tokens.g.dart';
import '../app_button.dart';
import '../app_skeleton.dart';
import 'calendar_text.dart';

/// Loading state of the calendar.
class CalendarSkeleton extends StatelessWidget {
  const CalendarSkeleton({super.key});

  static const List<double> _rowWidthFactors = [1, 1, 1, 1];

  @override
  Widget build(BuildContext context) {
    return const AppSkeleton(
      label: CalendarText.loading,
      lineWidthFactors: _rowWidthFactors,
      lineHeight: AppTokens.sizeTouchMin,
    );
  }
}

/// Error state: what happened and «Reintentar», which calls [onRetry].
class CalendarError extends StatelessWidget {
  const CalendarError({super.key, required this.onRetry});

  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final danger = context.appColors.danger;
    return Semantics(
      liveRegion: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              AppIcon(AppIcons.alert, color: danger),
              const SizedBox(width: AppTokens.space2),
              Expanded(
                child: Text(
                  CalendarText.loadError,
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppTokens.space3),
          AppButton(
            label: CalendarText.retry,
            variant: AppButtonVariant.secondary,
            onPressed: onRetry,
          ),
        ],
      ),
    );
  }
}

/// Fixed notice of the readonly mode.
class CalendarReadonlyNotice extends StatelessWidget {
  const CalendarReadonlyNotice({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    final mutedColor = Theme.of(context).colorScheme.onSurfaceVariant;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: colors.surfaceAlt,
        borderRadius: BorderRadius.circular(AppTokens.radiusMd),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppTokens.space3,
          vertical: AppTokens.space2,
        ),
        child: Row(
          children: [
            AppIcon(AppIcons.eye, color: mutedColor),
            const SizedBox(width: AppTokens.space2),
            Text(
              CalendarText.readonly,
              style: Theme.of(context).textTheme.labelMedium
                  ?.copyWith(color: mutedColor),
            ),
          ],
        ),
      ),
    );
  }
}
