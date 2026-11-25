import type { IsoDate, LocalTime } from '../../shared/ui'
import type { Instant } from '../api/staffBookingsApi'

// Conversion between the local time of the complex and the UTC instants of the api. The time zone of
// the complex is [POR DEFINIR] (PRODUCT.md); it is always passed in, never read from the device here.

export interface LocalDateTime {
  date: IsoDate
  time: LocalTime
}

const formatters = new Map<string, Intl.DateTimeFormat>()

function formatterFor(timeZone: string): Intl.DateTimeFormat {
  let formatter = formatters.get(timeZone)
  if (!formatter) {
    formatter = new Intl.DateTimeFormat('en-US', {
      timeZone,
      hourCycle: 'h23',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
    formatters.set(timeZone, formatter)
  }
  return formatter
}

function zonedParts(epochMs: number, timeZone: string): Record<string, string> {
  const parts = formatterFor(timeZone).formatToParts(new Date(epochMs))
  return Object.fromEntries(parts.map(({ type, value }) => [type, value]))
}

/** Milliseconds the zone is ahead of UTC at that instant. */
function offsetAt(epochMs: number, timeZone: string): number {
  const parts = zonedParts(epochMs, timeZone)
  const asUtc = Date.UTC(+parts.year, +parts.month - 1, +parts.day, +parts.hour, +parts.minute, +parts.second)
  return asUtc - Math.floor(epochMs / 1000) * 1000
}

/** The UTC instant of a local date and time of the complex. */
export function toInstant({ date, time }: LocalDateTime, timeZone: string): Instant {
  const [year, month, day] = date.split('-').map(Number)
  const [hours, minutes] = time.split(':').map(Number)
  const wallClockAsUtc = Date.UTC(year, month - 1, day, hours, minutes)
  const firstGuess = wallClockAsUtc - offsetAt(wallClockAsUtc, timeZone)
  // A second pass corrects the guess when a daylight-saving change falls between both instants.
  const epochMs = wallClockAsUtc - offsetAt(firstGuess, timeZone)
  return new Date(epochMs).toISOString()
}

/** The local date and time of the complex at an instant. */
export function toLocal(instant: Instant, timeZone: string): LocalDateTime {
  const parts = zonedParts(Date.parse(instant), timeZone)
  return { date: `${parts.year}-${parts.month}-${parts.day}`, time: `${parts.hour}:${parts.minute}` }
}
