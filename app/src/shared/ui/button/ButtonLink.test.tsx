import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { expectNoA11yViolations } from '../../../test/axe'
import { ButtonLink } from './ButtonLink'

describe('ButtonLink', () => {
  it('is a link named by its label that goes to href', () => {
    render(<ButtonLink label="Ir a Playtodoo" href="https://playtodoo.com/" />)

    expect(screen.getByRole('link', { name: 'Ir a Playtodoo' })).toHaveAttribute('href', 'https://playtodoo.com/')
  })

  it('has no axe violations', async () => {
    const { container } = render(<ButtonLink label="Ir a Playtodoo" href="/" variant="secondary" />)

    await expectNoA11yViolations(container)
  })
})
