import '../domain/booking_failures.dart';
import '../domain/bookings_api.dart';
import '../domain/player_booking.dart';

/// Mock of the player endpoints of `contracts/bookings.openapi.yaml`, used
/// until the app talks to the real `api`. It keeps the contract rules: the
/// server sets a 5-minute hold, overlapping ranges conflict, the player's own
/// overlap in this complex needs acknowledgement, and reads return the
/// effective status.
class InMemoryBookingsApi implements BookingsApi {
  InMemoryBookingsApi({
    required this._courtDurations,
    required this._now,
    Map<String, List<OccupiedRange>> occupiedByOthers = const {},
    List<PlayerBooking> playerBookings = const [],
    this.latency = Duration.zero,
  }) : _occupiedByOthers = {
         for (final entry in occupiedByOthers.entries)
           entry.key: [...entry.value],
       },
       _playerBookings = [...playerBookings];

  /// The hold length the server applies (fixed by the product team).
  static const Duration holdLength = Duration(minutes: 5);

  /// Delay of every answer, to see the loading states while trying the app.
  final Duration latency;

  final Map<String, Duration> _courtDurations;
  final DateTime Function() _now;
  final Map<String, List<OccupiedRange>> _occupiedByOthers;
  final List<PlayerBooking> _playerBookings;
  int _nextId = 1;

  /// Makes the next calls fail as if the device were offline.
  bool isOffline = false;

  /// Simulates another person taking a range of [courtId].
  void occupyByOthers(String courtId, OccupiedRange range) {
    _occupiedByOthers.putIfAbsent(courtId, () => []).add(range);
  }

  @override
  Future<List<OccupiedRange>> getCourtOccupancy({
    required String courtId,
    required DateTime from,
    required DateTime to,
  }) async {
    await _respond();
    _requireCourt(courtId);
    final ranges =
        _activeRanges(courtId)
            .where((range) => range.overlaps(from, to))
            .toList()
          ..sort((a, b) => a.start.compareTo(b.start));
    return ranges;
  }

  @override
  Future<PlayerBooking> createPendingBooking({
    required String courtId,
    required DateTime start,
    bool acknowledgeOwnOverlap = false,
  }) async {
    await _respond();
    final end = start.add(_requireCourt(courtId));
    final taken = _activeRanges(courtId).any((r) => r.overlaps(start, end));
    if (taken) {
      throw const BookingProblem(BookingProblemCode.slotUnavailable);
    }
    if (!acknowledgeOwnOverlap) {
      final ownOverlap = _activePlayerBookings().where(
        (booking) => OccupiedRange(
          start: booking.start,
          end: booking.end,
        ).overlaps(start, end),
      );
      if (ownOverlap.isNotEmpty) {
        throw OwnOverlapProblem(ownOverlap.first);
      }
    }
    final createdAt = _now().toUtc();
    final booking = PlayerBooking(
      id: 'booking-${_nextId++}',
      courtId: courtId,
      start: start.toUtc(),
      end: end.toUtc(),
      status: BookingStatus.pending,
      createdAt: createdAt,
      expiresAt: createdAt.add(holdLength),
    );
    _playerBookings.add(booking);
    return booking;
  }

  @override
  Future<PlayerBooking> getBooking(String bookingId) async {
    await _respond();
    return _effective(_requireBooking(bookingId));
  }

  @override
  Future<PlayerBooking> confirmBooking(String bookingId) async {
    await _respond();
    final booking = _effective(_requireBooking(bookingId));
    return switch (booking.status) {
      BookingStatus.pending => _replace(booking, BookingStatus.confirmed),
      BookingStatus.confirmed => booking,
      BookingStatus.expired => throw const BookingProblem(
        BookingProblemCode.bookingExpired,
      ),
      BookingStatus.cancelled => throw const BookingProblem(
        BookingProblemCode.bookingNotPending,
      ),
    };
  }

  @override
  Future<PlayerBooking> cancelBooking(String bookingId) async {
    await _respond();
    final booking = _effective(_requireBooking(bookingId));
    if (booking.status != BookingStatus.pending) {
      throw const BookingProblem(BookingProblemCode.bookingNotPending);
    }
    return _replace(booking, BookingStatus.cancelled);
  }

  Future<void> _respond() async {
    if (latency > Duration.zero) {
      await Future<void>.delayed(latency);
    }
    if (isOffline) {
      throw const BookingConnectionFailure();
    }
  }

  Duration _requireCourt(String courtId) {
    final duration = _courtDurations[courtId];
    if (duration == null) {
      throw const BookingProblem(BookingProblemCode.courtNotFound);
    }
    return duration;
  }

  PlayerBooking _requireBooking(String bookingId) {
    for (final booking in _playerBookings) {
      if (booking.id == bookingId) return booking;
    }
    throw const BookingProblem(BookingProblemCode.bookingNotFound);
  }

  PlayerBooking _effective(PlayerBooking booking) {
    final expiresAt = booking.expiresAt;
    final isPastHold = expiresAt != null && !_now().isBefore(expiresAt);
    return booking.status == BookingStatus.pending && isPastHold
        ? booking.copyWith(status: BookingStatus.expired)
        : booking;
  }

  PlayerBooking _replace(PlayerBooking booking, BookingStatus status) {
    final updated = booking.copyWith(status: status);
    final index = _playerBookings.indexWhere((b) => b.id == booking.id);
    _playerBookings[index] = updated;
    return updated;
  }

  Iterable<PlayerBooking> _activePlayerBookings() => _playerBookings
      .map(_effective)
      .where(
        (booking) =>
            booking.status == BookingStatus.pending ||
            booking.status == BookingStatus.confirmed,
      );

  Iterable<OccupiedRange> _activeRanges(String courtId) => [
    ...?_occupiedByOthers[courtId],
    for (final booking in _activePlayerBookings())
      if (booking.courtId == courtId)
        OccupiedRange(start: booking.start, end: booking.end),
  ];
}
