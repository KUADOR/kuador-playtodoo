import 'package:material_ui/material_ui.dart';

import '../../../core/design/tokens.g.dart';
import 'calendar_keyboard.dart';
import 'calendar_model.dart';
import 'calendar_slot_tile.dart';
import 'calendar_text.dart';

/// Monday-to-Sunday columns with one row per start time. It scrolls
/// horizontally because seven columns do not fit on a phone.
class CalendarWeekView extends StatelessWidget {
  const CalendarWeekView({
    super.key,
    required this.week,
    required this.onActivateSlot,
  });

  /// Wide enough for a slot label (`22:30–00:00`) on one line.
  static const double _dayColumnWidth = 128;
  static const double _timeColumnWidth = AppTokens.space7 + AppTokens.space2;
  static const EdgeInsets _cellPadding = EdgeInsets.all(AppTokens.space1);

  final WeekModel week;
  final ValueChanged<SlotModel> onActivateSlot;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final mutedColor = Theme.of(context).colorScheme.onSurfaceVariant;
    final dayContexts = [
      for (final column in week.columns) formatLongDate(column.date),
    ];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: CalendarKeyboardRegion(
        child: Table(
          defaultColumnWidth: const FixedColumnWidth(_dayColumnWidth),
          columnWidths: const {0: FixedColumnWidth(_timeColumnWidth)},
          defaultVerticalAlignment: TableCellVerticalAlignment.middle,
          children: [
            TableRow(
              children: [
                Padding(
                  padding: _cellPadding,
                  child: Text(CalendarText.time, style: textTheme.labelSmall),
                ),
                for (final column in week.columns)
                  Padding(
                    padding: _cellPadding,
                    child: _ColumnHeader(column: column),
                  ),
              ],
            ),
            for (final (rowIndex, start) in week.rows.indexed)
              TableRow(
                children: [
                  Padding(
                    padding: _cellPadding,
                    child: Text(
                      start,
                      style: textTheme.labelSmall?.copyWith(color: mutedColor),
                    ),
                  ),
                  for (final (columnIndex, slot)
                      in week.cells[rowIndex].indexed)
                    Padding(
                      padding: _cellPadding,
                      child: slot == null
                          ? const SizedBox(height: AppTokens.sizeTouchMin)
                          : CalendarSlotTile(
                              slot: slot,
                              semanticHint: dayContexts[columnIndex],
                              onActivate: onActivateSlot,
                            ),
                    ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}

class _ColumnHeader extends StatelessWidget {
  const _ColumnHeader({required this.column});

  final WeekColumnModel column;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final mutedColor = Theme.of(context).colorScheme.onSurfaceVariant;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(column.header, style: textTheme.labelMedium),
        if (column.isToday) const TodayMark(),
        Text(
          column.summary,
          style: textTheme.bodySmall?.copyWith(color: mutedColor),
        ),
      ],
    );
  }
}
