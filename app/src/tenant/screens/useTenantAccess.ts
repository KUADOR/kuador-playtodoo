import { useCallback, useEffect, useState } from 'react'
import type { TenantAccess, TenantApi } from '../api/tenantApi'

export type TenantCheck = { phase: 'loading' } | { phase: 'failed' } | { phase: 'resolved'; access: TenantAccess }

const LOADING: TenantCheck = { phase: 'loading' }

/** Checks the complex of the current subdomain once; `retry` checks it again from the loading state. */
export function useTenantAccess(api: TenantApi) {
  const [check, setCheck] = useState<TenantCheck>(LOADING)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let current = true
    api.getTenantAccess().then(
      (access) => current && setCheck({ phase: 'resolved', access }),
      // Any failure (network, unexpected answer) is shown as «could not check» with a retry; it never
      // becomes «not available» or «not found», which are answers about the complex.
      () => current && setCheck({ phase: 'failed' }),
    )
    return () => {
      current = false
    }
  }, [api, attempt])

  const retry = useCallback(() => {
    setCheck(LOADING)
    setAttempt((value) => value + 1)
  }, [])

  return { check, retry }
}
