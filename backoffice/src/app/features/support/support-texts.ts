import { ErrorMessages } from '../../core/api/error-messages';
import { StatusTagLook } from '../../shared/ui/status-tag/status-tag';
import { AuditEntry } from './support-access-api';

/** Texts of P7 to P9 («Pantallas», PLATODO-27, with the adjustments of «Diseño de la solución» v2). */

export const REASON_MAX_LENGTH = 500;

export const ACCESS_TEXTS = {
  reasonRequired: 'Escribe el motivo para continuar.',
  reasonHelp: 'No escribas datos personales (nombres, correos o teléfonos).',
  notice:
    'Podrás ver y editar datos de este complejo. El motivo y cada operación quedan en la auditoría.',
  denied: 'No tienes permiso para acceder a los datos de este complejo.',
  failed: 'No pudimos abrir el acceso. No se mostró ningún dato.',
} as const;

export const ACCESS_ERROR_MESSAGES: ErrorMessages = {
  FORBIDDEN: ACCESS_TEXTS.denied,
  CAPABILITY_REVOKED: ACCESS_TEXTS.denied,
  TENANT_NOT_ACCESSIBLE: ACCESS_TEXTS.denied,
  REASON_REQUIRED: ACCESS_TEXTS.reasonRequired,
};

export const SUPPORT_TEXTS = {
  expiring: 'El acceso está por vencer',
  expired: 'El acceso venció. Para continuar, registra un nuevo motivo.',
  closed: 'Cerraste el acceso a los datos del complejo.',
  suspended: 'Complejo suspendido. Los cambios que hagas quedan inactivos hasta que se reactive.',
  emptyDay: 'No hay reservas ni bloqueos este día.',
  noCourts:
    'Todavía no podemos mostrar las canchas de este complejo, así que desde aquí no se pueden crear reservas ni bloquear horarios.',
  slotRequired: 'Elige un horario libre.',
  holderRequired: 'Escribe el nombre del titular.',
  bookingCreated: 'Creamos la reserva.',
  slotBlocked: 'Bloqueamos el horario.',
  bookingCancelled: 'Cancelamos la reserva.',
  blockRemoved: 'Quitamos el bloqueo.',
} as const;

/** Fixed notice of P9 on a cancelled complex, with the date its data is deleted. */
export function cancelledNotice(purgeDue: string | null): string {
  const deadline = purgeDue === null ? '' : ` a partir del ${purgeDue}`;
  return `Suscripción cancelada. Los cambios no se activarán y los datos se borrarán${deadline}.`;
}

/** «quedan {n} min» of the support bar. */
export function minutesLeftText(minutes: number): string {
  return minutes <= 1 ? 'queda menos de 1 min' : `quedan ${minutes} min`;
}

/** Failures of the support operations on the complex host. */
export const SUPPORT_ERROR_MESSAGES: ErrorMessages = {
  SLOT_UNAVAILABLE: 'Ese horario ya fue reservado. Elige otro disponible.',
  SLOT_HAS_BOOKING: 'Ese horario tiene una reserva. Cancélala antes de bloquearlo.',
  INVALID_SLOT: 'Ese horario no es válido para esta cancha.',
  COURT_NOT_FOUND: 'Esa cancha no existe en este complejo.',
  BOOKING_NOT_FOUND: 'Esa reserva ya no existe. Actualizamos la lista.',
  INVALID_REQUEST: 'Revisa los datos e inténtalo de nuevo.',
};

/** Answers that mean the session can no longer be used: P9 drops its data. */
export const SESSION_ENDED_CODES: readonly string[] = [
  'SUPPORT_SESSION_INVALID',
  'TENANT_UNAVAILABLE',
  'TENANT_NOT_ACCESSIBLE',
];

/** «Acción» of P7: what the person did, by the operationId of the contracts. */
const AUDIT_ACTION_LABELS: Readonly<Record<string, string>> = {
  openSupportSession: 'Abrió el acceso',
  closeSupportSession: 'Cerró el acceso',
  listStaffBookings: 'Consultó reservas',
  getStaffBooking: 'Consultó una reserva',
  createStaffBooking: 'Creó una reserva',
  blockSlot: 'Bloqueó un horario',
  cancelBookingByStaff: 'Canceló una reserva o quitó un bloqueo',
  // Name used by the example of `listComplexAuditEntries` for the same operation.
  cancelStaffBooking: 'Canceló una reserva o quitó un bloqueo',
};

export const OTHER_OPERATION = 'Otra operación';

export interface AuditAction {
  readonly label: string;
  /** Technical detail shown only when the operation has no Spanish name. */
  readonly technicalDetail: string | null;
}

export function auditAction(entry: AuditEntry): AuditAction {
  const label = entry.operationId === null ? undefined : AUDIT_ACTION_LABELS[entry.operationId];
  return label === undefined
    ? { label: OTHER_OPERATION, technicalDetail: `${entry.httpMethod} ${entry.pathTemplate}` }
    : { label, technicalDetail: null };
}

export const AUDIT_RESULT_LOOKS: Readonly<Record<AuditEntry['result'], StatusTagLook>> = {
  ALLOWED: { label: 'Permitido', tone: 'success', icon: 'check' },
  DENIED: { label: 'Denegado', tone: 'danger', icon: 'cross' },
};

export const REDACTED_REASON = 'Motivo anonimizado';
