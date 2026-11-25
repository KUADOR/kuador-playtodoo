import 'package:flutter/foundation.dart';

/// Effective status of a booking, as `contracts/bookings.openapi.yaml`
/// (`BookingStatus`) reports it: a pending booking past `expiresAt` is
/// already [expired].
enum BookingStatus { pending, confirmed, cancelled, expired }

/// A booking of the signed-in player (`PlayerBooking` in the contract).
/// Instants are UTC, as the API returns them.
@immutable
class PlayerBooking {
  const PlayerBooking({
    required this.id,
    required this.courtId,
    required this.start,
    required this.end,
    required this.status,
    required this.createdAt,
    this.expiresAt,
  });

  final String id;
  final String courtId;
  final DateTime start;
  final DateTime end;
  final BookingStatus status;
  final DateTime createdAt;

  /// End of the 5-minute hold, set by the server. Null once it no longer
  /// applies.
  final DateTime? expiresAt;

  Duration get duration => end.difference(start);

  PlayerBooking copyWith({BookingStatus? status}) => PlayerBooking(
    id: id,
    courtId: courtId,
    start: start,
    end: end,
    status: status ?? this.status,
    createdAt: createdAt,
    expiresAt: expiresAt,
  );
}

/// A time range taken on a court (`OccupiedRange`). The contract does not say
/// whether it is pending, confirmed or a block: players only see «Ocupado».
@immutable
class OccupiedRange {
  const OccupiedRange({required this.start, required this.end});

  final DateTime start;
  final DateTime end;

  /// Half-open ranges `[start, end)`: touching ranges do not overlap.
  bool overlaps(DateTime otherStart, DateTime otherEnd) =>
      start.isBefore(otherEnd) && otherStart.isBefore(end);
}
