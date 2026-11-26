import { CurrentPlatformUser } from '../../core/auth/platform-user';
import { MockComplexesBackend } from './mock-complexes-backend';
import { MockResponse } from './mock-responses';

const SUPER_ADMIN: CurrentPlatformUser = {
  id: 'admin',
  displayName: 'Sam Example',
  role: 'SUPER_ADMIN',
  capabilities: [
    'COMPLEX_CREATE',
    'SUBSCRIPTION_PAYMENT_RECORD',
    'SUBSCRIPTION_STATUS_CHANGE',
    'TENANT_DATA_ACCESS',
  ],
};
const OPERATOR: CurrentPlatformUser = {
  id: 'operator',
  displayName: 'Robin Example',
  role: 'OPERATOR',
  capabilities: [],
};
const EL_SOL = '/platform/complexes/5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c';

describe('MockComplexesBackend', () => {
  let backend: MockComplexesBackend;

  beforeEach(() => (backend = new MockComplexesBackend()));

  function call(
    method: string,
    path: string,
    user: CurrentPlatformUser = SUPER_ADMIN,
    body: unknown = null,
  ): MockResponse | null {
    return backend.handle(
      { method, path, query: new URLSearchParams(), bearerToken: 't', body },
      user,
    );
  }

  const bodyOf = (response: MockResponse | null) => response?.body as Record<string, unknown>;

  it('checks subdomains like the contract', () => {
    const availability = (subdomain: string) =>
      bodyOf(
        backend.handlePublic({
          method: 'GET',
          path: `/subdomains/${subdomain}/availability`,
          query: new URLSearchParams(),
          bearerToken: null,
          body: null,
        }),
      )['reason'];

    expect(availability('canchas-el-sol')).toBe('TAKEN');
    expect(availability('admin')).toBe('RESERVED');
    expect(availability('el--sol')).toBe('INVALID');
    expect(availability('nuevo-club')).toBeNull();
  });

  it('registers a complex as active and rejects a taken subdomain', () => {
    const request = {
      displayName: 'Nuevo Club',
      subdomain: 'nuevo-club',
      ownerEmail: 'owner@example.com',
      planId: '1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d',
    };

    expect(call('POST', '/platform/complexes', OPERATOR, request)?.status).toBe(403);
    expect(bodyOf(call('POST', '/platform/complexes', SUPER_ADMIN, request))['tenantStatus']).toBe(
      'ACTIVE',
    );
    expect(call('POST', '/platform/complexes', SUPER_ADMIN, request)?.status).toBe(409);
  });

  it('suspends with a purge date, checks the version and reactivates', () => {
    const suspended = bodyOf(
      call('POST', `${EL_SOL}/status-changes`, SUPER_ADMIN, {
        targetStatus: 'SUSPENDED',
        version: 3,
      }),
    );

    expect(suspended).toEqual(
      expect.objectContaining({
        tenantStatus: 'SUSPENDED',
        retentionDays: 45,
        allowedTransitions: ['ACTIVE', 'CANCELLED'],
        version: 4,
      }),
    );
    expect(
      call('POST', `${EL_SOL}/status-changes`, SUPER_ADMIN, { targetStatus: 'ACTIVE', version: 3 })
        ?.status,
    ).toBe(409);
    expect(
      bodyOf(
        call('POST', `${EL_SOL}/status-changes`, SUPER_ADMIN, {
          targetStatus: 'ACTIVE',
          version: 4,
        }),
      )['purgeDueAt'],
    ).toBeNull();
    expect(bodyOf(call('GET', `${EL_SOL}/history`))['totalItems']).toBe(4);
  });

  it('records payments newest first and rejects a zero amount', () => {
    expect(
      call('POST', `${EL_SOL}/payments`, SUPER_ADMIN, {
        amount: '0',
        paidOn: '2026-01-01',
        method: 'TRANSFER',
      })?.status,
    ).toBe(422);
    expect(
      call('POST', `${EL_SOL}/payments`, SUPER_ADMIN, {
        amount: '10.00',
        paidOn: new Date().toISOString().slice(0, 10),
        method: 'INVOICE',
      })?.status,
    ).toBe(201);

    const list = call('GET', '/platform/complexes')?.body as { items: { lastPayment: unknown }[] };
    expect(list.items[0].lastPayment).toEqual(expect.objectContaining({ amount: '10.00' }));
  });
});
