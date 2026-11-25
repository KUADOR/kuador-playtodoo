import type { LocalTime } from '../../shared/ui'

// What the staff calendar needs to know about a court. Its source is the «Complejos y canchas» module,
// whose contract is [POR DEFINIR: endpoint de canchas, duración y horario de apertura]; until then the
// screen receives the courts from its composition root.

export interface Court {
  id: string
  name: string
  /** Slot length configured by the owner. Integer greater than 0. */
  slotDurationMinutes: number
  /** First slot start, local time of the complex. */
  opensAt: LocalTime
  /** No slot ends after this local time. */
  closesAt: LocalTime
}

const MINUTES_PER_HOUR = 60

export function toMinutes(time: LocalTime): number {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * MINUTES_PER_HOUR + minutes
}

export function fromMinutes(totalMinutes: number): LocalTime {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${pad(Math.floor(totalMinutes / MINUTES_PER_HOUR))}:${pad(totalMinutes % MINUTES_PER_HOUR)}`
}

/** Every valid slot start of the court in one day: from `opensAt`, one every `slotDurationMinutes`, ending by `closesAt`. */
export function slotStartsOf(court: Court): LocalTime[] {
  const starts: LocalTime[] = []
  const closing = toMinutes(court.closesAt)
  for (let start = toMinutes(court.opensAt); start + court.slotDurationMinutes <= closing; start += court.slotDurationMinutes) {
    starts.push(fromMinutes(start))
  }
  return starts
}
