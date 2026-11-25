import { CalendarRange, IsoDate, LocalTime } from './booking-calendar.types';

/**
 * Date and time helpers for the calendar. Dates are plain local dates of the complex, so they
 * are handled in UTC to avoid any time zone or daylight saving shift. Names are always Spanish,
 * regardless of the browser language, as the contract requires.
 */

export const DAYS_PER_WEEK = 7;

const MINUTES_PER_HOUR = 60;
const MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR;
const MONDAY_INDEX = 1;
const ISO_DATE_LENGTH = 10;

const WEEKDAY_NAMES = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
const WEEKDAY_SHORT_NAMES = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb'];
const MONTH_NAMES = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
];
const MONTH_SHORT_NAMES = [
  'ene',
  'feb',
  'mar',
  'abr',
  'may',
  'jun',
  'jul',
  'ago',
  'sep',
  'oct',
  'nov',
  'dic',
];

/** Weekday headers of week and month grids, which always start on Monday. */
export const WEEKDAY_HEADERS: readonly string[] = ['lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom'];

function parseIsoDate(date: IsoDate): Date {
  return new Date(`${date}T00:00:00Z`);
}

function toIsoDate(date: Date): IsoDate {
  return date.toISOString().slice(0, ISO_DATE_LENGTH);
}

function padTwoDigits(value: number): string {
  return String(value).padStart(2, '0');
}

export function addDays(date: IsoDate, amount: number): IsoDate {
  const result = parseIsoDate(date);
  result.setUTCDate(result.getUTCDate() + amount);
  return toIsoDate(result);
}

/** First day of the month that is `amount` months away from the month of `date`. */
export function firstDayOfMonthOffset(date: IsoDate, amount: number): IsoDate {
  const parsed = parseIsoDate(date);
  return toIsoDate(new Date(Date.UTC(parsed.getUTCFullYear(), parsed.getUTCMonth() + amount, 1)));
}

export function mondayOf(date: IsoDate): IsoDate {
  const weekday = parseIsoDate(date).getUTCDay();
  const daysSinceMonday = (weekday - MONDAY_INDEX + DAYS_PER_WEEK) % DAYS_PER_WEEK;
  return addDays(date, -daysSinceMonday);
}

export function monthOf(date: IsoDate): CalendarRange {
  const from = firstDayOfMonthOffset(date, 0);
  return { from, to: addDays(firstDayOfMonthOffset(date, 1), -1) };
}

export function datesBetween({ from, to }: CalendarRange): IsoDate[] {
  const dates: IsoDate[] = [];
  for (let date = from; date <= to; date = addDays(date, 1)) {
    dates.push(date);
  }
  return dates;
}

export function isWithin(date: IsoDate, { from, to }: CalendarRange): boolean {
  return date >= from && date <= to;
}

export function dayOfMonth(date: IsoDate): number {
  return parseIsoDate(date).getUTCDate();
}

export function toMinutes(time: LocalTime): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * MINUTES_PER_HOUR + minutes;
}

/** Adds minutes to a local time, wrapping past midnight (`22:30` + 90 → `00:00`). */
export function addMinutes(time: LocalTime, minutes: number): LocalTime {
  const minutesInDay = (toMinutes(time) + minutes) % MINUTES_PER_DAY;
  const hours = Math.floor(minutesInDay / MINUTES_PER_HOUR);
  return `${padTwoDigits(hours)}:${padTwoDigits(minutesInDay % MINUTES_PER_HOUR)}`;
}

/** `viernes 4 de diciembre de 2026` */
export function formatLongDate(date: IsoDate): string {
  const parsed = parseIsoDate(date);
  const weekday = WEEKDAY_NAMES[parsed.getUTCDay()];
  const month = MONTH_NAMES[parsed.getUTCMonth()];
  return `${weekday} ${parsed.getUTCDate()} de ${month} de ${parsed.getUTCFullYear()}`;
}

/** `30 nov – 6 dic 2026` */
export function formatWeekTitle({ from, to }: CalendarRange): string {
  return `${formatShortDate(from)} – ${formatShortDate(to)} ${parseIsoDate(to).getUTCFullYear()}`;
}

/** `diciembre 2026` */
export function formatMonthTitle(date: IsoDate): string {
  const parsed = parseIsoDate(date);
  return `${MONTH_NAMES[parsed.getUTCMonth()]} ${parsed.getUTCFullYear()}`;
}

/** `lun 30` */
export function formatColumnHeader(date: IsoDate): string {
  const parsed = parseIsoDate(date);
  return `${WEEKDAY_SHORT_NAMES[parsed.getUTCDay()]} ${parsed.getUTCDate()}`;
}

function formatShortDate(date: IsoDate): string {
  const parsed = parseIsoDate(date);
  return `${parsed.getUTCDate()} ${MONTH_SHORT_NAMES[parsed.getUTCMonth()]}`;
}
