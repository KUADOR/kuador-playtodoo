import { Field, formatSlotLabel } from '../../shared/ui'
import type { FieldOption } from '../../shared/ui'
import { fromMinutes, slotStartsOf, toMinutes } from '../model/court'
import type { Court } from '../model/court'
import type { SlotDraft, SlotErrors } from '../model/slotDraft'
import { STAFF_TEXT } from '../model/staffText'

interface SlotFieldsProps {
  courts: Court[]
  draft: SlotDraft
  onChange: (draft: SlotDraft) => void
  errors: SlotErrors
  disabled: boolean
}

function hourOptions(court: Court | undefined): FieldOption[] {
  if (!court) return []
  return slotStartsOf(court).map((start) => ({
    value: start,
    label: formatSlotLabel(start, fromMinutes(toMinutes(start) + court.slotDurationMinutes)),
  }))
}

/** Court, day and hour: the hours follow the slot duration of the chosen court. */
export function SlotFields({ courts, draft, onChange, errors, disabled }: SlotFieldsProps) {
  const court = courts.find(({ id }) => id === draft.courtId)
  const hours = hourOptions(court)

  function changeCourt(courtId: string) {
    const nextCourt = courts.find(({ id }) => id === courtId)
    const keepsHour = hourOptions(nextCourt).some(({ value }) => value === draft.start)
    onChange({ ...draft, courtId, start: keepsHour ? draft.start : '' })
  }

  return (
    <>
      <Field
        kind="select"
        label={STAFF_TEXT.court}
        options={courts.map(({ id, name }) => ({ value: id, label: name }))}
        value={draft.courtId}
        disabled={disabled}
        onChange={(event) => changeCourt(event.target.value)}
      />
      <Field
        kind="text"
        type="date"
        label={STAFF_TEXT.day}
        value={draft.date}
        required
        error={errors.date}
        disabled={disabled}
        onChange={(event) => onChange({ ...draft, date: event.target.value })}
      />
      <Field
        kind="select"
        label={STAFF_TEXT.hour}
        options={hours}
        placeholder=""
        value={draft.start}
        required
        error={errors.hour}
        disabled={disabled}
        onChange={(event) => onChange({ ...draft, start: event.target.value })}
      />
    </>
  )
}
