import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { expectNoA11yViolations } from '../../../test/axe'
import { Button, DEFAULT_LOADING_LABEL } from './Button'
import type { ButtonVariant } from './Button'

const VARIANTS: ButtonVariant[] = ['primary', 'secondary', 'danger', 'text']
const LABEL = 'Reservar cancha'

describe('Button', () => {
  it('uses the label as accessible name and defaults to type button', () => {
    render(<Button label={LABEL} />)

    const button = screen.getByRole('button', { name: LABEL })
    expect(button).toHaveAttribute('type', 'button')
  })

  it('calls onClick when activated with mouse, Enter or Space', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()
    render(<Button label={LABEL} onClick={onClick} />)

    await user.click(screen.getByRole('button'))
    await user.keyboard('{Enter}')
    await user.keyboard(' ')

    expect(onClick).toHaveBeenCalledTimes(3)
  })

  it('does not activate while disabled and leaves the tab order', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()
    render(<Button label={LABEL} onClick={onClick} disabled />)

    const button = screen.getByRole('button', { name: LABEL })
    await user.click(button)
    await user.tab()

    expect(button).toBeDisabled()
    expect(button).not.toHaveFocus()
    expect(onClick).not.toHaveBeenCalled()
  })

  it('shows the loading text, announces busy and blocks activation while staying focusable', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()
    render(<Button label={LABEL} onClick={onClick} loading />)

    const button = screen.getByRole('button', { name: DEFAULT_LOADING_LABEL })
    await user.tab()
    await user.click(button)
    await user.keyboard('{Enter}')

    expect(button).toHaveFocus()
    expect(button).toHaveAttribute('aria-busy', 'true')
    expect(button).toHaveAttribute('aria-disabled', 'true')
    expect(onClick).not.toHaveBeenCalled()
  })

  it('accepts a custom loading text', () => {
    render(<Button label="Reservar cancha" loading loadingLabel="Reservando…" />)

    expect(screen.getByRole('button', { name: 'Reservando…' })).toBeInTheDocument()
  })

  describe.each(VARIANTS)('variant %s', (variant) => {
    it.each([
      ['normal', {}],
      ['loading', { loading: true }],
      ['disabled', { disabled: true }],
    ])('has no axe violations when %s', async (_state, stateProps) => {
      const { container } = render(<Button variant={variant} label={LABEL} {...stateProps} />)

      await expectNoA11yViolations(container)
    })
  })
})
