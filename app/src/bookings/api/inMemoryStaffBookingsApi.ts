import { BookingApiError } from './staffBookingsApi'
import type {
  BlockSlotRequest,
  BookingKind,
  CreateStaffBookingRequest,
  Instant,
  StaffBooking,
  StaffBookingsApi,
} from './staffBookingsApi'

// Mock of the staff endpoints that follows the rules of contracts/bookings.openapi.yaml: half-open ranges,
// one active entry per court and range, blocks rejected with SLOT_HAS_BOOKING over a booking. It lets the
// screen run before the api exists. The real guarantee lives in the database (ADR-006).

const MS_PER_MINUTE = 60_000

export interface CourtDuration {
  id: string
  slotDurationMinutes: number
}

export interface InMemoryStaffBookingsApiOptions {
  courts: CourtDuration[]
  seed?: StaffBooking[]
  /** Simulated latency, so loading states can be seen in development. */
  latencyMs?: number
  newId?: () => string
}

const isActive = (booking: StaffBooking) => booking.status === 'PENDING' || booking.status === 'CONFIRMED'

const overlaps = (booking: StaffBooking, start: Instant, end: Instant) =>
  Date.parse(booking.start) < Date.parse(end) && Date.parse(start) < Date.parse(booking.end)

export function createInMemoryStaffBookingsApi({
  courts,
  seed = [],
  latencyMs = 0,
  newId = () => crypto.randomUUID(),
}: InMemoryStaffBookingsApiOptions): StaffBookingsApi {
  const bookings = new Map(seed.map((booking) => [booking.id, { ...booking }]))
  const durations = new Map(courts.map(({ id, slotDurationMinutes }) => [id, slotDurationMinutes]))

  const respond = <T>(produce: () => T): Promise<T> =>
    new Promise((resolve, reject) => {
      setTimeout(() => {
        try {
          resolve(structuredClone(produce()))
        } catch (error) {
          reject(error)
        }
      }, latencyMs)
    })

  function endOf(courtId: string, start: Instant): Instant {
    const duration = durations.get(courtId)
    if (duration === undefined) throw new BookingApiError('COURT_NOT_FOUND', `Court ${courtId} not found`)
    return new Date(Date.parse(start) + duration * MS_PER_MINUTE).toISOString()
  }

  function activeOverlapping(courtId: string, start: Instant, end: Instant): StaffBooking[] {
    return [...bookings.values()].filter(
      (booking) => booking.courtId === courtId && isActive(booking) && overlaps(booking, start, end),
    )
  }

  function find(bookingId: string): StaffBooking {
    const booking = bookings.get(bookingId)
    if (!booking) throw new BookingApiError('BOOKING_NOT_FOUND', `Booking ${bookingId} not found`)
    return booking
  }

  function insert(kind: BookingKind, courtId: string, start: Instant, end: Instant, details: Partial<StaffBooking>) {
    const booking: StaffBooking = {
      id: newId(),
      courtId,
      kind,
      status: 'CONFIRMED',
      start: new Date(start).toISOString(),
      end,
      holderName: null,
      blockReason: null,
      ...details,
    }
    bookings.set(booking.id, booking)
    return booking
  }

  return {
    listBookings: ({ from, to, courtId }) =>
      respond(() =>
        [...bookings.values()]
          .filter((booking) => isActive(booking) && overlaps(booking, from, to))
          .filter((booking) => !courtId || booking.courtId === courtId)
          .sort((a, b) => Date.parse(a.start) - Date.parse(b.start)),
      ),

    getBooking: (bookingId) => respond(() => find(bookingId)),

    createBooking: ({ courtId, start, holderName }: CreateStaffBookingRequest) =>
      respond(() => {
        const end = endOf(courtId, start)
        if (activeOverlapping(courtId, start, end).length > 0) {
          throw new BookingApiError('SLOT_UNAVAILABLE', 'Slot unavailable')
        }
        return insert('RESERVATION', courtId, start, end, { holderName })
      }),

    blockSlot: ({ courtId, start, reason }: BlockSlotRequest) =>
      respond(() => {
        const end = endOf(courtId, start)
        const conflicts = activeOverlapping(courtId, start, end)
        const booking = conflicts.find(({ kind }) => kind === 'RESERVATION')
        if (booking) throw new BookingApiError('SLOT_HAS_BOOKING', 'Slot has an active booking', booking.id)
        if (conflicts.length > 0) throw new BookingApiError('SLOT_UNAVAILABLE', 'Slot unavailable')
        return insert('BLOCK', courtId, start, end, { blockReason: reason || null })
      }),

    cancelBooking: (bookingId) =>
      respond(() => {
        const booking = find(bookingId)
        if (isActive(booking)) booking.status = 'CANCELLED'
        return booking
      }),
  }
}
