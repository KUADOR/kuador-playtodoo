import { AuditEntry, SupportSession } from '../app/features/support/support-access-api';
import { ActiveSupportAccess } from '../app/features/support/support-access';
import { StaffBooking } from '../app/features/support/support-bookings-api';
import { Court } from '../app/features/support/support-courts';
import { ACTIVE_COMPLEX, COMPLEX_URL } from './complex-fixtures';
import { TEST_API_BASE_URL } from './platform-fixtures';

/** Examples of the «Support access» tag and of the staff bookings seen by Kuador support. */

export const SUPPORT_SESSION_ID = '2c3d4e5f-6a7b-4c8d-9e0f-1a2b3c4d5e6f';
export const SUPPORT_SESSIONS_URL = `${COMPLEX_URL}/support-sessions`;
export const CLOSE_SESSION_URL = `${TEST_API_BASE_URL}/platform/support-sessions/${SUPPORT_SESSION_ID}`;
export const AUDIT_URL = `${COMPLEX_URL}/audit-entries`;
export const TENANT_HOST = 'canchas-el-sol.playtodoo.com';
/** `TENANT_API_BASE_URL` of the tests: the complex host over https. */
export const tenantApiBaseUrl = (host: string) => `https://${host}/api/v1`;
export const STAFF_BOOKINGS_URL = `${tenantApiBaseUrl(TENANT_HOST)}/staff/bookings`;
export const STAFF_BLOCKS_URL = `${tenantApiBaseUrl(TENANT_HOST)}/staff/blocks`;
export const REASON = 'Complaint about a duplicated booking, ticket 1234';

const MINUTE_MS = 60_000;

export const COURTS: readonly Court[] = [
  {
    id: '3f0c6a52-6a0e-4c1e-9d2b-1f7d1c2b9a01',
    name: 'Cancha 1',
    slotDurationMinutes: 60,
    opensAt: '18:00',
    closesAt: '22:00',
  },
  {
    id: '7b9e2d14-5c3a-4f6e-8a1b-2c3d4e5f6a7b',
    name: 'Cancha 2',
    slotDurationMinutes: 90,
    opensAt: '09:00',
    closesAt: '22:30',
  },
];

/** `active` example of `openSupportSession`, valid for `minutes` from `now`. */
export function supportSession(
  minutes = 30,
  now = Date.now(),
  tenantStatus: SupportSession['tenantStatus'] = 'ACTIVE',
): SupportSession {
  return {
    sessionId: SUPPORT_SESSION_ID,
    expiresAt: new Date(now + minutes * MINUTE_MS).toISOString(),
    tenantHost: TENANT_HOST,
    tenantStatus,
  };
}

export function supportAccess(session: SupportSession = supportSession()): ActiveSupportAccess {
  return {
    complex: {
      id: ACTIVE_COMPLEX.id,
      displayName: ACTIVE_COMPLEX.displayName,
      purgeDueAt: session.tenantStatus === 'ARCHIVED' ? '2027-02-24T12:00:00Z' : null,
    },
    reason: REASON,
    session,
  };
}

/** Local `time` of today plus `days`, as an api instant. */
export function localInstantToday(time: string, days = 0): string {
  const [hours, minutes] = time.split(':').map(Number);
  const instant = new Date();
  instant.setDate(instant.getDate() + days);
  instant.setHours(hours, minutes, 0, 0);
  return instant.toISOString();
}

function entry(fields: Partial<StaffBooking> & Pick<StaffBooking, 'id' | 'start'>): StaffBooking {
  return {
    courtId: COURTS[0].id,
    kind: 'RESERVATION',
    status: 'CONFIRMED',
    end: new Date(new Date(fields.start).getTime() + 60 * MINUTE_MS).toISOString(),
    holderName: null,
    blockReason: null,
    inactive: false,
    ...fields,
  };
}

/** The `mixed` example of `listStaffBookings`, on today's local day. */
export function dayEntries(inactive = false): StaffBooking[] {
  return [
    entry({
      id: '5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d',
      start: localInstantToday('20:00'),
      holderName: 'Alex Example',
      inactive,
    }),
    entry({
      id: '0b8f3c1e-2d4a-4b6c-8e0f-1a2b3c4d5e6f',
      start: localInstantToday('19:00'),
      status: 'PENDING',
      inactive,
    }),
    entry({
      id: '9c8b7a6f-5e4d-4c3b-8a2f-1e0d9c8b7a6f',
      courtId: COURTS[1].id,
      kind: 'BLOCK',
      start: localInstantToday('13:00'),
      blockReason: 'Maintenance',
      inactive,
    }),
  ];
}

/** The two items of the `listComplexAuditEntries` example, plus an unknown and a redacted one. */
export const AUDIT_ENTRIES: AuditEntry[] = [
  {
    id: '7d8e9f0a-1b2c-4d3e-8f4a-5b6c7d8e9f0a',
    sessionId: SUPPORT_SESSION_ID,
    actor: { id: '4e5f6a7b-8c9d-4e0f-a1b2-c3d4e5f6a7b8', displayName: 'Sam Example' },
    reason: REASON,
    reasonRedacted: false,
    operationId: 'cancelStaffBooking',
    httpMethod: 'POST',
    pathTemplate: '/api/v1/staff/bookings/{bookingId}/cancel',
    result: 'ALLOWED',
    denialCode: null,
    occurredAt: '2027-01-11T15:02:10Z',
  },
  {
    id: '8e9f0a1b-2c3d-4e4f-9a5b-6c7d8e9f0a1b',
    sessionId: null,
    actor: { id: '6a7b8c9d-0e1f-4a2b-8c3d-4e5f6a7b8c9d', displayName: 'Robin Example' },
    reason: null,
    reasonRedacted: false,
    operationId: 'listStaffBookings',
    httpMethod: 'GET',
    pathTemplate: '/api/v1/staff/bookings',
    result: 'DENIED',
    denialCode: 'SUPPORT_SESSION_INVALID',
    occurredAt: '2027-01-11T15:01:00Z',
  },
  {
    id: '9f0a1b2c-3d4e-4f5a-8b6c-7d8e9f0a1b2c',
    sessionId: SUPPORT_SESSION_ID,
    actor: { id: '4e5f6a7b-8c9d-4e0f-a1b2-c3d4e5f6a7b8', displayName: 'Sam Example' },
    reason: null,
    reasonRedacted: true,
    operationId: null,
    httpMethod: 'GET',
    pathTemplate: '/api/v1/staff/reports',
    result: 'ALLOWED',
    denialCode: null,
    occurredAt: '2027-01-10T09:00:00Z',
  },
];
