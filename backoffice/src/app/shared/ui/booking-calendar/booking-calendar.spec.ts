import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { BookingCalendar } from './booking-calendar';
import {
  CalendarRange,
  CalendarStatus,
  CalendarView,
  SlotSelection,
} from './booking-calendar.types';
import { CalendarFixture, CALENDAR_FIXTURES } from './calendar-fixtures';

describe('BookingCalendar (read-only)', () => {
  let fixture: ComponentFixture<BookingCalendar>;
  let ranges: CalendarRange[];
  let retryCount: number;

  async function render(
    source: CalendarFixture,
    options: { view?: CalendarView; status?: CalendarStatus } = {},
  ) {
    fixture = TestBed.createComponent(BookingCalendar);
    const { componentRef } = fixture;
    componentRef.setInput('slotDurationMinutes', source.input.slotDurationMinutes);
    componentRef.setInput('today', source.input.today);
    componentRef.setInput('days', source.input.days);
    componentRef.setInput('initialView', options.view ?? source.input.view);
    componentRef.setInput('status', options.status ?? 'ready');
    ranges = [];
    retryCount = 0;
    fixture.componentInstance.rangeChange.subscribe((range) => ranges.push(range));
    fixture.componentInstance.retry.subscribe(() => retryCount++);
    await fixture.whenStable();
  }

  const element = (): HTMLElement => fixture.nativeElement;
  const all = (selector: string) => Array.from(element().querySelectorAll<HTMLElement>(selector));
  const text = (selector: string) => element().querySelector(selector)?.textContent?.trim();
  const title = () => text('.calendar__title');
  const slotLabels = () =>
    all('app-calendar-slot .visually-hidden').map((node) => node.textContent);

  function button(name: string): HTMLButtonElement {
    const match = all('button').find(
      (candidate) =>
        candidate.getAttribute('aria-label') === name || candidate.textContent?.trim() === name,
    );
    if (!match) {
      throw new Error(`No button named «${name}»`);
    }
    return match as HTMLButtonElement;
  }

  async function click(name: string) {
    button(name).click();
    await fixture.whenStable();
  }

  async function press(target: HTMLElement, key: string) {
    target.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
    await fixture.whenStable();
  }

  describe('read-only mode', () => {
    it('always shows the read-only notice with the eye icon', async () => {
      await render(CALENDAR_FIXTURES.weekReadonly);

      expect(text('.calendar__readonly')).toBe('Solo lectura');
      expect(element().querySelector('.calendar__readonly svg')?.getAttribute('aria-hidden')).toBe(
        'true',
      );
    });

    it.each([
      ['day', CALENDAR_FIXTURES.day60],
      ['week', CALENDAR_FIXTURES.weekMixed],
    ] as const)(
      'renders %s slots as non-interactive content, even when the data carries a selection',
      async (_view, source) => {
        await render(source);

        const slots = all('app-calendar-slot');
        expect(slots.length).toBeGreaterThan(0);
        for (const slot of slots) {
          expect(slot.closest('button')).toBeNull();
          expect(slot.querySelector('button, [tabindex], [aria-selected]')).toBeNull();
          expect(slot.hasAttribute('tabindex')).toBe(false);
          expect(slot.dataset['status']).not.toBe('selected');
        }
        expect(element().querySelector('[aria-selected]')).toBeNull();
      },
    );

    it('does not react nor emit anything when a slot is clicked', async () => {
      await render(CALENDAR_FIXTURES.day60);
      const before = element().innerHTML;

      all('app-calendar-slot').forEach((slot) => slot.click());
      await fixture.whenStable();

      expect(element().innerHTML).toBe(before);
      expect(ranges).toEqual([]);
    });

    it('is read-only by default: no slot is a button and nothing can be selected', async () => {
      await render(CALENDAR_FIXTURES.day60);
      const emitted: unknown[] = [];
      fixture.componentInstance.selectionChange.subscribe((selection) => emitted.push(selection));

      all('app-calendar-slot').forEach((slot) => slot.click());
      await fixture.whenStable();

      expect(all('.calendar__slot-button')).toEqual([]);
      expect(emitted).toEqual([]);
    });
  });

  describe('day view', () => {
    it('shows sorted slots with their end time, accessible label and summary (30 min)', async () => {
      await render(CALENDAR_FIXTURES.day30);

      expect(title()).toBe('viernes 4 de diciembre de 2026');
      expect(text('.calendar__summary')).toBe('3 libres');
      expect(text('.calendar__today')).toBe('Hoy');
      expect(slotLabels()).toEqual([
        '18:00 a 18:30, Libre',
        '18:30 a 19:00, Ocupado',
        '19:00 a 19:30, Libre',
        '19:30 a 20:00, Libre',
      ]);
    });

    it('marks occupied slots with the lock icon and text, not only colour', async () => {
      await render(CALENDAR_FIXTURES.dayAllOccupied);

      const occupied = all('app-calendar-slot[data-status="occupied"]');
      expect(occupied.length).toBe(all('app-calendar-slot').length);
      for (const slot of occupied) {
        expect(slot.querySelector('.slot__occupied')?.textContent?.trim()).toBe('Ocupado');
        expect(slot.querySelector('.slot__occupied svg')).not.toBeNull();
      }
      expect(text('.calendar__summary')).toBe('Sin horarios');
    });

    it('shows the empty message for a day without slots', async () => {
      await render(CALENDAR_FIXTURES.dayEmpty);

      expect(text('.calendar__empty')).toBe('Este día no tiene horarios. Prueba con otro día.');
      expect(all('app-calendar-slot')).toEqual([]);
    });
  });

  describe('slot duration per complex', () => {
    @Component({
      imports: [BookingCalendar],
      template: `
        @for (source of sources; track source.name) {
          <app-booking-calendar
            [slotDurationMinutes]="source.input.slotDurationMinutes"
            [today]="source.input.today"
            [days]="source.input.days"
            initialView="day"
          />
        }
      `,
    })
    class ThreeComplexes {
      readonly sources = [
        CALENDAR_FIXTURES.day30,
        CALENDAR_FIXTURES.day60,
        CALENDAR_FIXTURES.day90,
      ];
    }

    it('shows each complex with its own duration side by side (30, 60 and 90 min)', async () => {
      const host = TestBed.createComponent(ThreeComplexes);
      await host.whenStable();

      const calendars = Array.from<HTMLElement>(
        host.nativeElement.querySelectorAll('app-booking-calendar'),
      );
      const visibleLabels = calendars.map((calendar) =>
        Array.from(calendar.querySelectorAll('app-calendar-slot .slot__label')).map(
          (label) => label.textContent,
        ),
      );
      expect(visibleLabels[0]).toEqual([
        '18:00–18:30',
        '18:30–19:00',
        '19:00–19:30',
        '19:30–20:00',
      ]);
      expect(visibleLabels[1][0]).toBe('18:00–19:00');
      expect(visibleLabels[2].at(-1)).toBe('22:30–00:00');
    });
  });

  describe('week view', () => {
    it('shows the week columns, start times and the status of each cell', async () => {
      await render(CALENDAR_FIXTURES.weekReadonly);
      const expected = CALENDAR_FIXTURES.weekReadonly.expected['week'] as {
        rows: string[];
        cells: (string | null)[][];
      };

      expect(title()).toBe('30 nov – 6 dic 2026');
      expect(all('thead .calendar__day-name').map((node) => node.textContent?.trim())).toEqual([
        'lun 30',
        'mar 1',
        'mié 2',
        'jue 3',
        'vie 4',
        'sáb 5',
        'dom 6',
      ]);
      expect(all('tbody th').map((node) => node.textContent?.trim())).toEqual(expected.rows);
      const cells = all('tbody tr').map((row) =>
        Array.from(row.querySelectorAll('td')).map(
          (cell) => cell.querySelector<HTMLElement>('app-calendar-slot')?.dataset['status'] ?? null,
        ),
      );
      expect(cells).toEqual(expected.cells);
    });

    it('opens the day view when a day header is chosen', async () => {
      await render(CALENDAR_FIXTURES.weekReadonly);

      await click('viernes 4 de diciembre de 2026, 2 libres');

      expect(title()).toBe('viernes 4 de diciembre de 2026');
      expect(ranges).toEqual([{ from: '2026-12-04', to: '2026-12-04' }]);
    });

    it('keeps the horizontally scrollable week reachable with the keyboard', async () => {
      await render(CALENDAR_FIXTURES.weekReadonly);

      expect(element().querySelector('.calendar__scroll')?.getAttribute('tabindex')).toBe('0');
    });
  });

  describe('month view', () => {
    it('shows the month grid starting on Monday with a summary per day', async () => {
      await render(CALENDAR_FIXTURES.monthMixed);

      expect(title()).toBe('diciembre 2026');
      expect(all('.calendar__month-day').length).toBe(31);
      expect(all('tbody tr:first-child td')[0].querySelector('button')).toBeNull();
      expect(button('miércoles 2 de diciembre de 2026, 1 libre, Hoy')).toBeTruthy();
      expect(button('sábado 5 de diciembre de 2026, Sin horarios')).toBeTruthy();
    });

    it('opens the day view of the chosen day', async () => {
      await render(CALENDAR_FIXTURES.monthMixed);

      await click('jueves 31 de diciembre de 2026, 3 libres');

      expect(title()).toBe('jueves 31 de diciembre de 2026');
      expect(slotLabels()).toEqual([
        '10:00 a 11:00, Libre',
        '11:00 a 12:00, Libre',
        '12:00 a 13:00, Libre',
      ]);
      expect(ranges).toEqual([{ from: '2026-12-31', to: '2026-12-31' }]);
    });
  });

  describe('keyboard', () => {
    const focusable = () => all('.calendar__month-day').filter((day) => day.tabIndex === 0);

    it('keeps a single month day in the tab order, starting on today', async () => {
      await render(CALENDAR_FIXTURES.monthMixed);

      expect(focusable().map((day) => day.dataset['date'])).toEqual(['2026-12-02']);
    });

    it('moves the focus between month days with the arrow keys', async () => {
      await render(CALENDAR_FIXTURES.monthMixed);
      const today = focusable()[0];
      today.focus();

      await press(today, 'ArrowDown');
      expect(document.activeElement?.getAttribute('data-date')).toBe('2026-12-09');

      await press(document.activeElement as HTMLElement, 'ArrowRight');
      expect(document.activeElement?.getAttribute('data-date')).toBe('2026-12-10');
      expect(focusable().map((day) => day.dataset['date'])).toEqual(['2026-12-10']);
    });

    it('does not leave the month with the arrow keys', async () => {
      await render(CALENDAR_FIXTURES.monthMixed);
      const today = focusable()[0];

      await press(today, 'ArrowUp');

      expect(focusable().map((day) => day.dataset['date'])).toEqual(['2026-12-02']);
    });

    it('uses native buttons for every action, so Tab, Enter and Space work', async () => {
      await render(CALENDAR_FIXTURES.monthMixed);

      const actions = all('[aria-pressed], .calendar__icon-button, .calendar__month-day');
      expect(actions.every((action) => action.tagName === 'BUTTON')).toBe(true);
    });
  });

  describe('navigation (rangeChange)', () => {
    it('marks the current view and emits the new range when the view changes', async () => {
      await render(CALENDAR_FIXTURES.weekReadonly);
      expect(button('Semana').getAttribute('aria-pressed')).toBe('true');

      await click('Mes');
      await click('Día');

      expect(button('Día').getAttribute('aria-pressed')).toBe('true');
      expect(button('Semana').getAttribute('aria-pressed')).toBe('false');
      expect(ranges).toEqual([
        { from: '2026-12-01', to: '2026-12-31' },
        { from: '2026-12-02', to: '2026-12-02' },
      ]);
    });

    it('does not emit when the current view is chosen again', async () => {
      await render(CALENDAR_FIXTURES.weekReadonly);

      await click('Semana');

      expect(ranges).toEqual([]);
    });

    it('moves to the previous and next week', async () => {
      await render(CALENDAR_FIXTURES.weekReadonly);

      await click('Semana siguiente');
      expect(title()).toBe('7 dic – 13 dic 2026');
      await click('Semana anterior');
      await click('Semana anterior');

      expect(title()).toBe('23 nov – 29 nov 2026');
      expect(ranges).toEqual([
        { from: '2026-12-07', to: '2026-12-13' },
        { from: '2026-11-30', to: '2026-12-06' },
        { from: '2026-11-23', to: '2026-11-29' },
      ]);
    });

    it('moves to the next month and day with labels that name the period', async () => {
      await render(CALENDAR_FIXTURES.monthMixed);

      await click('Mes siguiente');
      expect(title()).toBe('enero 2027');

      await click('Día');
      await click('Día anterior');

      expect(title()).toBe('jueves 31 de diciembre de 2026');
    });
  });

  describe('loading and error', () => {
    it('shows a skeleton, announces busy and disables navigation while loading', async () => {
      await render(CALENDAR_FIXTURES.weekReadonly, { status: 'loading' });

      expect(element().querySelector('.calendar')?.getAttribute('aria-busy')).toBe('true');
      expect(all('.calendar__skeleton-line').length).toBeGreaterThan(0);
      expect(text('.calendar__skeleton .visually-hidden')).toBe('Cargando horarios…');
      expect(
        all('.calendar__toolbar button').every((action) => action.hasAttribute('disabled')),
      ).toBe(true);
      expect(all('app-calendar-slot')).toEqual([]);
    });

    it('shows the error message and emits retry from «Reintentar»', async () => {
      await render(CALENDAR_FIXTURES.weekReadonly, { status: 'error' });

      expect(element().querySelector('[role="alert"]')?.textContent?.trim()).toBe(
        'No pudimos cargar los horarios.',
      );
      await click('Reintentar');

      expect(retryCount).toBe(1);
      expect(all('app-calendar-slot')).toEqual([]);
    });
  });

  describe('accessibility', () => {
    it.each([
      ['day', CALENDAR_FIXTURES.day90, {}],
      ['empty day', CALENDAR_FIXTURES.dayEmpty, {}],
      ['week', CALENDAR_FIXTURES.weekReadonly, {}],
      ['month', CALENDAR_FIXTURES.monthMixed, {}],
      ['loading', CALENDAR_FIXTURES.weekReadonly, { status: 'loading' as const }],
      ['error', CALENDAR_FIXTURES.weekReadonly, { status: 'error' as const }],
    ])('has no axe violations in the %s state', async (_state, source, options) => {
      await render(source, options);

      await expectNoAxeViolations(element());
    });
  });
});

describe('BookingCalendar (editable)', () => {
  let fixture: ComponentFixture<BookingCalendar>;
  let selections: (SlotSelection | null)[];

  /** Mounts `source` in editable mode with its selection; a new selection is fed back. */
  async function render(source: CalendarFixture, view: CalendarView = source.input.view) {
    fixture = TestBed.createComponent(BookingCalendar);
    const { componentRef } = fixture;
    componentRef.setInput('slotDurationMinutes', source.input.slotDurationMinutes);
    componentRef.setInput('today', source.input.today);
    componentRef.setInput('days', source.input.days);
    componentRef.setInput('initialView', view);
    componentRef.setInput('mode', 'editable');
    componentRef.setInput('selected', source.input.selected ?? null);
    selections = [];
    fixture.componentInstance.selectionChange.subscribe((selection) => {
      selections.push(selection);
      componentRef.setInput('selected', selection);
    });
    await fixture.whenStable();
  }

  const element = (): HTMLElement => fixture.nativeElement;
  const slotButtons = () =>
    Array.from(element().querySelectorAll<HTMLButtonElement>('.calendar__slot-button'));
  const slotButton = (label: string) =>
    slotButtons().find((button) => button.textContent?.includes(label)) as HTMLButtonElement;
  const tabbable = () => slotButtons().filter((button) => button.tabIndex === 0);

  async function press(target: HTMLElement, key: string) {
    target.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
    await fixture.whenStable();
  }

  it('hides the read-only notice and shows the selection with its state and label', async () => {
    await render(CALENDAR_FIXTURES.day60);

    expect(element().querySelector('.calendar__readonly')).toBeNull();
    const selected = slotButton('19:00–20:00');
    expect(selected.getAttribute('aria-pressed')).toBe('true');
    expect(selected.textContent).toContain('19:00 a 20:00, Seleccionado');
    expect(selected.querySelector('svg')).not.toBeNull();
    expect(slotButton('18:00–19:00').getAttribute('aria-disabled')).toBe('true');
  });

  it('selects a free slot, clears it when activated again and ignores occupied ones', async () => {
    await render(CALENDAR_FIXTURES.day60);

    slotButton('20:00–21:00').click();
    await fixture.whenStable();
    slotButton('20:00–21:00').click();
    await fixture.whenStable();
    slotButton('18:00–19:00').click();
    await fixture.whenStable();

    expect(selections).toEqual([{ date: '2026-12-04', start: '20:00' }, null]);
  });

  it('keeps a single slot in the tab order and moves it with the arrow keys', async () => {
    await render(CALENDAR_FIXTURES.day60);
    expect(tabbable().map((button) => button.textContent?.includes('19:00–20:00'))).toEqual([true]);

    await press(tabbable()[0], 'ArrowRight');
    expect(document.activeElement?.textContent).toContain('20:00–21:00');
    await press(document.activeElement as HTMLElement, 'End');
    expect(document.activeElement?.textContent).toContain('21:00–22:00');
    await press(document.activeElement as HTMLElement, 'Home');

    expect(document.activeElement?.textContent).toContain('18:00–19:00');
    expect(tabbable()).toEqual([document.activeElement]);
  });

  it('selects in the week view too', async () => {
    await render(CALENDAR_FIXTURES.weekMixed);

    const free = slotButtons().find(
      (button) => button.getAttribute('aria-pressed') === 'false' && !button.ariaDisabled,
    ) as HTMLButtonElement;
    free.click();
    await fixture.whenStable();

    expect(selections).toHaveLength(1);
    expect(
      slotButtons().filter((button) => button.getAttribute('aria-pressed') === 'true'),
    ).toEqual([free]);
  });

  it.each([
    ['day', CALENDAR_FIXTURES.day60],
    ['week', CALENDAR_FIXTURES.weekMixed],
  ] as const)('has no axe violations in the editable %s view', async (_view, source) => {
    await render(source);

    await expectNoAxeViolations(element());
  });
});
