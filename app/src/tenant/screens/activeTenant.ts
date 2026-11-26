import { createContext, useContext } from 'react'

/** The complex of the current subdomain, once the api answered that it is active. */
export interface ActiveTenant {
  displayName: string
}

/** Provided by `TenantGate` to the complex app. */
export const ActiveTenantContext = createContext<ActiveTenant | null>(null)

/** The active complex; only valid inside `TenantGate`, which renders its children only for an active complex. */
export function useActiveTenant(): ActiveTenant {
  const tenant = useContext(ActiveTenantContext)
  if (!tenant) throw new Error('useActiveTenant must be used inside TenantGate')
  return tenant
}
