import { StatusBadgeSpec } from '../../shared/ui/status-badge/status-badge';
import { PlanField, PlanFieldType } from './plan-fields-api';
import { Plan, PlanStatus } from './plans-api';

/** Shown where a plan has no value yet (drafts). */
export const NO_VALUE = '—';

export const PLAN_STATUS_BADGES: Readonly<Record<PlanStatus, StatusBadgeSpec>> = {
  DRAFT: { label: 'Borrador', tone: 'neutral', icon: 'pencil' },
  PUBLISHED: { label: 'Publicado', tone: 'success', icon: 'check' },
};

export const RETIRED_FIELD_BADGE: StatusBadgeSpec = {
  label: 'Retirado',
  tone: 'neutral',
  icon: 'dash',
};

/**
 * Spanish names of the periodicities the api may allow. The values are
 * [POR DEFINIR] (`GET /plan-options`): an unknown one is shown as the api sends it.
 */
const PERIODICITY_LABELS: Readonly<Record<string, string>> = {
  MONTHLY: 'Mensual',
  QUARTERLY: 'Trimestral',
  SEMIANNUAL: 'Semestral',
  YEARLY: 'Anual',
};

export function periodicityLabel(periodicity: string | null): string {
  if (periodicity === null) {
    return NO_VALUE;
  }
  return PERIODICITY_LABELS[periodicity] ?? periodicity;
}

/**
 * Price with its currency («Pantallas»: amounts always with their currency). The amount keeps the
 * decimal string of the api; the number format per country is [POR DEFINIR: launch country].
 */
export function priceLabel(plan: Pick<Plan, 'price' | 'currency'>): string {
  if (plan.price === null) {
    return NO_VALUE;
  }
  return plan.currency === null ? plan.price : `${plan.price} ${plan.currency}`;
}

function countLabel(count: number | null, singular: string, plural: string): string | null {
  if (count === null) {
    return null;
  }
  return `${count} ${count === 1 ? singular : plural}`;
}

/** «10 canchas · 25 usuarios». */
export function limitsLabel(plan: Pick<Plan, 'courtLimit' | 'userLimit'>): string {
  const parts = [
    countLabel(plan.courtLimit, 'cancha', 'canchas'),
    countLabel(plan.userLimit, 'usuario', 'usuarios'),
  ].filter((part) => part !== null);
  return parts.length > 0 ? parts.join(' · ') : NO_VALUE;
}

export const FIELD_TYPE_LABELS: Readonly<Record<PlanFieldType, string>> = {
  TEXT: 'Texto',
  NUMBER: 'Número',
  BOOLEAN: 'Sí/No',
};

/** Basic fields keep their Spanish name here: the api labels are developer-facing. */
export const BASIC_FIELD_LABELS: Readonly<Record<string, string>> = {
  name: 'Nombre',
  price: 'Precio',
  currency: 'Moneda',
  periodicity: 'Periodicidad',
  courtLimit: 'Límite de canchas',
  userLimit: 'Límite de usuarios',
};

export function planFieldLabel(field: Pick<PlanField, 'key' | 'label' | 'origin'>): string {
  return field.origin === 'BASIC' ? (BASIC_FIELD_LABELS[field.key] ?? field.label) : field.label;
}
