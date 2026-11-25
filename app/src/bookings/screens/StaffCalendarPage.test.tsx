import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { expectNoA11yViolations } from '../../test/axe'
import { createInMemoryStaffBookingsApi } from '../api/inMemoryStaffBookingsApi'
import { BookingApiError } from '../api/staffBookingsApi'
import type { StaffBooking, StaffBookingsApi } from '../api/staffBookingsApi'
import type { Court } from '../model/court'
import { StaffCalendarPage } from './StaffCalendarPage'

const TODAY = '2026-12-04'
const TIME_ZONE = 'UTC'
const COURTS: Court[] = [
  { id: 'court-1', name: 'Cancha 1', slotDurationMinutes: 60, opensAt: '18:00', closesAt: '22:00' },
  { id: 'court-2', name: 'Cancha 2', slotDurationMinutes: 90, opensAt: '18:00', closesAt: '21:00' },
]

const CONFIRMED: StaffBooking = {
  id: 'confirmed',
  courtId: 'court-1',
  kind: 'RESERVATION',
  status: 'CONFIRMED',
  start: '2026-12-04T20:00:00Z',
  end: '2026-12-04T21:00:00Z',
  holderName: 'Alex',
  blockReason: null,
}
const PENDING: StaffBooking = { ...CONFIRMED, id: 'pending', status: 'PENDING', start: '2026-12-04T18:00:00Z', end: '2026-12-04T19:00:00Z', holderName: null }
const BLOCK: StaffBooking = { ...CONFIRMED, id: 'block', kind: 'BLOCK', start: '2026-12-04T21:00:00Z', end: '2026-12-04T22:00:00Z', holderName: null, blockReason: 'Mantenimiento' }

function renderPage(api: StaffBookingsApi = createInMemoryStaffBookingsApi({ courts: COURTS, seed: [PENDING, CONFIRMED, BLOCK] })) {
  const user = userEvent.setup()
  render(<StaffCalendarPage api={api} courts={COURTS} timeZone={TIME_ZONE} today={TODAY} />)
  return { user, api }
}

async function rowAt(time: string) {
  const list = await screen.findByRole('list', { name: 'Franjas del día' })
  const row = within(list)
    .getAllByRole('listitem')
    .find((item) => item.textContent?.startsWith(time))
  if (!row) throw new Error(`No row starts at ${time}`)
  return row
}

describe('StaffCalendarPage', () => {
  it('shows the skeleton while loading and then each entry with icon and text', async () => {
    renderPage()

    expect(screen.getByText('Cargando reservas…')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Nueva reserva' })).not.toBeInTheDocument()

    expect(await rowAt('18:00–19:00')).toHaveTextContent('Pendiente')
    expect(await rowAt('19:00–20:00')).toHaveTextContent('Libre')
    expect(await rowAt('20:00–21:00')).toHaveTextContent('ConfirmadaAlex')
    expect(await rowAt('21:00–22:00')).toHaveTextContent('BloqueoMantenimiento')
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Cancha 1 · viernes 4 de diciembre de 2026')
    await expectNoA11yViolations(document.body)
  })

  it('offers per row only the actions its state allows, each described by its time', async () => {
    renderPage()

    const free = await rowAt('19:00–20:00')
    const newBooking = within(free).getByRole('button', { name: 'Nueva reserva' })
    expect(newBooking).toHaveAccessibleDescription(/^19:00–20:00/)
    expect(within(free).getByRole('button', { name: 'Bloquear franja' })).toBeInTheDocument()

    const booked = await rowAt('20:00–21:00')
    expect(within(booked).getAllByRole('button').map((button) => button.textContent)).toEqual(['Ver reserva', 'Cancelar reserva'])

    const blocked = await rowAt('21:00–22:00')
    expect(within(blocked).getAllByRole('button').map((button) => button.textContent)).toEqual(['Quitar bloqueo'])
  })

  it('says when the day has no bookings nor blocks', async () => {
    renderPage(createInMemoryStaffBookingsApi({ courts: COURTS }))

    expect(await screen.findByText('No hay reservas ni bloqueos este día.')).toBeInTheDocument()
  })

  it('shows the load error and retries', async () => {
    const api = createInMemoryStaffBookingsApi({ courts: COURTS })
    const listBookings = vi
      .spyOn(api, 'listBookings')
      .mockRejectedValueOnce(new BookingApiError('NETWORK_ERROR', 'offline'))
    const { user } = renderPage(api)

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar las reservas.')
    await user.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('No hay reservas ni bloqueos este día.')).toBeInTheDocument()
    expect(listBookings).toHaveBeenCalledTimes(2)
  })

  it('loads the day of another court and of the next day', async () => {
    const api = createInMemoryStaffBookingsApi({ courts: COURTS })
    const listBookings = vi.spyOn(api, 'listBookings')
    const { user } = renderPage(api)
    await screen.findByText('No hay reservas ni bloqueos este día.')

    await user.selectOptions(screen.getByRole('combobox', { name: 'Cancha' }), 'court-2')
    await user.click(screen.getByRole('button', { name: 'Día siguiente' }))

    expect(await rowAt('19:30–21:00')).toHaveTextContent('Libre')
    expect(listBookings).toHaveBeenLastCalledWith({
      from: '2026-12-05T00:00:00.000Z',
      to: '2026-12-06T00:00:00.000Z',
      courtId: 'court-2',
    })
  })
})

describe('StaffCalendarPage · manual booking (criterion 8)', () => {
  it('creates a confirmed booking from a free slot and shows it in the calendar', async () => {
    const { user } = renderPage()
    await user.click(within(await rowAt('19:00–20:00')).getByRole('button', { name: 'Nueva reserva' }))

    const dialog = screen.getByRole('dialog', { name: 'Nueva reserva manual' })
    expect(within(dialog).getByRole('combobox', { name: 'Hora' })).toHaveValue('19:00')
    await user.type(within(dialog).getByRole('textbox', { name: 'Nombre de quien reserva' }), 'Sam')
    await user.click(within(dialog).getByRole('button', { name: 'Guardar reserva' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(screen.getByRole('status')).toHaveTextContent('Reserva guardada.')
    expect(await rowAt('19:00–20:00')).toHaveTextContent('ConfirmadaSam')
  })

  it('asks for the holder name before saving', async () => {
    const api = createInMemoryStaffBookingsApi({ courts: COURTS })
    const createBooking = vi.spyOn(api, 'createBooking')
    const { user } = renderPage(api)
    await user.click(within(await rowAt('19:00–20:00')).getByRole('button', { name: 'Nueva reserva' }))

    await user.click(screen.getByRole('button', { name: 'Guardar reserva' }))

    expect(screen.getByRole('textbox', { name: 'Nombre de quien reserva' })).toHaveAccessibleDescription(
      'Escribe el nombre de quien reserva.',
    )
    expect(createBooking).not.toHaveBeenCalled()
  })

  it('shows SLOT_UNAVAILABLE on the hour field, described by aria-describedby', async () => {
    const api = createInMemoryStaffBookingsApi({ courts: COURTS })
    vi.spyOn(api, 'createBooking').mockRejectedValueOnce(new BookingApiError('SLOT_UNAVAILABLE', 'taken'))
    const { user } = renderPage(api)
    await user.click(within(await rowAt('19:00–20:00')).getByRole('button', { name: 'Nueva reserva' }))
    const dialog = screen.getByRole('dialog')

    await user.type(within(dialog).getByRole('textbox', { name: 'Nombre de quien reserva' }), 'Sam')
    await user.click(within(dialog).getByRole('button', { name: 'Guardar reserva' }))

    const hour = await within(dialog).findByRole('combobox', { name: 'Hora' })
    await waitFor(() => expect(hour).toHaveAccessibleDescription('Ese horario ya no está disponible. Elige otro.'))
    expect(hour).toHaveAttribute('aria-invalid', 'true')
    expect(dialog).toBeInTheDocument()
    await expectNoA11yViolations(document.body)
  })

  it('disables the form while saving', async () => {
    const api = createInMemoryStaffBookingsApi({ courts: COURTS })
    vi.spyOn(api, 'createBooking').mockReturnValueOnce(new Promise(() => {}))
    const { user } = renderPage(api)
    await user.click(within(await rowAt('19:00–20:00')).getByRole('button', { name: 'Nueva reserva' }))

    await user.type(screen.getByRole('textbox', { name: 'Nombre de quien reserva' }), 'Sam')
    await user.click(screen.getByRole('button', { name: 'Guardar reserva' }))

    expect(screen.getByRole('button', { name: 'Guardando…' })).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByRole('combobox', { name: 'Hora' })).toBeDisabled()
  })
})

describe('StaffCalendarPage · block (criteria 9 and 10)', () => {
  it('blocks a free slot', async () => {
    const { user } = renderPage()
    await user.click(within(await rowAt('19:00–20:00')).getByRole('button', { name: 'Bloquear franja' }))

    await user.type(screen.getByRole('textbox', { name: 'Motivo' }), 'Mantenimiento')
    await user.click(screen.getByRole('button', { name: 'Guardar bloqueo' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Bloqueo guardado.')
    expect(await rowAt('19:00–20:00')).toHaveTextContent('BloqueoMantenimiento')
  })

  it('rejects a block over a booking, lets the staff see and cancel it, and then blocks', async () => {
    const { user, api } = renderPage()
    await user.click(within(await rowAt('19:00–20:00')).getByRole('button', { name: 'Bloquear franja' }))
    const blockDialog = screen.getByRole('dialog', { name: 'Bloquear franja' })
    await user.selectOptions(within(blockDialog).getByRole('combobox', { name: 'Hora' }), '20:00')

    await user.click(within(blockDialog).getByRole('button', { name: 'Guardar bloqueo' }))

    const rejection = await within(blockDialog).findByRole('alert')
    expect(rejection).toHaveTextContent(
      'No se puede bloquear esta franja porque ya tiene una reserva. Cancela primero la reserva y luego bloquea el horario.',
    )
    expect(blockDialog.querySelector('form')).toHaveAccessibleDescription(expect.stringContaining('No se puede bloquear'))
    expect(await api.getBooking(CONFIRMED.id)).toMatchObject({ status: 'CONFIRMED' })

    const viewBooking = within(rejection).getByRole('button', { name: 'Ver reserva' })
    await waitFor(() => expect(viewBooking).toBeEnabled())
    await user.click(viewBooking)
    const detail = screen.getByRole('dialog', { name: 'Detalle de la reserva' })
    expect(detail).toHaveTextContent('Reservada porAlex')
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog', { name: 'Detalle de la reserva' })).not.toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Bloquear franja' })).toBeInTheDocument()
    expect(viewBooking).toHaveFocus()

    await user.click(within(rejection).getByRole('button', { name: 'Cancelar reserva' }))
    const confirm = screen.getByRole('dialog', { name: '¿Cancelar la reserva de Alex?' })
    await expectNoA11yViolations(document.body)
    await user.click(within(confirm).getByRole('button', { name: 'Cancelar reserva' }))

    expect(await within(blockDialog).findByRole('status')).toHaveTextContent('Reserva cancelada')
    await user.click(within(blockDialog).getByRole('button', { name: 'Guardar bloqueo' }))

    expect(await screen.findByText('Bloqueo guardado. La franja ya no está disponible para jugadores.')).toBeInTheDocument()
    expect(await rowAt('20:00–21:00')).toHaveTextContent('Bloqueo')
  })

  it('keeps the booking when the cancellation is not confirmed', async () => {
    const { user, api } = renderPage()

    await user.click(within(await rowAt('20:00–21:00')).getByRole('button', { name: 'Cancelar reserva' }))
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Volver' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(await api.getBooking(CONFIRMED.id)).toMatchObject({ status: 'CONFIRMED' })
  })

  it('asks a generic question for a pending booking, whose holder is not shown', async () => {
    const { user } = renderPage()

    await user.click(within(await rowAt('18:00–19:00')).getByRole('button', { name: 'Cancelar reserva' }))

    expect(screen.getByRole('dialog', { name: '¿Cancelar esta reserva?' })).toBeInTheDocument()
  })
})
