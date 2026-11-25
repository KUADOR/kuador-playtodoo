import { ALL_CALENDAR_FIXTURES, CalendarFixture, CALENDAR_FIXTURES } from './calendar-fixtures';
import {
  buildDayView,
  buildMonthView,
  buildWeekView,
  periodTitle,
  shiftAnchor,
  visibleRange,
} from './calendar-model';

const SELECTED = 'selected';
const AVAILABLE = 'available';

interface ExpectedSlot {
  readonly start: string;
  readonly end: string;
  readonly label: string;
  readonly status: string;
  readonly accessibleLabel: string;
  readonly selectable: boolean;
}

/**
 * The backoffice is always read-only (decision D6): a slot the editable fixture shows as
 * selected keeps the status it received, which is always available.
 */
function withoutSelection(status: string | null): string | null {
  return status === SELECTED ? AVAILABLE : status;
}

function readonlySlot({ selectable: _ignored, ...slot }: ExpectedSlot) {
  return {
    ...slot,
    status: withoutSelection(slot.status),
    accessibleLabel: slot.accessibleLabel.replace(', Seleccionado', ', Libre'),
  };
}

function render(fixture: CalendarFixture) {
  const range = visibleRange(fixture.input.view, fixture.input.today);
  return { range, title: periodTitle(fixture.input.view, range) };
}

describe('calendar model (read-only)', () => {
  it.each(ALL_CALENDAR_FIXTURES.map((fixture) => [fixture.name, fixture] as const))(
    'matches the range and title of %s',
    (_name, fixture) => {
      const { range, title } = render(fixture);

      expect(range).toEqual(fixture.expected['range']);
      expect(title).toBe(fixture.expected['title']);
    },
  );

  it.each(
    ALL_CALENDAR_FIXTURES.filter((fixture) => fixture.input.view === 'day').map(
      (fixture) => [fixture.name, fixture] as const,
    ),
  )('matches the expected day of %s without any selection', (_name, fixture) => {
    const { slots: expectedSlots, ...expectedDay } = fixture.expected['day'] as {
      slots: ExpectedSlot[];
    };

    const day = buildDayView(fixture.input.today, fixture.input);

    expect(day).toMatchObject(expectedDay);
    expect(day.slots).toEqual(expectedSlots.map(readonlySlot));
  });

  it('respects the slot duration of each complex on the same day (30, 60 and 90 min)', () => {
    const labelsOf = (fixture: CalendarFixture) =>
      buildDayView(fixture.input.today, fixture.input).slots.map((slot) => slot.label);

    expect(labelsOf(CALENDAR_FIXTURES.day30)[0]).toBe('18:00–18:30');
    expect(labelsOf(CALENDAR_FIXTURES.day60)[0]).toBe('18:00–19:00');
    expect(labelsOf(CALENDAR_FIXTURES.day90).at(-1)).toBe('22:30–00:00');
  });

  it.each([CALENDAR_FIXTURES.weekMixed, CALENDAR_FIXTURES.weekReadonly])(
    'matches the expected week of $name without any selection',
    (fixture) => {
      const expected = fixture.expected['week'] as {
        columns: unknown[];
        rows: string[];
        cells: (string | null)[][];
      };

      const week = buildWeekView(render(fixture).range, fixture.input);

      expect(week.columns).toMatchObject(expected.columns);
      expect(week.rows).toEqual(expected.rows);
      expect(week.cells.map((row) => row.map((cell) => cell?.status ?? null))).toEqual(
        expected.cells.map((row) => row.map(withoutSelection)),
      );
    },
  );

  it('matches the expected month', () => {
    const fixture = CALENDAR_FIXTURES.monthMixed;

    const month = buildMonthView(render(fixture).range, fixture.input);

    expect(month).toMatchObject(fixture.expected['month'] as object);
  });

  it('labels the days that open the day view with date, summary and today', () => {
    const fixture = CALENDAR_FIXTURES.weekReadonly;

    const week = buildWeekView(render(fixture).range, fixture.input);

    expect(week.columns[2].accessibleLabel).toBe('miércoles 2 de diciembre de 2026, 2 libres, Hoy');
    expect(week.columns[1].accessibleLabel).toBe('martes 1 de diciembre de 2026, Sin horarios');
  });

  it.each([
    ['day', '2026-12-31', '2027-01-01'],
    ['week', '2026-12-02', '2026-12-09'],
    ['month', '2026-12-31', '2027-01-01'],
  ] as const)('moves a %s forward from %s to %s', (view, anchor, expected) => {
    expect(shiftAnchor(view, anchor, 1)).toBe(expected);
  });

  it.each([
    ['day', '2026-12-01', '2026-11-30'],
    ['week', '2026-12-02', '2026-11-25'],
    ['month', '2026-03-31', '2026-02-01'],
  ] as const)('moves a %s back from %s to %s', (view, anchor, expected) => {
    expect(shiftAnchor(view, anchor, -1)).toBe(expected);
  });
});
