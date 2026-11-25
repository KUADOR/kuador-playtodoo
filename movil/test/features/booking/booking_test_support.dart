import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:playtodoo/core/design/app_theme.dart';
import 'package:playtodoo/features/booking/data/in_memory_bookings_api.dart';
import 'package:playtodoo/features/booking/domain/bookable_court.dart';
import 'package:playtodoo/features/booking/domain/player_booking.dart';

/// Friday 4 December 2026, as in the contract examples.
const String testToday = '2026-12-04';

const ComplexTimeZone testTimeZone = ComplexTimeZone(
  utcOffset: Duration(hours: -3),
);

/// Five 60-minute slots: 18:00 … 22:00.
const BookableCourt testCourt = BookableCourt(
  id: 'court-1',
  name: 'Cancha 1',
  slotDurationMinutes: 60,
  opensAt: Duration(hours: 18),
  closesAt: Duration(hours: 23),
);

const String otherCourtId = 'court-2';

const Size phoneSize = Size(390, 844);

/// Local [hour] of [testToday] in UTC.
DateTime localHour(int hour) =>
    testTimeZone.toUtc(testToday, Duration(hours: hour));

/// Device clock the tests move by hand.
class FakeClock {
  FakeClock(this.current);

  DateTime current;

  DateTime call() => current;

  void advance(Duration duration) => current = current.add(duration);
}

/// A clock at 12:00 local time of [testToday].
FakeClock morningClock() => FakeClock(localHour(12));

InMemoryBookingsApi testApi(
  FakeClock clock, {
  List<OccupiedRange> occupiedByOthers = const [],
  List<PlayerBooking> playerBookings = const [],
  Duration latency = Duration.zero,
}) => InMemoryBookingsApi(
  courtDurations: {
    testCourt.id: testCourt.slotDuration,
    otherCourtId: testCourt.slotDuration,
  },
  now: clock.call,
  occupiedByOthers: {testCourt.id: occupiedByOthers},
  playerBookings: playerBookings,
  latency: latency,
);

/// Pumps [home] in the app theme on a phone-sized screen.
Future<void> pumpPhoneApp(
  WidgetTester tester,
  Widget home, {
  bool disableAnimations = false,
}) {
  tester.view
    ..devicePixelRatio = 1
    ..physicalSize = phoneSize;
  addTearDown(tester.view.reset);
  return tester.pumpWidget(
    MediaQuery(
      data: MediaQueryData(
        size: phoneSize,
        disableAnimations: disableAnimations,
      ),
      child: MaterialApp(theme: AppTheme.light(), home: home),
    ),
  );
}

/// Sends the app to the background and back, as the platform does.
void sendAppToBackgroundAndBack(WidgetTester tester) {
  const away = [
    AppLifecycleState.inactive,
    AppLifecycleState.hidden,
    AppLifecycleState.paused,
  ];
  const back = [
    AppLifecycleState.hidden,
    AppLifecycleState.inactive,
    AppLifecycleState.resumed,
  ];
  for (final state in [...away, ...back]) {
    tester.binding.handleAppLifecycleStateChanged(state);
  }
}
