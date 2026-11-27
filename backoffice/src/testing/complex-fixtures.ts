import { Page } from '../app/core/api/page';
import {
  ComplexDetail,
  ComplexSummary,
  HistoryEntry,
  Payment,
} from '../app/features/complexes/complexes-api';
import { PublishedPlan } from '../app/features/complexes/published-plans-api';
import { TEST_API_BASE_URL } from './platform-fixtures';

/** Examples of contracts/platform-backoffice.openapi.yaml used by the tests of P2 to P6. */

export const COMPLEXES_URL = `${TEST_API_BASE_URL}/platform/complexes`;
export const PLANS_URL = `${TEST_API_BASE_URL}/platform/plans`;
export const SUBDOMAINS_URL = `${TEST_API_BASE_URL}/subdomains`;
export const COMPLEX_ID = '5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c';
export const COMPLEX_URL = `${COMPLEXES_URL}/${COMPLEX_ID}`;

const SAM = { id: '4e5f6a7b-8c9d-4e0f-a1b2-c3d4e5f6a7b8', displayName: 'Sam Example' };
const ROBIN = { id: '6a7b8c9d-0e1f-4a2b-8c3d-4e5f6a7b8c9d', displayName: 'Robin Example' };

export const BASIC_PLAN: PublishedPlan = {
  id: '1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d',
  name: 'Basic',
  price: '60.00',
  currency: 'USD',
  periodicity: 'MONTHLY',
  courtLimit: 4,
  userLimit: 8,
  customFields: {},
};

export const PREMIUM_PLAN: PublishedPlan = {
  id: '2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e',
  name: 'Premium',
  price: '120.00',
  currency: 'USD',
  periodicity: 'MONTHLY',
  courtLimit: 10,
  userLimit: 25,
  customFields: { 'support-hours': 'Weekdays 9 to 18' },
};

/** `ActiveComplex` example. */
export const ACTIVE_COMPLEX: ComplexDetail = {
  id: COMPLEX_ID,
  displayName: 'Canchas El Sol',
  subdomain: 'canchas-el-sol',
  ownerEmail: 'owner@example.com',
  createdAt: '2026-12-01T14:00:00Z',
  tenantStatus: 'ACTIVE',
  suspendedAt: null,
  archivedAt: null,
  retentionDays: null,
  purgeDueAt: null,
  purgedAt: null,
  subscription: {
    status: 'ACTIVE',
    statusSince: '2026-12-01T14:00:02Z',
    statusChangedBy: SAM,
    plan: {
      planId: PREMIUM_PLAN.id,
      name: 'Premium',
      price: '120.00',
      currency: 'USD',
      periodicity: 'MONTHLY',
      courtLimit: 10,
      userLimit: 25,
      customFields: { 'support-hours': 'Weekdays 9 to 18' },
      assignedAt: '2026-12-01T14:00:00Z',
    },
  },
  allowedTransitions: ['EXPIRING', 'SUSPENDED', 'CANCELLED'],
  version: 3,
};

/** A suspended complex whose data is deleted in `daysLeft` days (`SuspendedComplex` example). */
export function suspendedComplex(daysLeft: number, now: Date = new Date()): ComplexDetail {
  const day = 86_400_000;
  return {
    ...ACTIVE_COMPLEX,
    tenantStatus: 'SUSPENDED',
    suspendedAt: new Date(now.getTime() - (45 - daysLeft) * day).toISOString(),
    retentionDays: 45,
    purgeDueAt: new Date(now.getTime() + daysLeft * day).toISOString(),
    subscription: { ...ACTIVE_COMPLEX.subscription, status: 'SUSPENDED' },
    allowedTransitions: ['ACTIVE', 'CANCELLED'],
    version: 4,
  };
}

/** `ProvisioningFailedComplex` example. */
export const PROVISIONING_FAILED_COMPLEX: ComplexDetail = {
  ...ACTIVE_COMPLEX,
  tenantStatus: 'PROVISIONING_FAILED',
  allowedTransitions: [],
  version: 1,
};

/** `PurgedComplex` example. */
export const PURGED_COMPLEX: ComplexDetail = {
  ...ACTIVE_COMPLEX,
  subdomain: null,
  ownerEmail: null,
  tenantStatus: 'PURGED',
  suspendedAt: '2027-01-10T12:00:00Z',
  archivedAt: '2027-01-20T12:00:00Z',
  purgedAt: '2027-02-25T03:00:00Z',
  subscription: { ...ACTIVE_COMPLEX.subscription, status: 'CANCELLED' },
  allowedTransitions: [],
  version: 7,
};

/** The two items of the `listComplexes` example. */
export const COMPLEX_SUMMARIES: ComplexSummary[] = [
  {
    id: COMPLEX_ID,
    displayName: 'Canchas El Sol',
    subdomain: 'canchas-el-sol',
    plan: { id: PREMIUM_PLAN.id, name: 'Premium' },
    subscriptionStatus: 'ACTIVE',
    tenantStatus: 'ACTIVE',
    purgeDueAt: null,
    lastPayment: { amount: '120.00', currency: 'USD', paidOn: '2026-12-01' },
  },
  {
    id: '8c9d0e1f-2a3b-4c4d-9e5f-6a7b8c9d0e1f',
    displayName: 'Complejo La Esquina',
    subdomain: 'la-esquina',
    plan: { id: BASIC_PLAN.id, name: 'Basic' },
    subscriptionStatus: 'SUSPENDED',
    tenantStatus: 'SUSPENDED',
    purgeDueAt: '2027-02-24T12:00:00Z',
    lastPayment: null,
  },
];

/** `RecordedPayment` example. */
export const RECORDED_PAYMENT: Payment = {
  id: '6f7a8b9c-0d1e-4f2a-9b3c-4d5e6f7a8b9c',
  amount: '120.00',
  currency: 'USD',
  paidOn: '2026-12-01',
  method: 'TRANSFER',
  recordedBy: ROBIN,
  recordedAt: '2026-12-02T10:15:00Z',
};

/** The two items of the `listComplexHistory` example. */
export const HISTORY_ENTRIES: HistoryEntry[] = [
  {
    id: '3c4d5e6f-7a8b-4c9d-8e0f-1a2b3c4d5e6f',
    kind: 'STATUS',
    before: 'ACTIVE',
    after: 'SUSPENDED',
    changedBy: SAM,
    changedAt: '2027-01-10T12:00:00Z',
  },
  {
    id: '4d5e6f7a-8b9c-4d0e-9f1a-2b3c4d5e6f7a',
    kind: 'PLAN',
    before: 'Basic',
    after: 'Premium',
    changedBy: SAM,
    changedAt: '2026-12-20T09:30:00Z',
  },
];

export function pageWith<T>(items: T[], totalItems = items.length, page = 0): Page<T> {
  return { items, page, size: 20, totalItems };
}

export const EMPTY_PAGE: Page<never> = pageWith([]);
