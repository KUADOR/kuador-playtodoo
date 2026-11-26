import { UserRef } from '../../core/api/page';
import { Capability, CurrentPlatformUser } from '../../core/auth/platform-user';
import {
  ChangePlanRequest,
  ChangeRetentionRequest,
  ChangeStatusRequest,
  ComplexDetail,
  ComplexSummary,
  HistoryEntry,
  Payment,
  RecordPaymentRequest,
  RegisterComplexRequest,
  SubscriptionStatus,
} from '../../features/complexes/complexes-api';
import { PublishedPlan } from '../../features/complexes/published-plans-api';
import { MockRequest, MockResponse, ok, pageOf, problem } from './mock-responses';

/**
 * In-memory stand-in of the «Complexes», «Subscriptions» and published-plan reads of
 * contracts/platform-backoffice.openapi.yaml, plus the public subdomain operations of
 * contracts/tenancy.openapi.yaml. Built from the examples of the contracts. Development only.
 *
 * Try it: a subdomain containing `fallo` registers with `PROVISIONING_FAILED`; «Reintentar alta»
 * then provisions it on the next read.
 */

const MILLISECONDS_PER_DAY = 86_400_000;
const DEFAULT_RETENTION_DAYS = 45;
const FAILING_SUBDOMAIN_MARK = 'fallo';
const RESERVED_SUBDOMAINS = ['admin', 'api', 'www', 'app', 'mail'];
const SUBDOMAIN_PATTERN = /^(?!xn--)[a-z0-9]+(?:-[a-z0-9]+)*$/;
const SUBDOMAIN_MIN_LENGTH = 3;
const SUBDOMAIN_MAX_LENGTH = 63;

const SAM: UserRef = { id: '4e5f6a7b-8c9d-4e0f-a1b2-c3d4e5f6a7b8', displayName: 'Sam Example' };
const ROBIN: UserRef = { id: '6a7b8c9d-0e1f-4a2b-8c3d-4e5f6a7b8c9d', displayName: 'Robin Example' };

const PLANS: readonly PublishedPlan[] = [
  {
    id: '1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d',
    name: 'Basic',
    price: '60.00',
    currency: 'USD',
    periodicity: 'MONTHLY',
    courtLimit: 4,
    userLimit: 8,
    customFields: {},
  },
  {
    id: '2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e',
    name: 'Premium',
    price: '120.00',
    currency: 'USD',
    periodicity: 'MONTHLY',
    courtLimit: 10,
    userLimit: 25,
    customFields: { 'support-hours': 'Weekdays 9 to 18', 'tournaments-included': true },
  },
];

function daysFromNow(days: number): string {
  return new Date(Date.now() + days * MILLISECONDS_PER_DAY).toISOString();
}

function snapshotOf(
  plan: PublishedPlan,
  assignedAt: string,
): ComplexDetail['subscription']['plan'] {
  const { id, ...conditions } = plan;
  return { planId: id, ...conditions, assignedAt };
}

interface StoredComplex {
  detail: ComplexDetail;
  payments: Payment[];
  history: HistoryEntry[];
}

function seedComplex(
  id: string,
  displayName: string,
  subdomain: string,
  plan: PublishedPlan,
  lifecycle: Partial<ComplexDetail> & { readonly status: SubscriptionStatus },
): ComplexDetail {
  const createdAt = daysFromNow(-120);
  const { status, ...rest } = lifecycle;
  return {
    id,
    displayName,
    subdomain,
    ownerEmail: `owner@${subdomain}.example`,
    createdAt,
    tenantStatus: 'ACTIVE',
    suspendedAt: null,
    archivedAt: null,
    retentionDays: null,
    purgeDueAt: null,
    purgedAt: null,
    subscription: {
      status,
      statusSince: rest.suspendedAt ?? rest.archivedAt ?? createdAt,
      statusChangedBy: SAM,
      plan: snapshotOf(plan, createdAt),
    },
    allowedTransitions: [],
    version: 3,
    ...rest,
  };
}

function seedData(): StoredComplex[] {
  const [basic, premium] = PLANS;
  const elSolPayment = (index: number, daysAgo: number, method: Payment['method']): Payment => ({
    id: `mock-payment-sol-${index}`,
    amount: '120.00',
    currency: 'USD',
    paidOn: daysFromNow(-daysAgo).slice(0, 10),
    method,
    recordedBy: index % 2 === 0 ? SAM : ROBIN,
    recordedAt: daysFromNow(-daysAgo + 1),
  });
  return [
    {
      detail: seedComplex(
        '5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c',
        'Canchas El Sol',
        'canchas-el-sol',
        premium,
        {
          status: 'ACTIVE',
        },
      ),
      payments: [
        elSolPayment(1, 5, 'TRANSFER'),
        elSolPayment(2, 35, 'INVOICE'),
        elSolPayment(3, 65, 'TRANSFER'),
      ],
      history: [
        {
          id: 'mock-history-sol-2',
          kind: 'PLAN',
          before: 'Basic',
          after: 'Premium',
          changedBy: SAM,
          changedAt: daysFromNow(-60),
        },
        {
          id: 'mock-history-sol-1',
          kind: 'STATUS',
          before: null,
          after: 'ACTIVE',
          changedBy: SAM,
          changedAt: daysFromNow(-120),
        },
      ],
    },
    {
      detail: seedComplex(
        '8c9d0e1f-2a3b-4c4d-9e5f-6a7b8c9d0e1f',
        'Complejo La Esquina',
        'la-esquina',
        basic,
        {
          status: 'SUSPENDED',
          tenantStatus: 'SUSPENDED',
          suspendedAt: daysFromNow(-40),
          retentionDays: DEFAULT_RETENTION_DAYS,
          purgeDueAt: daysFromNow(5),
        },
      ),
      payments: [],
      history: [
        {
          id: 'mock-history-esq-2',
          kind: 'STATUS',
          before: 'ACTIVE',
          after: 'SUSPENDED',
          changedBy: ROBIN,
          changedAt: daysFromNow(-40),
        },
        {
          id: 'mock-history-esq-1',
          kind: 'STATUS',
          before: null,
          after: 'ACTIVE',
          changedBy: SAM,
          changedAt: daysFromNow(-120),
        },
      ],
    },
    {
      detail: seedComplex(
        '9d0e1f2a-3b4c-4d5e-8f6a-7b8c9d0e1f2a',
        'Fútbol Norte',
        'futbol-norte',
        basic,
        {
          status: 'EXPIRING',
        },
      ),
      payments: [],
      history: [],
    },
    {
      detail: seedComplex(
        '0e1f2a3b-4c5d-4e6f-9a7b-8c9d0e1f2a3b',
        'Pádel Centro',
        'padel-centro',
        premium,
        {
          status: 'ACTIVE',
          tenantStatus: 'PROVISIONING_FAILED',
        },
      ),
      payments: [],
      history: [],
    },
    {
      detail: seedComplex(
        '1f2a3b4c-5d6e-4f7a-8b9c-0d1e2f3a4b5c',
        'Club Ribera',
        'club-ribera',
        basic,
        {
          status: 'CANCELLED',
          tenantStatus: 'PURGED',
          subdomain: null,
          ownerEmail: null,
          suspendedAt: daysFromNow(-100),
          archivedAt: daysFromNow(-90),
          purgedAt: daysFromNow(-55),
        },
      ),
      payments: [],
      history: [],
    },
  ];
}

function allowedTransitions(complex: ComplexDetail, now: number): SubscriptionStatus[] {
  const provisioned = !['PROVISIONING', 'PROVISIONING_FAILED', 'PURGING', 'PURGED'].includes(
    complex.tenantStatus,
  );
  if (!provisioned) {
    return [];
  }
  switch (complex.subscription.status) {
    case 'ACTIVE':
      return ['EXPIRING', 'SUSPENDED', 'CANCELLED'];
    case 'EXPIRING':
      return ['ACTIVE', 'SUSPENDED', 'CANCELLED'];
    case 'SUSPENDED':
      return complex.purgeDueAt !== null && new Date(complex.purgeDueAt).getTime() > now
        ? ['ACTIVE', 'CANCELLED']
        : [];
    case 'CANCELLED':
      return [];
  }
}

export class MockComplexesBackend {
  private readonly complexes = seedData();
  private nextId = 1;

  /** Public operations (no token): subdomain suggestion and availability. */
  handlePublic(request: MockRequest): MockResponse | null {
    if (request.method !== 'GET') {
      return null;
    }
    if (request.path === '/subdomains/suggestion') {
      return ok({ subdomain: this.suggest(request.query.get('name') ?? '') });
    }
    const availability = /^\/subdomains\/([^/]+)\/availability$/.exec(request.path);
    if (availability) {
      const candidate = decodeURIComponent(availability[1]).toLowerCase();
      const reason = this.unavailableReason(candidate);
      return ok({ subdomain: candidate, available: reason === null, reason });
    }
    return null;
  }

  /** Operations for a signed-in platform user; null when the route is not one of them. */
  handle(request: MockRequest, user: CurrentPlatformUser): MockResponse | null {
    const { method, path } = request;
    if (method === 'GET' && path === '/platform/plans') {
      return request.query.get('status') === 'PUBLISHED' || user.role === 'SUPER_ADMIN'
        ? ok(pageOf(PLANS, request.query))
        : problem(403, 'FORBIDDEN');
    }
    if (path === '/platform/complexes') {
      if (method === 'GET') {
        return ok(
          pageOf(
            this.complexes.map((stored) => this.summaryOf(stored)),
            request.query,
          ),
        );
      }
      if (method === 'POST') {
        return this.guarded(user, 'COMPLEX_CREATE', () =>
          this.register(user, request.body as RegisterComplexRequest),
        );
      }
    }
    const match = /^\/platform\/complexes\/([^/]+)(\/[a-z-]+)?$/.exec(path);
    if (!match) {
      return null;
    }
    const stored = this.complexes.find((candidate) => candidate.detail.id === match[1]);
    if (!stored) {
      return problem(404, 'COMPLEX_NOT_FOUND');
    }
    return this.handleComplex(`${method} ${match[2] ?? ''}`, stored, request, user);
  }

  private handleComplex(
    route: string,
    stored: StoredComplex,
    request: MockRequest,
    user: CurrentPlatformUser,
  ): MockResponse | null {
    switch (route) {
      case 'GET ':
        return ok(this.read(stored));
      case 'POST /provisioning-retries':
        return this.guarded(user, 'COMPLEX_CREATE', () => this.retry(stored));
      case 'PUT /plan':
        return this.superAdmin(user, () =>
          this.changePlan(stored, user, request.body as ChangePlanRequest),
        );
      case 'POST /status-changes':
        return this.guarded(user, 'SUBSCRIPTION_STATUS_CHANGE', () =>
          this.changeStatus(stored, user, request.body as ChangeStatusRequest),
        );
      case 'PUT /retention':
        return this.superAdmin(user, () =>
          this.changeRetention(stored, request.body as ChangeRetentionRequest),
        );
      case 'GET /payments':
        return ok(pageOf(stored.payments, request.query));
      case 'POST /payments':
        return this.guarded(user, 'SUBSCRIPTION_PAYMENT_RECORD', () =>
          this.recordPayment(stored, user, request.body as RecordPaymentRequest),
        );
      case 'GET /history':
        return ok(pageOf(stored.history, request.query));
      default:
        return null;
    }
  }

  private guarded(
    user: CurrentPlatformUser,
    capability: Capability,
    respond: () => MockResponse,
  ): MockResponse {
    return user.capabilities.includes(capability) ? respond() : problem(403, 'CAPABILITY_REVOKED');
  }

  private superAdmin(user: CurrentPlatformUser, respond: () => MockResponse): MockResponse {
    return user.role === 'SUPER_ADMIN' ? respond() : problem(403, 'FORBIDDEN');
  }

  private read(stored: StoredComplex): ComplexDetail {
    // A provisioning retry finishes by the next read.
    if (stored.detail.tenantStatus === 'PROVISIONING') {
      this.update(stored, { tenantStatus: 'ACTIVE' });
    }
    return this.withTransitions(stored.detail);
  }

  private withTransitions(detail: ComplexDetail): ComplexDetail {
    return { ...detail, allowedTransitions: allowedTransitions(detail, Date.now()) };
  }

  private update(stored: StoredComplex, changes: Partial<ComplexDetail>): ComplexDetail {
    stored.detail = this.withTransitions({
      ...stored.detail,
      ...changes,
      version: stored.detail.version + 1,
    });
    return stored.detail;
  }

  private summaryOf(stored: StoredComplex): ComplexSummary {
    const { detail } = stored;
    const last = stored.payments[0];
    return {
      id: detail.id,
      displayName: detail.displayName,
      subdomain: detail.subdomain,
      plan: { id: detail.subscription.plan.planId, name: detail.subscription.plan.name },
      subscriptionStatus: detail.subscription.status,
      tenantStatus: detail.tenantStatus,
      purgeDueAt: detail.purgeDueAt,
      lastPayment: last
        ? { amount: last.amount, currency: last.currency, paidOn: last.paidOn }
        : null,
    };
  }

  private suggest(name: string): string {
    const base =
      name
        .normalize('NFD')
        .replace(/\p{Diacritic}/gu, '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, SUBDOMAIN_MAX_LENGTH) || 'complejo';
    let candidate = base;
    for (let suffix = 2; this.unavailableReason(candidate) !== null; suffix++) {
      candidate = `${base}-${suffix}`;
    }
    return candidate;
  }

  private unavailableReason(candidate: string): 'INVALID' | 'RESERVED' | 'TAKEN' | null {
    if (
      candidate.length < SUBDOMAIN_MIN_LENGTH ||
      candidate.length > SUBDOMAIN_MAX_LENGTH ||
      !SUBDOMAIN_PATTERN.test(candidate)
    ) {
      return 'INVALID';
    }
    if (RESERVED_SUBDOMAINS.includes(candidate)) {
      return 'RESERVED';
    }
    return this.complexes.some((stored) => stored.detail.subdomain === candidate) ? 'TAKEN' : null;
  }

  private register(user: CurrentPlatformUser, request: RegisterComplexRequest): MockResponse {
    const subdomain = request.subdomain.toLowerCase();
    const reason = this.unavailableReason(subdomain);
    if (reason === 'TAKEN') {
      return problem(409, 'SUBDOMAIN_TAKEN');
    }
    if (reason !== null) {
      return problem(422, `SUBDOMAIN_${reason}`);
    }
    const plan = PLANS.find((candidate) => candidate.id === request.planId);
    if (!plan) {
      return problem(422, 'PLAN_NOT_PUBLISHED');
    }
    const now = new Date().toISOString();
    const changedBy = { id: user.id, displayName: user.displayName };
    const detail = this.withTransitions({
      id: `mock-complex-${this.nextId++}`,
      displayName: request.displayName,
      subdomain,
      ownerEmail: request.ownerEmail,
      createdAt: now,
      tenantStatus: subdomain.includes(FAILING_SUBDOMAIN_MARK) ? 'PROVISIONING_FAILED' : 'ACTIVE',
      suspendedAt: null,
      archivedAt: null,
      retentionDays: null,
      purgeDueAt: null,
      purgedAt: null,
      subscription: {
        status: 'ACTIVE',
        statusSince: now,
        statusChangedBy: changedBy,
        plan: snapshotOf(plan, now),
      },
      allowedTransitions: [],
      version: 0,
    });
    this.complexes.unshift({
      detail,
      payments: [],
      history: [
        {
          id: `mock-history-${this.nextId++}`,
          kind: 'STATUS',
          before: null,
          after: 'ACTIVE',
          changedBy,
          changedAt: now,
        },
      ],
    });
    return { status: 201, body: detail };
  }

  private retry(stored: StoredComplex): MockResponse {
    if (stored.detail.tenantStatus !== 'PROVISIONING_FAILED') {
      return problem(409, 'NOT_PROVISIONING_FAILED');
    }
    return { status: 202, body: this.update(stored, { tenantStatus: 'PROVISIONING' }) };
  }

  private stale(stored: StoredComplex, version: number): boolean {
    return version !== stored.detail.version;
  }

  private changePlan(
    stored: StoredComplex,
    user: CurrentPlatformUser,
    request: ChangePlanRequest,
  ): MockResponse {
    if (this.stale(stored, request.version)) {
      return problem(409, 'STALE_STATE');
    }
    const plan = PLANS.find((candidate) => candidate.id === request.planId);
    if (!plan) {
      return problem(422, 'PLAN_NOT_PUBLISHED');
    }
    const current = stored.detail.subscription;
    if (plan.id === current.plan.planId) {
      return problem(422, 'SAME_PLAN');
    }
    if (current.status === 'CANCELLED') {
      return problem(422, 'SUBSCRIPTION_CANCELLED');
    }
    const now = new Date().toISOString();
    this.record(stored, user, 'PLAN', current.plan.name, plan.name, now);
    return ok(this.update(stored, { subscription: { ...current, plan: snapshotOf(plan, now) } }));
  }

  private changeStatus(
    stored: StoredComplex,
    user: CurrentPlatformUser,
    request: ChangeStatusRequest,
  ): MockResponse {
    if (this.stale(stored, request.version)) {
      return problem(409, 'STALE_STATE');
    }
    const detail = stored.detail;
    if (!allowedTransitions(detail, Date.now()).includes(request.targetStatus)) {
      return problem(422, 'TRANSITION_NOT_ALLOWED');
    }
    const now = new Date().toISOString();
    this.record(stored, user, 'STATUS', detail.subscription.status, request.targetStatus, now);
    const subscription = {
      ...detail.subscription,
      status: request.targetStatus,
      statusSince: now,
      statusChangedBy: { id: user.id, displayName: user.displayName },
    };
    return ok(
      this.update(stored, {
        subscription,
        ...this.lifecycleEffect(detail, request.targetStatus, now),
      }),
    );
  }

  /** Effect of a status change on the tenant (table of the contract description). */
  private lifecycleEffect(
    detail: ComplexDetail,
    target: SubscriptionStatus,
    now: string,
  ): Partial<ComplexDetail> {
    const purgeFrom = (start: string) =>
      new Date(
        new Date(start).getTime() + DEFAULT_RETENTION_DAYS * MILLISECONDS_PER_DAY,
      ).toISOString();
    switch (target) {
      case 'SUSPENDED':
        return {
          tenantStatus: 'SUSPENDED',
          suspendedAt: now,
          retentionDays: DEFAULT_RETENTION_DAYS,
          purgeDueAt: purgeFrom(now),
        };
      case 'CANCELLED':
        return detail.tenantStatus === 'SUSPENDED'
          ? { tenantStatus: 'ARCHIVED', archivedAt: now }
          : {
              tenantStatus: 'ARCHIVED',
              archivedAt: now,
              retentionDays: DEFAULT_RETENTION_DAYS,
              purgeDueAt: purgeFrom(now),
            };
      case 'ACTIVE':
        return detail.tenantStatus === 'SUSPENDED'
          ? { tenantStatus: 'ACTIVE', suspendedAt: null, retentionDays: null, purgeDueAt: null }
          : {};
      default:
        return {};
    }
  }

  private changeRetention(stored: StoredComplex, request: ChangeRetentionRequest): MockResponse {
    if (this.stale(stored, request.version)) {
      return problem(409, 'STALE_STATE');
    }
    const detail = stored.detail;
    const start = detail.suspendedAt ?? detail.archivedAt;
    if (start === null || !['SUSPENDED', 'ARCHIVED'].includes(detail.tenantStatus)) {
      return problem(422, 'NOT_IN_RETENTION');
    }
    const purgeDueAt = new Date(
      new Date(start).getTime() + request.retentionDays * MILLISECONDS_PER_DAY,
    );
    if (purgeDueAt.getTime() <= Date.now()) {
      return problem(422, 'RETENTION_DEADLINE_IN_PAST');
    }
    return ok(
      this.update(stored, {
        retentionDays: request.retentionDays,
        purgeDueAt: purgeDueAt.toISOString(),
      }),
    );
  }

  private recordPayment(
    stored: StoredComplex,
    user: CurrentPlatformUser,
    request: RecordPaymentRequest,
  ): MockResponse {
    const fieldErrors = [
      Number(request.amount) > 0 ? null : { field: 'amount', code: 'NOT_POSITIVE' },
      request.paidOn <= new Date().toISOString().slice(0, 10)
        ? null
        : { field: 'paidOn', code: 'DATE_IN_FUTURE' },
    ].filter((error) => error !== null);
    if (fieldErrors.length > 0) {
      return {
        status: 422,
        body: { ...(problem(422, 'VALIDATION_FAILED').body as object), fieldErrors },
      };
    }
    const payment: Payment = {
      id: `mock-payment-${this.nextId++}`,
      amount: request.amount,
      currency: stored.detail.subscription.plan.currency,
      paidOn: request.paidOn,
      method: request.method,
      recordedBy: { id: user.id, displayName: user.displayName },
      recordedAt: new Date().toISOString(),
    };
    stored.payments = [payment, ...stored.payments].sort((a, b) =>
      b.paidOn.localeCompare(a.paidOn),
    );
    return { status: 201, body: payment };
  }

  private record(
    stored: StoredComplex,
    user: CurrentPlatformUser,
    kind: HistoryEntry['kind'],
    before: string,
    after: string,
    changedAt: string,
  ): void {
    stored.history.unshift({
      id: `mock-history-${this.nextId++}`,
      kind,
      before,
      after,
      changedBy: { id: user.id, displayName: user.displayName },
      changedAt,
    });
  }
}
