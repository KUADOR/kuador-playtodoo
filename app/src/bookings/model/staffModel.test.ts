import { describe, expect, it } from 'vitest'
import type { StaffBooking } from '../api/staffBookingsApi'
import { slotStartsOf } from './court'
import type { Court } from './court'
import { buildSchedule, dayQuery, entryStateOf } from './staffSchedule'
import { errorText, submitErrorTexts } from './staffText'
import { toInstant, toLocal } from './zonedTime'

const COURT: Court = { id: 'court-1', name: 'Cancha 1', slotDurationMinutes: 60, opensAt: '18:00', closesAt: '22:00' }
const UTC = 'UTC'
// Fixed offset of UTC-5 with no daylight saving: a stable zone for conversion tests.
const MINUS_FIVE = 'America/Bogota'
// Has daylight saving: 2026-03-08 jumps from 02:00 to 03:00.
const NEW_YORK = 'America/New_York'

function entry(overrides: Partial<StaffBooking>): StaffBooking {
  return {
    id: 'b-1',
    courtId: COURT.id,
    kind: 'RESERVATION',
    status: 'CONFIRMED',
    start: '2026-12-04T19:00:00Z',
    end: '2026-12-04T20:00:00Z',
    holderName: 'Alex',
    blockReason: null,
    ...overrides,
  }
}

describe('zonedTime', () => {
  it('converts the local time of the complex to a UTC instant and back', () => {
    const instant = toInstant({ date: '2026-12-04', time: '20:00' }, MINUS_FIVE)

    expect(instant).toBe('2026-12-05T01:00:00.000Z')
    expect(toLocal(instant, MINUS_FIVE)).toEqual({ date: '2026-12-04', time: '20:00' })
  })

  it('uses the offset in force on that date across daylight saving', () => {
    expect(toInstant({ date: '2026-01-15', time: '20:00' }, NEW_YORK)).toBe('2026-01-16T01:00:00.000Z')
    expect(toInstant({ date: '2026-07-15', time: '20:00' }, NEW_YORK)).toBe('2026-07-16T00:00:00.000Z')
  })
})

describe('slotStartsOf', () => {
  it('lists the slot starts that end by the closing time', () => {
    expect(slotStartsOf({ ...COURT, slotDurationMinutes: 90, opensAt: '18:00', closesAt: '22:00' })).toEqual(['18:00', '19:30'])
  })
})

describe('dayQuery', () => {
  it('covers the whole local day of the complex for one court', () => {
    expect(dayQuery(COURT, '2026-12-04', MINUS_FIVE)).toEqual({
      from: '2026-12-04T05:00:00.000Z',
      to: '2026-12-05T05:00:00.000Z',
      courtId: COURT.id,
    })
  })
})

describe('entryStateOf', () => {
  it.each([
    [entry({ status: 'PENDING', holderName: null }), 'pending'],
    [entry({ status: 'CONFIRMED' }), 'confirmed'],
    [entry({ kind: 'BLOCK', holderName: null, blockReason: 'Mantenimiento' }), 'block'],
  ] as const)('labels %#', (booking, state) => {
    expect(entryStateOf(booking)).toBe(state)
  })
})

describe('buildSchedule', () => {
  it('merges entries and the free slots that no entry overlaps, sorted by start', () => {
    const rows = buildSchedule(
      COURT,
      '2026-12-04',
      [entry({ id: 'confirmed', start: '2026-12-04T19:00:00Z', end: '2026-12-04T20:00:00Z' })],
      UTC,
    )

    expect(rows.map((row) => [row.type, row.start, row.end])).toEqual([
      ['free', '18:00', '19:00'],
      ['entry', '19:00', '20:00'],
      ['free', '20:00', '21:00'],
      ['free', '21:00', '22:00'],
    ])
  })

  it('hides every slot that an entry overlaps partially (half-open ranges)', () => {
    const rows = buildSchedule(COURT, '2026-12-04', [entry({ start: '2026-12-04T18:30:00Z', end: '2026-12-04T20:00:00Z' })], UTC)

    expect(rows.map((row) => `${row.type} ${row.start}`)).toEqual(['entry 18:30', 'free 20:00', 'free 21:00'])
  })

  it('ignores other courts and inactive entries', () => {
    const rows = buildSchedule(
      COURT,
      '2026-12-04',
      [entry({ courtId: 'court-2' }), entry({ id: 'cancelled', status: 'CANCELLED' }), entry({ id: 'expired', status: 'EXPIRED' })],
      UTC,
    )

    expect(rows.every((row) => row.type === 'free')).toBe(true)
  })

  it('shows an entry that began the day before from the start of the day', () => {
    const rows = buildSchedule(
      { ...COURT, opensAt: '00:00', closesAt: '02:00' },
      '2026-12-04',
      [entry({ start: '2026-12-03T23:00:00Z', end: '2026-12-04T01:00:00Z' })],
      UTC,
    )

    expect(rows.map((row) => `${row.type} ${row.start}–${row.end}`)).toEqual(['entry 00:00–01:00', 'free 01:00–02:00'])
  })
})

describe('submitErrorTexts', () => {
  it('puts slot conflicts on the hour field', () => {
    expect(submitErrorTexts('SLOT_UNAVAILABLE')).toEqual({ hour: 'Ese horario ya no está disponible. Elige otro.' })
  })

  it('puts any other failure over the form', () => {
    expect(submitErrorTexts('NETWORK_ERROR')).toEqual({ form: 'Revisa tu conexión e inténtalo de nuevo.' })
  })
})

describe('errorText for missing resources', () => {
  it.each(['BOOKING_NOT_FOUND', 'COURT_NOT_FOUND'] as const)('%s uses the generic not-found message', (code) => {
    expect(errorText(code)).toMatch(/^No encontramos lo que buscas\./)
    expect(errorText(code)).not.toMatch(/complejo/i)
  })
})
