import 'player_booking.dart';

/// Stable `code` of the API Problem Details (`ProblemCode` in the contract)
/// that the player flow handles. Any other code is [unexpected].
enum BookingProblemCode {
  slotUnavailable,
  bookingExpired,
  ownOverlapNeedsConfirmation,
  bookingNotPending,
  invalidSlot,
  courtNotFound,
  bookingNotFound,
  unexpected,
}

/// The API rejected the request with a Problem Details `code`. The text the
/// player reads is chosen by the screen, never taken from the API (D10).
class BookingProblem implements Exception {
  const BookingProblem(this.code);

  final BookingProblemCode code;

  @override
  String toString() => 'BookingProblem($code)';
}

/// `OWN_OVERLAP_NEEDS_CONFIRMATION`: the player already holds [existingBooking]
/// at an overlapping time in this complex. Retrying with
/// `acknowledgeOwnOverlap` creates the booking.
class OwnOverlapProblem extends BookingProblem {
  const OwnOverlapProblem(this.existingBooking)
    : super(BookingProblemCode.ownOverlapNeedsConfirmation);

  final PlayerBooking existingBooking;
}

/// The API could not be reached or did not answer.
class BookingConnectionFailure implements Exception {
  const BookingConnectionFailure();

  @override
  String toString() => 'BookingConnectionFailure';
}
