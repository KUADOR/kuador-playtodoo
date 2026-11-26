import { CurrentPlatformUser } from '../app/core/auth/platform-user';
import { OperatorCapabilities } from '../app/features/operator-permissions/operator-capabilities-api';

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
