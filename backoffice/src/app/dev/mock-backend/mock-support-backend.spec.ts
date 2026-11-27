import { CurrentPlatformUser } from '../../core/auth/platform-user';
import { MockComplexesBackend } from './mock-complexes-backend';
import { MockRequest, MockResponse } from './mock-responses';
import { MOCK_COURTS, MockSupportBackend } from './mock-support-backend';

const SUPER_ADMIN: CurrentPlatformUser = {
  id: 'admin',
  displayName: 'Sam Example',
  role: 'SUPER_ADMIN',
  capabilities: ['TENANT_DATA_ACCESS'],
};
const OPERATOR: CurrentPlatformUser = {
  id: 'operator',
  displayName: 'Robin Example',
  role: 'OPERATOR',
  capabilities: ['TENANT_DATA_ACCESS'],
};
const EL_SOL_ID = '5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c';
const LA_ESQUINA_ID = '8c9d0e1f-2a3b-4c4d-9e5f-6a7b8c9d0e1f';

describe('MockSupportBackend', () => {
  let backend: MockSupportBackend;

  beforeEach(() => (backend = new MockSupportBackend(new MockComplexesBackend())));

  function request(method: string, path: string, body: unknown = null, query = ''): MockRequest {
    return { method, path, query: new URLSearchParams(query), bearerToken: 't', body };
  }

  function open(complexId: string, user: CurrentPlatformUser, reason = 'Ticket 1234') {
    return backend.handlePlatform(
      request('POST', `/platform/complexes/${complexId}/support-sessions`, { reason }),
      user,
    ) as MockResponse;
  }

  const sessionIdOf = (response: MockResponse) =>
    (response.body as { sessionId: string }).sessionId;

  it('opens sessions by role and tenant status and requires a reason', () => {
    expect(open(EL_SOL_ID, OPERATOR).status).toBe(201);
    expect(open(LA_ESQUINA_ID, OPERATOR).body).toEqual(
      expect.objectContaining({ code: 'TENANT_NOT_ACCESSIBLE' }),
    );
    expect(open(LA_ESQUINA_ID, SUPER_ADMIN).status).toBe(201);
    expect(open(EL_SOL_ID, SUPER_ADMIN, '   ').body).toEqual(
      expect.objectContaining({ code: 'REASON_REQUIRED' }),
    );
  });

  it('serves only the complex of the session and audits allowed and denied requests', () => {
    const sessionId = sessionIdOf(open(EL_SOL_ID, SUPER_ADMIN));
    const list = request('GET', '/staff/bookings', null, 'from=2000-01-01&to=2100-01-01');

    expect(backend.handleTenant('canchas-el-sol', list, SUPER_ADMIN, sessionId).status).toBe(200);
    expect(backend.handleTenant('la-esquina', list, SUPER_ADMIN, sessionId).body).toEqual(
      expect.objectContaining({ code: 'SUPPORT_SESSION_INVALID' }),
    );
    const audit = backend.handlePlatform(
      request('GET', `/platform/complexes/${EL_SOL_ID}/audit-entries`),
      SUPER_ADMIN,
    );
    expect(
      (audit?.body as { items: { operationId: string; result: string }[] }).items.map(
        (entry) => `${entry.operationId} ${entry.result}`,
      ),
    ).toEqual(['listStaffBookings ALLOWED', 'openSupportSession ALLOWED']);
  });

  it('rejects overlapping slots and marks entries inactive on a suspended complex', () => {
    const sessionId = sessionIdOf(open(LA_ESQUINA_ID, SUPER_ADMIN));
    const tomorrow = new Date(Date.now() + 86_400_000);
    tomorrow.setHours(10, 0, 0, 0);
    const create = request('POST', '/staff/bookings', {
      courtId: MOCK_COURTS[0].id,
      start: tomorrow.toISOString(),
      holderName: 'Alex Example',
    });

    const created = backend.handleTenant('la-esquina', create, SUPER_ADMIN, sessionId);
    const again = backend.handleTenant('la-esquina', create, SUPER_ADMIN, sessionId);

    expect(created.body).toEqual(expect.objectContaining({ inactive: true, status: 'CONFIRMED' }));
    expect(again.body).toEqual(expect.objectContaining({ code: 'SLOT_UNAVAILABLE' }));
  });
});
