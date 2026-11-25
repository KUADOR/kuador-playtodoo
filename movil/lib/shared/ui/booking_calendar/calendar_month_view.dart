import 'dart:math' as math;

import 'package:material_ui/material_ui.dart';

import '../../../core/design/app_colors.dart';
import '../../../core/design/tokens.g.dart';
import '../app_focus_ring.dart';
import 'calendar_dates.dart';
import 'calendar_keyboard.dart';
import 'calendar_model.dart';
import 'calendar_slot_tile.dart';
import 'calendar_text.dart';
import 'calendar_types.dart';

/// Monday-to-Sunday grid of the month with the summary of each day.
/// Choosing a day calls [onOpenDay]. When seven columns do not fit, the grid
/// scrolls horizontally.
class CalendarMonthView extends StatelessWidget {
  const CalendarMonthView({
    super.key,
    required this.month,
    required this.onOpenDay,
  });

  /// Narrowest day column that still fits «Sin horarios» in two lines.
  static const double _minColumnWidth = 84;

  final MonthModel month;
  final ValueChanged<IsoDate> onOpenDay;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columnWidth = math.max(
          constraints.maxWidth / daysPerWeek,
          _minColumnWidth,
        );
        return SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: CalendarKeyboardRegion(
            child: Table(
              defaultColumnWidth: FixedColumnWidth(columnWidth),
              children: [
                TableRow(
                  children: [
                    for (final header in month.weekdayHeaders)
                      Padding(
                        padding: const EdgeInsets.all(AppTokens.space1),
                        child: Text(
                          header,
                          textAlign: TextAlign.center,
                          style: Theme.of(context).textTheme.labelSmall,
                        ),
                      ),
                  ],
                ),
                for (final week in month.weeks)
                  TableRow(
                    children: [
                      for (final day in week)
                        Padding(
                          padding: const EdgeInsets.all(AppTokens.space1),
                          child: day == null
                              ? const SizedBox.shrink()
                              : _MonthDayTile(day: day, onOpenDay: onOpenDay),
                        ),
                    ],
                  ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _MonthDayTile extends StatelessWidget {
  const _MonthDayTile({required this.day, required this.onOpenDay});

  final MonthDayModel day;
  final ValueChanged<IsoDate> onOpenDay;

  static final BorderRadius _radius = BorderRadius.circular(AppTokens.radiusMd);

  String get _semanticLabel => [
    formatLongDate(day.date),
    if (day.isToday) CalendarText.today,
    day.counts.summary,
  ].join(', ');

  @override
  Widget build(BuildContext context) {
    final colors = context.appColors;
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final hasFreeSlots = day.counts.availableCount > 0;

    return Semantics(
      container: true,
      excludeSemantics: true,
      button: true,
      label: _semanticLabel,
      onTap: () => onOpenDay(day.date),
      child: AppFocusRing(
        borderRadius: _radius,
        child: Material(
          type: MaterialType.transparency,
          child: Ink(
            decoration: BoxDecoration(
              color: hasFreeSlots ? colors.accentTint : scheme.surface,
              border: Border.all(
                color: day.isToday ? scheme.primary : scheme.outlineVariant,
                width: day.isToday
                    ? AppTokens.borderWidthLg
                    : AppTokens.borderWidthSm,
              ),
              borderRadius: _radius,
            ),
            child: InkWell(
              onTap: () => onOpenDay(day.date),
              borderRadius: _radius,
              child: ConstrainedBox(
                constraints: const BoxConstraints(
                  minWidth: AppTokens.sizeTouchMin,
                  minHeight: AppTokens.sizeTouchMin,
                ),
                child: Padding(
                  padding: const EdgeInsets.all(AppTokens.space1),
                  child: Column(
                    children: [
                      Text('${day.dayOfMonth}', style: textTheme.titleSmall),
                      if (day.isToday) const TodayMark(),
                      Text(
                        day.counts.summary,
                        textAlign: TextAlign.center,
                        style: textTheme.labelSmall?.copyWith(
                          color: hasFreeSlots
                              ? colors.brandNavy
                              : scheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
