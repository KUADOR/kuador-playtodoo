import { useState } from 'react'
import { createInMemoryStaffBookingsApi } from '../../bookings/api/inMemoryStaffBookingsApi'
import type { StaffBooking } from '../../bookings/api/staffBookingsApi'
import type { Court } from '../../bookings/model/court'
import { toInstant, toLocal } from '../../bookings/model/zonedTime'
import { StaffCalendarPage } from '../../bookings/screens/StaffCalendarPage'

// Development-only composition of the staff calendar against the mock of contracts/bookings.openapi.yaml.
// Courts, opening hours and the time zone are sample data until their contract exists ([POR DEFINIR]).

const MOCK_LATENCY_MS = 400

const COURTS: Court[] = [
  { id: '3f0c6a52-6a0e-4c1e-9d2b-1f7d1c2b9a01', name: 'Cancha 1', slotDurationMinutes: 60, opensAt: '08:00', closesAt: '23:00' },
  { id: '7b9e2d14-5c3a-4f6e-8a1b-2c3d4e5f6a7b', name: 'Cancha 2', slotDurationMinutes: 90, opensAt: '09:00', closesAt: '22:30' },
]

function seedFor(today: string, timeZone: string): StaffBooking[] {
  const at = (time: string) => toInstant({ date: today, time }, timeZone)
  const [court1, court2] = COURTS
  return [
    { id: 'seed-pending', courtId: court1.id, kind: 'RESERVATION', status: 'PENDING', start: at('18:00'), end: at('19:00'), holderName: null, blockReason: null },
    { id: 'seed-confirmed', courtId: court1.id, kind: 'RESERVATION', status: 'CONFIRMED', start: at('20:00'), end: at('21:00'), holderName: 'Alex Ejemplo', blockReason: null },
    { id: 'seed-block', courtId: court1.id, kind: 'BLOCK', status: 'CONFIRMED', start: at('09:00'), end: at('10:00'), holderName: null, blockReason: 'Mantenimiento' },
    { id: 'seed-court2', courtId: court2.id, kind: 'RESERVATION', status: 'CONFIRMED', start: at('19:30'), end: at('21:00'), holderName: 'Sam Ejemplo', blockReason: null },
  ]
}

export default function StaffCalendarMock() {
  const [setup] = useState(() => {
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone
    const today = toLocal(new Date().toISOString(), timeZone).date
    const api = createInMemoryStaffBookingsApi({ courts: COURTS, seed: seedFor(today, timeZone), latencyMs: MOCK_LATENCY_MS })
    return { timeZone, today, api }
  })
  return <StaffCalendarPage api={setup.api} courts={COURTS} timeZone={setup.timeZone} today={setup.today} />
}
