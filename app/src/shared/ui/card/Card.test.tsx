import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { expectNoA11yViolations } from '../../../test/axe'
import { Card, LOADING_LABEL, SELECTED_LABEL } from './Card'
import type { CardVariant } from './Card'

const VARIANTS: CardVariant[] = ['complex', 'court', 'booking', 'tournament', 'news']

describe('Card', () => {
  it('renders its content and exposes the variant', () => {
    const { container } = render(<Card variant="court">Cancha 1</Card>)

    expect(screen.getByText('Cancha 1')).toBeInTheDocument()
    expect(container.firstElementChild).toHaveAttribute('data-variant', 'court')
    expect(screen.queryByText(SELECTED_LABEL)).not.toBeInTheDocument()
  })

  it('announces the selected state with text, not only color', () => {
    const { container } = render(
      <Card variant="booking" selected>
        Viernes 20:00
      </Card>,
    )

    expect(screen.getByText(SELECTED_LABEL)).toBeInTheDocument()
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
  })

  it('replaces the content with a busy skeleton while loading', () => {
    const { container } = render(
      <Card variant="complex" loading>
        Canchas El Sol
      </Card>,
    )

    expect(screen.queryByText('Canchas El Sol')).not.toBeInTheDocument()
    expect(screen.getByText(LOADING_LABEL)).toBeInTheDocument()
    expect(container.firstElementChild).toHaveAttribute('aria-busy', 'true')
  })

  describe.each(VARIANTS)('variant %s', (variant) => {
    it.each([
      ['normal', {}],
      ['selected', { selected: true }],
      ['loading', { loading: true }],
    ])('has no axe violations when %s', async (_state, stateProps) => {
      const { container } = render(
        <Card variant={variant} {...stateProps}>
          Contenido
        </Card>,
      )

      await expectNoA11yViolations(container)
    })
  })
})
