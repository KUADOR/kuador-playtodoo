import { useState } from 'react'
import { createInMemoryTenantApi } from '../../tenant/api/inMemoryTenantApi'
import type { TenantMockOutcome } from '../../tenant/api/inMemoryTenantApi'
import { HomePage } from '../../app/HomePage'
import { TenantGate } from '../../tenant/screens/TenantGate'

// Development-only composition of the tenant gate against the mock of contracts/tenancy.openapi.yaml.
// `?state=active|unavailable|not-found|network-error` picks the answer of `GET /tenant`.

const MOCK_LATENCY_MS = 600
const MOCK_PLATFORM_URL = '/'

const OUTCOMES: Record<string, TenantMockOutcome> = {
  active: { kind: 'active', displayName: 'Complejo de ejemplo' },
  unavailable: { kind: 'unavailable' },
  'not-found': { kind: 'not-found' },
  'network-error': { kind: 'network-error' },
}

export default function TenantStateMock() {
  const [api] = useState(() => {
    const requested = new URLSearchParams(window.location.search).get('state') ?? 'unavailable'
    return createInMemoryTenantApi({ outcome: OUTCOMES[requested] ?? OUTCOMES.unavailable, latencyMs: MOCK_LATENCY_MS })
  })
  return (
    <TenantGate api={api} platformUrl={MOCK_PLATFORM_URL}>
      <HomePage />
    </TenantGate>
  )
}
