import {
  MOCK_ACCOUNTS,
  MockPlatformBackend,
  MockRequest,
  MockResponse,
} from './mock-platform-backend';

describe('MockPlatformBackend', () => {
  let backend: MockPlatformBackend;

  beforeEach(() => (backend = new MockPlatformBackend()));

  function call(
    method: string,
    path: string,
    bearerToken: string | null,
    body: unknown = null,
  ): MockResponse {
    const request: MockRequest = { method, path, query: new URLSearchParams(), bearerToken, body };
    return backend.handle(request);
  }

  function tokenOf(email: string): string {
    return (
      call('POST', '/auth/platform/login', null, { email, password: 'any' }).body as {
        accessToken: string;
      }
    ).accessToken;
  }

  it('rejects unknown accounts and requests without a token like the contract', () => {
    expect(
      call('POST', '/auth/platform/login', null, { email: 'nobody@example.com', password: 'x' }),
    ).toEqual(
      expect.objectContaining({
        status: 401,
        body: expect.objectContaining({ code: 'INVALID_CREDENTIALS' }),
      }),
    );
    expect(call('GET', '/platform/me', null).status).toBe(401);
  });

  it('answers GET /me per role', () => {
    expect(call('GET', '/platform/me', tokenOf(MOCK_ACCOUNTS.superAdmin)).body).toEqual(
      expect.objectContaining({
        role: 'SUPER_ADMIN',
        capabilities: expect.arrayContaining(['COMPLEX_CREATE']),
      }),
    );
    expect(call('GET', '/platform/me', tokenOf(MOCK_ACCOUNTS.operator)).body).toEqual(
      expect.objectContaining({ role: 'OPERATOR', capabilities: [] }),
    );
  });

  it('lets only the super admin change capabilities, records the change and checks the version', () => {
    const admin = tokenOf(MOCK_ACCOUNTS.superAdmin);
    const operator = tokenOf(MOCK_ACCOUNTS.operator);
    const enablePayments = {
      version: 0,
      changes: [{ key: 'SUBSCRIPTION_PAYMENT_RECORD', enabled: true }],
    };

    expect(call('PUT', '/platform/operator-capabilities', operator, enablePayments).status).toBe(
      403,
    );
    expect(call('PUT', '/platform/operator-capabilities', admin, enablePayments).status).toBe(200);
    expect(call('PUT', '/platform/operator-capabilities', admin, enablePayments).status).toBe(409);

    expect(call('GET', '/platform/me', operator).body).toEqual(
      expect.objectContaining({ capabilities: ['SUBSCRIPTION_PAYMENT_RECORD'] }),
    );
    expect(call('GET', '/platform/operator-capabilities/changes', admin).body).toEqual(
      expect.objectContaining({ totalItems: 1 }),
    );
  });
});
