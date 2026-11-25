import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { expectNoA11yViolations } from '../../../test/axe'
import { Alert } from '../alert/Alert'
import { Button } from '../button/Button'
import { Dialog } from './Dialog'

function DialogHarness({ onClose = vi.fn() }: { onClose?: () => void }) {
  const [open, setOpen] = useState(false)
  const close = () => {
    onClose()
    setOpen(false)
  }
  return (
    <>
      <Button label="Abrir" onClick={() => setOpen(true)} />
      {open && (
        <Dialog
          title="¿Cancelar la reserva?"
          description="La franja quedará libre."
          onClose={close}
          actions={
            <>
              <Button variant="secondary" label="Volver" onClick={close} />
              <Button variant="danger" label="Cancelar reserva" />
            </>
          }
        />
      )}
    </>
  )
}

describe('Dialog', () => {
  it('is a labelled modal dialog that moves the focus inside', async () => {
    const user = userEvent.setup()
    render(<DialogHarness />)

    await user.click(screen.getByRole('button', { name: 'Abrir' }))

    const dialog = screen.getByRole('dialog', { name: '¿Cancelar la reserva?' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(dialog).toHaveAccessibleDescription('La franja quedará libre.')
    expect(screen.getByRole('button', { name: 'Volver' })).toHaveFocus()
    await expectNoA11yViolations(document.body)
  })

  it('makes the rest of the page inert while open', async () => {
    const user = userEvent.setup()
    const { container } = render(<DialogHarness />)

    await user.click(screen.getByRole('button', { name: 'Abrir' }))
    expect(container).toHaveAttribute('inert')

    await user.keyboard('{Escape}')
    expect(container).not.toHaveAttribute('inert')
  })

  it('traps the focus in both directions', async () => {
    const user = userEvent.setup()
    render(<DialogHarness />)
    await user.click(screen.getByRole('button', { name: 'Abrir' }))

    await user.tab()
    expect(screen.getByRole('button', { name: 'Cancelar reserva' })).toHaveFocus()
    await user.tab()
    expect(screen.getByRole('button', { name: 'Volver' })).toHaveFocus()
    await user.tab({ shift: true })
    expect(screen.getByRole('button', { name: 'Cancelar reserva' })).toHaveFocus()
  })

  it('closes with Esc and returns the focus to the element that opened it', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(<DialogHarness onClose={onClose} />)
    await user.click(screen.getByRole('button', { name: 'Abrir' }))

    await user.keyboard('{Escape}')

    expect(onClose).toHaveBeenCalledOnce()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Abrir' })).toHaveFocus()
  })
})

describe('Alert', () => {
  it('interrupts screen readers only for errors, and always shows a title', () => {
    render(
      <>
        <Alert variant="error" title="No se pudo guardar">Revisa tu conexión.</Alert>
        <Alert variant="success" title="Reserva guardada." />
      </>,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo guardarRevisa tu conexión.')
    expect(screen.getByRole('status')).toHaveTextContent('Reserva guardada.')
  })
})
