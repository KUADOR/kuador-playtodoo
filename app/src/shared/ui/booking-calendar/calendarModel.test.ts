import { describe, expect, it } from 'vitest'
import { addMinutes } from './calendarDates'
import { CALENDAR_FIXTURES } from './calendarFixtures'
import { buildDay, buildMonth, buildWeek, createCalendarSource, rangeFor, shiftAnchor, titleFor } from './calendarModel'
import type { SlotModel } from './calendarModel'
import { formatSummary, formatWeekTitle } from './calendarText'

function withoutDate({ date: _date, ...slot }: SlotModel) {
  return slot
}

describe('calendar model · shared fixtures', () => {
  it('loads every fixture copied from design-system', () => {
    expect(CALENDAR_FIXTURES.map(({ name }) => name).sort()).toEqual([
      'day-30min',
      'day-60min',
      'day-90min',
      'day-all-occupied',
      'day-empty',
      'month-mixed',
      'week-mixed',
      'week-readonly',
    ])
  })

  describe.each(CALENDAR_FIXTURES)('$name', ({ input, expected }) => {
    // The calendar opens on the period that contains `today`.
    const source = createCalendarSource(input)
    const anchor = input.today

    it('resolves range, title and effective selection', () => {
      expect(rangeFor(input.view, anchor)).toEqual(expected.range)
      expect(titleFor(input.view, anchor)).toBe(expected.title)
      expect(source.selection).toEqual(expected.selected)
    })

    it(`builds the ${input.view} view`, () => {
      if (expected.day) {
        const { slots, ...day } = buildDay(source, anchor)
        expect({ ...day, slots: slots.map(withoutDate) }).toEqual(expected.day)
      }
      if (expected.week) {
        const week = buildWeek(source, anchor)
        expect(week.columns).toEqual(expected.week.columns)
        expect(week.rows).toEqual(expected.week.rows)
        expect(week.cells.map((cells) => cells.map((slot) => slot?.status ?? null))).toEqual(expected.week.cells)
      }
      if (expected.month) {
        expect(buildMonth(source, anchor)).toEqual(expected.month)
      }
    })
  })
})

describe('calendar model · rules beyond the fixtures', () => {
  it('wraps the end of a slot past midnight', () => {
    expect(addMinutes('22:30', 90)).toBe('00:00')
    expect(addMinutes('23:30', 60)).toBe('00:30')
  })

  it('writes the week title according to the months and years it spans', () => {
    expect(formatWeekTitle('2026-12-07', '2026-12-13')).toBe('7 – 13 dic 2026')
    expect(formatWeekTitle('2026-11-30', '2026-12-06')).toBe('30 nov – 6 dic 2026')
    expect(formatWeekTitle('2026-12-28', '2027-01-03')).toBe('28 dic 2026 – 3 ene 2027')
  })

  it('summarises free slots in singular and plural', () => {
    expect(formatSummary(0)).toBe('Sin horarios')
    expect(formatSummary(1)).toBe('1 libre')
    expect(formatSummary(2)).toBe('2 libres')
  })

  it('moves to the previous and next period of each view', () => {
    expect(shiftAnchor('day', '2026-12-31', 1)).toBe('2027-01-01')
    expect(shiftAnchor('week', '2026-12-02', -1)).toBe('2026-11-25')
    expect(shiftAnchor('month', '2026-03-31', -1)).toBe('2026-02-01')
  })

  it('ignores a selection on a slot that does not exist', () => {
    const source = createCalendarSource({
      slotDurationMinutes: 60,
      mode: 'editable',
      today: '2026-12-04',
      selected: { date: '2026-12-04', start: '07:00' },
      days: [{ date: '2026-12-04', slots: [{ start: '18:00', status: 'available' }] }],
    })

    expect(source.selection).toBeNull()
  })
})
