import { describe, expect, it, vi } from 'vitest'
import { createHttpTenantApi } from './httpTenantApi'
import { TenantCheckError } from './tenantApi'

const problem = (status: number, code: string) =>
  new Response(JSON.stringify({ type: 'about:blank', title: code, status, code }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })

function apiAnswering(response: Response | Error) {
  const fetchFn = vi.fn<typeof fetch>(() => (response instanceof Error ? Promise.reject(response) : Promise.resolve(response)))
  return { api: createHttpTenantApi('/api/v1', fetchFn), fetchFn }
}

async function checkErrorOf(promise: Promise<unknown>): Promise<TenantCheckError> {
  const error = await promise.then(() => null, (cause: unknown) => cause)
  expect(error).toBeInstanceOf(TenantCheckError)
  return error as TenantCheckError
}

describe('createHttpTenantApi', () => {
  it('calls GET /tenant on the same origin without credentials', async () => {
    const { api, fetchFn } = apiAnswering(Response.json({ status: 'ACTIVE', displayName: 'Canchas El Sol' }))

    await api.getTenantAccess()

    expect(fetchFn).toHaveBeenCalledWith('/api/v1/tenant', expect.not.objectContaining({ credentials: 'include' }))
  })

  it('maps 200 to an active complex', async () => {
    const { api } = apiAnswering(Response.json({ status: 'ACTIVE', displayName: 'Canchas El Sol' }))

    await expect(api.getTenantAccess()).resolves.toEqual({ kind: 'active', displayName: 'Canchas El Sol' })
  })

  it('maps 403 TENANT_UNAVAILABLE to unavailable', async () => {
    const { api } = apiAnswering(problem(403, 'TENANT_UNAVAILABLE'))

    await expect(api.getTenantAccess()).resolves.toEqual({ kind: 'unavailable' })
  })

  it('maps 404 TENANT_NOT_FOUND to not found', async () => {
    const { api } = apiAnswering(problem(404, 'TENANT_NOT_FOUND'))

    await expect(api.getTenantAccess()).resolves.toEqual({ kind: 'not-found' })
  })

  it('reports a failed request as a network error', async () => {
    const { api } = apiAnswering(new TypeError('Failed to fetch'))

    expect((await checkErrorOf(api.getTenantAccess())).code).toBe('NETWORK_ERROR')
  })

  it.each([
    ['a 404 without a problem body (for example, a proxy page)', new Response('<html>Not Found</html>', { status: 404 })],
    ['a 500 problem', problem(500, 'INTERNAL_ERROR')],
    ['a 200 without the complex name', Response.json({ status: 'ACTIVE' })],
  ])('reports %s as unexpected instead of guessing the state', async (_case, response) => {
    const { api } = apiAnswering(response)

    expect((await checkErrorOf(api.getTenantAccess())).code).toBe('UNEXPECTED_RESPONSE')
  })
})
