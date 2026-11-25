import type { BookingErrorCode } from '../api/staffBookingsApi'
import type { EntryState } from './staffSchedule'

// Every text of the staff calendar (Pantallas · Pantalla 6). Neutral, professional tone for staff.

export const STAFF_TEXT = {
  pageTitle: 'Calendario del personal',
  court: 'Cancha',
  day: 'Día',
  hour: 'Hora',
  previousDay: 'Día anterior',
  nextDay: 'Día siguiente',
  scheduleLabel: 'Franjas del día',
  loading: 'Cargando reservas…',
  loadError: 'No se pudieron cargar las reservas.',
  retry: 'Reintentar',
  emptyDay: 'No hay reservas ni bloqueos este día.',
  newBooking: 'Nueva reserva',
  blockSlot: 'Bloquear franja',
  viewBooking: 'Ver reserva',
  cancelBooking: 'Cancelar reserva',
  removeBlock: 'Quitar bloqueo',
  close: 'Cerrar',
  cancel: 'Cancelar',
  saving: 'Guardando…',
  cancelling: 'Cancelando…',
  manualBookingTitle: 'Nueva reserva manual',
  holderName: 'Nombre de quien reserva',
  holderPhone: 'Teléfono',
  optional: 'Opcional',
  saveBooking: 'Guardar reserva',
  blockTitle: 'Bloquear franja',
  blockReason: 'Motivo',
  blockReasonPlaceholder: 'Mantenimiento',
  saveBlock: 'Guardar bloqueo',
  blockRejectedTitle: 'No se puede bloquear esta franja',
  blockRejected:
    'No se puede bloquear esta franja porque ya tiene una reserva. Cancela primero la reserva y luego bloquea el horario.',
  bookingCancelledTitle: 'Reserva cancelada',
  bookingCancelled: 'La franja quedó libre. Ya puedes guardar el bloqueo.',
  bookingDetailTitle: 'Detalle de la reserva',
  holder: 'Reservada por',
  holderHidden: 'Se muestra al confirmarse',
  status: 'Estado',
  confirmCancelGeneric: '¿Cancelar esta reserva?',
  confirmRemoveBlock: '¿Quitar este bloqueo?',
  confirmCancelHelp: 'La franja quedará libre para otras reservas.',
  keepBooking: 'Volver',
  requiredName: 'Escribe el nombre de quien reserva.',
  requiredHour: 'Elige una hora.',
  requiredDay: 'Elige un día.',
  bookingSaved: 'Reserva guardada.',
  blockSaved: 'Bloqueo guardado. La franja ya no está disponible para jugadores.',
  entryCancelled: 'Cancelación guardada. La franja quedó libre.',
  saveErrorTitle: 'No se pudo guardar',
  cancelErrorTitle: 'No se pudo cancelar',
} as const

export const ENTRY_STATE_TEXT: Record<EntryState, string> = {
  free: 'Libre',
  pending: 'Pendiente',
  confirmed: 'Confirmada',
  block: 'Bloqueo',
}

/** `¿Cancelar la reserva de Alex?`, or the generic question when the holder is not shown. */
export function confirmCancelQuestion(holderName: string | null): string {
  return holderName ? `¿Cancelar la reserva de ${holderName}?` : STAFF_TEXT.confirmCancelGeneric
}

/** `18:00 a 19:00, Confirmada, Alex`: the accessible name of a row of the day. */
export function rowAccessibleLabel(start: string, end: string, state: EntryState, detail: string | null): string {
  return [`${start} a ${end}`, ENTRY_STATE_TEXT[state], detail].filter(Boolean).join(', ')
}

const SLOT_TAKEN = 'Ese horario ya no está disponible. Elige otro.'
const INVALID_SLOT = 'Esa hora no es una franja válida de la cancha o ya pasó. Elige otra.'
const NETWORK = 'Revisa tu conexión e inténtalo de nuevo.'
const GENERIC = 'Ocurrió un problema. Inténtalo de nuevo en unos minutos.'
// A resource of another complex answers 404 like a missing one (contracts/tenancy.openapi.yaml), so the text
// never says whether it exists elsewhere (Pantallas PLATODO-26 · cambios en pantallas existentes).
const NOT_FOUND = 'No encontramos lo que buscas.'

const ERROR_TEXT: Partial<Record<BookingErrorCode, string>> = {
  SLOT_UNAVAILABLE: SLOT_TAKEN,
  INVALID_SLOT,
  COURT_NOT_FOUND: `${NOT_FOUND} Recarga la página y elige otra cancha.`,
  BOOKING_NOT_FOUND: `${NOT_FOUND} Recarga el calendario.`,
  UNAUTHENTICATED: 'Tu sesión terminó. Vuelve a iniciar sesión.',
  FORBIDDEN: 'No tienes permiso para gestionar reservas de este complejo.',
  NETWORK_ERROR: NETWORK,
}

/** What happened and what to do, for any error code. */
export function errorText(code: BookingErrorCode): string {
  return ERROR_TEXT[code] ?? GENERIC
}

/** Codes whose message belongs to the hour field instead of the form. */
const HOUR_FIELD_ERRORS: ReadonlySet<BookingErrorCode> = new Set(['SLOT_UNAVAILABLE', 'INVALID_SLOT'])

export interface SubmitErrorTexts {
  /** Shown on the hour field. */
  hour?: string
  /** Shown in an alert over the form. */
  form?: string
}

/** Where the message of a failed save goes: slot conflicts on the hour field, the rest over the form. */
export function submitErrorTexts(code: BookingErrorCode): SubmitErrorTexts {
  return HOUR_FIELD_ERRORS.has(code) ? { hour: errorText(code) } : { form: errorText(code) }
}
