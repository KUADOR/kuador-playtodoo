import {
  CalendarDay,
  CalendarRange,
} from '../../shared/ui/booking-calendar/booking-calendar.types';
import { addDays } from '../../shared/ui/booking-calendar/calendar-dates';
import { StatusTagLook } from '../../shared/ui/status-tag/status-tag';
import { InstantRange, StaffBooking } from './support-bookings-api';
import { Court } from './support-courts';

/**
 * Presentation model of the day P9 shows. Instants come in UTC and are shown in the browser time
 * zone, like every date of the backoffice ([POR DEFINIR: zona horaria del complejo]).
 */

const MINUTES_PER_HOUR = 60;
const MILLISECONDS_PER_MINUTE = 60_000;
const SHORT_ID_LENGTH = 8;

const KIND_LABELS: Readonly<Record<StaffBooking['kind'], string>> = {
  RESERVATION: 'Reserva',
  BLOCK: 'Bloqueo',
};

const PENDING_LOOK: StatusTagLook = { label: 'Pendiente', tone: 'warning', icon: 'clock' };
const CONFIRMED_LOOK: StatusTagLook = { label: 'Confirmada', tone: 'success', icon: 'check' };
export const INACTIVE_LOOK: StatusTagLook = { label: 'Inactiva', tone: 'neutral', icon: 'pause' };

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

/** `yyyy-mm-dd` and `HH:mm` of an instant, in the browser time zone. */
function localDate(instant: Date): string {
  return `${instant.getFullYear()}-${pad(instant.getMonth() + 1)}-${pad(instant.getDate())}`;
}

function localTime(instant: Date): string {
  return `${pad(instant.getHours())}:${pad(instant.getMinutes())}`;
}

/** The instant (ISO-8601, UTC) of `time` on `date`, both local. */
export function localInstant(date: string, time: string): string {
  return new Date(`${date}T${time}:00`).toISOString();
}

const START_OF_DAY = '00:00';

/** Whole local days, from the start of `from` to the end of `to`, as the api range. */
export function periodRange({ from, to }: CalendarRange): InstantRange {
  return { from: localInstant(from, START_OF_DAY), to: localInstant(addDays(to, 1), START_OF_DAY) };
}

/** The whole local day `[00:00, next day 00:00)` as the api range. */
export function dayRange(date: string): InstantRange {
  return periodRange({ from: date, to: date });
}

function toMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * MINUTES_PER_HOUR + minutes;
}

function fromMinutes(total: number): string {
  return `${pad(Math.floor(total / MINUTES_PER_HOUR))}:${pad(total % MINUTES_PER_HOUR)}`;
}

/** Name of a court; one the courts source does not know is named by the start of its id. */
export function courtName(courtId: string, courts: readonly Court[]): string {
  return (
    courts.find((court) => court.id === courtId)?.name ??
    `Cancha ${courtId.slice(0, SHORT_ID_LENGTH)}`
  );
}

/** Active entries occupy their slot; cancelled and expired ones are not shown. */
export function isActiveEntry(entry: StaffBooking): boolean {
  return entry.status === 'PENDING' || entry.status === 'CONFIRMED';
}

/** One row of the day list of P9. */
export interface EntryRow {
  readonly entry: StaffBooking;
  /** `18:00–19:00`. */
  readonly time: string;
  readonly startTime: string;
  readonly date: string;
  readonly court: string;
  readonly kind: string;
  /** Null for blocks, which have no booking status. */
  readonly statusLook: StatusTagLook | null;
  /** Holder of a confirmed booking or reason of a block; null when there is none to show. */
  readonly detail: string | null;
}

export function entryRows(entries: readonly StaffBooking[], courts: readonly Court[]): EntryRow[] {
  return entries
    .filter(isActiveEntry)
    .sort((a, b) => a.start.localeCompare(b.start))
    .map((entry) => {
      const start = new Date(entry.start);
      const isBlock = entry.kind === 'BLOCK';
      return {
        entry,
        time: `${localTime(start)}–${localTime(new Date(entry.end))}`,
        startTime: localTime(start),
        date: localDate(start),
        court: courtName(entry.courtId, courts),
        kind: KIND_LABELS[entry.kind],
        statusLook: isBlock ? null : entry.status === 'PENDING' ? PENDING_LOOK : CONFIRMED_LOOK,
        detail: isBlock ? entry.blockReason : entry.holderName,
      };
    });
}

/** Every valid slot start of `court` in one day: from `opensAt`, one per slot, ending by `closesAt`. */
function slotStartsOf(court: Court): string[] {
  const starts: string[] = [];
  const closing = toMinutes(court.closesAt);
  for (
    let start = toMinutes(court.opensAt);
    start + court.slotDurationMinutes <= closing;
    start += court.slotDurationMinutes
  ) {
    starts.push(fromMinutes(start));
  }
  return starts;
}

/**
 * The slots of `court` on `date` for the shared calendar: occupied when an active entry of that
 * court overlaps them. Slots that already started are not offered (the api rejects them with
 * `INVALID_SLOT`).
 */
export function courtDay(
  court: Court,
  date: string,
  entries: readonly StaffBooking[],
  now: Date = new Date(),
): CalendarDay {
  const occupied = entries.filter((entry) => entry.courtId === court.id && isActiveEntry(entry));
  const slots = slotStartsOf(court)
    .map((start) => {
      const from = new Date(localInstant(date, start)).getTime();
      const to = from + court.slotDurationMinutes * MILLISECONDS_PER_MINUTE;
      return { start, from, to };
    })
    .filter((slot) => slot.from > now.getTime())
    .map(({ start, from, to }) => ({
      start,
      status: occupied.some(
        (entry) => new Date(entry.start).getTime() < to && from < new Date(entry.end).getTime(),
      )
        ? ('occupied' as const)
        : ('available' as const),
    }));
  return { date, slots };
}
