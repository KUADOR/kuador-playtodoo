import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { expectNoA11yViolations } from '../../test/axe'
import { TenantCheckError } from '../api/tenantApi'
import type { TenantAccess, TenantApi } from '../api/tenantApi'
import { TENANT_TEXT } from '../model/tenantText'
import { useActiveTenant } from './activeTenant'
import { TenantGate } from './TenantGate'

const PLATFORM_URL = 'https://playtodoo.example/'
const COMPLEX_CONTENT = 'Contenido del complejo'
const COMPLEX_NAME = 'Canchas El Sol'

function apiOf(...answers: (TenantAccess | TenantCheckError)[]): TenantApi {
  // Once the given answers run out, the check stays pending.
  const getTenantAccess = vi.fn<TenantApi['getTenantAccess']>(() => new Promise(() => {}))
  for (const answer of answers) {
    getTenantAccess.mockImplementationOnce(() =>
      answer instanceof TenantCheckError ? Promise.reject(answer) : Promise.resolve(answer),
    )
  }
  return { getTenantAccess }
}

function renderGate(api: TenantApi) {
  return render(
    <TenantGate api={api} platformUrl={PLATFORM_URL}>
      <p>{COMPLEX_CONTENT}</p>
    </TenantGate>,
  )
}

const networkError = () => new TenantCheckError('NETWORK_ERROR', 'offline')

describe('TenantGate', () => {
  it('shows the card skeleton while the subdomain is resolved', () => {
    renderGate(apiOf())

    expect(screen.getByText('Cargando…')).toBeInTheDocument()
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
    expect(screen.queryByText(COMPLEX_CONTENT)).not.toBeInTheDocument()
  })

  it('shows the complex when it is active', async () => {
    renderGate(apiOf({ kind: 'active', displayName: COMPLEX_NAME }))

    expect(await screen.findByText(COMPLEX_CONTENT)).toBeInTheDocument()
  })

  it('gives the complex app the name the api answered', async () => {
    function ComplexName() {
      return <p>{useActiveTenant().displayName}</p>
    }
    render(
      <TenantGate api={apiOf({ kind: 'active', displayName: COMPLEX_NAME })} platformUrl={PLATFORM_URL}>
        <ComplexName />
      </TenantGate>,
    )

    expect(await screen.findByText(COMPLEX_NAME)).toBeInTheDocument()
  })

  it('shows «Complejo no disponible» with the focus on its title and a way back to Playtodoo', async () => {
    renderGate(apiOf({ kind: 'unavailable' }))

    const title = await screen.findByRole('heading', { level: 1, name: TENANT_TEXT.unavailableTitle })
    expect(title).toHaveFocus()
    expect(screen.getByText(TENANT_TEXT.unavailableText)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: TENANT_TEXT.goToPlatform })).toHaveAttribute('href', PLATFORM_URL)
    expect(screen.queryByText(COMPLEX_CONTENT)).not.toBeInTheDocument()
  })

  it('shows «Complejo no encontrado» with the focus on its title and a way back to Playtodoo', async () => {
    renderGate(apiOf({ kind: 'not-found' }))

    const title = await screen.findByRole('heading', { level: 1, name: TENANT_TEXT.notFoundTitle })
    expect(title).toHaveFocus()
    expect(screen.getByText(TENANT_TEXT.notFoundText)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: TENANT_TEXT.goToPlatform })).toHaveAttribute('href', PLATFORM_URL)
    expect(screen.queryByText(COMPLEX_CONTENT)).not.toBeInTheDocument()
  })

  it('never reveals the reason nor any deadline on the blocked screens', async () => {
    const { container } = renderGate(apiOf({ kind: 'unavailable' }))
    await screen.findByRole('heading', { level: 1 })

    expect(container).not.toHaveTextContent(/suspend|archiv|baja|días|plazo/i)
  })

  it('announces a network failure as an alert, distinct from «not available», and retries', async () => {
    const user = userEvent.setup()
    renderGate(apiOf(networkError(), { kind: 'active', displayName: COMPLEX_NAME }))

    expect(await screen.findByRole('alert')).toHaveTextContent(TENANT_TEXT.checkFailed)
    expect(screen.queryByText(TENANT_TEXT.unavailableTitle)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: TENANT_TEXT.retry }))

    expect(await screen.findByText(COMPLEX_CONTENT)).toBeInTheDocument()
  })

  it('shows the skeleton again while retrying', async () => {
    const user = userEvent.setup()
    const api = apiOf(networkError())
    renderGate(api)
    await user.click(await screen.findByRole('button', { name: TENANT_TEXT.retry }))

    expect(screen.getByText('Cargando…')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(api.getTenantAccess).toHaveBeenCalledTimes(2)
  })

  it.each([
    ['unavailable', apiOf({ kind: 'unavailable' }), 'heading'],
    ['not found', apiOf({ kind: 'not-found' }), 'heading'],
    ['network error', apiOf(networkError()), 'alert'],
  ] as const)('has no axe violations when %s', async (_state, api, role) => {
    const { container } = renderGate(api)
    await screen.findByRole(role)

    await expectNoA11yViolations(container)
  })
})
