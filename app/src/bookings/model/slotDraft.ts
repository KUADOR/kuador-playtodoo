import type { IsoDate, LocalTime } from '../../shared/ui'
import { STAFF_TEXT } from './staffText'

/** The slot a staff form acts on, in the local time of the complex. */
export interface SlotDraft {
  courtId: string
  date: IsoDate
  start: LocalTime
}

export interface SlotErrors {
  date?: string
  hour?: string
}

/** Errors of the values the slot still needs; no message when it is complete. */
export function validateSlot(draft: SlotDraft): SlotErrors {
  return {
    date: draft.date ? undefined : STAFF_TEXT.requiredDay,
    hour: draft.start ? undefined : STAFF_TEXT.requiredHour,
  }
}
