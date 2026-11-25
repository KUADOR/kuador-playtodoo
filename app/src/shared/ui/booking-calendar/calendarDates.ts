import type { IsoDate, LocalTime } from './calendarTypes'

// Pure calendar arithmetic on `YYYY-MM-DD` strings. Dates are handled in UTC only to avoid the
// device time zone; they always represent the local date of the complex. The clock is never read.

export const DAYS_PER_WEEK = 7
const MINUTES_PER_HOUR = 60
const MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

export interface DateParts {
  year: number
  /** 0 = January. */
  monthIndex: number
  dayOfMonth: number
  /** 0 = Monday … 6 = Sunday. */
  weekdayIndex: number
}

function toUtcDate(date: IsoDate): Date {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day))
}

function toIsoDate(date: Date): IsoDate {
  return date.toISOString().slice(0, 10)
}

export function dateParts(date: IsoDate): DateParts {
  const utc = toUtcDate(date)
  return {
    year: utc.getUTCFullYear(),
    monthIndex: utc.getUTCMonth(),
    dayOfMonth: utc.getUTCDate(),
    weekdayIndex: (utc.getUTCDay() + DAYS_PER_WEEK - 1) % DAYS_PER_WEEK,
  }
}

export function addDays(date: IsoDate, days: number): IsoDate {
  const utc = toUtcDate(date)
  utc.setUTCDate(utc.getUTCDate() + days)
  return toIsoDate(utc)
}

/** First day of the month `months` away from the month of `date`. */
export function addMonths(date: IsoDate, months: number): IsoDate {
  const { year, monthIndex } = dateParts(date)
  return toIsoDate(new Date(Date.UTC(year, monthIndex + months, 1)))
}

export function startOfWeek(date: IsoDate): IsoDate {
  return addDays(date, -dateParts(date).weekdayIndex)
}

export function endOfWeek(date: IsoDate): IsoDate {
  return addDays(startOfWeek(date), DAYS_PER_WEEK - 1)
}

export function startOfMonth(date: IsoDate): IsoDate {
  return addMonths(date, 0)
}

export function endOfMonth(date: IsoDate): IsoDate {
  return addDays(addMonths(date, 1), -1)
}

/** Every date from `from` to `to`, both included. */
export function datesBetween(from: IsoDate, to: IsoDate): IsoDate[] {
  const dates: IsoDate[] = []
  for (let date = from; date <= to; date = addDays(date, 1)) {
    dates.push(date)
  }
  return dates
}

/** `start` plus `minutes`, wrapping past midnight (`22:30` + 90 → `00:00`). */
export function addMinutes(start: LocalTime, minutes: number): LocalTime {
  const [hours, mins] = start.split(':').map(Number)
  const total = (hours * MINUTES_PER_HOUR + mins + minutes) % MINUTES_PER_DAY
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${pad(Math.floor(total / MINUTES_PER_HOUR))}:${pad(total % MINUTES_PER_HOUR)}`
}
