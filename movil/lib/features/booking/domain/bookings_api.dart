import 'player_booking.dart';

/// Player endpoints of `contracts/bookings.openapi.yaml` (tag «Player
/// bookings»). The tenant is the one resolved by the subdomain the
/// implementation talks to.
///
/// Every method throws [BookingProblem] when the API answers with a Problem
/// Details `code`, and [BookingConnectionFailure] when it cannot be reached.
abstract interface class BookingsApi {
  /// `GET /courts/{courtId}/occupancy`: ranges overlapping `[from, to)`.
  Future<List<OccupiedRange>> getCourtOccupancy({
    required String courtId,
    required DateTime from,
    required DateTime to,
  });

  /// `POST /bookings`: a `PENDING` booking that holds the slot until
  /// `expiresAt`. The server computes the end from the court duration.
  Future<PlayerBooking> createPendingBooking({
    required String courtId,
    required DateTime start,
    bool acknowledgeOwnOverlap = false,
  });

  /// `GET /bookings/{bookingId}`: the effective status.
  Future<PlayerBooking> getBooking(String bookingId);

  /// `POST /bookings/{bookingId}/confirm`.
  Future<PlayerBooking> confirmBooking(String bookingId);

  /// `POST /bookings/{bookingId}/cancel`.
  Future<PlayerBooking> cancelBooking(String bookingId);
}
