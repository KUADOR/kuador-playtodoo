import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { expectNoA11yViolations } from '../../test/axe'
import DesignCatalog from './DesignCatalog'

describe('DesignCatalog', () => {
  it('shows every button variant in normal, loading and disabled states', () => {
    render(<DesignCatalog />)

    const buttons = within(screen.getByRole('region', { name: 'Botón' })).getAllByRole('button')
    expect(buttons).toHaveLength(12)
    expect(buttons.filter((button) => button.hasAttribute('disabled'))).toHaveLength(4)
    expect(buttons.filter((button) => button.getAttribute('aria-busy') === 'true')).toHaveLength(4)
  })

  it('shows text and select fields including the error state', () => {
    render(<DesignCatalog />)

    const fields = within(screen.getByRole('region', { name: 'Campo' }))
    expect(fields.getAllByRole('textbox')).toHaveLength(4)
    expect(fields.getAllByRole('combobox')).toHaveLength(4)
    expect(fields.getAllByText('Ese horario ya fue reservado. Elige otro disponible.')).toHaveLength(2)
  })

  it('has no axe violations', async () => {
    const { container } = render(<DesignCatalog />)

    await expectNoA11yViolations(container)
  })
})
