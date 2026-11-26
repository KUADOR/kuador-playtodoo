import { useActiveTenant } from '../tenant/screens/activeTenant'
import styles from './HomePage.module.css'

/** Start page of the complex, until its screens arrive with their epics. */
export function HomePage() {
  const { displayName } = useActiveTenant()
  return (
    <main className={styles.page}>
      <h1>{displayName}</h1>
      <p>Reserva. Juega. Disfruta.</p>
    </main>
  )
}
