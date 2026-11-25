// Port of `GET /tenant` (contracts/tenancy.openapi.yaml · getTenantState). The tenant is resolved by the api
// from the subdomain the app is served on, so the call takes no arguments.

/** What the app may show for the complex of the current subdomain. */
export type TenantAccess =
  | { kind: 'active'; displayName: string }
  /** `403 TENANT_UNAVAILABLE`: suspended or archived. The contract never says which. */
  | { kind: 'unavailable' }
  /** `404 TENANT_NOT_FOUND`: malformed, unknown, retired or purged subdomain. */
  | { kind: 'not-found' }

export type TenantCheckErrorCode = 'NETWORK_ERROR' | 'UNEXPECTED_RESPONSE'

/** The state of the complex could not be checked; the screen offers a retry. */
export class TenantCheckError extends Error {
  readonly code: TenantCheckErrorCode

  constructor(code: TenantCheckErrorCode, message: string) {
    super(message)
    this.name = 'TenantCheckError'
    this.code = code
  }
}

export interface TenantApi {
  /** Resolves the access to the complex, or rejects with `TenantCheckError`. */
  getTenantAccess(): Promise<TenantAccess>
}
