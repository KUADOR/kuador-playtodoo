import {
  addDays,
  addMinutes,
  addMonths,
  dateParts,
  datesBetween,
  DAYS_PER_WEEK,
  endOfMonth,
  endOfWeek,
  startOfMonth,
  startOfWeek,
} from './calendarDates'
import {
  CALENDAR_TEXT,
  formatLongDate,
  formatMonthTitle,
  formatSlotAccessibleLabel,
  formatSlotLabel,
  formatSummary,
  formatWeekdayHeader,
  formatWeekTitle,
  WEEKDAY_HEADERS,
} from './calendarText'
import type {
  CalendarDayInput,
  CalendarMode,
  CalendarSlotInput,
  CalendarView,
  DateRange,
  IsoDate,
  LocalTime,
  SlotSelection,
  SlotStatus,
} from './calendarTypes'

// Presentation rules of calendar.md turned into plain data. The shapes match `expected` in the shared fixtures.

export interface SlotModel {
  date: IsoDate
  start: LocalTime
  end: LocalTime
  label: string
  status: SlotStatus
  accessibleLabel: string
  selectable: boolean
}

export interface DayModel {
  date: IsoDate
  isToday: boolean
  availableCount: number
  occupiedCount: number
  summary: string
  emptyMessage: string | null
  slots: SlotModel[]
}

export interface WeekColumnModel {
  date: IsoDate
  header: string
  isToday: boolean
  summary: string
}

export interface WeekModel {
  columns: WeekColumnModel[]
  /** Every start time of the week, sorted. */
  rows: LocalTime[]
  /** `cells[row][column]`, `null` when that day has no slot at that time. */
  cells: (SlotModel | null)[][]
}

export interface MonthDayModel {
  date: IsoDate
  dayOfMonth: number
  isToday: boolean
  availableCount: number
  occupiedCount: number
  summary: string
}

export interface MonthModel {
  weekdayHeaders: string[]
  /** Monday-to-Sunday weeks; days outside the month are `null`. */
  weeks: (MonthDayModel | null)[][]
}

export interface CalendarSourceInput {
  slotDurationMinutes: number
  days: CalendarDayInput[]
  mode: CalendarMode
  selected: SlotSelection | null
  today: IsoDate
}

/** The inputs indexed once per change, ready to build any view. */
export interface CalendarSource {
  slotDurationMinutes: number
  mode: CalendarMode
  today: IsoDate
  /** The selection that is shown: `null` when it points to an occupied or missing slot, or in readonly. */
  selection: SlotSelection | null
  slotsByDate: ReadonlyMap<IsoDate, CalendarSlotInput[]>
}

export function createCalendarSource(input: CalendarSourceInput): CalendarSource {
  const slotsByDate = new Map(
    input.days.map(({ date, slots }) => [date, [...slots].sort((a, b) => a.start.localeCompare(b.start))]),
  )
  return {
    slotDurationMinutes: input.slotDurationMinutes,
    mode: input.mode,
    today: input.today,
    selection: resolveSelection(input.selected, input.mode, slotsByDate),
    slotsByDate,
  }
}

function resolveSelection(
  selected: SlotSelection | null,
  mode: CalendarMode,
  slotsByDate: ReadonlyMap<IsoDate, CalendarSlotInput[]>,
): SlotSelection | null {
  if (!selected || mode === 'readonly') return null
  const slot = slotsByDate.get(selected.date)?.find(({ start }) => start === selected.start)
  return slot?.status === 'available' ? { date: selected.date, start: selected.start } : null
}

export function rangeFor(view: CalendarView, anchor: IsoDate): DateRange {
  switch (view) {
    case 'day':
      return { from: anchor, to: anchor }
    case 'week':
      return { from: startOfWeek(anchor), to: endOfWeek(anchor) }
    case 'month':
      return { from: startOfMonth(anchor), to: endOfMonth(anchor) }
  }
}

export function titleFor(view: CalendarView, anchor: IsoDate): string {
  switch (view) {
    case 'day':
      return formatLongDate(anchor)
    case 'week':
      return formatWeekTitle(startOfWeek(anchor), endOfWeek(anchor))
    case 'month':
      return formatMonthTitle(anchor)
  }
}

/** The anchor date of the previous (`-1`) or next (`1`) period. */
export function shiftAnchor(view: CalendarView, anchor: IsoDate, step: -1 | 1): IsoDate {
  switch (view) {
    case 'day':
      return addDays(anchor, step)
    case 'week':
      return addDays(anchor, step * DAYS_PER_WEEK)
    case 'month':
      return addMonths(anchor, step)
  }
}

function isSelected(selection: SlotSelection | null, date: IsoDate, start: LocalTime): boolean {
  return selection?.date === date && selection.start === start
}

function buildSlot(source: CalendarSource, date: IsoDate, slot: CalendarSlotInput): SlotModel {
  const end = addMinutes(slot.start, source.slotDurationMinutes)
  const status: SlotStatus = isSelected(source.selection, date, slot.start) ? 'selected' : slot.status
  return {
    date,
    start: slot.start,
    end,
    label: formatSlotLabel(slot.start, end),
    status,
    accessibleLabel: formatSlotAccessibleLabel(slot.start, end, status),
    selectable: source.mode === 'editable' && status !== 'occupied',
  }
}

function countSlots(source: CalendarSource, date: IsoDate) {
  const slots = source.slotsByDate.get(date) ?? []
  const occupiedCount = slots.filter(({ status }) => status === 'occupied').length
  const availableCount = slots.length - occupiedCount
  return { availableCount, occupiedCount, summary: formatSummary(availableCount) }
}

export function buildDay(source: CalendarSource, date: IsoDate): DayModel {
  const slots = (source.slotsByDate.get(date) ?? []).map((slot) => buildSlot(source, date, slot))
  return {
    date,
    isToday: date === source.today,
    ...countSlots(source, date),
    emptyMessage: slots.length === 0 ? CALENDAR_TEXT.emptyDay : null,
    slots,
  }
}

export function buildWeek(source: CalendarSource, anchor: IsoDate): WeekModel {
  const dates = datesBetween(startOfWeek(anchor), endOfWeek(anchor))
  const columns = dates.map((date) => ({
    date,
    header: formatWeekdayHeader(date),
    isToday: date === source.today,
    summary: countSlots(source, date).summary,
  }))
  const slotsByStart = dates.map(
    (date) => new Map((source.slotsByDate.get(date) ?? []).map((slot) => [slot.start, buildSlot(source, date, slot)])),
  )
  const rows = [...new Set(slotsByStart.flatMap((slots) => [...slots.keys()]))].sort()
  const cells = rows.map((start) => slotsByStart.map((slots) => slots.get(start) ?? null))
  return { columns, rows, cells }
}

export function buildMonth(source: CalendarSource, anchor: IsoDate): MonthModel {
  const first = startOfMonth(anchor)
  const last = endOfMonth(anchor)
  const gridDates = datesBetween(startOfWeek(first), endOfWeek(last))
  const days = gridDates.map((date) =>
    date < first || date > last
      ? null
      : { date, dayOfMonth: dateParts(date).dayOfMonth, isToday: date === source.today, ...countSlots(source, date) },
  )
  const weeks = Array.from({ length: days.length / DAYS_PER_WEEK }, (_, week) =>
    days.slice(week * DAYS_PER_WEEK, (week + 1) * DAYS_PER_WEEK),
  )
  return { weekdayHeaders: WEEKDAY_HEADERS, weeks }
}
