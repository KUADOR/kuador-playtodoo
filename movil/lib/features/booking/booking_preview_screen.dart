import 'package:material_ui/material_ui.dart';

import 'data/in_memory_bookings_api.dart';
import 'domain/bookable_court.dart';
import 'domain/player_booking.dart';
import 'presentation/court_booking_screen.dart';

/// Debug-only entry to try the player booking flow against the contract mock,
/// until the app talks to the real `api`.
///
/// Every value here is sample data, not business data: the court schedule
/// belongs to the «Complejos y canchas» module and the time zone of the
/// complex is [POR DEFINIR] (the offset matches the contract examples).
class BookingPreviewScreen extends StatefulWidget {
  const BookingPreviewScreen({super.key});

  static const BookableCourt sampleCourt = BookableCourt(
    id: 'sample-court-1',
    name: 'Cancha 1',
    slotDurationMinutes: 60,
    opensAt: Duration(hours: 8),
    closesAt: Duration(hours: 23),
  );
  static const String _otherCourtId = 'sample-court-2';
  static const ComplexTimeZone sampleTimeZone = ComplexTimeZone(
    utcOffset: Duration(hours: -3),
  );
  static const Duration _sampleLatency = Duration(milliseconds: 600);

  @override
  State<BookingPreviewScreen> createState() => _BookingPreviewScreenState();
}

class _BookingPreviewScreenState extends State<BookingPreviewScreen> {
  static DateTime _now() => DateTime.now().toUtc();

  late final String _today = _localDate(
    BookingPreviewScreen.sampleTimeZone.toLocal(_now()),
  );
  late final InMemoryBookingsApi _api = _sampleApi();

  InMemoryBookingsApi _sampleApi() {
    const court = BookingPreviewScreen.sampleCourt;
    const timeZone = BookingPreviewScreen.sampleTimeZone;
    DateTime at(int hour) => timeZone.toUtc(_today, Duration(hours: hour));
    return InMemoryBookingsApi(
      courtDurations: {
        court.id: court.slotDuration,
        BookingPreviewScreen._otherCourtId: court.slotDuration,
      },
      now: _now,
      latency: BookingPreviewScreen._sampleLatency,
      occupiedByOthers: {
        court.id: [
          OccupiedRange(start: at(18), end: at(19)),
          OccupiedRange(start: at(21), end: at(22)),
        ],
      },
      // The player already holds 20:00 on another court, to try the warning
      // of several bookings at the same time.
      playerBookings: [
        PlayerBooking(
          id: 'sample-own-booking',
          courtId: BookingPreviewScreen._otherCourtId,
          start: at(20),
          end: at(21),
          status: BookingStatus.confirmed,
          createdAt: _now(),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return CourtBookingScreen(
      api: _api,
      court: BookingPreviewScreen.sampleCourt,
      timeZone: BookingPreviewScreen.sampleTimeZone,
      today: _today,
      now: _now,
    );
  }
}

String _localDate(DateTime local) =>
    '${local.year}-${local.month.toString().padLeft(2, '0')}-'
    '${local.day.toString().padLeft(2, '0')}';
