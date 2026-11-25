import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { UserEvent } from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { expectNoA11yViolations } from '../../../test/axe'
import { BookingCalendar } from './BookingCalendar'
import type { BookingCalendarProps } from './BookingCalendar'
import { CALENDAR_FIXTURES, fixtureNamed } from './calendarFixtures'
import type { CalendarFixture } from './calendarFixtures'

const MAX_TABS = 20

function renderFixture(fixture: CalendarFixture, overrides: Partial<BookingCalendarProps> = {}) {
  const handlers = { onSelectionChange: vi.fn(), onRangeChange: vi.fn(), onRetry: vi.fn() }
  const { input } = fixture
  const result = render(
    <BookingCalendar
      slotDurationMinutes={input.slotDurationMinutes}
      days={input.days}
      today={input.today}
      initialView={input.view}
      mode={input.mode}
      selected={input.selected}
      {...handlers}
      {...overrides}
    />,
  )
  return { ...result, ...handlers }
}

function expectNothingEmitted(handlers: Record<'onSelectionChange' | 'onRangeChange' | 'onRetry', ReturnType<typeof vi.fn>>) {
  expect(handlers.onSelectionChange).not.toHaveBeenCalled()
  expect(handlers.onRangeChange).not.toHaveBeenCalled()
  expect(handlers.onRetry).not.toHaveBeenCalled()
}

/** Presses Tab until `isTarget` holds for the focused element, as a keyboard-only user would. */
async function tabUntil(user: UserEvent, isTarget: (element: Element) => boolean): Promise<Element> {
  for (let tabs = 0; tabs < MAX_TABS; tabs++) {
    await user.tab()
    if (document.activeElement && isTarget(document.activeElement)) return document.activeElement
  }
  throw new Error('Target not reachable with Tab')
}

function bodyRows(grid: HTMLElement): HTMLElement[] {
  return within(grid).getAllByRole('row').slice(1)
}

describe('BookingCalendar · shared fixtures', () => {
  describe.each(CALENDAR_FIXTURES)('$name', (fixture) => {
    const { expected } = fixture

    it('shows the title of the period that contains today', () => {
      renderFixture(fixture)

      expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(expected.title)
    })

    it('renders the expected output', () => {
      renderFixture(fixture)

      if (expected.day) {
        const day = expected.day
        expect(screen.getByText(day.summary)).toBeInTheDocument()
        expect(screen.queryByText('Hoy') !== null).toBe(day.isToday)
        if (day.emptyMessage) {
          expect(screen.getByText(day.emptyMessage)).toBeInTheDocument()
          expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
        } else {
          const options = within(screen.getByRole('listbox')).getAllByRole('option')
          expect(options.map((option) => option.getAttribute('aria-label'))).toEqual(
            day.slots.map(({ accessibleLabel }) => accessibleLabel),
          )
          day.slots.forEach((slot, index) => {
            expect(options[index]).toHaveTextContent(slot.label)
            expect(options[index]).toHaveAttribute('aria-selected', String(slot.status === 'selected'))
            expect(options[index].getAttribute('aria-disabled')).toBe(slot.status === 'occupied' ? 'true' : null)
          })
        }
      }

      if (expected.week) {
        const grid = screen.getByRole('grid', { name: expected.title })
        const headers = within(grid).getAllByRole('columnheader').slice(1)
        expected.week.columns.forEach((column, index) => {
          expect(headers[index]).toHaveTextContent(column.header)
          expect(headers[index]).toHaveTextContent(column.summary)
          expect(within(headers[index]).queryByText('Hoy') !== null).toBe(column.isToday)
        })
        const rows = bodyRows(grid)
        expect(rows.map((row) => within(row).getByRole('rowheader').textContent)).toEqual(expected.week.rows)
        expect(
          rows.map((row) => within(row).getAllByRole('gridcell').map((cell) => cell.getAttribute('data-status'))),
        ).toEqual(expected.week.cells)
      }

      if (expected.month) {
        const grid = screen.getByRole('grid', { name: expected.title })
        expect(within(grid).getAllByRole('columnheader').map((header) => header.textContent)).toEqual(
          expected.month.weekdayHeaders,
        )
        const cells = bodyRows(grid).map((row) => within(row).getAllByRole('gridcell'))
        expected.month.weeks.forEach((week, row) =>
          week.forEach((day, column) => {
            const cell = cells[row][column]
            if (day === null) {
              expect(cell).toBeEmptyDOMElement()
            } else {
              expect(cell).toHaveTextContent(String(day.dayOfMonth))
              expect(cell).toHaveTextContent(day.summary)
              expect(cell.getAttribute('aria-current')).toBe(day.isToday ? 'date' : null)
            }
          }),
        )
      }
    })

    it('has no axe violations', async () => {
      const { container } = renderFixture(fixture)

      await expectNoA11yViolations(container)
    })
  })

  it('renders the 30, 60 and 90 minute complexes at the same time without affecting each other', () => {
    const fixtures = ['day-30min', 'day-60min', 'day-90min'].map(fixtureNamed)
    const containers = fixtures.map((fixture) => renderFixture(fixture).container)

    fixtures.forEach(({ expected }, index) => {
      const slots = expected.day?.slots ?? []
      const options = within(containers[index]).getAllByRole('option')
      expect(options).toHaveLength(slots.length)
      slots.forEach(({ label }, slot) => expect(options[slot]).toHaveTextContent(label))
      expect(options.map((option) => option.getAttribute('aria-label'))).toEqual(
        expected.day?.slots.map(({ accessibleLabel }) => accessibleLabel),
      )
    })
  })
})

describe('BookingCalendar · selection', () => {
  it('emits the slot when a free slot is activated', async () => {
    const user = userEvent.setup()
    const { onSelectionChange } = renderFixture(fixtureNamed('day-60min'))

    await user.click(screen.getByRole('option', { name: '20:00 a 21:00, Libre' }))

    expect(onSelectionChange).toHaveBeenCalledExactlyOnceWith({ date: '2026-12-04', start: '20:00' })
  })

  it('clears the selection when the selected slot is activated again', async () => {
    const user = userEvent.setup()
    const { onSelectionChange } = renderFixture(fixtureNamed('day-60min'))

    await user.click(screen.getByRole('option', { name: '19:00 a 20:00, Seleccionado' }))

    expect(onSelectionChange).toHaveBeenCalledExactlyOnceWith(null)
  })

  it('does not emit selectionChange for an occupied slot', async () => {
    const user = userEvent.setup()
    const { onSelectionChange } = renderFixture(fixtureNamed('day-60min'))
    const occupied = screen.getByRole('option', { name: '18:00 a 19:00, Ocupado' })

    await user.click(occupied)
    occupied.focus()
    await user.keyboard('{Enter}[Space]')

    expect(onSelectionChange).not.toHaveBeenCalled()
  })

  it('emits nothing in readonly, with mouse or keyboard', async () => {
    const user = userEvent.setup()
    const handlers = renderFixture(fixtureNamed('week-readonly'))
    const grid = screen.getByRole('grid')

    for (const cell of within(grid).getAllByRole('gridcell').filter((element) => element.hasAttribute('data-status'))) {
      await user.click(cell)
      cell.focus()
      await user.keyboard('{Enter}[Space]')
    }

    expectNothingEmitted(handlers)
    expect(grid).toHaveAttribute('aria-readonly', 'true')
    expect(screen.getByText('Solo lectura')).toBeInTheDocument()
  })
})

describe('BookingCalendar · keyboard only', () => {
  it('keeps a single slot in the tab order and starts on the selection', async () => {
    const user = userEvent.setup()
    renderFixture(fixtureNamed('week-mixed'))
    const grid = screen.getByRole('grid')

    const focused = await tabUntil(user, (element) => grid.contains(element))

    expect(focused).toHaveAttribute('aria-label', '19:00 a 20:00, Seleccionado')
    expect(grid.querySelectorAll('[tabindex="0"]')).toHaveLength(1)
  })

  it('moves with the arrows and selects with Enter or Space', async () => {
    const user = userEvent.setup()
    const { onSelectionChange } = renderFixture(fixtureNamed('day-30min'))
    const listbox = screen.getByRole('listbox')

    expect(await tabUntil(user, (element) => listbox.contains(element))).toHaveAccessibleName('18:00 a 18:30, Libre')

    await user.keyboard('{ArrowDown}')
    expect(document.activeElement).toHaveAccessibleName('18:30 a 19:00, Ocupado')
    await user.keyboard('[Space]')
    expect(onSelectionChange).not.toHaveBeenCalled()

    await user.keyboard('{ArrowDown}{Enter}')
    expect(onSelectionChange).toHaveBeenLastCalledWith({ date: '2026-12-04', start: '19:00' })

    await user.keyboard('{End}[Space]')
    expect(document.activeElement).toHaveAccessibleName('19:30 a 20:00, Libre')
    expect(onSelectionChange).toHaveBeenLastCalledWith({ date: '2026-12-04', start: '19:30' })

    await user.keyboard('{Home}')
    expect(document.activeElement).toHaveAccessibleName('18:00 a 18:30, Libre')
  })

  it('moves across days and times in the week grid, skipping days without that slot', async () => {
    const user = userEvent.setup()
    renderFixture(fixtureNamed('week-mixed'))
    const grid = screen.getByRole('grid')
    await tabUntil(user, (element) => grid.contains(element))

    await user.keyboard('{ArrowRight}')
    expect(document.activeElement).toHaveAccessibleName('19:00 a 20:00, Libre')
    await user.keyboard('{ArrowDown}')
    expect(document.activeElement).toHaveAccessibleName('20:00 a 21:00, Ocupado')
    await user.keyboard('{ArrowUp}{ArrowUp}')
    expect(document.activeElement).toHaveAccessibleName('19:00 a 20:00, Libre')
    expect(grid.querySelectorAll('[tabindex="0"]')).toHaveLength(1)
  })

  it('opens a day of the month with Enter', async () => {
    const user = userEvent.setup()
    const { onRangeChange } = renderFixture(fixtureNamed('month-mixed'))
    const grid = screen.getByRole('grid')

    expect(await tabUntil(user, (element) => grid.contains(element))).toHaveAttribute('aria-current', 'date')
    await user.keyboard('{ArrowLeft}{Enter}')

    expect(onRangeChange).toHaveBeenCalledExactlyOnceWith({ from: '2026-12-01', to: '2026-12-01' })
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('martes 1 de diciembre de 2026')
    expect(screen.getByRole('listbox')).toBeInTheDocument()
  })
})

describe('BookingCalendar · views and periods', () => {
  const weekFixture = fixtureNamed('week-mixed')

  it('opens in the week view by default', () => {
    const { input } = weekFixture
    render(<BookingCalendar slotDurationMinutes={60} days={input.days} today={input.today} />)

    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('30 nov – 6 dic 2026')
    expect(screen.getByRole('button', { name: 'Semana' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('emits rangeChange when the period changes', async () => {
    const user = userEvent.setup()
    const { onRangeChange } = renderFixture(weekFixture)

    await user.click(screen.getByRole('button', { name: 'Semana siguiente' }))
    expect(onRangeChange).toHaveBeenLastCalledWith({ from: '2026-12-07', to: '2026-12-13' })
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('7 – 13 dic 2026')

    await user.click(screen.getByRole('button', { name: 'Semana anterior' }))
    expect(onRangeChange).toHaveBeenLastCalledWith({ from: '2026-11-30', to: '2026-12-06' })
  })

  it('emits rangeChange when the view changes', async () => {
    const user = userEvent.setup()
    const { onRangeChange } = renderFixture(weekFixture)

    await user.click(screen.getByRole('button', { name: 'Mes' }))
    expect(onRangeChange).toHaveBeenLastCalledWith({ from: '2026-12-01', to: '2026-12-31' })

    await user.click(screen.getByRole('button', { name: 'Día' }))
    expect(onRangeChange).toHaveBeenLastCalledWith({ from: '2026-12-02', to: '2026-12-02' })

    await user.click(screen.getByRole('button', { name: 'Día' }))
    expect(onRangeChange).toHaveBeenCalledTimes(2)
  })

  it('still lets readonly users move between periods', async () => {
    const user = userEvent.setup()
    const { onRangeChange } = renderFixture(fixtureNamed('week-readonly'))

    await user.click(screen.getByRole('button', { name: 'Semana siguiente' }))

    expect(onRangeChange).toHaveBeenCalledExactlyOnceWith({ from: '2026-12-07', to: '2026-12-13' })
  })
})

describe('BookingCalendar · loading and error', () => {
  it('shows a busy skeleton and disables changing view and period while loading', async () => {
    const { container } = renderFixture(fixtureNamed('week-mixed'), { status: 'loading' })

    expect(screen.getByText('Cargando horarios…')).toBeInTheDocument()
    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull()
    expect(screen.queryByRole('grid')).not.toBeInTheDocument()
    for (const name of ['Semana anterior', 'Semana siguiente', 'Día', 'Semana', 'Mes']) {
      expect(screen.getByRole('button', { name })).toBeDisabled()
    }
    await expectNoA11yViolations(container)
  })

  it('shows the error and emits retry from «Reintentar»', async () => {
    const user = userEvent.setup()
    const { container, onRetry } = renderFixture(fixtureNamed('week-mixed'), { status: 'error' })

    expect(screen.getByRole('alert')).toHaveTextContent('No pudimos cargar los horarios.')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(onRetry).toHaveBeenCalledOnce()
    await expectNoA11yViolations(container)
  })
})
