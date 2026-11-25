import { TenantCheckError } from './tenantApi'
import type { TenantAccess, TenantApi } from './tenantApi'

/** Same-origin prefix of the api: the tenant is resolved from the subdomain the app is served on. */
export const DEFAULT_API_BASE_URL = '/api/v1'

const TENANT_PATH = '/tenant'
const ACCEPT_HEADERS = { Accept: 'application/json, application/problem+json' }

interface TenantStateBody {
  displayName: string
}

interface ProblemBody {
  code?: string
}

/** Problem codes of `GET /tenant` that are an answer about the complex, not a failure of the check. */
const ACCESS_BY_PROBLEM_CODE: Readonly<Record<string, TenantAccess>> = {
  TENANT_UNAVAILABLE: { kind: 'unavailable' },
  TENANT_NOT_FOUND: { kind: 'not-found' },
}

/** Adapter of `GET /tenant` over `fetch`. The endpoint is public: no credentials are sent. */
export function createHttpTenantApi(baseUrl = DEFAULT_API_BASE_URL, fetchFn: typeof fetch = fetch): TenantApi {
  return {
    async getTenantAccess() {
      let response: Response
      try {
        response = await fetchFn(`${baseUrl}${TENANT_PATH}`, { headers: ACCEPT_HEADERS })
      } catch (cause) {
        throw new TenantCheckError('NETWORK_ERROR', `Request to ${TENANT_PATH} failed: ${String(cause)}`)
      }
      if (response.ok) {
        const body = await readJson<TenantStateBody>(response)
        if (typeof body?.displayName !== 'string') throw unexpected(response)
        return { kind: 'active', displayName: body.displayName }
      }
      const problem = await readJson<ProblemBody>(response)
      const access = problem?.code ? ACCESS_BY_PROBLEM_CODE[problem.code] : undefined
      if (!access) throw unexpected(response)
      return access
    },
  }
}

function unexpected(response: Response): TenantCheckError {
  return new TenantCheckError('UNEXPECTED_RESPONSE', `Unexpected ${response.status} from ${TENANT_PATH}`)
}

async function readJson<T>(response: Response): Promise<T | null> {
  try {
    return (await response.json()) as T
  } catch {
    // A body that is not JSON (for example, a proxy error page) is reported by the caller as UNEXPECTED_RESPONSE.
    return null
  }
}
