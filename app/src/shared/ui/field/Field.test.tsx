import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { expectNoA11yViolations } from '../../../test/axe'
import { Field } from './Field'
import type { FieldOption } from './Field'

const OPTIONS: FieldOption[] = [
  { value: 'court-1', label: 'Cancha 1' },
  { value: 'court-2', label: 'Cancha 2' },
]
const ERROR = 'Ese horario ya fue reservado. Elige otro disponible.'
const HELP = 'Así lo verán los jugadores.'

describe('Field · text', () => {
  it('links the visible label to the input', () => {
    render(<Field kind="text" label="Nombre del complejo" />)

    expect(screen.getByRole('textbox', { name: 'Nombre del complejo' })).toHaveAttribute('type', 'text')
  })

  it('reports changes', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<Field kind="text" label="Nombre" onChange={onChange} />)

    await user.type(screen.getByRole('textbox', { name: 'Nombre' }), 'Sol')

    expect(onChange).toHaveBeenCalledTimes(3)
    expect(screen.getByRole('textbox', { name: 'Nombre' })).toHaveValue('Sol')
  })

  it('describes the input with its help text', () => {
    render(<Field kind="text" label="Nombre" help={HELP} />)

    const input = screen.getByRole('textbox', { name: 'Nombre' })
    expect(input).toHaveAccessibleDescription(HELP)
    expect(input).not.toHaveAttribute('aria-invalid')
  })

  it('marks the input invalid and associates the error message through aria-describedby', () => {
    render(<Field kind="text" label="Horario" help={HELP} error={ERROR} />)

    const input = screen.getByRole('textbox', { name: 'Horario' })
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription(`${HELP} ${ERROR}`)
    expect(screen.getByText(ERROR).closest('p')?.querySelector('svg')).toHaveAttribute('aria-hidden', 'true')
  })

  it('disables the input', () => {
    render(<Field kind="text" label="Nombre" disabled />)

    expect(screen.getByRole('textbox', { name: 'Nombre' })).toBeDisabled()
  })

  it.each([
    ['empty', {}],
    ['with value', { defaultValue: 'Canchas El Sol' }],
    ['with help', { help: HELP }],
    ['error', { error: ERROR }],
    ['disabled', { disabled: true }],
  ])('has no axe violations when %s', async (_state, stateProps) => {
    const { container } = render(<Field kind="text" label="Nombre" {...stateProps} />)

    await expectNoA11yViolations(container)
  })
})

describe('Field · select', () => {
  it('renders the options and the placeholder', () => {
    render(<Field kind="select" label="Cancha" options={OPTIONS} placeholder="Elige una cancha" />)

    const select = screen.getByRole('combobox', { name: 'Cancha' })
    expect(select).toHaveDisplayValue('Elige una cancha')
    expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual([
      'Elige una cancha',
      'Cancha 1',
      'Cancha 2',
    ])
  })

  it('reports the selected value', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<Field kind="select" label="Cancha" options={OPTIONS} onChange={onChange} />)

    await user.selectOptions(screen.getByRole('combobox', { name: 'Cancha' }), 'court-2')

    expect(onChange).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('combobox', { name: 'Cancha' })).toHaveValue('court-2')
  })

  it('marks the select invalid and associates the error message through aria-describedby', () => {
    render(<Field kind="select" label="Cancha" options={OPTIONS} error={ERROR} />)

    const select = screen.getByRole('combobox', { name: 'Cancha' })
    expect(select).toHaveAttribute('aria-invalid', 'true')
    expect(select).toHaveAccessibleDescription(ERROR)
  })

  it('gives every field its own ids', () => {
    render(
      <>
        <Field kind="select" label="Cancha" options={OPTIONS} error={ERROR} />
        <Field kind="text" label="Horario" error={ERROR} />
      </>,
    )

    const ids = [screen.getByRole('combobox'), screen.getByRole('textbox')].map((control) => control.getAttribute('aria-describedby'))
    expect(new Set(ids).size).toBe(2)
  })

  it.each([
    ['empty', { placeholder: 'Elige una cancha' }],
    ['with value', { defaultValue: 'court-1' }],
    ['error', { error: ERROR }],
    ['disabled', { disabled: true }],
  ])('has no axe violations when %s', async (_state, stateProps) => {
    const { container } = render(<Field kind="select" label="Cancha" options={OPTIONS} {...stateProps} />)

    await expectNoA11yViolations(container)
  })
})
