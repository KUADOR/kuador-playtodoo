import a11y from '../a11y.module.css'
import type { DayModel, SlotModel } from './calendarModel'
import { slotAttributes } from './slotAttributes'
import { SlotContent, TodayMark } from './SlotContent'
import { useRovingGrid } from './useRovingGrid'
import styles from './BookingCalendar.module.css'

interface DayViewProps {
  day: DayModel
  title: string
  readonly: boolean
  onActivateSlot: (slot: SlotModel) => void
}

/** The slots of one day as a single-select listbox; arrows move between slots. */
export function DayView({ day, title, readonly, onActivateSlot }: DayViewProps) {
  const mask = day.slots.map(() => [true])
  const selectedRow = day.slots.findIndex(({ status }) => status === 'selected')
  const cellProps = useRovingGrid(mask, selectedRow >= 0 ? { row: selectedRow, column: 0 } : null)

  return (
    <div className={styles.day}>
      <p className={styles.daySummary}>
        {day.isToday && <TodayMark />}
        <span>{day.summary}</span>
      </p>
      {day.emptyMessage ? (
        <p className={styles.message}>{day.emptyMessage}</p>
      ) : (
        <div role="listbox" aria-label={title} aria-readonly={readonly || undefined} className={styles.slotList}>
          {day.slots.map((slot, row) => (
            <div
              key={slot.start}
              role="option"
              className={`${styles.slot} ${a11y.focusRing}`}
              {...slotAttributes(slot)}
              {...cellProps({ row, column: 0 }, () => onActivateSlot(slot))}
              onClick={() => onActivateSlot(slot)}
            >
              <SlotContent slot={slot} />
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
