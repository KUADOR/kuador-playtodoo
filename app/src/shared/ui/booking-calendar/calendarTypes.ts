/** Inputs and outputs of the BookingCalendar contract (design-system/docs/components/calendar.md). */

/** Local date of the complex, `YYYY-MM-DD`, without time zone conversion. */
export type IsoDate = string

/** Local time of the complex, `HH:mm`. */
export type LocalTime = string

export type CalendarView = 'day' | 'week' | 'month'
export type CalendarMode = 'editable' | 'readonly'
export type CalendarStatus = 'loading' | 'error' | 'ready'

export type SlotAvailability = 'available' | 'occupied'
export type SlotStatus = SlotAvailability | 'selected'

export interface CalendarSlotInput {
  start: LocalTime
  status: SlotAvailability
}

export interface CalendarDayInput {
  date: IsoDate
  slots: CalendarSlotInput[]
}

export interface SlotSelection {
  date: IsoDate
  start: LocalTime
}

export interface DateRange {
  from: IsoDate
  to: IsoDate
}
