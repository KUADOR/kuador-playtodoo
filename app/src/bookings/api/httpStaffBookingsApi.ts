import { BookingApiError } from './staffBookingsApi'
import type {
  BlockSlotRequest,
  CreateStaffBookingRequest,
  ProblemCode,
  StaffBooking,
  StaffBookingsApi,
  StaffBookingsQuery,
} from './staffBookingsApi'

/** Same-origin prefix of the api: the tenant is resolved from the subdomain the app is served on. */
export const DEFAULT_API_BASE_URL = '/api/v1'

const JSON_HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json, application/problem+json' }

interface ProblemBody {
  code?: ProblemCode
  title?: string
  conflictingBookingId?: string
}

/** Adapter of the staff endpoints over `fetch`. Authentication is [POR DEFINIR] by the Identity and Access module. */
export function createHttpStaffBookingsApi(baseUrl = DEFAULT_API_BASE_URL, fetchFn: typeof fetch = fetch): StaffBookingsApi {
  async function request<T>(path: string, init?: RequestInit): Promise<T> {
    let response: Response
    try {
      response = await fetchFn(`${baseUrl}${path}`, { ...init, headers: JSON_HEADERS, credentials: 'include' })
    } catch (cause) {
      throw new BookingApiError('NETWORK_ERROR', `Request to ${path} failed: ${String(cause)}`)
    }
    if (!response.ok) throw await toBookingApiError(response, path)
    return (await response.json()) as T
  }

  const post = <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })

  return {
    listBookings: ({ from, to, courtId }: StaffBookingsQuery) => {
      const params = new URLSearchParams({ from, to })
      if (courtId) params.set('courtId', courtId)
      return request<StaffBooking[]>(`/staff/bookings?${params}`)
    },
    getBooking: (bookingId) => request<StaffBooking>(`/staff/bookings/${encodeURIComponent(bookingId)}`),
    createBooking: (body: CreateStaffBookingRequest) => post<StaffBooking>('/staff/bookings', body),
    blockSlot: (body: BlockSlotRequest) => post<StaffBooking>('/staff/blocks', body),
    cancelBooking: (bookingId) => post<StaffBooking>(`/staff/bookings/${encodeURIComponent(bookingId)}/cancel`),
  }
}

async function toBookingApiError(response: Response, path: string): Promise<BookingApiError> {
  const problem = await readProblem(response)
  if (!problem?.code) {
    return new BookingApiError('UNEXPECTED_RESPONSE', `Unexpected ${response.status} from ${path}`)
  }
  return new BookingApiError(
    problem.code,
    problem.title ?? `${problem.code} from ${path}`,
    problem.conflictingBookingId ?? null,
  )
}

async function readProblem(response: Response): Promise<ProblemBody | null> {
  try {
    return (await response.json()) as ProblemBody
  } catch {
    // A body that is not JSON (for example, a proxy error page) is reported as UNEXPECTED_RESPONSE.
    return null
  }
}
