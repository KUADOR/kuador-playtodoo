import 'package:flutter_test/flutter_test.dart';
import 'package:playtodoo/features/booking/data/in_memory_bookings_api.dart';
import 'package:playtodoo/features/booking/domain/booking_failures.dart';
import 'package:playtodoo/features/booking/domain/player_booking.dart';

import '../booking_test_support.dart';

Matcher _problem(BookingProblemCode code) =>
    isA<BookingProblem>().having((p) => p.code, 'code', code);

void main() {
  late FakeClock clock;
  late InMemoryBookingsApi api;

  setUp(() {
    clock = morningClock();
    api = testApi(
      clock,
      occupiedByOthers: [
        OccupiedRange(start: localHour(18), end: localHour(19)),
      ],
    );
  });

  test('creates a pending booking held for 5 minutes by the server', () async {
    final booking = await api.createPendingBooking(
      courtId: testCourt.id,
      start: localHour(20),
    );

    expect(booking.status, BookingStatus.pending);
    expect(booking.end, localHour(21));
    expect(booking.expiresAt, clock.current.add(const Duration(minutes: 5)));
  });

  test('the pending booking occupies the slot for everyone', () async {
    await api.createPendingBooking(courtId: testCourt.id, start: localHour(20));

    final occupied = await api.getCourtOccupancy(
      courtId: testCourt.id,
      from: localHour(0),
      to: localHour(24),
    );

    expect(occupied.map((r) => r.start), [localHour(18), localHour(20)]);
  });

  test('an overlapping request is SLOT_UNAVAILABLE', () {
    expect(
      api.createPendingBooking(courtId: testCourt.id, start: localHour(18)),
      throwsA(_problem(BookingProblemCode.slotUnavailable)),
    );
  });

  test('touching ranges do not conflict', () async {
    final booking = await api.createPendingBooking(
      courtId: testCourt.id,
      start: localHour(19),
    );

    expect(booking.status, BookingStatus.pending);
  });

  test('an own overlap on another court needs acknowledgement', () async {
    final own = await api.createPendingBooking(
      courtId: otherCourtId,
      start: localHour(20),
    );

    await expectLater(
      api.createPendingBooking(courtId: testCourt.id, start: localHour(20)),
      throwsA(
        isA<OwnOverlapProblem>().having(
          (p) => p.existingBooking.id,
          'existing booking',
          own.id,
        ),
      ),
    );
    final acknowledged = await api.createPendingBooking(
      courtId: testCourt.id,
      start: localHour(20),
      acknowledgeOwnOverlap: true,
    );
    expect(acknowledged.status, BookingStatus.pending);
  });

  test('a pending booking past expiresAt reads as EXPIRED, frees the slot '
      'and cannot be confirmed', () async {
    final booking = await api.createPendingBooking(
      courtId: testCourt.id,
      start: localHour(20),
    );
    clock.advance(const Duration(minutes: 5));

    expect((await api.getBooking(booking.id)).status, BookingStatus.expired);
    await expectLater(
      api.confirmBooking(booking.id),
      throwsA(_problem(BookingProblemCode.bookingExpired)),
    );
    final again = await api.createPendingBooking(
      courtId: testCourt.id,
      start: localHour(20),
    );
    expect(again.status, BookingStatus.pending);
  });

  test(
    'confirms within the hold, and confirming again is a safe retry',
    () async {
      final booking = await api.createPendingBooking(
        courtId: testCourt.id,
        start: localHour(20),
      );
      clock.advance(const Duration(minutes: 4));

      expect(
        (await api.confirmBooking(booking.id)).status,
        BookingStatus.confirmed,
      );
      clock.advance(const Duration(minutes: 10));
      expect(
        (await api.confirmBooking(booking.id)).status,
        BookingStatus.confirmed,
      );
    },
  );

  test(
    'cancelling frees the slot; only pending bookings can be cancelled',
    () async {
      final booking = await api.createPendingBooking(
        courtId: testCourt.id,
        start: localHour(20),
      );

      expect(
        (await api.cancelBooking(booking.id)).status,
        BookingStatus.cancelled,
      );
      await expectLater(
        api.cancelBooking(booking.id),
        throwsA(_problem(BookingProblemCode.bookingNotPending)),
      );
      await expectLater(
        api.confirmBooking(booking.id),
        throwsA(_problem(BookingProblemCode.bookingNotPending)),
      );
    },
  );

  test('unknown court and booking are not found', () {
    expect(
      api.createPendingBooking(courtId: 'nope', start: localHour(20)),
      throwsA(_problem(BookingProblemCode.courtNotFound)),
    );
    expect(
      api.getBooking('nope'),
      throwsA(_problem(BookingProblemCode.bookingNotFound)),
    );
  });

  test('offline calls fail with a connection failure', () {
    api.isOffline = true;

    expect(
      api.getCourtOccupancy(
        courtId: testCourt.id,
        from: localHour(0),
        to: localHour(24),
      ),
      throwsA(isA<BookingConnectionFailure>()),
    );
  });
}
