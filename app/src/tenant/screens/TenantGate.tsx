import type { ReactNode } from 'react'
import type { TenantApi } from '../api/tenantApi'
import { ActiveTenantContext } from './activeTenant'
import { TenantBlockedScreen, TenantCheckFailedScreen, TenantCheckingScreen } from './TenantStatusScreen'
import { useTenantAccess } from './useTenantAccess'

export interface TenantGateProps {
  api: TenantApi
  /** Destination of «Ir a Playtodoo». */
  platformUrl: string
  /** The complex app, rendered only once the complex is active. It reads the complex with `useActiveTenant`. */
  children: ReactNode
}

/** Shows the complex only when its subdomain resolves to an active complex; otherwise, the matching state. */
export function TenantGate({ api, platformUrl, children }: TenantGateProps) {
  const { check, retry } = useTenantAccess(api)

  if (check.phase === 'loading') return <TenantCheckingScreen />
  if (check.phase === 'failed') return <TenantCheckFailedScreen onRetry={retry} />
  if (check.access.kind === 'active') {
    return <ActiveTenantContext value={{ displayName: check.access.displayName }}>{children}</ActiveTenantContext>
  }
  return <TenantBlockedScreen kind={check.access.kind} platformUrl={platformUrl} />
}
