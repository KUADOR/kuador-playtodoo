/** Inputs and outputs of the shared BookingCalendar contract (design-system/docs/components/calendar.md). */

/** Local date of the complex, `YYYY-MM-DD`. */
export type IsoDate = string;

/** Local time of the complex, `HH:mm`. */
export type LocalTime = string;

export type CalendarView = 'day' | 'week' | 'month';

export type CalendarStatus = 'loading' | 'error' | 'ready';

/**
 * `readonly` only shows the occupancy; `editable` also lets the person choose one free slot
 * (Kuador support, ADR-020).
 */
export type CalendarMode = 'readonly' | 'editable';

export type SlotStatus = 'available' | 'occupied';

export interface CalendarSlot {
  readonly start: LocalTime;
  readonly status: SlotStatus;
}

export interface CalendarDay {
  readonly date: IsoDate;
  readonly slots: readonly CalendarSlot[];
}

/** The slot chosen in `editable` mode. */
export interface SlotSelection {
  readonly date: IsoDate;
  readonly start: LocalTime;
}

/** Inclusive period shown by the calendar; emitted so the screen can load its data. */
export interface CalendarRange {
  readonly from: IsoDate;
  readonly to: IsoDate;
}
