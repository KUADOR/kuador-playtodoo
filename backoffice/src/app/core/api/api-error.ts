import { HttpErrorResponse } from '@angular/common/http';

/**
 * `code` of the Problem Details (RFC 9457) answered by the api: `ProblemCode` of
 * contracts/platform-backoffice.openapi.yaml, `INVALID_CREDENTIALS` of the platform login
 * (contracts/tenancy.openapi.yaml) and the staff booking codes of contracts/bookings.openapi.yaml
 * that Kuador support receives on the complex host.
 */
export type ProblemCode =
  | 'INVALID_REQUEST'
  | 'VALIDATION_FAILED'
  | 'UNAUTHENTICATED'
  | 'INVALID_CREDENTIALS'
  | 'FORBIDDEN'
  | 'CAPABILITY_REVOKED'
  | 'STALE_STATE'
  | 'COMPLEX_NOT_FOUND'
  | 'PLAN_NOT_FOUND'
  | 'PLAN_FIELD_NOT_FOUND'
  | 'SUPPORT_SESSION_NOT_FOUND'
  | 'SUBDOMAIN_TAKEN'
  | 'SUBDOMAIN_INVALID'
  | 'SUBDOMAIN_RESERVED'
  | 'PLAN_NOT_PUBLISHED'
  | 'NOT_PROVISIONING_FAILED'
  | 'SAME_PLAN'
  | 'SUBSCRIPTION_CANCELLED'
  | 'TRANSITION_NOT_ALLOWED'
  | 'RETENTION_EXPIRED'
  | 'RETENTION_DEADLINE_IN_PAST'
  | 'NOT_IN_RETENTION'
  | 'REASON_REQUIRED'
  | 'TENANT_NOT_ACCESSIBLE'
  | 'SUPPORT_SESSION_INVALID'
  | 'PLAN_NAME_TAKEN'
  | 'PLAN_INCOMPLETE'
  | 'FIELD_NAME_TAKEN'
  | 'BASIC_FIELD_READ_ONLY'
  | 'TENANT_UNAVAILABLE'
  | 'SLOT_UNAVAILABLE'
  | 'SLOT_HAS_BOOKING'
  | 'INVALID_SLOT'
  | 'COURT_NOT_FOUND'
  | 'BOOKING_NOT_FOUND';

/** Failures that do not come with a Problem Details body. */
export type TransportErrorCode = 'NETWORK_ERROR' | 'UNEXPECTED_RESPONSE';

export type ApiErrorCode = ProblemCode | TransportErrorCode;

export interface FieldError {
  readonly field: string;
  readonly code: string;
}

interface ProblemBody {
  readonly code?: ProblemCode;
  readonly title?: string;
  readonly fieldErrors?: readonly FieldError[];
}

/** Error of any api call. Screens map `code` to the Spanish text the user reads. */
export class ApiError extends Error {
  constructor(
    readonly code: ApiErrorCode,
    readonly status: number,
    message: string,
    readonly fieldErrors: readonly FieldError[] = [],
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const NETWORK_FAILURE_STATUS = 0;

/** Converts an `HttpClient` failure into an `ApiError`, reading the Problem Details if present. */
export function toApiError(response: HttpErrorResponse): ApiError {
  if (response.status === NETWORK_FAILURE_STATUS) {
    return new ApiError('NETWORK_ERROR', response.status, `Request to ${response.url} failed`);
  }
  const problem = readProblem(response.error);
  if (!problem?.code) {
    return new ApiError(
      'UNEXPECTED_RESPONSE',
      response.status,
      `Unexpected ${response.status} from ${response.url}`,
    );
  }
  return new ApiError(
    problem.code,
    response.status,
    problem.title ?? `${problem.code} from ${response.url}`,
    problem.fieldErrors ?? [],
  );
}

/** Narrows an unknown failure (for example from `firstValueFrom`) to an `ApiError`. */
export function asApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }
  if (error instanceof HttpErrorResponse) {
    return toApiError(error);
  }
  return new ApiError('UNEXPECTED_RESPONSE', NETWORK_FAILURE_STATUS, String(error));
}

function readProblem(body: unknown): ProblemBody | null {
  if (typeof body === 'string') {
    try {
      return JSON.parse(body) as ProblemBody;
    } catch {
      // A body that is not JSON (for example a proxy error page) is reported as UNEXPECTED_RESPONSE.
      return null;
    }
  }
  return body !== null && typeof body === 'object' ? (body as ProblemBody) : null;
}
