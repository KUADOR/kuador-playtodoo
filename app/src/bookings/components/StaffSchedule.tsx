import { useId } from 'react'
import a11y from '../../shared/ui/a11y.module.css'
import { Button, formatSlotLabel } from '../../shared/ui'
import type { StaffBooking } from '../api/staffBookingsApi'
import { entryStateOf } from '../model/staffSchedule'
import type { FreeRow, ScheduleRow } from '../model/staffSchedule'
import { STAFF_TEXT } from '../model/staffText'
import { EntryStateTag } from './EntryStateTag'
import styles from './StaffCalendar.module.css'

const SKELETON_ROWS = 4

export type ScheduleStatus = 'loading' | 'error' | 'ready'

export interface StaffScheduleActions {
  onNewBooking: (slot: FreeRow) => void
  onBlockSlot: (slot: FreeRow) => void
  onViewBooking: (booking: StaffBooking) => void
  onCancelBooking: (booking: StaffBooking) => void
}

interface StaffScheduleProps extends StaffScheduleActions {
  status: ScheduleStatus
  rows: ScheduleRow[]
  onRetry: () => void
}

/** The rows of the day with their state and the actions each state allows. */
export function StaffSchedule({ status, rows, onRetry, ...actions }: StaffScheduleProps) {
  if (status === 'loading') return <ScheduleSkeleton />
  if (status === 'error') {
    return (
      <div className={styles.loadError} role="alert">
        <p className={styles.message}>{STAFF_TEXT.loadError}</p>
        <Button variant="secondary" label={STAFF_TEXT.retry} onClick={onRetry} />
      </div>
    )
  }

  const hasEntries = rows.some(({ type }) => type === 'entry')
  return (
    <>
      {!hasEntries && <p className={styles.message}>{STAFF_TEXT.emptyDay}</p>}
      <ul className={styles.rows} aria-label={STAFF_TEXT.scheduleLabel}>
        {rows.map((row) => (
          <ScheduleRowItem key={row.type === 'entry' ? row.booking.id : row.start} row={row} {...actions} />
        ))}
      </ul>
    </>
  )
}

function ScheduleRowItem({ row, ...actions }: { row: ScheduleRow } & StaffScheduleActions) {
  const timeId = useId()
  const state = row.type === 'free' ? 'free' : entryStateOf(row.booking)
  const detail = row.type === 'entry' ? (row.booking.holderName ?? row.booking.blockReason) : null

  return (
    <li className={styles.row} data-state={state}>
      <p id={timeId} className={styles.rowInfo}>
        <span className={styles.rowTime}>{formatSlotLabel(row.start, row.end)}</span>
        <EntryStateTag state={state} />
        {detail && <span className={styles.rowDetail}>{detail}</span>}
      </p>
      <div className={styles.rowActions}>
        <RowActions row={row} describedBy={timeId} {...actions} />
      </div>
    </li>
  )
}

interface RowActionsProps extends StaffScheduleActions {
  row: ScheduleRow
  /** Each button reads its row time, so «Nueva reserva» is not ambiguous for screen readers. */
  describedBy: string
}

function RowActions({ row, describedBy, onNewBooking, onBlockSlot, onViewBooking, onCancelBooking }: RowActionsProps) {
  if (row.type === 'free') {
    return (
      <>
        <Button variant="secondary" label={STAFF_TEXT.newBooking} aria-describedby={describedBy} onClick={() => onNewBooking(row)} />
        <Button variant="text" label={STAFF_TEXT.blockSlot} aria-describedby={describedBy} onClick={() => onBlockSlot(row)} />
      </>
    )
  }
  const isBlock = row.booking.kind === 'BLOCK'
  return (
    <>
      {!isBlock && (
        <Button variant="secondary" label={STAFF_TEXT.viewBooking} aria-describedby={describedBy} onClick={() => onViewBooking(row.booking)} />
      )}
      <Button
        variant="text"
        label={isBlock ? STAFF_TEXT.removeBlock : STAFF_TEXT.cancelBooking}
        aria-describedby={describedBy}
        onClick={() => onCancelBooking(row.booking)}
      />
    </>
  )
}

function ScheduleSkeleton() {
  return (
    <div className={styles.skeleton} aria-busy="true">
      <span className={a11y.visuallyHidden}>{STAFF_TEXT.loading}</span>
      {Array.from({ length: SKELETON_ROWS }, (_, row) => (
        <span key={row} className={styles.skeletonRow} aria-hidden="true" />
      ))}
    </div>
  )
}
