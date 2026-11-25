import { Button, Dialog, formatLongDate, formatSlotLabel } from '../../shared/ui'
import type { StaffBooking } from '../api/staffBookingsApi'
import type { Court } from '../model/court'
import { entryStateOf } from '../model/staffSchedule'
import { STAFF_TEXT } from '../model/staffText'
import { toLocal } from '../model/zonedTime'
import { EntryStateTag } from './EntryStateTag'
import styles from './StaffCalendar.module.css'

interface BookingDetailDialogProps {
  booking: StaffBooking
  courts: Court[]
  timeZone: string
  onCancelBooking: (booking: StaffBooking) => void
  onClose: () => void
}

/** Read-only detail of a reservation, with the way to cancel it. */
export function BookingDetailDialog({ booking, courts, timeZone, onCancelBooking, onClose }: BookingDetailDialogProps) {
  const start = toLocal(booking.start, timeZone)
  const end = toLocal(booking.end, timeZone)
  const courtName = courts.find(({ id }) => id === booking.courtId)?.name ?? booking.courtId

  return (
    <Dialog
      title={STAFF_TEXT.bookingDetailTitle}
      onClose={onClose}
      actions={
        <>
          <Button variant="secondary" label={STAFF_TEXT.close} onClick={onClose} />
          <Button variant="danger" label={STAFF_TEXT.cancelBooking} onClick={() => onCancelBooking(booking)} />
        </>
      }
    >
      <dl className={styles.details}>
        <dt>{STAFF_TEXT.status}</dt>
        <dd>
          <EntryStateTag state={entryStateOf(booking)} />
        </dd>
        <dt>{STAFF_TEXT.court}</dt>
        <dd>{courtName}</dd>
        <dt>{STAFF_TEXT.day}</dt>
        <dd>{formatLongDate(start.date)}</dd>
        <dt>{STAFF_TEXT.hour}</dt>
        <dd>{formatSlotLabel(start.time, end.time)}</dd>
        <dt>{STAFF_TEXT.holder}</dt>
        <dd>{booking.holderName ?? STAFF_TEXT.holderHidden}</dd>
      </dl>
    </Dialog>
  )
}
