import day30 from './fixtures/day-30min.json';
import day60 from './fixtures/day-60min.json';
import day90 from './fixtures/day-90min.json';
import dayAllOccupied from './fixtures/day-all-occupied.json';
import dayEmpty from './fixtures/day-empty.json';
import monthMixed from './fixtures/month-mixed.json';
import weekMixed from './fixtures/week-mixed.json';
import weekReadonly from './fixtures/week-readonly.json';
import { CalendarDay, CalendarView, IsoDate } from './booking-calendar.types';

/** Shared fixture (design-system/fixtures/calendar), typed for the backoffice tests and catalog. */
export interface CalendarFixture {
  readonly name: string;
  readonly input: {
    readonly slotDurationMinutes: number;
    readonly view: CalendarView;
    readonly today: IsoDate;
    readonly days: readonly CalendarDay[];
  };
  // The expected output is compared structurally, so its shape is not repeated here.
  readonly expected: Record<string, unknown>;
}

const asFixture = (fixture: unknown) => fixture as CalendarFixture;

export const CALENDAR_FIXTURES = {
  day30: asFixture(day30),
  day60: asFixture(day60),
  day90: asFixture(day90),
  dayEmpty: asFixture(dayEmpty),
  dayAllOccupied: asFixture(dayAllOccupied),
  weekMixed: asFixture(weekMixed),
  weekReadonly: asFixture(weekReadonly),
  monthMixed: asFixture(monthMixed),
} as const;

export const ALL_CALENDAR_FIXTURES: readonly CalendarFixture[] = Object.values(CALENDAR_FIXTURES);
