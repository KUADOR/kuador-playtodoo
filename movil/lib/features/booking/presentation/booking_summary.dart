import 'package:material_ui/material_ui.dart';

import '../../../core/design/tokens.g.dart';
import '../../../shared/ui/booking_calendar/calendar_text.dart';
import '../domain/bookable_court.dart';
import 'booking_text.dart';

/// Court, day, time and duration of a booking, in the local time of the
/// complex. [courtName] is omitted when the screen does not know it.
class BookingSummary extends StatelessWidget {
  const BookingSummary({
    super.key,
    required this.start,
    required this.end,
    required this.timeZone,
    this.courtName,
  });

  final DateTime start;
  final DateTime end;
  final ComplexTimeZone timeZone;
  final String? courtName;

  @override
  Widget build(BuildContext context) {
    final localStart = timeZone.toLocal(start);
    final localEnd = timeZone.toLocal(end);
    final rows = [
      if (courtName != null) (BookingText.courtLabel, courtName!),
      (BookingText.dateLabel, formatLongDate(_isoDate(localStart))),
      (
        BookingText.timeLabel,
        formatSlotLabel(_localTime(localStart), _localTime(localEnd)),
      ),
      (BookingText.durationLabel, formatDuration(end.difference(start))),
    ];
    final textTheme = Theme.of(context).textTheme;
    final mutedColor = Theme.of(context).colorScheme.onSurfaceVariant;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final (index, (label, value)) in rows.indexed) ...[
          if (index > 0) const SizedBox(height: AppTokens.space2),
          MergeSemantics(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Text(
                    label,
                    style: textTheme.labelMedium?.copyWith(color: mutedColor),
                  ),
                ),
                Expanded(
                  flex: 2,
                  child: Text(value, style: textTheme.bodyMedium),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }
}

String _twoDigits(int value) => value.toString().padLeft(2, '0');

String _isoDate(DateTime local) =>
    '${local.year}-${_twoDigits(local.month)}-${_twoDigits(local.day)}';

String _localTime(DateTime local) =>
    '${_twoDigits(local.hour)}:${_twoDigits(local.minute)}';
