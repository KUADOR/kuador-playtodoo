import { StatusTagLook } from '../../shared/ui/status-tag/status-tag';
import {
  ComplexSummary,
  CustomFieldValues,
  PaymentMethod,
  PlanSnapshot,
  SubscriptionStatus,
} from './complexes-api';
import { PublishedPlan } from './published-plans-api';

/** Texts and labels of P2 to P6 («Pantallas», PLATODO-27) shared by several screens. */

export const SUBSCRIPTION_STATUS_LOOKS: Readonly<Record<SubscriptionStatus, StatusTagLook>> = {
  ACTIVE: { label: 'Activa', tone: 'success', icon: 'check' },
  EXPIRING: { label: 'Por vencer', tone: 'warning', icon: 'clock' },
  SUSPENDED: { label: 'Suspendida', tone: 'danger', icon: 'pause' },
  CANCELLED: { label: 'Cancelada', tone: 'danger', icon: 'cross' },
};

const INCOMPLETE_REGISTRATION_LOOK: StatusTagLook = {
  label: 'Alta incompleta',
  tone: 'warning',
  icon: 'clock',
};
const DATA_DELETED_LOOK: StatusTagLook = {
  label: 'Datos eliminados',
  tone: 'neutral',
  icon: 'trash',
};

/**
 * Tag of a complex: the lifecycle of the tenant wins while it is not provisioned or once its data
 * was deleted; otherwise the commercial status of its subscription.
 */
export function complexStatusLook(
  complex: Pick<ComplexSummary, 'subscriptionStatus' | 'tenantStatus'>,
): StatusTagLook {
  switch (complex.tenantStatus) {
    case 'PROVISIONING':
    case 'PROVISIONING_FAILED':
      return INCOMPLETE_REGISTRATION_LOOK;
    case 'PURGED':
      return DATA_DELETED_LOOK;
    default:
      return SUBSCRIPTION_STATUS_LOOKS[complex.subscriptionStatus];
  }
}

/** Tag of a status stored in the history, which is a `SubscriptionStatus` name. */
export function historyStatusLook(status: string): StatusTagLook {
  return (
    SUBSCRIPTION_STATUS_LOOKS[status as SubscriptionStatus] ?? {
      label: status,
      tone: 'neutral',
      icon: 'dash',
    }
  );
}

export const PAYMENT_METHOD_LABELS: Readonly<Record<PaymentMethod, string>> = {
  TRANSFER: 'Transferencia',
  INVOICE: 'Factura',
};

/**
 * Spanish names of the periodicities the examples of the contract use. The allowed values come
 * from `GET /plan-options` ([POR DEFINIR: values]); an unknown one is shown as the api sends it.
 */
const PERIODICITY_LABELS: Readonly<Record<string, string>> = {
  MONTHLY: 'Mensual',
  QUARTERLY: 'Trimestral',
  YEARLY: 'Anual',
};

export function periodicityLabel(periodicity: string): string {
  return PERIODICITY_LABELS[periodicity] ?? periodicity;
}

/** An amount always goes with its currency. The decimal string of the api is shown as it is. */
export function formatMoney(amount: string, currency: string): string {
  return `${amount} ${currency}`;
}

export const EMPTY_VALUE = '—';

const BOOLEAN_LABELS = { true: 'Sí', false: 'No' } as const;

export function customFieldValueText(value: string | number | boolean): string {
  return typeof value === 'boolean' ? BOOLEAN_LABELS[`${value}`] : String(value);
}

export interface PlanFieldRow {
  readonly label: string;
  readonly value: string;
}

type PlanConditions = Pick<
  PlanSnapshot | PublishedPlan,
  'price' | 'currency' | 'periodicity' | 'courtLimit' | 'userLimit' | 'customFields'
>;

/**
 * Every field of a plan as «label: value» rows. Custom fields are listed by their `key`: the
 * snapshot does not carry their labels and `GET /plan-fields` is for the SUPER_ADMIN only.
 */
export function planFieldRows(plan: PlanConditions): PlanFieldRow[] {
  return [
    { label: 'Precio', value: formatMoney(plan.price, plan.currency) },
    { label: 'Periodicidad', value: periodicityLabel(plan.periodicity) },
    { label: 'Límite de canchas', value: String(plan.courtLimit) },
    { label: 'Límite de usuarios', value: String(plan.userLimit) },
    ...customFieldRows(plan.customFields),
  ];
}

function customFieldRows(fields: CustomFieldValues): PlanFieldRow[] {
  return Object.entries(fields).map(([key, value]) => ({
    label: key,
    value: customFieldValueText(value),
  }));
}

/** One-line summary of a plan: «120.00 USD · Mensual · 10 canchas · 25 usuarios». */
export function planSummary(plan: PlanConditions): string {
  return [
    formatMoney(plan.price, plan.currency),
    periodicityLabel(plan.periodicity),
    `${plan.courtLimit} canchas`,
    `${plan.userLimit} usuarios`,
  ].join(' · ');
}
