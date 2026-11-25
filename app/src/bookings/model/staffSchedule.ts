import { addDays } from '../../shared/ui'
import type { IsoDate, LocalTime } from '../../shared/ui'
import type { StaffBooking, StaffBookingsQuery } from '../api/staffBookingsApi'
import { fromMinutes, slotStartsOf, toMinutes } from './court'
import type { Court } from './court'
import { toInstant, toLocal } from './zonedTime'

// The day of one court as the staff sees it: active entries and the free slots left between them.

const START_OF_DAY: LocalTime = '00:00'

export type EntryState = 'free' | 'pending' | 'confirmed' | 'block'

export interface FreeRow {
  type: 'free'
  start: LocalTime
  end: LocalTime
}

export interface EntryRow {
  type: 'entry'
  start: LocalTime
  end: LocalTime
  booking: StaffBooking
}

export type ScheduleRow = FreeRow | EntryRow

export function entryStateOf(booking: StaffBooking): Exclude<EntryState, 'free'> {
  if (booking.kind === 'BLOCK') return 'block'
  return booking.status === 'PENDING' ? 'pending' : 'confirmed'
}

/** The api query for the whole local day of the complex. */
export function dayQuery(court: Court, date: IsoDate, timeZone: string): StaffBookingsQuery {
  return {
    from: toInstant({ date, time: START_OF_DAY }, timeZone),
    to: toInstant({ date: addDays(date, 1), time: START_OF_DAY }, timeZone),
    courtId: court.id,
  }
}

/** Rows sorted by start: every active entry of the court, plus each slot that no entry overlaps. */
export function buildSchedule(court: Court, date: IsoDate, bookings: StaffBooking[], timeZone: string): ScheduleRow[] {
  const entries: EntryRow[] = bookings
    .filter(({ courtId, status }) => courtId === court.id && (status === 'PENDING' || status === 'CONFIRMED'))
    .map((booking): EntryRow => {
      const start = toLocal(booking.start, timeZone)
      // An entry that began the day before is shown from the start of this day.
      return {
        type: 'entry',
        start: start.date < date ? START_OF_DAY : start.time,
        end: toLocal(booking.end, timeZone).time,
        booking,
      }
    })

  const free: FreeRow[] = slotStartsOf(court)
    .map((start) => ({ type: 'free' as const, start, end: fromMinutes(toMinutes(start) + court.slotDurationMinutes) }))
    .filter((slot) => !entries.some((entry) => rangesOverlap(slot, entry)))

  return [...entries, ...free].sort((a, b) => a.start.localeCompare(b.start))
}

function rangesOverlap(slot: FreeRow, entry: EntryRow): boolean {
  // An entry that ends at local midnight reads «00:00»: it lasts until the end of the day.
  const entryEnd = entry.end <= entry.start ? Number.POSITIVE_INFINITY : toMinutes(entry.end)
  return toMinutes(slot.start) < entryEnd && toMinutes(entry.start) < toMinutes(slot.end)
}
