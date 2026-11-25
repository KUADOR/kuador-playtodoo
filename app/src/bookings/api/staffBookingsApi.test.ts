import { describe, expect, it, vi } from 'vitest'
import { createHttpStaffBookingsApi } from './httpStaffBookingsApi'
import { createInMemoryStaffBookingsApi } from './inMemoryStaffBookingsApi'
import { BookingApiError, bookingErrorCodeOf } from './staffBookingsApi'

const COURT_ID = 'court-1'
const SLOT = '2026-12-04T19:00:00Z'
const HALF_HOUR_LATER = '2026-12-04T19:30:00Z'

function mockApi() {
  let next = 0
  return createInMemoryStaffBookingsApi({
    courts: [{ id: COURT_ID, slotDurationMinutes: 60 }],
    newId: () => `id-${++next}`,
  })
}

async function codeOf(promise: Promise<unknown>) {
  try {
    await promise
  } catch (error) {
    return bookingErrorCodeOf(error)
  }
  throw new Error('Expected the call to fail')
}

describe('in-memory staff bookings api (contract mock)', () => {
  it('creates confirmed manual bookings with the end taken from the court duration', async () => {
    const api = mockApi()

    const booking = await api.createBooking({ courtId: COURT_ID, start: SLOT, holderName: 'Alex' })

    expect(booking).toMatchObject({ kind: 'RESERVATION', status: 'CONFIRMED', end: '2026-12-04T20:00:00.000Z', holderName: 'Alex' })
  })

  it('rejects a booking that overlaps an active one with SLOT_UNAVAILABLE', async () => {
    const api = mockApi()
    await api.createBooking({ courtId: COURT_ID, start: SLOT, holderName: 'Alex' })

    expect(await codeOf(api.createBooking({ courtId: COURT_ID, start: HALF_HOUR_LATER, holderName: 'Sam' }))).toBe('SLOT_UNAVAILABLE')
  })

  it('accepts adjacent ranges', async () => {
    const api = mockApi()
    await api.createBooking({ courtId: COURT_ID, start: SLOT, holderName: 'Alex' })

    await expect(api.createBooking({ courtId: COURT_ID, start: '2026-12-04T20:00:00Z', holderName: 'Sam' })).resolves.toBeTruthy()
  })

  it('rejects a block over a booking with SLOT_HAS_BOOKING and leaves the booking untouched', async () => {
    const api = mockApi()
    const booking = await api.createBooking({ courtId: COURT_ID, start: SLOT, holderName: 'Alex' })

    const failure = await api.blockSlot({ courtId: COURT_ID, start: SLOT }).catch((error: BookingApiError) => error)

    expect(failure).toMatchObject({ code: 'SLOT_HAS_BOOKING', conflictingBookingId: booking.id })
    expect(await api.getBooking(booking.id)).toMatchObject({ status: 'CONFIRMED' })
  })

  it('blocks the slot once the booking is cancelled', async () => {
    const api = mockApi()
    const booking = await api.createBooking({ courtId: COURT_ID, start: SLOT, holderName: 'Alex' })

    await api.cancelBooking(booking.id)
    const block = await api.blockSlot({ courtId: COURT_ID, start: SLOT, reason: 'Mantenimiento' })

    expect(block).toMatchObject({ kind: 'BLOCK', status: 'CONFIRMED', blockReason: 'Mantenimiento' })
    expect(await api.listBookings({ from: SLOT, to: '2026-12-04T20:00:00Z' })).toEqual([block])
  })

  it('rejects a block over another block with SLOT_UNAVAILABLE', async () => {
    const api = mockApi()
    await api.blockSlot({ courtId: COURT_ID, start: SLOT })

    expect(await codeOf(api.blockSlot({ courtId: COURT_ID, start: SLOT }))).toBe('SLOT_UNAVAILABLE')
  })

  it('answers COURT_NOT_FOUND and BOOKING_NOT_FOUND', async () => {
    const api = mockApi()

    expect(await codeOf(api.createBooking({ courtId: 'missing', start: SLOT, holderName: 'Alex' }))).toBe('COURT_NOT_FOUND')
    expect(await codeOf(api.getBooking('missing'))).toBe('BOOKING_NOT_FOUND')
  })
})

describe('http staff bookings api', () => {
  const jsonResponse = (status: number, body: unknown) =>
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

  it('lists the bookings of a range and court', async () => {
    const fetchFn = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(200, []))
    const api = createHttpStaffBookingsApi('/api/v1', fetchFn)

    await api.listBookings({ from: '2026-12-04T05:00:00.000Z', to: '2026-12-05T05:00:00.000Z', courtId: COURT_ID })

    expect(fetchFn).toHaveBeenCalledWith(
      '/api/v1/staff/bookings?from=2026-12-04T05%3A00%3A00.000Z&to=2026-12-05T05%3A00%3A00.000Z&courtId=court-1',
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('posts a block and reads SLOT_HAS_BOOKING with the conflicting booking', async () => {
    const fetchFn = vi
      .fn<typeof fetch>()
      .mockResolvedValue(jsonResponse(409, { code: 'SLOT_HAS_BOOKING', title: 'Slot has an active booking', conflictingBookingId: 'b-9' }))
    const api = createHttpStaffBookingsApi('/api/v1', fetchFn)

    const failure = await api.blockSlot({ courtId: COURT_ID, start: SLOT }).catch((error: BookingApiError) => error)

    expect(fetchFn).toHaveBeenCalledWith('/api/v1/staff/blocks', expect.objectContaining({ method: 'POST', body: JSON.stringify({ courtId: COURT_ID, start: SLOT }) }))
    expect(failure).toBeInstanceOf(BookingApiError)
    expect(failure).toMatchObject({ code: 'SLOT_HAS_BOOKING', conflictingBookingId: 'b-9' })
  })

  it('reports a failed connection as NETWORK_ERROR', async () => {
    const api = createHttpStaffBookingsApi('/api/v1', vi.fn<typeof fetch>().mockRejectedValue(new TypeError('Failed to fetch')))

    expect(await codeOf(api.getBooking('b-1'))).toBe('NETWORK_ERROR')
  })

  it('reports an error without Problem Details as UNEXPECTED_RESPONSE', async () => {
    const api = createHttpStaffBookingsApi('/api/v1', vi.fn<typeof fetch>().mockResolvedValue(new Response('Bad gateway', { status: 502 })))

    expect(await codeOf(api.cancelBooking('b-1'))).toBe('UNEXPECTED_RESPONSE')
  })
})

describe('bookingErrorCodeOf', () => {
  it('rethrows errors that do not come from the api', () => {
    const bug = new TypeError('bug')

    expect(() => bookingErrorCodeOf(bug)).toThrow(bug)
  })
})
