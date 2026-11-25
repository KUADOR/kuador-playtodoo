import '../../../shared/ui/booking_calendar/calendar_dates.dart';
import '../../../shared/ui/booking_calendar/calendar_types.dart';
import '../domain/bookable_court.dart';
import '../domain/player_booking.dart';

/// The calendar days of [range] for [court]: its slots from opening to
/// closing, each occupied when it overlaps any of [occupied] (UTC).
List<CalendarDay> buildCourtCalendarDays({
  required BookableCourt court,
  required ComplexTimeZone timeZone,
  required DateRange range,
  required List<OccupiedRange> occupied,
}) {
  return [
    for (final date in datesBetween(range.from, range.to))
      CalendarDay(
        date: date,
        slots: [
          for (final start in _slotStarts(court))
            CalendarSlot(
              start: _formatTimeOfDay(start),
              availability:
                  _isOccupied(
                    timeZone.toUtc(date, start),
                    court.slotDuration,
                    occupied,
                  )
                  ? SlotAvailability.occupied
                  : SlotAvailability.available,
            ),
        ],
      ),
  ];
}

/// UTC range `[from, to)` covering every local day of [range].
({DateTime from, DateTime to}) occupancyWindow(
  DateRange range,
  ComplexTimeZone timeZone,
) => (
  from: timeZone.toUtc(range.from, Duration.zero),
  to: timeZone.toUtc(addDays(range.to, 1), Duration.zero),
);

/// UTC start of the [selection] slot.
DateTime selectionStartUtc(SlotSelection selection, ComplexTimeZone timeZone) {
  final [hours, minutes] = selection.start.split(':').map(int.parse).toList();
  return timeZone.toUtc(
    selection.date,
    Duration(hours: hours, minutes: minutes),
  );
}

Iterable<Duration> _slotStarts(BookableCourt court) sync* {
  for (
    var start = court.opensAt;
    start + court.slotDuration <= court.closesAt;
    start += court.slotDuration
  ) {
    yield start;
  }
}

bool _isOccupied(
  DateTime startUtc,
  Duration duration,
  List<OccupiedRange> occupied,
) {
  final endUtc = startUtc.add(duration);
  return occupied.any((range) => range.overlaps(startUtc, endUtc));
}

String _formatTimeOfDay(Duration time) {
  String twoDigits(int value) => value.toString().padLeft(2, '0');
  final hours = time.inHours;
  final minutes = time.inMinutes % Duration.minutesPerHour;
  return '${twoDigits(hours)}:${twoDigits(minutes)}';
}
