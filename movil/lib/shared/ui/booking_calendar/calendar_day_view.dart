import 'package:material_ui/material_ui.dart';

import '../../../core/design/tokens.g.dart';
import 'calendar_keyboard.dart';
import 'calendar_model.dart';
import 'calendar_slot_tile.dart';

/// Slots of one day in a grid of [columnCount] columns.
class CalendarDayView extends StatelessWidget {
  const CalendarDayView({
    super.key,
    required this.day,
    required this.onActivateSlot,
  });

  /// Movil shows the day as a three-column grid (calendar.md · Diferencias).
  static const int columnCount = 3;

  final DayModel day;
  final ValueChanged<SlotModel> onActivateSlot;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final mutedColor = Theme.of(context).colorScheme.onSurfaceVariant;
    final emptyMessage = day.emptyMessage;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: [
        Row(
          children: [
            if (day.isToday) ...[
              const TodayMark(),
              const SizedBox(width: AppTokens.space3),
            ],
            Text(
              day.counts.summary,
              style: textTheme.bodySmall?.copyWith(color: mutedColor),
            ),
          ],
        ),
        const SizedBox(height: AppTokens.space3),
        if (emptyMessage != null)
          Text(emptyMessage, style: textTheme.bodyMedium)
        else
          CalendarKeyboardRegion(
            child: _SlotGrid(slots: day.slots, onActivateSlot: onActivateSlot),
          ),
      ],
    );
  }
}

class _SlotGrid extends StatelessWidget {
  const _SlotGrid({required this.slots, required this.onActivateSlot});

  final List<SlotModel> slots;
  final ValueChanged<SlotModel> onActivateSlot;

  @override
  Widget build(BuildContext context) {
    const columnCount = CalendarDayView.columnCount;
    final rows = [
      for (var start = 0; start < slots.length; start += columnCount)
        slots.skip(start).take(columnCount).toList(),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final (index, row) in rows.indexed) ...[
          if (index > 0) const SizedBox(height: AppTokens.space2),
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                for (var column = 0; column < columnCount; column++) ...[
                  if (column > 0) const SizedBox(width: AppTokens.space2),
                  Expanded(
                    child: column < row.length
                        ? CalendarSlotTile(
                            slot: row[column],
                            onActivate: onActivateSlot,
                          )
                        : const SizedBox.shrink(),
                  ),
                ],
              ],
            ),
          ),
        ],
      ],
    );
  }
}
