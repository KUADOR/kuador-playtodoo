import 'package:flutter_test/flutter_test.dart';
import 'package:playtodoo/features/booking/domain/bookable_court.dart';
import 'package:playtodoo/features/booking/domain/player_booking.dart';
import 'package:playtodoo/features/booking/presentation/court_calendar_days.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_types.dart';

import '../booking_test_support.dart';

void main() {
  const oneDay = DateRange(from: testToday, to: testToday);

  test('builds the court slots from opening to closing', () {
    final days = buildCourtCalendarDays(
      court: testCourt,
      timeZone: testTimeZone,
      range: oneDay,
      occupied: const [],
    );

    expect(days.single.date, testToday);
    expect(days.single.slots.map((s) => s.start), [
      '18:00',
      '19:00',
      '20:00',
      '21:00',
      '22:00',
    ]);
  });

  test('a slot is occupied when any occupied range overlaps it, even '
      'partially', () {
    const court = BookableCourt(
      id: 'c',
      name: 'Cancha 2',
      slotDurationMinutes: 90,
      opensAt: Duration(hours: 18),
      closesAt: Duration(hours: 23),
    );
    final days = buildCourtCalendarDays(
      court: court,
      timeZone: testTimeZone,
      range: oneDay,
      // 20:00–21:00 local overlaps the 19:30–21:00 slot only.
      occupied: [OccupiedRange(start: localHour(20), end: localHour(21))],
    );

    expect(
      {for (final s in days.single.slots) s.start: s.availability},
      {
        '18:00': SlotAvailability.available,
        '19:30': SlotAvailability.occupied,
        '21:00': SlotAvailability.available,
      },
    );
  });

  test('the occupancy window covers every local day of the range in UTC', () {
    final window = occupancyWindow(
      const DateRange(from: '2026-11-30', to: '2026-12-06'),
      testTimeZone,
    );

    expect(window.from, DateTime.utc(2026, 11, 30, 3));
    expect(window.to, DateTime.utc(2026, 12, 7, 3));
  });

  test('the selected slot start is converted to UTC', () {
    expect(
      selectionStartUtc(
        const SlotSelection(date: testToday, start: '20:00'),
        testTimeZone,
      ),
      DateTime.utc(2026, 12, 4, 23),
    );
  });
}
