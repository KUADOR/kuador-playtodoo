import 'package:flutter/semantics.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_colors.dart';
import 'package:playtodoo/core/design/app_icon.dart';
import 'package:playtodoo/features/booking/data/in_memory_bookings_api.dart';
import 'package:playtodoo/features/booking/domain/booking_failures.dart';
import 'package:playtodoo/features/booking/domain/player_booking.dart';
import 'package:playtodoo/features/booking/presentation/booking_text.dart';
import 'package:playtodoo/features/booking/presentation/pending_booking_screen.dart';

import '../booking_test_support.dart';

const Duration _tick = Duration(seconds: 1);
const String _openLabel = 'Abrir reserva';

/// The server answers that the hold is over on confirm, although the device
/// still shows time left (for example, a device clock behind the server).
class _ServerExpiredApi extends InMemoryBookingsApi {
  _ServerExpiredApi(FakeClock clock)
    : super(
        courtDurations: {testCourt.id: testCourt.slotDuration},
        now: clock.call,
      );

  @override
  Future<PlayerBooking> confirmBooking(String bookingId) async =>
      throw const BookingProblem(BookingProblemCode.bookingExpired);
}

/// Confirms after [_confirmLatency], to observe the confirming state.
class _SlowConfirmApi extends InMemoryBookingsApi {
  _SlowConfirmApi(FakeClock clock)
    : super(
        courtDurations: {testCourt.id: testCourt.slotDuration},
        now: clock.call,
      );

  static const Duration _confirmLatency = Duration(milliseconds: 500);

  @override
  Future<PlayerBooking> confirmBooking(String bookingId) async {
    await Future<void>.delayed(_confirmLatency);
    return super.confirmBooking(bookingId);
  }
}

/// Creates a pending booking for 20:00 and opens its screen from a host
/// route, so closing it can be observed.
Future<void> _openPending(
  WidgetTester tester,
  InMemoryBookingsApi api,
  FakeClock clock, {
  bool disableAnimations = false,
}) async {
  final booking = await api.createPendingBooking(
    courtId: testCourt.id,
    start: localHour(20),
  );
  await pumpPhoneApp(
    tester,
    Builder(
      builder: (context) => Scaffold(
        body: Center(
          child: TextButton(
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (_) => PendingBookingScreen(
                  api: api,
                  booking: booking,
                  courtName: testCourt.name,
                  timeZone: testTimeZone,
                  receivedAt: clock.current,
                  now: clock.call,
                ),
              ),
            ),
            child: const Text(_openLabel),
          ),
        ),
      ),
    ),
    disableAnimations: disableAnimations,
  );
  await tester.tap(find.text(_openLabel));
  await tester.pumpAndSettle();
  tester.takeAnnouncements();
}

/// Moves the device clock and lets the countdown tick.
Future<void> _elapse(
  WidgetTester tester,
  FakeClock clock,
  Duration time,
) async {
  clock.advance(time);
  await tester.pump(_tick);
}

Future<void> _closeAll(WidgetTester tester) =>
    tester.pumpWidget(const SizedBox.shrink());

void main() {
  late FakeClock clock;
  late InMemoryBookingsApi api;

  setUp(() {
    clock = morningClock();
    api = testApi(clock);
  });

  testWidgets('shows the countdown from 5:00, the booking and the help text, '
      'and announces the start', (tester) async {
    final booking = await api.createPendingBooking(
      courtId: testCourt.id,
      start: localHour(20),
    );
    await pumpPhoneApp(
      tester,
      PendingBookingScreen(
        api: api,
        booking: booking,
        courtName: testCourt.name,
        timeZone: testTimeZone,
        receivedAt: clock.current,
        now: clock.call,
      ),
    );
    await tester.pump();

    expect(find.text('Tienes 5:00 para confirmar'), findsOneWidget);
    expect(find.text('Cancha 1'), findsOneWidget);
    expect(find.text('viernes 4 de diciembre de 2026'), findsOneWidget);
    expect(find.text('20:00–21:00'), findsOneWidget);
    expect(find.text('60 minutos'), findsOneWidget);
    expect(find.text(BookingText.holdHelp), findsOneWidget);
    expect(tester.takeAnnouncements(), [
      isAccessibilityAnnouncement(
        'Tienes 5:00 para confirmar',
        assertiveness: Assertiveness.polite,
      ),
    ]);
    await _closeAll(tester);
  });

  testWidgets('counts down each second without announcing every second', (
    tester,
  ) async {
    await _openPending(tester, api, clock);

    await _elapse(tester, clock, const Duration(seconds: 28));
    expect(find.text('Tienes 4:32 para confirmar'), findsOneWidget);
    await _elapse(tester, clock, _tick);
    expect(find.text('Tienes 4:31 para confirmar'), findsOneWidget);
    expect(tester.takeAnnouncements(), isEmpty);
    await _closeAll(tester);
  });

  testWidgets('announces 1:00 and 0:30, and the last minute turns to the '
      'warning color with an alert icon', (tester) async {
    await _openPending(tester, api, clock);
    final textFinder = find.textContaining('para confirmar');

    expect(find.byIcon(AppIcons.clock.data), findsOneWidget);
    await _elapse(tester, clock, const Duration(minutes: 4));
    expect(find.text('Tienes 1:00 para confirmar'), findsOneWidget);
    expect(find.byIcon(AppIcons.warning.data), findsOneWidget);
    expect(
      tester.widget<Text>(textFinder).style?.color,
      AppColors.light.warning,
    );
    expect(tester.takeAnnouncements(), [
      isAccessibilityAnnouncement('Te queda 1 minuto para confirmar'),
    ]);

    await _elapse(tester, clock, const Duration(seconds: 30));
    expect(tester.takeAnnouncements(), [
      isAccessibilityAnnouncement('Te quedan 30 segundos para confirmar'),
    ]);
    await _closeAll(tester);
  });

  testWidgets('at 0:00 it shows «Tu tiempo terminó» and announces it '
      'assertively; «Elegir un horario» goes back', (tester) async {
    await _openPending(tester, api, clock);

    await _elapse(tester, clock, const Duration(minutes: 5));
    await tester.pumpAndSettle();

    expect(find.text(BookingText.expiredTitle), findsOneWidget);
    expect(find.text(BookingText.expiredBody), findsOneWidget);
    expect(tester.takeAnnouncements(), [
      isAccessibilityAnnouncement(
        BookingText.expiredTitle,
        assertiveness: Assertiveness.assertive,
      ),
    ]);

    await tester.tap(find.text(BookingText.chooseSlot));
    await tester.pumpAndSettle();
    expect(find.byType(PendingBookingScreen), findsNothing);
  });

  testWidgets('confirming within the hold shows «Reserva confirmada»', (
    tester,
  ) async {
    await _openPending(tester, api, clock);
    await _elapse(tester, clock, const Duration(minutes: 2));

    await tester.tap(find.text(BookingText.confirm));
    await tester.pumpAndSettle();

    expect(find.text(BookingText.confirmedTitle), findsOneWidget);
    expect(find.text('Cancha 1'), findsOneWidget);
    expect(find.textContaining('para confirmar'), findsNothing);
  });

  testWidgets('while confirming, the button says «Confirmando…» and Cancelar '
      'is disabled', (tester) async {
    await _openPending(tester, _SlowConfirmApi(clock), clock);

    await tester.tap(find.text(BookingText.confirm));
    await tester.pump();

    expect(find.text(BookingText.confirming), findsOneWidget);
    expect(
      tester
          .widget<TextButton>(
            find.widgetWithText(TextButton, BookingText.cancel),
          )
          .onPressed,
      isNull,
    );
    await tester.pumpAndSettle();
    expect(find.text(BookingText.confirmedTitle), findsOneWidget);
  });

  testWidgets('a network error on confirm offers Reintentar and the countdown '
      'goes on', (tester) async {
    await _openPending(tester, api, clock);
    api.isOffline = true;

    await tester.tap(find.text(BookingText.confirm));
    await tester.pump();
    expect(find.text(BookingText.confirmFailed), findsOneWidget);

    await _elapse(tester, clock, const Duration(seconds: 10));
    expect(find.text('Tienes 4:50 para confirmar'), findsOneWidget);

    api.isOffline = false;
    await tester.tap(find.text(BookingText.retry));
    await tester.pumpAndSettle();
    expect(find.text(BookingText.confirmedTitle), findsOneWidget);
  });

  testWidgets('BOOKING_EXPIRED on confirm shows «Tu tiempo terminó», not a '
      'technical error', (tester) async {
    await _openPending(tester, _ServerExpiredApi(clock), clock);

    await tester.tap(find.text(BookingText.confirm));
    await tester.pumpAndSettle();

    expect(find.text(BookingText.expiredTitle), findsOneWidget);
    expect(find.text(BookingText.confirmFailed), findsNothing);
  });

  testWidgets('back from the background, the remaining time is recomputed '
      'from expiresAt', (tester) async {
    await _openPending(tester, api, clock);

    clock.advance(const Duration(minutes: 3, seconds: 30));
    sendAppToBackgroundAndBack(tester);
    await tester.pump();

    expect(find.text('Tienes 1:30 para confirmar'), findsOneWidget);
    await _closeAll(tester);
  });

  testWidgets('back from the background after the hold ended, it shows «Tu '
      'tiempo terminó»', (tester) async {
    await _openPending(tester, api, clock);

    clock.advance(const Duration(minutes: 6));
    sendAppToBackgroundAndBack(tester);
    await tester.pumpAndSettle();

    expect(find.text(BookingText.expiredTitle), findsOneWidget);
  });

  testWidgets('Cancelar releases the slot and goes back', (tester) async {
    await _openPending(tester, api, clock);

    await tester.tap(find.text(BookingText.cancel));
    await tester.pumpAndSettle();

    expect(find.byType(PendingBookingScreen), findsNothing);
    final occupied = await api.getCourtOccupancy(
      courtId: testCourt.id,
      from: localHour(0),
      to: localHour(24),
    );
    expect(occupied, isEmpty);
  });

  testWidgets('with reduced motion the progress bar is hidden', (tester) async {
    await _openPending(tester, api, clock, disableAnimations: true);

    expect(find.byType(LinearProgressIndicator), findsNothing);
    expect(find.text('Tienes 5:00 para confirmar'), findsOneWidget);
    await _closeAll(tester);
  });

  testWidgets('meets the tap target and contrast guidelines', (tester) async {
    final handle = tester.ensureSemantics();
    await _openPending(tester, api, clock);

    await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
    await expectLater(tester, meetsGuideline(textContrastGuideline));
    await _elapse(tester, clock, const Duration(minutes: 4, seconds: 30));
    await expectLater(tester, meetsGuideline(textContrastGuideline));
    handle.dispose();
    await _closeAll(tester);
  });
}
