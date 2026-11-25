import a11y from '../a11y.module.css'
import type { MonthDayModel, MonthModel } from './calendarModel'
import type { IsoDate } from './calendarTypes'
import { CALENDAR_TEXT, formatLongDate } from './calendarText'
import { TodayMark } from './SlotContent'
import type { GridPosition } from './useRovingGrid'
import { useRovingGrid } from './useRovingGrid'
import styles from './BookingCalendar.module.css'

interface MonthViewProps {
  month: MonthModel
  title: string
  onOpenDay: (date: IsoDate) => void
}

function dayAccessibleLabel(day: MonthDayModel): string {
  const parts = [formatLongDate(day.date), day.summary]
  return (day.isToday ? [...parts, CALENDAR_TEXT.today] : parts).join(', ')
}

function todayCell(month: MonthModel): GridPosition | null {
  for (const [row, days] of month.weeks.entries()) {
    const column = days.findIndex((day) => day?.isToday)
    if (column >= 0) return { row, column }
  }
  return null
}

/** Monday-to-Sunday weeks of the month with the summary of each day; opening a day shows its day view. */
export function MonthView({ month, title, onOpenDay }: MonthViewProps) {
  const mask = month.weeks.map((days) => days.map((day) => day !== null))
  const cellProps = useRovingGrid(mask, todayCell(month))

  return (
    <div className={styles.scroll}>
      <table role="grid" aria-label={title} className={`${styles.table} ${styles.monthTable}`}>
        <thead>
          <tr>
            {month.weekdayHeaders.map((header) => (
              <th key={header} scope="col" className={styles.columnHeader}>
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {month.weeks.map((days, row) => (
            <tr key={days.find(Boolean)?.date}>
              {days.map((day, column) =>
                day ? (
                  <td
                    role="gridcell"
                    key={day.date}
                    className={`${styles.monthDay} ${a11y.focusRing}`}
                    aria-label={dayAccessibleLabel(day)}
                    aria-current={day.isToday ? 'date' : undefined}
                    {...cellProps({ row, column }, () => onOpenDay(day.date))}
                    onClick={() => onOpenDay(day.date)}
                  >
                    <span className={styles.dayNumber}>{day.dayOfMonth}</span>
                    {day.isToday && <TodayMark />}
                    <span className={styles.summary}>{day.summary}</span>
                  </td>
                ) : (
                  <td key={`${row}:${column}`} role="gridcell" className={styles.emptyCell} />
                ),
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
