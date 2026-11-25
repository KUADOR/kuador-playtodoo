import { useId, useState } from 'react'
import type { FormEvent } from 'react'
import { Alert, Button, Dialog, Field } from '../../shared/ui'
import { bookingErrorCodeOf } from '../api/staffBookingsApi'
import type { StaffBooking, StaffBookingsApi } from '../api/staffBookingsApi'
import type { Court } from '../model/court'
import { STAFF_TEXT, submitErrorTexts } from '../model/staffText'
import type { SubmitErrorTexts } from '../model/staffText'
import { toInstant } from '../model/zonedTime'
import { validateSlot } from '../model/slotDraft'
import type { SlotDraft, SlotErrors } from '../model/slotDraft'
import { SlotFields } from './SlotFields'
import styles from './StaffCalendar.module.css'

interface ManualBookingDialogProps {
  api: Pick<StaffBookingsApi, 'createBooking'>
  courts: Court[]
  timeZone: string
  initialSlot: SlotDraft
  onSaved: (booking: StaffBooking) => void
  onClose: () => void
}

interface FormErrors extends SlotErrors, SubmitErrorTexts {
  holderName?: string
}

/** Manual booking taken by the staff (for example, by phone). It is created confirmed. */
export function ManualBookingDialog({ api, courts, timeZone, initialSlot, onSaved, onClose }: ManualBookingDialogProps) {
  const [slot, setSlot] = useState(initialSlot)
  const [holderName, setHolderName] = useState('')
  const [holderPhone, setHolderPhone] = useState('')
  const [errors, setErrors] = useState<FormErrors>({})
  const [saving, setSaving] = useState(false)
  const formAlertId = useId()

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const validation: FormErrors = {
      ...validateSlot(slot),
      holderName: holderName.trim() ? undefined : STAFF_TEXT.requiredName,
    }
    setErrors(validation)
    if (Object.values(validation).some(Boolean)) return

    setSaving(true)
    try {
      const booking = await api.createBooking({
        courtId: slot.courtId,
        start: toInstant({ date: slot.date, time: slot.start }, timeZone),
        holderName: holderName.trim(),
        holderPhone: holderPhone.trim() || undefined,
      })
      onSaved(booking)
    } catch (error) {
      setErrors(submitErrorTexts(bookingErrorCodeOf(error)))
      setSaving(false)
    }
  }

  return (
    <Dialog title={STAFF_TEXT.manualBookingTitle} onClose={onClose}>
      <form className={styles.form} noValidate aria-describedby={errors.form ? formAlertId : undefined} onSubmit={submit}>
        {errors.form && <Alert id={formAlertId} variant="error" title={STAFF_TEXT.saveErrorTitle}>{errors.form}</Alert>}
        <SlotFields courts={courts} draft={slot} onChange={setSlot} errors={errors} disabled={saving} />
        <Field
          kind="text"
          label={STAFF_TEXT.holderName}
          value={holderName}
          autoComplete="off"
          maxLength={120}
          required
          error={errors.holderName}
          disabled={saving}
          onChange={(event) => setHolderName(event.target.value)}
        />
        <Field
          kind="text"
          type="tel"
          label={STAFF_TEXT.holderPhone}
          help={STAFF_TEXT.optional}
          value={holderPhone}
          autoComplete="off"
          maxLength={32}
          disabled={saving}
          onChange={(event) => setHolderPhone(event.target.value)}
        />
        <div className={styles.formActions}>
          <Button variant="secondary" label={STAFF_TEXT.cancel} disabled={saving} onClick={onClose} />
          <Button type="submit" label={STAFF_TEXT.saveBooking} loading={saving} loadingLabel={STAFF_TEXT.saving} />
        </div>
      </form>
    </Dialog>
  )
}
