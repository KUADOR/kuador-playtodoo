import { useId, useMemo, useState } from 'react'
import { Eye } from 'lucide-react'
import a11y from '../a11y.module.css'
import { Button } from '../button/Button'
import { buildDay, buildMonth, buildWeek, createCalendarSource, rangeFor, shiftAnchor, titleFor } from './calendarModel'
import type { CalendarSource, SlotModel } from './calendarModel'
import { CALENDAR_TEXT, PERIOD_NAVIGATION, VIEW_NAMES } from './calendarText'
import type {
  CalendarDayInput,
  CalendarMode,
  CalendarStatus,
  CalendarView,
  DateRange,
  IsoDate,
  SlotSelection,
} from './calendarTypes'
import { DayView } from './DayView'
import { MonthView } from './MonthView'
import { WeekView } from './WeekView'
import styles from './BookingCalendar.module.css'

const VIEWS: CalendarView[] = ['day', 'week', 'month']
const SKELETON_ROWS = 4

export interface BookingCalendarProps {
  /** Slot length of the complex; defines the end time of every slot. Integer greater than 0. */
  slotDurationMinutes: number
  /** Days in the local time of the complex. A missing day, or one without slots, has no schedule. */
  days: CalendarDayInput[]
  /** Injected current date of the complex; the component never reads the clock. It opens on the period containing it. */
  today: IsoDate
  /** View shown first. */
  initialView?: CalendarView
  mode?: CalendarMode
  /** One slot at a time. Ignored when it points to an occupied or missing slot. */
  selected?: SlotSelection | null
  status?: CalendarStatus
  /** A free slot was activated, or the selected one was activated again to clear it (`null`). Never in readonly. */
  onSelectionChange?: (selection: SlotSelection | null) => void
  /** The view or the period changed: the screen should load the days of the new range. */
  onRangeChange?: (range: DateRange) => void
  /** «Reintentar» was pressed in the error state. */
  onRetry?: () => void
}

/**
 * Presentational booking calendar (design-system/docs/components/calendar.md): it shows the slots it
 * receives and reports the selection. Availability and booking rules belong to the Reservas module.
 */
export function BookingCalendar({
  slotDurationMinutes,
  days,
  today,
  initialView = 'week',
  mode = 'editable',
  selected = null,
  status = 'ready',
  onSelectionChange,
  onRangeChange,
  onRetry,
}: BookingCalendarProps) {
  const [view, setView] = useState(initialView)
  const [anchor, setAnchor] = useState(today)
  const titleId = useId()
  const isLoading = status === 'loading'
  const title = titleFor(view, anchor)

  const source = useMemo(
    () => createCalendarSource({ slotDurationMinutes, days, mode, selected, today }),
    [slotDurationMinutes, days, mode, selected, today],
  )

  function navigate(nextView: CalendarView, nextAnchor: IsoDate) {
    setView(nextView)
    setAnchor(nextAnchor)
    onRangeChange?.(rangeFor(nextView, nextAnchor))
  }

  function activateSlot(slot: SlotModel) {
    if (!slot.selectable) return
    onSelectionChange?.(slot.status === 'selected' ? null : { date: slot.date, start: slot.start })
  }

  return (
    // A group, not a region landmark: the screen owns its landmarks and may show several calendars of the same period.
    <div role="group" className={styles.calendar} aria-labelledby={titleId}>
      <header className={styles.toolbar}>
        <h2 id={titleId} className={styles.title} aria-live="polite">
          {title}
        </h2>
        <div className={styles.controls}>
          <div className={styles.navigation}>
            <Button
              variant="secondary"
              label={PERIOD_NAVIGATION[view].previous}
              disabled={isLoading}
              onClick={() => navigate(view, shiftAnchor(view, anchor, -1))}
            />
            <Button
              variant="secondary"
              label={PERIOD_NAVIGATION[view].next}
              disabled={isLoading}
              onClick={() => navigate(view, shiftAnchor(view, anchor, 1))}
            />
          </div>
          <div role="group" aria-label={CALENDAR_TEXT.viewSwitcher} className={styles.viewSwitcher}>
            {VIEWS.map((option) => (
              <Button
                key={option}
                variant={option === view ? 'primary' : 'secondary'}
                label={VIEW_NAMES[option]}
                aria-pressed={option === view}
                disabled={isLoading}
                onClick={() => option !== view && navigate(option, anchor)}
              />
            ))}
          </div>
        </div>
      </header>

      {mode === 'readonly' && (
        <p className={styles.readonlyNotice}>
          <Eye className={styles.icon} aria-hidden="true" />
          {CALENDAR_TEXT.readonly}
        </p>
      )}

      {status === 'loading' && <CalendarSkeleton />}
      {status === 'error' && <CalendarError onRetry={onRetry} />}
      {status === 'ready' && (
        <CalendarPeriod
          view={view}
          anchor={anchor}
          title={title}
          source={source}
          onActivateSlot={activateSlot}
          onOpenDay={(date) => navigate('day', date)}
        />
      )}
    </div>
  )
}

interface CalendarPeriodProps {
  view: CalendarView
  anchor: IsoDate
  title: string
  source: CalendarSource
  onActivateSlot: (slot: SlotModel) => void
  onOpenDay: (date: IsoDate) => void
}

function CalendarPeriod({ view, anchor, title, source, onActivateSlot, onOpenDay }: CalendarPeriodProps) {
  const readonly = source.mode === 'readonly'
  switch (view) {
    case 'day':
      return <DayView day={buildDay(source, anchor)} title={title} readonly={readonly} onActivateSlot={onActivateSlot} />
    case 'week':
      return (
        <WeekView week={buildWeek(source, anchor)} title={title} readonly={readonly} onActivateSlot={onActivateSlot} />
      )
    case 'month':
      return <MonthView month={buildMonth(source, anchor)} title={title} onOpenDay={onOpenDay} />
  }
}

function CalendarSkeleton() {
  return (
    <div className={styles.skeleton} aria-busy="true">
      <span className={a11y.visuallyHidden}>{CALENDAR_TEXT.loading}</span>
      {Array.from({ length: SKELETON_ROWS }, (_, row) => (
        <span key={row} className={styles.skeletonRow} aria-hidden="true" />
      ))}
    </div>
  )
}

function CalendarError({ onRetry }: { onRetry?: () => void }) {
  return (
    <div className={styles.error} role="alert">
      <p className={styles.message}>{CALENDAR_TEXT.loadError}</p>
      <Button variant="secondary" label={CALENDAR_TEXT.retry} onClick={onRetry} />
    </div>
  )
}
