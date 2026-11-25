import type { MonthModel, WeekColumnModel } from './calendarModel'
import type {
  CalendarDayInput,
  CalendarMode,
  CalendarView,
  DateRange,
  IsoDate,
  LocalTime,
  SlotSelection,
  SlotStatus,
} from './calendarTypes'

// Typed access to the shared fixtures copied from design-system/fixtures/calendar (test-only module).

export interface ExpectedSlot {
  start: LocalTime
  end: LocalTime
  label: string
  status: SlotStatus
  accessibleLabel: string
  selectable: boolean
}

export interface ExpectedDay {
  date: IsoDate
  isToday: boolean
  availableCount: number
  occupiedCount: number
  summary: string
  emptyMessage: string | null
  slots: ExpectedSlot[]
}

export interface CalendarFixture {
  name: string
  description: string
  input: {
    slotDurationMinutes: number
    view: CalendarView
    mode: CalendarMode
    today: IsoDate
    selected: SlotSelection | null
    days: CalendarDayInput[]
  }
  expected: {
    range: DateRange
    selected: SlotSelection | null
    title: string
    day?: ExpectedDay
    week?: { columns: WeekColumnModel[]; rows: LocalTime[]; cells: (SlotStatus | null)[][] }
    month?: MonthModel
  }
}

const modules = import.meta.glob<CalendarFixture>('./fixtures/*.json', { eager: true, import: 'default' })

export const CALENDAR_FIXTURES: CalendarFixture[] = Object.values(modules)

export function fixtureNamed(name: string): CalendarFixture {
  const fixture = CALENDAR_FIXTURES.find((candidate) => candidate.name === name)
  if (!fixture) throw new Error(`Calendar fixture not found: ${name}`)
  return fixture
}
