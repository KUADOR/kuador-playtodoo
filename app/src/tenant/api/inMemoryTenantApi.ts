import { TenantCheckError } from './tenantApi'
import type { TenantAccess, TenantApi } from './tenantApi'

// Mock of `GET /tenant` (contracts/tenancy.openapi.yaml): it answers one of the outcomes of the contract, or a
// network failure, so every screen state can be seen before the api resolves tenants.

export type TenantMockOutcome = TenantAccess | { kind: 'network-error' }

export interface InMemoryTenantApiOptions {
  outcome: TenantMockOutcome
  /** Simulated latency, so the loading state can be seen in development. */
  latencyMs?: number
}

export function createInMemoryTenantApi({ outcome, latencyMs = 0 }: InMemoryTenantApiOptions): TenantApi {
  return {
    getTenantAccess: () =>
      new Promise((resolve, reject) => {
        setTimeout(() => {
          if (outcome.kind === 'network-error') {
            reject(new TenantCheckError('NETWORK_ERROR', 'Simulated network failure'))
          } else {
            resolve(outcome)
          }
        }, latencyMs)
      }),
  }
}
