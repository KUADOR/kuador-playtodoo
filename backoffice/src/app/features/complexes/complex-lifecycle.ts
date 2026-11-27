import { AlertTone } from '../../shared/ui/alert/alert';
import { canAccess, requiresCapability, SUPER_ADMIN_ONLY } from '../../core/auth/access';
import { CurrentPlatformUser } from '../../core/auth/platform-user';
import { daysUntil, formatDate, formatDateTime } from '../../core/format/date-format';
import { canOpenSupportSession } from '../support/support-permissions';
import { ComplexDetail } from './complexes-api';

/** With this many days left or fewer, the purge notice turns from a warning into an error. */
export const PURGE_SOON_DAYS = 7;

/** Why a fixed notice of P4 is shown; the screen adds the actions that go with it. */
export type LifecycleNoticeKind =
  | 'provisioning'
  | 'provisioning-failed'
  | 'suspended'
  | 'cancelled'
  | 'purge-scheduled'
  | 'purging'
  | 'purged';

export interface LifecycleNotice {
  readonly kind: LifecycleNoticeKind;
  readonly tone: AlertTone;
  readonly text: string;
}

type Lifecycle = Pick<
  ComplexDetail,
  'tenantStatus' | 'suspendedAt' | 'archivedAt' | 'purgeDueAt' | 'purgedAt'
>;

/** The retention ran out: the purge job deletes the data and the complex cannot be reactivated. */
export function isRetentionExpired(complex: Lifecycle, now: Date = new Date()): boolean {
  return complex.purgeDueAt !== null && new Date(complex.purgeDueAt).getTime() <= now.getTime();
}

/** In retention (suspended or cancelled) with a purge date still in the future. */
export function isInRetention(complex: Lifecycle, now: Date = new Date()): boolean {
  const retained = complex.tenantStatus === 'SUSPENDED' || complex.tenantStatus === 'ARCHIVED';
  return retained && complex.purgeDueAt !== null && !isRetentionExpired(complex, now);
}

/** Its data is being deleted or was deleted: nothing can be managed any more. */
export function isBeyondManagement(complex: Lifecycle, now: Date = new Date()): boolean {
  return (
    complex.tenantStatus === 'PURGING' ||
    complex.tenantStatus === 'PURGED' ||
    isRetentionExpired(complex, now)
  );
}

function daysLeftText(days: number): string {
  return days === 1 ? 'falta 1 día' : `faltan ${days} días`;
}

function purgeNotice(complex: Lifecycle, now: Date): LifecycleNotice | null {
  if (!isInRetention(complex, now) || complex.purgeDueAt === null) {
    return null;
  }
  const days = daysUntil(complex.purgeDueAt, now);
  const deadline = `a partir del ${formatDateTime(complex.purgeDueAt)} (${daysLeftText(days)})`;
  const text =
    complex.tenantStatus === 'SUSPENDED'
      ? `Si no se reactiva, todos sus datos se borrarán ${deadline}. No se puede deshacer.`
      : `Todos sus datos se borrarán ${deadline}. No se puede deshacer.`;
  return { kind: 'purge-scheduled', tone: days <= PURGE_SOON_DAYS ? 'error' : 'warning', text };
}

function statusNotice(complex: Lifecycle): LifecycleNotice | null {
  switch (complex.tenantStatus) {
    case 'PROVISIONING':
      return { kind: 'provisioning', tone: 'info', text: 'Creando el espacio del complejo…' };
    case 'PROVISIONING_FAILED':
      return {
        kind: 'provisioning-failed',
        tone: 'warning',
        text: 'No se terminó de crear el complejo.',
      };
    case 'SUSPENDED':
      return {
        kind: 'suspended',
        tone: 'warning',
        text: `Complejo suspendido desde el ${formatDate(complex.suspendedAt ?? '')}. Ni su personal ni los jugadores pueden acceder. Las reservas futuras se conservan inactivas.`,
      };
    case 'ARCHIVED':
      return {
        kind: 'cancelled',
        tone: 'warning',
        text: `Suscripción cancelada el ${formatDate(complex.archivedAt ?? '')}. Ni su personal ni los jugadores pueden acceder y ya no se puede reactivar.`,
      };
    case 'PURGED':
      return {
        kind: 'purged',
        tone: 'info',
        text: `Los datos de este complejo se borraron el ${formatDate(complex.purgedAt ?? '')}.`,
      };
    default:
      return null;
  }
}

const PURGING_NOTICE: LifecycleNotice = {
  kind: 'purging',
  tone: 'error',
  text: 'Borrado de datos en curso. Ya no se puede reactivar.',
};

/** Fixed notices of P4 for the lifecycle of the tenant, most important first. */
export function lifecycleNotices(complex: Lifecycle, now: Date = new Date()): LifecycleNotice[] {
  if (
    complex.tenantStatus === 'PURGING' ||
    (isRetentionExpired(complex, now) && complex.tenantStatus !== 'PURGED')
  ) {
    return [PURGING_NOTICE];
  }
  return [statusNotice(complex), purgeNotice(complex, now)].filter(
    (notice): notice is LifecycleNotice => notice !== null,
  );
}

/** Actions of P4 the signed-in user may take on this complex now. The api still decides. */
export interface ComplexActions {
  readonly recordPayment: boolean;
  readonly changeStatus: boolean;
  readonly changePlan: boolean;
  readonly changeRetention: boolean;
  readonly retryRegistration: boolean;
  /** «Acceder a datos del complejo» (P8). */
  readonly accessData: boolean;
}

const NO_ACTIONS: ComplexActions = {
  recordPayment: false,
  changeStatus: false,
  changePlan: false,
  changeRetention: false,
  retryRegistration: false,
  accessData: false,
};

export function complexActions(
  user: CurrentPlatformUser | null,
  complex: ComplexDetail,
  now: Date = new Date(),
): ComplexActions {
  if (isBeyondManagement(complex, now)) {
    return NO_ACTIONS;
  }
  const isSuperAdmin = canAccess(user, SUPER_ADMIN_ONLY);
  return {
    recordPayment: canAccess(user, requiresCapability('SUBSCRIPTION_PAYMENT_RECORD')),
    changeStatus: canAccess(user, requiresCapability('SUBSCRIPTION_STATUS_CHANGE')),
    changePlan: isSuperAdmin && complex.subscription.status !== 'CANCELLED',
    changeRetention: isSuperAdmin && isInRetention(complex, now),
    retryRegistration:
      complex.tenantStatus === 'PROVISIONING_FAILED' &&
      canAccess(user, requiresCapability('COMPLEX_CREATE')),
    accessData: canOpenSupportSession(user, complex.tenantStatus),
  };
}
