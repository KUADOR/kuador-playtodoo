import {
  CalendarDay,
  CalendarRange,
  CalendarSlot,
  CalendarView,
  IsoDate,
  CalendarMode,
  LocalTime,
  SlotSelection,
  SlotStatus,
} from './booking-calendar.types';
import {
  addDays,
  addMinutes,
  datesBetween,
  dayOfMonth,
  DAYS_PER_WEEK,
  firstDayOfMonthOffset,
  formatColumnHeader,
  formatLongDate,
  formatMonthTitle,
  formatWeekTitle,
  isWithin,
  mondayOf,
  monthOf,
  toMinutes,
  WEEKDAY_HEADERS,
} from './calendar-dates';

/**
 * Presentation model of the BookingCalendar contract. In `readonly` mode every slot shows the
 * status it receives; in `editable` mode the free slot that matches `selected` shows as selected.
 */

/** Status a slot is shown with: the received one, or `selected` for the chosen free slot. */
export type ShownSlotStatus = SlotStatus | 'selected';

const STATUS_TEXT: Record<ShownSlotStatus, string> = {
  available: 'Libre',
  occupied: 'Ocupado',
  selected: 'Seleccionado',
};
const NO_SLOTS_SUMMARY = 'Sin horarios';
const EMPTY_DAY_MESSAGE = 'Este día no tiene horarios. Prueba con otro día.';
const TODAY_TEXT = 'Hoy';

/** What the screen gives the calendar for the visible period. */
export interface CalendarData {
  readonly days: readonly CalendarDay[];
  readonly slotDurationMinutes: number;
  readonly today: IsoDate;
  /** `readonly` when absent. */
  readonly mode?: CalendarMode;
  /** Ignored in `readonly` mode and when it points to an occupied or missing slot. */
  readonly selected?: SlotSelection | null;
}

export interface SlotView {
  readonly start: LocalTime;
  readonly end: LocalTime;
  readonly label: string;
  readonly status: ShownSlotStatus;
  readonly accessibleLabel: string;
  /** Only in `editable` mode and when it is not occupied. */
  readonly selectable: boolean;
}

interface DaySummary {
  readonly date: IsoDate;
  readonly isToday: boolean;
  readonly availableCount: number;
  readonly occupiedCount: number;
  readonly summary: string;
  /** Long date, summary and «Hoy», for the buttons that open the day. */
  readonly accessibleLabel: string;
}

export interface DayView extends DaySummary {
  readonly emptyMessage: string | null;
  readonly slots: readonly SlotView[];
}

export interface WeekColumn {
  readonly date: IsoDate;
  readonly header: string;
  readonly isToday: boolean;
  readonly summary: string;
  readonly accessibleLabel: string;
}

export interface WeekView {
  readonly columns: readonly WeekColumn[];
  readonly rows: readonly LocalTime[];
  /** One row per start time, one cell per weekday; `null` when that day has no such slot. */
  readonly cells: readonly (readonly (SlotView | null)[])[];
}

export interface MonthDay extends DaySummary {
  readonly dayOfMonth: number;
}

export interface MonthView {
  readonly weekdayHeaders: readonly string[];
  /** Monday-to-Sunday weeks; days outside the month are `null`. */
  readonly weeks: readonly (readonly (MonthDay | null)[])[];
}

export function visibleRange(view: CalendarView, anchor: IsoDate): CalendarRange {
  if (view === 'day') {
    return { from: anchor, to: anchor };
  }
  if (view === 'week') {
    const from = mondayOf(anchor);
    return { from, to: addDays(from, DAYS_PER_WEEK - 1) };
  }
  return monthOf(anchor);
}

/** Anchor date of the previous (`-1`) or next (`1`) period of the view. */
export function shiftAnchor(view: CalendarView, anchor: IsoDate, direction: -1 | 1): IsoDate {
  if (view === 'day') {
    return addDays(anchor, direction);
  }
  if (view === 'week') {
    return addDays(anchor, direction * DAYS_PER_WEEK);
  }
  return firstDayOfMonthOffset(anchor, direction);
}

export function periodTitle(view: CalendarView, range: CalendarRange): string {
  if (view === 'day') {
    return formatLongDate(range.from);
  }
  return view === 'week' ? formatWeekTitle(range) : formatMonthTitle(range.from);
}

export function buildDayView(date: IsoDate, data: CalendarData): DayView {
  const editable = data.mode === 'editable';
  const selectedStart = editable && data.selected?.date === date ? data.selected.start : null;
  const slots = [...slotsOf(date, data.days)]
    .sort((a, b) => toMinutes(a.start) - toMinutes(b.start))
    .map((slot) => describeSlot(slot, data.slotDurationMinutes, editable, selectedStart));
  const availableCount = slots.filter((slot) => slot.status !== 'occupied').length;
  const isToday = date === data.today;
  const summary = availabilitySummary(availableCount);
  return {
    date,
    isToday,
    availableCount,
    occupiedCount: slots.length - availableCount,
    summary,
    accessibleLabel: dayAccessibleLabel(date, summary, isToday),
    emptyMessage: slots.length === 0 ? EMPTY_DAY_MESSAGE : null,
    slots,
  };
}

export function buildWeekView(range: CalendarRange, data: CalendarData): WeekView {
  const days = datesBetween(range).map((date) => buildDayView(date, data));
  const rows = [...new Set(days.flatMap((day) => day.slots.map((slot) => slot.start)))].sort(
    (a, b) => toMinutes(a) - toMinutes(b),
  );
  return {
    columns: days.map((day) => ({
      date: day.date,
      header: formatColumnHeader(day.date),
      isToday: day.isToday,
      summary: day.summary,
      accessibleLabel: day.accessibleLabel,
    })),
    rows,
    cells: rows.map((start) =>
      days.map((day) => day.slots.find((slot) => slot.start === start) ?? null),
    ),
  };
}

export function buildMonthView(range: CalendarRange, data: CalendarData): MonthView {
  const gridRange = {
    from: mondayOf(range.from),
    to: addDays(mondayOf(range.to), DAYS_PER_WEEK - 1),
  };
  const cells = datesBetween(gridRange).map((date) =>
    isWithin(date, range) ? toMonthDay(buildDayView(date, data)) : null,
  );
  const weeks: (MonthDay | null)[][] = [];
  for (let index = 0; index < cells.length; index += DAYS_PER_WEEK) {
    weeks.push(cells.slice(index, index + DAYS_PER_WEEK));
  }
  return { weekdayHeaders: WEEKDAY_HEADERS, weeks };
}

function slotsOf(date: IsoDate, days: readonly CalendarDay[]): readonly CalendarSlot[] {
  return days.find((day) => day.date === date)?.slots ?? [];
}

function describeSlot(
  slot: CalendarSlot,
  slotDurationMinutes: number,
  editable: boolean,
  selectedStart: LocalTime | null,
): SlotView {
  const end = addMinutes(slot.start, slotDurationMinutes);
  const free = slot.status === 'available';
  const status: ShownSlotStatus = free && slot.start === selectedStart ? 'selected' : slot.status;
  return {
    start: slot.start,
    end,
    label: `${slot.start}–${end}`,
    status,
    accessibleLabel: `${slot.start} a ${end}, ${STATUS_TEXT[status]}`,
    selectable: editable && free,
  };
}

/** The selection that the calendar actually shows: `selected` if it is a free slot of `days`. */
export function effectiveSelection(data: CalendarData): SlotSelection | null {
  const selected = data.selected ?? null;
  if (data.mode !== 'editable' || selected === null) {
    return null;
  }
  const slot = slotsOf(selected.date, data.days).find((item) => item.start === selected.start);
  return slot?.status === 'available' ? selected : null;
}

function availabilitySummary(availableCount: number): string {
  if (availableCount === 0) {
    return NO_SLOTS_SUMMARY;
  }
  return availableCount === 1 ? '1 libre' : `${availableCount} libres`;
}

function dayAccessibleLabel(date: IsoDate, summary: string, isToday: boolean): string {
  const label = `${formatLongDate(date)}, ${summary}`;
  return isToday ? `${label}, ${TODAY_TEXT}` : label;
}

function toMonthDay({
  date,
  isToday,
  availableCount,
  occupiedCount,
  summary,
  accessibleLabel,
}: DayView): MonthDay {
  return {
    date,
    dayOfMonth: dayOfMonth(date),
    isToday,
    availableCount,
    occupiedCount,
    summary,
    accessibleLabel,
  };
}
