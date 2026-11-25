import { lazy, Suspense } from 'react'
import { resolveRoute } from './routes'
import { HomePage } from './HomePage'

// `import.meta.env.DEV` is replaced at build time, so the development screens are left out of production bundles.
const DesignCatalog = import.meta.env.DEV ? lazy(() => import('../dev/design/DesignCatalog')) : null
const StaffCalendarMock = import.meta.env.DEV ? lazy(() => import('../dev/staff-calendar/StaffCalendarMock')) : null

export function App() {
  const route = resolveRoute(window.location.pathname, import.meta.env.DEV)
  const DevelopmentScreen =
    (route === 'design-catalog' && DesignCatalog) || (route === 'staff-calendar-mock' && StaffCalendarMock) || null

  if (DevelopmentScreen) {
    return (
      <Suspense fallback={null}>
        <DevelopmentScreen />
      </Suspense>
    )
  }
  return <HomePage />
}
