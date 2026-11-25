import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/features/booking/data/in_memory_bookings_api.dart';
import 'package:playtodoo/features/booking/domain/player_booking.dart';
import 'package:playtodoo/features/booking/presentation/booking_text.dart';
import 'package:playtodoo/features/booking/presentation/court_booking_screen.dart';
import 'package:playtodoo/features/booking/presentation/pending_booking_screen.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_slot_tile.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_text.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_types.dart';

import '../booking_test_support.dart';

const Duration _latency = Duration(milliseconds: 500);

Finder _slot(String start) => find.byWidgetPredicate(
  (widget) => widget is CalendarSlotTile && widget.slot.start == start,
);

SlotStatus _slotStatus(WidgetTester tester, String start) =>
    tester.widget<CalendarSlotTile>(_slot(start)).slot.status;

Finder get _reserveButton =>
    find.widgetWithText(FilledButton, BookingText.reserve);

Future<void> _pumpScreen(
  WidgetTester tester,
  InMemoryBookingsApi api,
  FakeClock clock,
) async {
  await pumpPhoneApp(
    tester,
    CourtBookingScreen(
      api: api,
      court: testCourt,
      timeZone: testTimeZone,
      today: testToday,
      now: clock.call,
    ),
  );
  await tester.pump(_latency);
}

Future<void> _selectAndReserve(WidgetTester tester, String start) async {
  await tester.tap(_slot(start));
  await tester.pump();
  await tester.tap(_reserveButton);
  await tester.pump();
}

/// Removes the pending screen so its countdown timer stops.
Future<void> _unmount(WidgetTester tester) async {
  await tester.pumpWidget(const SizedBox.shrink());
}

void main() {
  late FakeClock clock;

  setUp(() => clock = morningClock());

  testWidgets('while the slots load, the button is disabled and says why', (
    tester,
  ) async {
    final api = testApi(clock, latency: _latency);
    await pumpPhoneApp(
      tester,
      CourtBookingScreen(
        api: api,
        court: testCourt,
        timeZone: testTimeZone,
        today: testToday,
        now: clock.call,
      ),
    );

    expect(find.bySemanticsLabel(CalendarText.loading), findsOneWidget);
    expect(find.text(BookingText.chooseFreeSlot), findsOneWidget);
    expect(tester.widget<FilledButton>(_reserveButton).onPressed, isNull);

    await tester.pump(_latency);
    expect(_slot('20:00'), findsOneWidget);
  });

  testWidgets('occupied ranges from the server show as Ocupado and cannot be '
      'selected', (tester) async {
    final api = testApi(
      clock,
      occupiedByOthers: [
        OccupiedRange(start: localHour(18), end: localHour(19)),
      ],
    );
    await _pumpScreen(tester, api, clock);

    expect(_slotStatus(tester, '18:00'), SlotStatus.occupied);
    expect(find.bySemanticsLabel('18:00 a 19:00, Ocupado'), findsOneWidget);

    await tester.tap(_slot('18:00'));
    await tester.pump();
    expect(find.text(BookingText.chooseFreeSlot), findsOneWidget);
  });

  testWidgets('a load error offers Reintentar, which loads again', (
    tester,
  ) async {
    final api = testApi(clock)..isOffline = true;
    await _pumpScreen(tester, api, clock);

    expect(find.text(CalendarText.loadError), findsOneWidget);

    api.isOffline = false;
    await tester.tap(find.text(CalendarText.retry));
    await tester.pump();
    expect(_slot('20:00'), findsOneWidget);
  });

  testWidgets('choosing a slot shows it in the footer; reserving shows '
      '«Reservando…» and then the countdown', (tester) async {
    final api = testApi(clock, latency: _latency);
    await _pumpScreen(tester, api, clock);

    await tester.tap(_slot('20:00'));
    await tester.pump();
    expect(
      find.text('Cancha 1 · viernes 4 de diciembre de 2026 · 20:00–21:00'),
      findsOneWidget,
    );

    await tester.tap(_reserveButton);
    await tester.pump();
    expect(find.text(BookingText.reserving), findsOneWidget);

    await tester.pump(_latency);
    await tester.pumpAndSettle();
    expect(find.byType(PendingBookingScreen), findsOneWidget);
    expect(find.text('Tienes 5:00 para confirmar'), findsOneWidget);
    await _unmount(tester);
  });

  testWidgets('when someone took the slot first, a sheet says so; the slot '
      'shows as Ocupado and the selection is cleared', (tester) async {
    final api = testApi(clock);
    await _pumpScreen(tester, api, clock);
    api.occupyByOthers(
      testCourt.id,
      OccupiedRange(start: localHour(20), end: localHour(21)),
    );

    await _selectAndReserve(tester, '20:00');
    await tester.pumpAndSettle();

    expect(find.text(BookingText.slotUnavailable), findsOneWidget);
    expect(find.text(BookingText.chooseAnotherSlot), findsOneWidget);

    await tester.tap(find.text(BookingText.chooseAnotherSlot));
    await tester.pumpAndSettle();
    expect(find.text(BookingText.slotUnavailable), findsNothing);
    expect(find.text(BookingText.chooseFreeSlot), findsOneWidget);
    expect(_slotStatus(tester, '20:00'), SlotStatus.occupied);
  });

  group('the player already has a booking at that time', () {
    late InMemoryBookingsApi api;

    setUp(() {
      api = testApi(
        clock,
        playerBookings: [
          PlayerBooking(
            id: 'own',
            courtId: otherCourtId,
            start: localHour(20),
            end: localHour(21),
            status: BookingStatus.confirmed,
            createdAt: clock.current,
          ),
        ],
      );
    });

    testWidgets('a warning shows the existing booking; Volver creates '
        'nothing', (tester) async {
      await _pumpScreen(tester, api, clock);

      await _selectAndReserve(tester, '20:00');
      await tester.pumpAndSettle();

      expect(find.text(BookingText.ownOverlapBody), findsOneWidget);
      expect(
        find.descendant(
          of: find.byType(BottomSheet),
          matching: find.text('20:00–21:00'),
        ),
        findsOneWidget,
      );

      await tester.tap(find.text(BookingText.goBack));
      await tester.pumpAndSettle();

      expect(find.byType(PendingBookingScreen), findsNothing);
      final occupied = await api.getCourtOccupancy(
        courtId: testCourt.id,
        from: localHour(0),
        to: localHour(24),
      );
      expect(occupied, isEmpty);
    });

    testWidgets('Continuar sends the request again with the acknowledgement '
        'and opens the countdown', (tester) async {
      await _pumpScreen(tester, api, clock);

      await _selectAndReserve(tester, '20:00');
      await tester.pumpAndSettle();
      await tester.tap(find.text(BookingText.continueBooking));
      await tester.pumpAndSettle();

      expect(find.byType(PendingBookingScreen), findsOneWidget);
      await _unmount(tester);
    });
  });

  testWidgets('a connection failure while reserving is shown inline and the '
      'selection is kept', (tester) async {
    final api = testApi(clock);
    await _pumpScreen(tester, api, clock);
    api.isOffline = true;

    await _selectAndReserve(tester, '20:00');
    await tester.pump();

    expect(find.text(BookingText.reserveFailed), findsOneWidget);
    expect(_slotStatus(tester, '20:00'), SlotStatus.selected);
  });

  testWidgets('coming back from the countdown clears the selection and '
      'reloads the slots', (tester) async {
    final api = testApi(clock);
    await _pumpScreen(tester, api, clock);

    await _selectAndReserve(tester, '20:00');
    await tester.pumpAndSettle();
    await tester.tap(find.text(BookingText.cancel));
    await tester.pumpAndSettle();

    expect(find.byType(PendingBookingScreen), findsNothing);
    expect(find.text(BookingText.chooseFreeSlot), findsOneWidget);
    expect(_slotStatus(tester, '20:00'), SlotStatus.available);
  });

  testWidgets('meets the tap target and contrast guidelines', (tester) async {
    final handle = tester.ensureSemantics();
    final api = testApi(
      clock,
      occupiedByOthers: [
        OccupiedRange(start: localHour(18), end: localHour(19)),
      ],
    );
    await _pumpScreen(tester, api, clock);
    await tester.tap(_slot('20:00'));
    await tester.pumpAndSettle();

    await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
    await expectLater(tester, meetsGuideline(textContrastGuideline));
    handle.dispose();
  });
}
