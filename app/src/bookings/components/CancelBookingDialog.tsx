import { useState } from 'react'
import { Alert, Button, Dialog } from '../../shared/ui'
import { bookingErrorCodeOf } from '../api/staffBookingsApi'
import type { StaffBooking, StaffBookingsApi } from '../api/staffBookingsApi'
import { confirmCancelQuestion, errorText, STAFF_TEXT } from '../model/staffText'

interface CancelBookingDialogProps {
  api: Pick<StaffBookingsApi, 'cancelBooking'>
  booking: StaffBooking
  onCancelled: (booking: StaffBooking) => void
  onClose: () => void
}

/** Asks before cancelling a booking or removing a block; nothing changes until it is confirmed. */
export function CancelBookingDialog({ api, booking, onCancelled, onClose }: CancelBookingDialogProps) {
  const [cancelling, setCancelling] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const isBlock = booking.kind === 'BLOCK'

  async function confirm() {
    setCancelling(true)
    setError(null)
    try {
      onCancelled(await api.cancelBooking(booking.id))
    } catch (failure) {
      setError(errorText(bookingErrorCodeOf(failure)))
      setCancelling(false)
    }
  }

  return (
    <Dialog
      title={isBlock ? STAFF_TEXT.confirmRemoveBlock : confirmCancelQuestion(booking.holderName)}
      description={STAFF_TEXT.confirmCancelHelp}
      onClose={onClose}
      actions={
        <>
          <Button variant="secondary" label={STAFF_TEXT.keepBooking} disabled={cancelling} onClick={onClose} />
          <Button
            variant="danger"
            label={isBlock ? STAFF_TEXT.removeBlock : STAFF_TEXT.cancelBooking}
            loading={cancelling}
            loadingLabel={STAFF_TEXT.cancelling}
            onClick={confirm}
          />
        </>
      }
    >
      {error && (
        <Alert variant="error" title={STAFF_TEXT.cancelErrorTitle}>
          {error}
        </Alert>
      )}
    </Dialog>
  )
}
