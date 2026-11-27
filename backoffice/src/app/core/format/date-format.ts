/**
 * Date and time as «Pantallas» shows them: `dd/mm/aaaa hh:mm`, in the browser time zone.
 * The display time zone per country is [POR DEFINIR: launch country] (contract «Dates»).
 */
const DATE_TIME_FORMAT = new Intl.DateTimeFormat('es', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

const MILLISECONDS_PER_DAY = 86_400_000;
const CALENDAR_DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;

function partsOf(isoInstant: string): Record<string, string> {
  return Object.fromEntries(
    DATE_TIME_FORMAT.formatToParts(new Date(isoInstant)).map((part) => [part.type, part.value]),
  );
}

export function formatDateTime(isoInstant: string): string {
  const parts = partsOf(isoInstant);
  return `${parts['day']}/${parts['month']}/${parts['year']} ${parts['hour']}:${parts['minute']}`;
}

/** Day of an instant, `dd/mm/aaaa`, in the browser time zone. */
export function formatDate(isoInstant: string): string {
  const parts = partsOf(isoInstant);
  return `${parts['day']}/${parts['month']}/${parts['year']}`;
}

/** A date without time of the contract (`2026-12-01`) as `01/12/2026`, with no time zone shift. */
export function formatCalendarDate(calendarDate: string): string {
  const match = CALENDAR_DATE_PATTERN.exec(calendarDate);
  return match ? `${match[3]}/${match[2]}/${match[1]}` : calendarDate;
}

/** Today in the browser time zone as a contract date (`yyyy-mm-dd`). */
export function todayAsCalendarDate(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

/** Whole days left until `isoInstant`, rounded up (0 once it passed). */
export function daysUntil(isoInstant: string, now: Date = new Date()): number {
  const remaining = new Date(isoInstant).getTime() - now.getTime();
  return Math.max(0, Math.ceil(remaining / MILLISECONDS_PER_DAY));
}

/** `isoInstant` plus `days` whole days. */
export function addDays(isoInstant: string, days: number): Date {
  return new Date(new Date(isoInstant).getTime() + days * MILLISECONDS_PER_DAY);
}
