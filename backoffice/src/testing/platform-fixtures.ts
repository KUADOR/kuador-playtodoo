import { Page } from '../app/core/api/page';
import { CurrentPlatformUser } from '../app/core/auth/platform-user';
import { OperatorCapabilities } from '../app/features/operator-permissions/operator-capabilities-api';
import { PlanField } from '../app/features/plans/plan-fields-api';
import { Plan, PlanOptions } from '../app/features/plans/plans-api';

/** Base URL used by every HTTP test (`API_BASE_URL`). */
export const TEST_API_BASE_URL = '/api/v1';
export const ME_URL = `${TEST_API_BASE_URL}/platform/me`;
export const LOGIN_REQUEST_URL = `${TEST_API_BASE_URL}/auth/platform/login`;
export const CAPABILITIES_URL = `${TEST_API_BASE_URL}/platform/operator-capabilities`;
export const CAPABILITY_CHANGES_URL = `${CAPABILITIES_URL}/changes`;

/** `superAdmin` example of `GET /me` in the contract. */
export const SUPER_ADMIN_USER: CurrentPlatformUser = {
  id: '4e5f6a7b-8c9d-4e0f-a1b2-c3d4e5f6a7b8',
  displayName: 'Sam Example',
  role: 'SUPER_ADMIN',
  capabilities: [
    'COMPLEX_CREATE',
    'SUBSCRIPTION_PAYMENT_RECORD',
    'SUBSCRIPTION_STATUS_CHANGE',
    'TENANT_DATA_ACCESS',
  ],
};

/** `operator` example of `GET /me` in the contract. */
export const OPERATOR_USER: CurrentPlatformUser = {
  id: '6a7b8c9d-0e1f-4a2b-8c3d-4e5f6a7b8c9d',
  displayName: 'Robin Example',
  role: 'OPERATOR',
  capabilities: ['SUBSCRIPTION_PAYMENT_RECORD'],
};

/** `OperatorCapabilitiesInitial` example of the contract: every capability disabled. */
export const INITIAL_CAPABILITIES: OperatorCapabilities = {
  version: 0,
  items: [
    {
      key: 'COMPLEX_CREATE',
      description: 'Register complexes and retry their provisioning.',
      enabled: false,
    },
    {
      key: 'SUBSCRIPTION_PAYMENT_RECORD',
      description: 'Record subscription payments.',
      enabled: false,
    },
    {
      key: 'SUBSCRIPTION_STATUS_CHANGE',
      description: 'Mark expiring, suspend, reactivate and cancel subscriptions.',
      enabled: false,
    },
    {
      key: 'TENANT_DATA_ACCESS',
      description:
        'Open support sessions with a reason on active complexes to read and edit their data.',
      enabled: false,
    },
  ],
};

export function problemBody(status: number, code: string): object {
  return {
    type: `https://playtodoo.com/problems/${code.toLowerCase()}`,
    title: code,
    status,
    code,
  };
}

/** Stores a valid access token, as a previous sign-in would. */
export function storeAccessToken(token = 'test-token'): void {
  sessionStorage.setItem(
    'playtodoo.backoffice.access-token',
    JSON.stringify({ accessToken: token, expiresAt: Date.now() + 3_600_000 }),
  );
}

export const PLANS_URL = `${TEST_API_BASE_URL}/platform/plans`;
export const PLAN_OPTIONS_URL = `${TEST_API_BASE_URL}/platform/plan-options`;
export const PLAN_FIELDS_URL = `${TEST_API_BASE_URL}/platform/plan-fields`;

/** `PublishedPlan` example of the contract. */
export const PUBLISHED_PLAN: Plan = {
  id: '2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e',
  name: 'Premium',
  price: '120.00',
  currency: 'USD',
  periodicity: 'MONTHLY',
  courtLimit: 10,
  userLimit: 25,
  customFields: { 'support-hours': 'Weekdays 9 to 18' },
  status: 'PUBLISHED',
  version: 2,
};

/** `DraftPlan` example of the contract: only the name. */
export const DRAFT_PLAN: Plan = {
  id: '3c4d5e6f-7a8b-4c9d-8e0f-1a2b3c4d5e6f',
  name: 'Inicial',
  price: null,
  currency: null,
  periodicity: null,
  courtLimit: null,
  userLimit: null,
  customFields: {},
  status: 'DRAFT',
  version: 0,
};

export const PLAN_OPTIONS: PlanOptions = {
  currencies: ['USD'],
  periodicities: ['MONTHLY', 'YEARLY'],
};

export const PRICE_FIELD: PlanField = {
  id: '0a1b2c3d-4e5f-4a6b-8c7d-8e9f0a1b2c3d',
  key: 'price',
  label: 'Price',
  type: 'NUMBER',
  required: true,
  origin: 'BASIC',
  retiredAt: null,
  version: 0,
};

/** `CustomPlanField` example of the contract. */
export const SUPPORT_HOURS_FIELD: PlanField = {
  id: '9f0a1b2c-3d4e-4f5a-8b6c-7d8e9f0a1b2c',
  key: 'support-hours',
  label: 'Horario de soporte',
  type: 'TEXT',
  required: true,
  origin: 'CUSTOM',
  retiredAt: null,
  version: 0,
};

export const TOURNAMENTS_FIELD: PlanField = {
  id: '8e9f0a1b-2c3d-4e4f-9a5b-6c7d8e9f0a1b',
  key: 'tournaments-included',
  label: 'Torneos incluidos',
  type: 'BOOLEAN',
  required: false,
  origin: 'CUSTOM',
  retiredAt: null,
  version: 1,
};

export const RETIRED_FIELD: PlanField = {
  id: '7d8e9f0a-1b2c-4d3e-8f4a-5b6c7d8e9f0a',
  key: 'old-discount',
  label: 'Descuento anterior',
  type: 'NUMBER',
  required: false,
  origin: 'CUSTOM',
  retiredAt: '2027-01-15T10:00:00Z',
  version: 2,
};

export function pageOf<T>(items: readonly T[]): Page<T> {
  return { items, page: 0, size: 20, totalItems: items.length };
}

export function validationProblem(
  status: number,
  code: string,
  fieldErrors: readonly { field: string; code: string }[],
): object {
  return { ...problemBody(status, code), fieldErrors };
}
