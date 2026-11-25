import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import { Alert, Button, Dialog, Field } from '../../shared/ui'
import { BookingApiError, bookingErrorCodeOf } from '../api/staffBookingsApi'
import type { StaffBooking, StaffBookingsApi } from '../api/staffBookingsApi'
import type { Court } from '../model/court'
import { errorText, STAFF_TEXT, submitErrorTexts } from '../model/staffText'
import type { SubmitErrorTexts } from '../model/staffText'
import { toInstant } from '../model/zonedTime'
import { BookingDetailDialog } from './BookingDetailDialog'
import { CancelBookingDialog } from './CancelBookingDialog'
import { validateSlot } from '../model/slotDraft'
import type { SlotDraft, SlotErrors } from '../model/slotDraft'
import { SlotFields } from './SlotFields'
import styles from './StaffCalendar.module.css'

const REASON_MAX_LENGTH = 200

interface BlockSlotDialogProps {
  api: Pick<StaffBookingsApi, 'blockSlot' | 'getBooking' | 'cancelBooking'>
  courts: Court[]
  timeZone: string
  initialSlot: SlotDraft
  onSaved: (block: StaffBooking) => void
  /** A booking was cancelled from here: the calendar behind must refresh. */
  onBookingCancelled: () => void
  onClose: () => void
}

/** The booking that made the block fail (criterion 9); `booking` is null while it loads or if it could not be read. */
interface BlockConflict {
  booking: StaffBooking | null
  loadError: string | null
}

type NestedDialog = 'detail' | 'confirm' | null

/** Blocks a free slot. Over a booking it is rejected and offers to see or cancel that booking first. */
export function BlockSlotDialog({ api, courts, timeZone, initialSlot, onSaved, onBookingCancelled, onClose }: BlockSlotDialogProps) {
  const [slot, setSlot] = useState(initialSlot)
  const [reason, setReason] = useState('')
  const [errors, setErrors] = useState<SlotErrors & SubmitErrorTexts>({})
  const [saving, setSaving] = useState(false)
  const [conflict, setConflict] = useState<BlockConflict | null>(null)
  const [nested, setNested] = useState<NestedDialog>(null)
  const [bookingCancelled, setBookingCancelled] = useState(false)
  const alertId = useId()

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validation = validateSlot(slot)
    setErrors(validation)
    setConflict(null)
    setBookingCancelled(false)
    if (Object.values(validation).some(Boolean)) return

    setSaving(true)
    try {
      onSaved(
        await api.blockSlot({
          courtId: slot.courtId,
          start: toInstant({ date: slot.date, time: slot.start }, timeZone),
          reason: reason.trim() || undefined,
        }),
      )
    } catch (error) {
      setSaving(false)
      if (error instanceof BookingApiError && error.code === 'SLOT_HAS_BOOKING' && error.conflictingBookingId) {
        await loadConflict(error.conflictingBookingId)
      } else {
        setErrors(submitErrorTexts(bookingErrorCodeOf(error)))
      }
    }
  }

  async function loadConflict(bookingId: string) {
    setConflict({ booking: null, loadError: null })
    try {
      setConflict({ booking: await api.getBooking(bookingId), loadError: null })
    } catch (error) {
      setConflict({ booking: null, loadError: errorText(bookingErrorCodeOf(error)) })
    }
  }

  function handleCancelled() {
    setNested(null)
    setConflict(null)
    setBookingCancelled(true)
    onBookingCancelled()
  }

  const describedBy = conflict || bookingCancelled || errors.form ? alertId : undefined

  return (
    <Dialog title={STAFF_TEXT.blockTitle} onClose={onClose}>
      <form className={styles.form} noValidate aria-describedby={describedBy} onSubmit={submit}>
        {conflict && (
          <Alert
            id={alertId}
            variant="error"
            title={STAFF_TEXT.blockRejectedTitle}
            actions={
              <>
                <Button variant="text" label={STAFF_TEXT.viewBooking} disabled={!conflict.booking} onClick={() => setNested('detail')} />
                <Button variant="danger" label={STAFF_TEXT.cancelBooking} disabled={!conflict.booking} onClick={() => setNested('confirm')} />
              </>
            }
          >
            <p>{STAFF_TEXT.blockRejected}</p>
            {conflict.loadError && <p>{conflict.loadError}</p>}
          </Alert>
        )}
        {bookingCancelled && (
          <Alert id={alertId} variant="success" title={STAFF_TEXT.bookingCancelledTitle}>
            {STAFF_TEXT.bookingCancelled}
          </Alert>
        )}
        {errors.form && (
          <Alert id={alertId} variant="error" title={STAFF_TEXT.saveErrorTitle}>
            {errors.form}
          </Alert>
        )}
        <SlotFields courts={courts} draft={slot} onChange={setSlot} errors={errors} disabled={saving} />
        <Field
          kind="text"
          label={STAFF_TEXT.blockReason}
          help={STAFF_TEXT.optional}
          placeholder={STAFF_TEXT.blockReasonPlaceholder}
          value={reason}
          maxLength={REASON_MAX_LENGTH}
          disabled={saving}
          onChange={(event) => setReason(event.target.value)}
        />
        <div className={styles.formActions}>
          <Button variant="secondary" label={STAFF_TEXT.cancel} disabled={saving} onClick={onClose} />
          <Button type="submit" label={STAFF_TEXT.saveBlock} loading={saving} loadingLabel={STAFF_TEXT.saving} />
        </div>
      </form>

      {nested === 'detail' && conflict?.booking && (
        <BookingDetailDialog
          booking={conflict.booking}
          courts={courts}
          timeZone={timeZone}
          onCancelBooking={() => setNested('confirm')}
          onClose={() => setNested(null)}
        />
      )}
      {nested === 'confirm' && conflict?.booking && (
        <CancelBookingDialog api={api} booking={conflict.booking} onCancelled={handleCancelled} onClose={() => setNested(null)} />
      )}
    </Dialog>
  )
}
