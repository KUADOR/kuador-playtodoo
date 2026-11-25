// Port of the staff endpoints of contracts/bookings.openapi.yaml (tag «Staff bookings»).
// Screens depend on this interface; the HTTP adapter and the in-memory mock implement it.

/** ISO-8601 instant. Responses are always in UTC (`Z`). */
export type Instant = string

export type BookingKind = 'RESERVATION' | 'BLOCK'
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED'

export interface StaffBooking {
  id: string
  courtId: string
  kind: BookingKind
  status: BookingStatus
  start: Instant
  end: Instant
  /** Null for blocks and for pending bookings (the hold does not expose personal data). */
  holderName: string | null
  blockReason: string | null
}

export interface StaffBookingsQuery {
  from: Instant
  to: Instant
  courtId?: string
}

export interface CreateStaffBookingRequest {
  courtId: string
  start: Instant
  holderName: string
  holderPhone?: string
}

export interface BlockSlotRequest {
  courtId: string
  start: Instant
  reason?: string
}

/** `code` values of the contract's Problem Details. */
export type ProblemCode =
  | 'SLOT_UNAVAILABLE'
  | 'BOOKING_EXPIRED'
  | 'OWN_OVERLAP_NEEDS_CONFIRMATION'
  | 'SLOT_HAS_BOOKING'
  | 'BOOKING_NOT_PENDING'
  | 'INVALID_SLOT'
  | 'COURT_NOT_FOUND'
  | 'BOOKING_NOT_FOUND'
  | 'INVALID_REQUEST'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'

/** Failures that do not come with a Problem Details body. */
export type TransportErrorCode = 'NETWORK_ERROR' | 'UNEXPECTED_RESPONSE'

export type BookingErrorCode = ProblemCode | TransportErrorCode

/** Error of any staff bookings call. Screens map `code` to the text the user reads. */
export class BookingApiError extends Error {
  readonly code: BookingErrorCode
  /** Only with `SLOT_HAS_BOOKING`: the booking that must be cancelled before blocking. */
  readonly conflictingBookingId: string | null

  constructor(code: BookingErrorCode, message: string, conflictingBookingId: string | null = null) {
    super(message)
    this.name = 'BookingApiError'
    this.code = code
    this.conflictingBookingId = conflictingBookingId
  }
}

export interface StaffBookingsApi {
  listBookings(query: StaffBookingsQuery): Promise<StaffBooking[]>
  getBooking(bookingId: string): Promise<StaffBooking>
  createBooking(request: CreateStaffBookingRequest): Promise<StaffBooking>
  blockSlot(request: BlockSlotRequest): Promise<StaffBooking>
  /** Cancels a booking or removes a block. Idempotent. */
  cancelBooking(bookingId: string): Promise<StaffBooking>
}

/** The code of an api failure. Anything else is a programming error and is rethrown, never hidden. */
export function bookingErrorCodeOf(error: unknown): BookingErrorCode {
  if (error instanceof BookingApiError) return error.code
  throw error
}
