import 'package:material_ui/material_ui.dart';

import '../../../core/design/app_icon.dart';
import '../../../core/design/tokens.g.dart';
import '../app_focus_ring.dart';
import 'calendar_text.dart';
import 'calendar_types.dart';

/// Title of the period, previous and next buttons and the view switcher.
/// Every control is disabled while [isEnabled] is false (loading).
class CalendarToolbar extends StatelessWidget {
  const CalendarToolbar({
    super.key,
    required this.title,
    required this.view,
    required this.isEnabled,
    required this.onShiftPeriod,
    required this.onViewChanged,
  });

  final String title;
  final CalendarView view;
  final bool isEnabled;

  /// Called with `-1` for the previous period and `1` for the next one.
  final ValueChanged<int> onShiftPeriod;
  final ValueChanged<CalendarView> onViewChanged;

  @override
  Widget build(BuildContext context) {
    final navigation = periodNavigation[view]!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            _PeriodButton(
              icon: AppIcons.chevronLeft,
              tooltip: navigation.previous,
              onPressed: isEnabled ? () => onShiftPeriod(-1) : null,
            ),
            Expanded(
              child: Semantics(
                header: true,
                liveRegion: true,
                child: Text(
                  title,
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
            ),
            _PeriodButton(
              icon: AppIcons.chevronRight,
              tooltip: navigation.next,
              onPressed: isEnabled ? () => onShiftPeriod(1) : null,
            ),
          ],
        ),
        const SizedBox(height: AppTokens.space2),
        _ViewSwitcher(
          view: view,
          onViewChanged: isEnabled ? onViewChanged : null,
        ),
      ],
    );
  }
}

class _PeriodButton extends StatelessWidget {
  const _PeriodButton({
    required this.icon,
    required this.tooltip,
    required this.onPressed,
  });

  final AppIcons icon;
  final String tooltip;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return AppFocusRing(
      borderRadius: BorderRadius.circular(AppTokens.radiusPill),
      child: IconButton(
        tooltip: tooltip,
        color: Theme.of(context).colorScheme.primary,
        constraints: const BoxConstraints(
          minWidth: AppTokens.sizeTouchMin,
          minHeight: AppTokens.sizeTouchMin,
        ),
        onPressed: onPressed,
        icon: AppIcon(icon),
      ),
    );
  }
}

class _ViewSwitcher extends StatelessWidget {
  const _ViewSwitcher({required this.view, required this.onViewChanged});

  final CalendarView view;
  final ValueChanged<CalendarView>? onViewChanged;

  ButtonStyle _style(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    return ButtonStyle(
      minimumSize: const WidgetStatePropertyAll(
        Size.square(AppTokens.sizeTouchMin),
      ),
      textStyle: WidgetStatePropertyAll(textTheme.labelMedium),
      backgroundColor: WidgetStateProperty.resolveWith((states) {
        return states.contains(WidgetState.selected)
            ? scheme.primary
            : scheme.surface;
      }),
      foregroundColor: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.selected)) {
          return scheme.onPrimary;
        }
        return states.contains(WidgetState.disabled)
            ? scheme.onSurfaceVariant
            : scheme.primary;
      }),
      side: WidgetStatePropertyAll(
        BorderSide(color: scheme.primary, width: AppTokens.borderWidthSm),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final onViewChanged = this.onViewChanged;
    return SegmentedButton<CalendarView>(
      style: _style(context),
      expandedInsets: EdgeInsets.zero,
      selectedIcon: const AppIcon(AppIcons.check),
      segments: [
        for (final option in CalendarView.values)
          ButtonSegment(value: option, label: Text(viewNames[option]!)),
      ],
      selected: {view},
      onSelectionChanged: onViewChanged == null
          ? null
          : (selection) => onViewChanged(selection.single),
    );
  }
}
