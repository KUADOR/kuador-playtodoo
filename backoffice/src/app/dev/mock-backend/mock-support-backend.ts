import { UserRef } from '../../core/api/page';
import { CurrentPlatformUser } from '../../core/auth/platform-user';
import { ComplexDetail } from '../../features/complexes/complexes-api';
import { AuditEntry, SupportSession } from '../../features/support/support-access-api';
import {
  BlockSlotRequest,
  CreateStaffBookingRequest,
  StaffBooking,
} from '../../features/support/support-bookings-api';
import { Court } from '../../features/support/support-courts';
import { MockComplexesBackend } from './mock-complexes-backend';
import { MockRequest, MockResponse, ok, pageOf, problem } from './mock-responses';

/**
 * In-memory stand-in of the «Support access» tag of contracts/platform-backoffice.openapi.yaml
 * and of the staff booking operations of contracts/bookings.openapi.yaml as Kuador support calls
 * them on a complex host (ADR-020). Development only.
 *
 * Try it: a reason that contains `vence` opens a session of 70 seconds, to see the warning and
 * the expiry of P9.
 */

const SESSION_MINUTES = 30;
const SHORT_SESSION_MARK = 'vence';
const SHORT_SESSION_MS = 70_000;
const MILLISECONDS_PER_MINUTE = 60_000;
const REASON_MAX_LENGTH = 500;
const SUPPORTED_TENANT_STATUSES = ['ACTIVE', 'SUSPENDED', 'ARCHIVED'];

/** Example courts of the contracts, served for every complex (see mock-backend.development.ts). */
export const MOCK_COURTS: readonly Court[] = [
  {
    id: '3f0c6a52-6a0e-4c1e-9d2b-1f7d1c2b9a01',
    name: 'Cancha 1',
    slotDurationMinutes: 60,
    opensAt: '08:00',
    closesAt: '23:00',
  },
  {
    id: '7b9e2d14-5c3a-4f6e-8a1b-2c3d4e5f6a7b',
    name: 'Cancha 2',
    slotDurationMinutes: 90,
    opensAt: '09:00',
    closesAt: '22:30',
  },
];

interface StoredSession {
  readonly sessionId: string;
  readonly user: UserRef & { readonly role: CurrentPlatformUser['role'] };
  readonly complexId: string;
  readonly reason: string;
  readonly expiresAt: number;
  closed: boolean;
}

/** Operation of the tenant host, as the audit names it. */
interface TenantOperation {
  readonly operationId: string;
  readonly pathTemplate: string;
}

function todayAt(time: string, daysFromToday = 0): string {
  const [hours, minutes] = time.split(':').map(Number);
  const instant = new Date();
  instant.setDate(instant.getDate() + daysFromToday);
  instant.setHours(hours, minutes, 0, 0);
  return instant.toISOString();
}

function plusMinutes(instant: string, minutes: number): string {
  return new Date(new Date(instant).getTime() + minutes * MILLISECONDS_PER_MINUTE).toISOString();
}

function seedEntries(): StaffBooking[] {
  const [first, second] = MOCK_COURTS;
  const entry = (
    id: string,
    court: Court,
    start: string,
    fields: Partial<StaffBooking>,
  ): StaffBooking => ({
    id,
    courtId: court.id,
    kind: 'RESERVATION',
    status: 'CONFIRMED',
    start,
    end: plusMinutes(start, court.slotDurationMinutes),
    holderName: null,
    blockReason: null,
    inactive: false,
    ...fields,
  });
  return [
    entry('mock-booking-1', first, todayAt('19:00'), { holderName: 'Alex Example' }),
    entry('mock-booking-2', first, todayAt('20:00'), { status: 'PENDING' }),
    entry('mock-booking-3', second, todayAt('13:00'), {
      kind: 'BLOCK',
      blockReason: 'Mantenimiento',
    }),
    entry('mock-booking-4', second, todayAt('18:00', 1), { holderName: 'Jamie Example' }),
  ];
}

export class MockSupportBackend {
  private readonly sessions = new Map<string, StoredSession>();
  private readonly audit = new Map<string, AuditEntry[]>();
  private readonly entries = new Map<string, StaffBooking[]>();
  private nextId = 1;

  constructor(private readonly complexes: MockComplexesBackend) {}

  /** «Support access» operations on the global host; null when the route is not one of them. */
  handlePlatform(request: MockRequest, user: CurrentPlatformUser): MockResponse | null {
    const open = /^\/platform\/complexes\/([^/]+)\/support-sessions$/.exec(request.path);
    if (open && request.method === 'POST') {
      return this.open(open[1], user, request.body);
    }
    const audit = /^\/platform\/complexes\/([^/]+)\/audit-entries$/.exec(request.path);
    if (audit && request.method === 'GET') {
      return user.role === 'SUPER_ADMIN'
        ? ok(pageOf(this.audit.get(audit[1]) ?? [], request.query))
        : problem(403, 'FORBIDDEN');
    }
    const close = /^\/platform\/support-sessions\/([^/]+)$/.exec(request.path);
    if (close && request.method === 'DELETE') {
      return this.close(decodeURIComponent(close[1]), user);
    }
    return null;
  }

  /** A staff operation on the host of `subdomain`, with the support session header. */
  handleTenant(
    subdomain: string,
    request: MockRequest,
    user: CurrentPlatformUser | null,
    sessionId: string | null,
  ): MockResponse {
    const complex = this.complexes.complexOnSubdomain(subdomain);
    if (complex === null) {
      return problem(404, 'TENANT_NOT_FOUND');
    }
    if (user === null) {
      return problem(401, 'UNAUTHENTICATED');
    }
    const operation = this.operationOf(request);
    const session = sessionId === null ? undefined : this.sessions.get(sessionId);
    const denial = this.denialOf(session, user, complex);
    if (denial !== null) {
      this.record(complex.id, session, user, operation, request.method, denial);
      return problem(403, denial);
    }
    this.record(complex.id, session, user, operation, request.method, null);
    return this.runStaffOperation(complex, operation, request);
  }

  private open(complexId: string, user: CurrentPlatformUser, body: unknown): MockResponse {
    const complex = this.complexes.complexById(complexId);
    if (complex === null) {
      return problem(404, 'COMPLEX_NOT_FOUND');
    }
    if (!user.capabilities.includes('TENANT_DATA_ACCESS')) {
      return problem(403, 'CAPABILITY_REVOKED');
    }
    if (!this.allows(user, complex)) {
      return problem(403, 'TENANT_NOT_ACCESSIBLE');
    }
    const reason = String((body as { reason?: unknown } | null)?.reason ?? '').trim();
    if (reason.length === 0 || reason.length > REASON_MAX_LENGTH) {
      return problem(422, 'REASON_REQUIRED');
    }
    const lifetime = reason.includes(SHORT_SESSION_MARK)
      ? SHORT_SESSION_MS
      : SESSION_MINUTES * MILLISECONDS_PER_MINUTE;
    const stored: StoredSession = {
      sessionId: `mock-session-${this.nextId++}`,
      user: { id: user.id, displayName: user.displayName, role: user.role },
      complexId,
      reason,
      expiresAt: Date.now() + lifetime,
      closed: false,
    };
    this.sessions.set(stored.sessionId, stored);
    this.record(
      complexId,
      stored,
      user,
      {
        operationId: 'openSupportSession',
        pathTemplate: '/api/v1/platform/complexes/{complexId}/support-sessions',
      },
      'POST',
      null,
    );
    const session: SupportSession = {
      sessionId: stored.sessionId,
      expiresAt: new Date(stored.expiresAt).toISOString(),
      tenantHost: `${complex.subdomain}.localhost`,
      tenantStatus: complex.tenantStatus,
    };
    return { status: 201, body: session };
  }

  private close(sessionId: string, user: CurrentPlatformUser): MockResponse {
    const session = this.sessions.get(sessionId);
    if (session === undefined || session.user.id !== user.id) {
      return problem(404, 'SUPPORT_SESSION_NOT_FOUND');
    }
    session.closed = true;
    return { status: 204, body: null };
  }

  /** ACTIVE: SUPER_ADMIN or OPERATOR with the capability; SUSPENDED and ARCHIVED: SUPER_ADMIN. */
  private allows(user: Pick<CurrentPlatformUser, 'role'>, complex: ComplexDetail): boolean {
    if (!SUPPORTED_TENANT_STATUSES.includes(complex.tenantStatus)) {
      return false;
    }
    return complex.tenantStatus === 'ACTIVE' || user.role === 'SUPER_ADMIN';
  }

  private denialOf(
    session: StoredSession | undefined,
    user: CurrentPlatformUser,
    complex: ComplexDetail,
  ): string | null {
    const valid =
      session !== undefined &&
      !session.closed &&
      session.expiresAt > Date.now() &&
      session.user.id === user.id &&
      session.complexId === complex.id;
    if (!valid || !this.allows(user, complex)) {
      return 'SUPPORT_SESSION_INVALID';
    }
    return null;
  }

  private operationOf(request: MockRequest): TenantOperation {
    const { method, path } = request;
    if (path === '/staff/bookings') {
      return method === 'GET'
        ? { operationId: 'listStaffBookings', pathTemplate: '/api/v1/staff/bookings' }
        : { operationId: 'createStaffBooking', pathTemplate: '/api/v1/staff/bookings' };
    }
    if (path === '/staff/blocks') {
      return { operationId: 'blockSlot', pathTemplate: '/api/v1/staff/blocks' };
    }
    if (/^\/staff\/bookings\/[^/]+\/cancel$/.test(path)) {
      return {
        operationId: 'cancelBookingByStaff',
        pathTemplate: '/api/v1/staff/bookings/{bookingId}/cancel',
      };
    }
    return { operationId: '', pathTemplate: path };
  }

  private runStaffOperation(
    complex: ComplexDetail,
    operation: TenantOperation,
    request: MockRequest,
  ): MockResponse {
    const inactive = complex.tenantStatus !== 'ACTIVE';
    const entries = this.entriesOf(complex.id);
    const shown = (entry: StaffBooking): StaffBooking => ({ ...entry, inactive });
    switch (operation.operationId) {
      case 'listStaffBookings': {
        const from = request.query.get('from') ?? '';
        const to = request.query.get('to') ?? '';
        const courtId = request.query.get('courtId');
        return ok(
          entries
            .filter((entry) => entry.status === 'PENDING' || entry.status === 'CONFIRMED')
            .filter((entry) => entry.start < to && from < entry.end)
            .filter((entry) => courtId === null || entry.courtId === courtId)
            .map(shown),
        );
      }
      case 'createStaffBooking':
        return this.add(entries, request.body as CreateStaffBookingRequest, 'RESERVATION', shown);
      case 'blockSlot':
        return this.add(entries, request.body as BlockSlotRequest, 'BLOCK', shown);
      case 'cancelBookingByStaff': {
        const id = decodeURIComponent(request.path.split('/')[3]);
        const index = entries.findIndex((entry) => entry.id === id);
        if (index < 0) {
          return problem(404, 'BOOKING_NOT_FOUND');
        }
        entries[index] = { ...entries[index], status: 'CANCELLED' };
        return ok(shown(entries[index]));
      }
      default:
        return { status: 404, body: null };
    }
  }

  private add(
    entries: StaffBooking[],
    request: CreateStaffBookingRequest | BlockSlotRequest,
    kind: StaffBooking['kind'],
    shown: (entry: StaffBooking) => StaffBooking,
  ): MockResponse {
    const court = MOCK_COURTS.find((candidate) => candidate.id === request.courtId);
    if (court === undefined) {
      return problem(404, 'COURT_NOT_FOUND');
    }
    if (new Date(request.start).getTime() <= Date.now()) {
      return problem(422, 'INVALID_SLOT');
    }
    const start = new Date(request.start).toISOString();
    const end = plusMinutes(start, court.slotDurationMinutes);
    const overlapping = entries.find(
      (entry) =>
        entry.courtId === court.id &&
        (entry.status === 'PENDING' || entry.status === 'CONFIRMED') &&
        entry.start < end &&
        start < entry.end,
    );
    if (overlapping !== undefined) {
      const code =
        kind === 'BLOCK' && overlapping.kind === 'RESERVATION'
          ? 'SLOT_HAS_BOOKING'
          : 'SLOT_UNAVAILABLE';
      return problem(409, code);
    }
    const entry: StaffBooking = {
      id: `mock-booking-new-${this.nextId++}`,
      courtId: court.id,
      kind,
      status: 'CONFIRMED',
      start,
      end,
      holderName: 'holderName' in request ? request.holderName : null,
      blockReason: 'reason' in request ? (request.reason ?? null) : null,
      inactive: false,
    };
    entries.push(entry);
    return { status: 201, body: shown(entry) };
  }

  private entriesOf(complexId: string): StaffBooking[] {
    let entries = this.entries.get(complexId);
    if (entries === undefined) {
      entries = seedEntries();
      this.entries.set(complexId, entries);
    }
    return entries;
  }

  private record(
    complexId: string,
    session: StoredSession | undefined,
    user: CurrentPlatformUser,
    operation: TenantOperation,
    httpMethod: string,
    denialCode: string | null,
  ): void {
    const entries = this.audit.get(complexId) ?? [];
    entries.unshift({
      id: `mock-audit-${this.nextId++}`,
      sessionId: session?.sessionId ?? null,
      actor: { id: user.id, displayName: user.displayName },
      reason: session?.reason ?? null,
      reasonRedacted: false,
      operationId: operation.operationId || null,
      httpMethod,
      pathTemplate: operation.pathTemplate,
      result: denialCode === null ? 'ALLOWED' : 'DENIED',
      denialCode,
      occurredAt: new Date().toISOString(),
    });
    this.audit.set(complexId, entries);
  }
}
