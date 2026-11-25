import { CalendarCheck, Check, Clock, Lock } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { EntryState } from '../model/staffSchedule'
import { ENTRY_STATE_TEXT } from '../model/staffText'
import styles from './StaffCalendar.module.css'

const ICONS: Record<EntryState, LucideIcon> = {
  free: CalendarCheck,
  pending: Clock,
  confirmed: Check,
  block: Lock,
}

/** Status label of an entry (Pantallas · componente 1): icon plus text, never only color. */
export function EntryStateTag({ state }: { state: EntryState }) {
  const Icon = ICONS[state]
  return (
    <span className={styles.tag} data-state={state}>
      <Icon className={styles.icon} aria-hidden="true" />
      {ENTRY_STATE_TEXT[state]}
    </span>
  )
}
