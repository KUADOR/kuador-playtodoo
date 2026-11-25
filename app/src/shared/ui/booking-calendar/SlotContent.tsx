import { Check, Lock } from 'lucide-react'
import type { SlotModel } from './calendarModel'
import { CALENDAR_TEXT, SLOT_STATUS_TEXT } from './calendarText'
import styles from './BookingCalendar.module.css'

/** Visible content of a slot: the state is told with icon and text, never only with color. */
export function SlotContent({ slot }: { slot: SlotModel }) {
  return (
    <span className={styles.slotContent}>
      <span className={styles.slotTime}>
        {slot.status === 'selected' && <Check className={styles.icon} aria-hidden="true" />}
        {slot.label}
      </span>
      {slot.status === 'occupied' && (
        <span className={styles.slotState}>
          <Lock className={styles.icon} aria-hidden="true" />
          {SLOT_STATUS_TEXT.occupied}
        </span>
      )}
    </span>
  )
}

/** Dot plus the text «Hoy». */
export function TodayMark() {
  return (
    <span className={styles.today}>
      <span className={styles.todayDot} aria-hidden="true" />
      {CALENDAR_TEXT.today}
    </span>
  )
}
