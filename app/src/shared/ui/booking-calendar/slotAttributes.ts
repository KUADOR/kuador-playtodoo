import type { SlotModel } from './calendarModel'

/** ARIA and data attributes shared by the slot cells of the day and week views. */
export function slotAttributes(slot: SlotModel) {
  return {
    'aria-label': slot.accessibleLabel,
    'aria-selected': slot.status === 'selected',
    'aria-disabled': slot.status === 'occupied' || undefined,
    'data-status': slot.status,
    'data-selectable': slot.selectable || undefined,
  }
}
