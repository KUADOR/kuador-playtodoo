import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ActiveTenantContext } from '../tenant/screens/activeTenant'
import { HomePage } from './HomePage'

describe('HomePage', () => {
  it('shows the name of the complex the api answered', () => {
    render(
      <ActiveTenantContext value={{ displayName: 'Canchas El Sol' }}>
        <HomePage />
      </ActiveTenantContext>,
    )

    expect(screen.getByRole('heading', { level: 1, name: 'Canchas El Sol' })).toBeInTheDocument()
  })

  it('fails loudly outside TenantGate instead of showing another complex', () => {
    // React reports the thrown render error on the console; the assertion below is what matters.
    vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => render(<HomePage />)).toThrow('useActiveTenant must be used inside TenantGate')
  })
})
