import { MockPlansBackend } from './mock-plans-backend';
import { MockRequest } from './mock-responses';

function request(method: string, path: string, body: unknown = null, query = ''): MockRequest {
  return { method, path, body, query: new URLSearchParams(query), bearerToken: null };
}

describe('MockPlansBackend', () => {
  let backend: MockPlansBackend;

  beforeEach(() => (backend = new MockPlansBackend()));

  const superAdmin = (mockRequest: MockRequest) => backend.handle(mockRequest, true)!;

  it('lets anyone list published plans and only the SUPER_ADMIN list every plan', () => {
    expect(backend.handle(request('GET', '/platform/plans'), false)?.status).toBe(403);
    const published = backend.handle(
      request('GET', '/platform/plans', null, 'status=PUBLISHED'),
      false,
    );
    expect((published?.body as { items: { name: string }[] }).items.map((p) => p.name)).toEqual([
      'Premium',
    ]);
    expect(
      (superAdmin(request('GET', '/platform/plans')).body as { totalItems: number }).totalItems,
    ).toBe(2);
  });

  it('creates a draft and publishes it only when it is complete', () => {
    const draft = superAdmin(
      request('POST', '/platform/plans', { name: 'Club', customFields: {} }),
    );
    expect(draft.status).toBe(201);
    const id = (draft.body as { id: string }).id;

    const incomplete = superAdmin(request('POST', `/platform/plans/${id}/publication`));
    expect(incomplete.status).toBe(422);
    expect((incomplete.body as { code: string }).code).toBe('PLAN_INCOMPLETE');

    superAdmin(
      request('PUT', `/platform/plans/${id}`, {
        name: 'Club',
        price: '50.00',
        currency: 'USD',
        periodicity: 'MONTHLY',
        courtLimit: 2,
        userLimit: 5,
        customFields: {},
        version: 0,
      }),
    );
    const published = superAdmin(request('POST', `/platform/plans/${id}/publication`));
    expect((published.body as { status: string }).status).toBe('PUBLISHED');
  });

  it('answers the conflicts of the contract', () => {
    expect(superAdmin(request('POST', '/platform/plans', { name: 'premium' })).status).toBe(409);
    expect(
      superAdmin(
        request('PUT', '/platform/plans/2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e', {
          name: 'Premium',
          version: 0,
        }),
      ).body,
    ).toEqual(expect.objectContaining({ code: 'STALE_STATE' }));
    expect(
      superAdmin(request('POST', '/platform/plan-fields', { label: 'horario de soporte' })).status,
    ).toBe(409);
  });

  it('creates, retires and rejects unknown custom values', () => {
    const field = superAdmin(
      request('POST', '/platform/plan-fields', {
        label: 'Torneos incluidos',
        type: 'BOOLEAN',
        required: false,
      }),
    ).body as { id: string; key: string };
    expect(field.key).toBe('torneos-incluidos');

    superAdmin(request('POST', `/platform/plan-fields/${field.id}/retirement`));
    const rejected = superAdmin(
      request('POST', '/platform/plans', {
        name: 'Nuevo',
        customFields: { 'torneos-incluidos': true },
      }),
    );
    expect(rejected.body).toEqual(
      expect.objectContaining({
        code: 'VALIDATION_FAILED',
        fieldErrors: [{ field: 'customFields.torneos-incluidos', code: 'UNKNOWN_FIELD' }],
      }),
    );
  });

  it('keeps basic fields read-only and plan fields for the SUPER_ADMIN only', () => {
    expect(
      superAdmin(request('POST', '/platform/plan-fields/mock-basic-price/retirement')).status,
    ).toBe(422);
    expect(backend.handle(request('GET', '/platform/plan-fields'), false)?.status).toBe(403);
    expect(backend.handle(request('GET', '/platform/complexes'), true)).toBeNull();
  });
});
