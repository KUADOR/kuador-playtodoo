import { Page } from '../../core/api/page';
import { Capability, CurrentPlatformUser } from '../../core/auth/platform-user';
import {
  CapabilityChange,
  ChangeOperatorCapabilitiesRequest,
  OperatorCapabilities,
} from '../../features/operator-permissions/operator-capabilities-api';

/**
 * In-memory stand-in of the platform operations this backoffice uses, built from the examples of
 * contracts/platform-backoffice.openapi.yaml and contracts/tenancy.openapi.yaml. Development
 * builds only (see mock-backend.development.ts). Every password is accepted: there are no secrets.
 */
export interface MockRequest {
  readonly method: string;
  /** Path after the api base URL, for example `/platform/me`. */
  readonly path: string;
  readonly query: URLSearchParams;
  readonly bearerToken: string | null;
  readonly body: unknown;
}

export interface MockResponse {
  readonly status: number;
  readonly body: unknown;
}

/** Sign in with one of these e-mails (any password) to try each role. */
export const MOCK_ACCOUNTS = {
  superAdmin: 'superadmin@kuador.test',
  operator: 'operador@kuador.test',
} as const;

const TOKEN_LIFETIME_SECONDS = 3600;
const DEFAULT_PAGE_SIZE = 20;

const CAPABILITY_DESCRIPTIONS: Record<Capability, string> = {
  COMPLEX_CREATE: 'Register complexes and retry their provisioning.',
  SUBSCRIPTION_PAYMENT_RECORD: 'Record subscription payments.',
  SUBSCRIPTION_STATUS_CHANGE: 'Mark expiring, suspend, reactivate and cancel subscriptions.',
  TENANT_DATA_ACCESS:
    'Open support sessions with a reason on active complexes to read and edit their data.',
};
const ALL_CAPABILITIES = Object.keys(CAPABILITY_DESCRIPTIONS) as Capability[];

interface MockUser {
  readonly id: string;
  readonly displayName: string;
  readonly role: CurrentPlatformUser['role'];
}

const USERS_BY_EMAIL: Record<string, MockUser> = {
  [MOCK_ACCOUNTS.superAdmin]: {
    id: '4e5f6a7b-8c9d-4e0f-a1b2-c3d4e5f6a7b8',
    displayName: 'Sam Example',
    role: 'SUPER_ADMIN',
  },
  [MOCK_ACCOUNTS.operator]: {
    id: '6a7b8c9d-0e1f-4a2b-8c3d-4e5f6a7b8c9d',
    displayName: 'Robin Example',
    role: 'OPERATOR',
  },
};

const TOKEN_PREFIX = 'mock-token-';

export class MockPlatformBackend {
  private capabilitiesVersion = 0;
  private readonly enabled = new Map<Capability, boolean>(
    ALL_CAPABILITIES.map((key) => [key, false]),
  );
  private readonly changes: CapabilityChange[] = [];
  private nextChangeId = 1;

  handle(request: MockRequest): MockResponse {
    const route = `${request.method} ${request.path}`;
    if (route === 'POST /auth/platform/login') {
      return this.login(request.body);
    }
    const user = this.userOf(request.bearerToken);
    if (user === null) {
      return problem(401, 'UNAUTHENTICATED');
    }
    switch (route) {
      case 'GET /platform/me':
        return ok(this.currentUser(user));
      case 'GET /platform/operator-capabilities':
        return this.onlySuperAdmin(user, () => ok(this.capabilities()));
      case 'PUT /platform/operator-capabilities':
        return this.onlySuperAdmin(user, () =>
          this.changeCapabilities(user, request.body as ChangeOperatorCapabilitiesRequest),
        );
      case 'GET /platform/operator-capabilities/changes':
        return this.onlySuperAdmin(user, () => ok(this.changesPage(request.query)));
      default:
        return { status: 404, body: null };
    }
  }

  private login(body: unknown): MockResponse {
    const email = String((body as { email?: unknown } | null)?.email ?? '').toLowerCase();
    const user = USERS_BY_EMAIL[email];
    if (!user) {
      return problem(401, 'INVALID_CREDENTIALS');
    }
    return ok({
      accessToken: `${TOKEN_PREFIX}${email}`,
      tokenType: 'Bearer',
      expiresIn: TOKEN_LIFETIME_SECONDS,
    });
  }

  private userOf(token: string | null): MockUser | null {
    if (!token?.startsWith(TOKEN_PREFIX)) {
      return null;
    }
    return USERS_BY_EMAIL[token.slice(TOKEN_PREFIX.length)] ?? null;
  }

  private currentUser(user: MockUser): CurrentPlatformUser {
    const capabilities =
      user.role === 'SUPER_ADMIN'
        ? ALL_CAPABILITIES
        : ALL_CAPABILITIES.filter((key) => this.enabled.get(key));
    return { ...user, capabilities };
  }

  private onlySuperAdmin(user: MockUser, respond: () => MockResponse): MockResponse {
    return user.role === 'SUPER_ADMIN' ? respond() : problem(403, 'FORBIDDEN');
  }

  private capabilities(): OperatorCapabilities {
    return {
      version: this.capabilitiesVersion,
      items: ALL_CAPABILITIES.map((key) => ({
        key,
        description: CAPABILITY_DESCRIPTIONS[key],
        enabled: this.enabled.get(key) ?? false,
      })),
    };
  }

  private changeCapabilities(
    user: MockUser,
    request: ChangeOperatorCapabilitiesRequest,
  ): MockResponse {
    if (request.version !== this.capabilitiesVersion) {
      return problem(409, 'STALE_STATE');
    }
    const changedAt = new Date().toISOString();
    for (const change of request.changes) {
      if (this.enabled.get(change.key) === change.enabled) {
        continue;
      }
      this.enabled.set(change.key, change.enabled);
      this.changes.unshift({
        id: `mock-change-${this.nextChangeId++}`,
        key: change.key,
        enabled: change.enabled,
        changedBy: { id: user.id, displayName: user.displayName },
        changedAt,
      });
    }
    this.capabilitiesVersion++;
    return ok(this.capabilities());
  }

  private changesPage(query: URLSearchParams): Page<CapabilityChange> {
    const page = Number(query.get('page') ?? 0);
    const size = Number(query.get('size') ?? DEFAULT_PAGE_SIZE);
    return {
      items: this.changes.slice(page * size, (page + 1) * size),
      page,
      size,
      totalItems: this.changes.length,
    };
  }
}

function ok(body: unknown): MockResponse {
  return { status: 200, body };
}

function problem(status: number, code: string): MockResponse {
  return {
    status,
    body: {
      type: `https://playtodoo.com/problems/${code.toLowerCase()}`,
      title: code,
      status,
      code,
    },
  };
}
