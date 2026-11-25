import a11y from '../a11y.module.css'
import type { SlotModel, WeekModel } from './calendarModel'
import { CALENDAR_TEXT } from './calendarText'
import { slotAttributes } from './slotAttributes'
import { SlotContent, TodayMark } from './SlotContent'
import type { GridPosition } from './useRovingGrid'
import { useRovingGrid } from './useRovingGrid'
import styles from './BookingCalendar.module.css'

interface WeekViewProps {
  week: WeekModel
  title: string
  readonly: boolean
  onActivateSlot: (slot: SlotModel) => void
}

function preferredCell(week: WeekModel): GridPosition | null {
  const todayColumn = week.columns.findIndex(({ isToday }) => isToday)
  for (const [row, cells] of week.cells.entries()) {
    const column = cells.findIndex((slot) => slot?.status === 'selected')
    if (column >= 0) return { row, column }
  }
  const todayRow = week.cells.findIndex((cells) => cells[todayColumn] !== null)
  return todayColumn >= 0 && todayRow >= 0 ? { row: todayRow, column: todayColumn } : null
}

/** Monday-to-Sunday columns and one row per start time of the week, as an ARIA grid. */
export function WeekView({ week, title, readonly, onActivateSlot }: WeekViewProps) {
  const mask = week.cells.map((cells) => cells.map((slot) => slot !== null))
  const cellProps = useRovingGrid(mask, preferredCell(week))

  return (
    <div className={styles.scroll}>
      <table role="grid" aria-label={title} aria-readonly={readonly || undefined} className={`${styles.table} ${styles.weekTable}`}>
        <thead>
          <tr>
            <th scope="col" className={styles.timeHeader}>
              <span className={a11y.visuallyHidden}>{CALENDAR_TEXT.time}</span>
            </th>
            {week.columns.map((column) => (
              <th key={column.date} scope="col" className={styles.columnHeader}>
                <span className={styles.columnTitle}>{column.header}</span>
                {column.isToday && <TodayMark />}
                <span className={styles.summary}>{column.summary}</span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {week.rows.map((start, row) => (
            <tr key={start}>
              <th scope="row" className={styles.rowHeader}>
                {start}
              </th>
              {week.cells[row].map((slot, column) =>
                slot ? (
                  <td
                    role="gridcell"
                    key={slot.date}
                    className={`${styles.slot} ${a11y.focusRing}`}
                    {...slotAttributes(slot)}
                    {...cellProps({ row, column }, () => onActivateSlot(slot))}
                    onClick={() => onActivateSlot(slot)}
                  >
                    <SlotContent slot={slot} />
                  </td>
                ) : (
                  <td key={week.columns[column].date} role="gridcell" className={styles.emptyCell} />
                ),
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
