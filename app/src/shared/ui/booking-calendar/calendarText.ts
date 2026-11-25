import { dateParts } from './calendarDates'
import type { CalendarView, IsoDate, LocalTime, SlotStatus } from './calendarTypes'

// Every text the calendar shows, in Spanish regardless of the device language (calendar.md · Títulos).

const WEEKDAYS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo']
const MONTHS = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
]
const MONTHS_SHORT = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

export const WEEKDAY_HEADERS = ['lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom']

export const CALENDAR_TEXT = {
  calendar: 'Calendario de reservas',
  noSlots: 'Sin horarios',
  emptyDay: 'Este día no tiene horarios. Prueba con otro día.',
  loadError: 'No pudimos cargar los horarios.',
  retry: 'Reintentar',
  loading: 'Cargando horarios…',
  readonly: 'Solo lectura',
  today: 'Hoy',
  time: 'Hora',
  viewSwitcher: 'Vista del calendario',
} as const

export const SLOT_STATUS_TEXT: Record<SlotStatus, string> = {
  available: 'Libre',
  occupied: 'Ocupado',
  selected: 'Seleccionado',
}

export const VIEW_NAMES: Record<CalendarView, string> = { day: 'Día', week: 'Semana', month: 'Mes' }

export const PERIOD_NAVIGATION: Record<CalendarView, { previous: string; next: string }> = {
  day: { previous: 'Día anterior', next: 'Día siguiente' },
  week: { previous: 'Semana anterior', next: 'Semana siguiente' },
  month: { previous: 'Mes anterior', next: 'Mes siguiente' },
}

/** `N libres`, `1 libre` or `Sin horarios`. */
export function formatSummary(freeCount: number): string {
  if (freeCount === 0) return CALENDAR_TEXT.noSlots
  return freeCount === 1 ? '1 libre' : `${freeCount} libres`
}

/** `18:00–19:00` with an en dash (U+2013). */
export function formatSlotLabel(start: LocalTime, end: LocalTime): string {
  return `${start}–${end}`
}

/** `18:00 a 19:00, Libre`. */
export function formatSlotAccessibleLabel(start: LocalTime, end: LocalTime, status: SlotStatus): string {
  return `${start} a ${end}, ${SLOT_STATUS_TEXT[status]}`
}

/** `viernes 4 de diciembre de 2026`. */
export function formatLongDate(date: IsoDate): string {
  const { year, monthIndex, dayOfMonth, weekdayIndex } = dateParts(date)
  return `${WEEKDAYS[weekdayIndex]} ${dayOfMonth} de ${MONTHS[monthIndex]} de ${year}`
}

/** `lun 30`. */
export function formatWeekdayHeader(date: IsoDate): string {
  const { dayOfMonth, weekdayIndex } = dateParts(date)
  return `${WEEKDAY_HEADERS[weekdayIndex]} ${dayOfMonth}`
}

/** `30 nov – 6 dic 2026`; the first month or year is repeated only when it differs. */
export function formatWeekTitle(from: IsoDate, to: IsoDate): string {
  const start = dateParts(from)
  const end = dateParts(to)
  const endText = `${end.dayOfMonth} ${MONTHS_SHORT[end.monthIndex]} ${end.year}`
  if (start.year !== end.year) {
    return `${start.dayOfMonth} ${MONTHS_SHORT[start.monthIndex]} ${start.year} – ${endText}`
  }
  if (start.monthIndex !== end.monthIndex) {
    return `${start.dayOfMonth} ${MONTHS_SHORT[start.monthIndex]} – ${endText}`
  }
  return `${start.dayOfMonth} – ${endText}`
}

/** `diciembre 2026`. */
export function formatMonthTitle(date: IsoDate): string {
  const { year, monthIndex } = dateParts(date)
  return `${MONTHS[monthIndex]} ${year}`
}
