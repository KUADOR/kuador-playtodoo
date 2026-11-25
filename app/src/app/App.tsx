import { lazy, Suspense } from 'react'
import { createHttpTenantApi } from '../tenant/api/httpTenantApi'
import { resolvePlatformUrl } from '../tenant/model/platformUrl'
import { TenantGate } from '../tenant/screens/TenantGate'
import { resolveRoute } from './routes'
import { HomePage } from './HomePage'

// `import.meta.env.DEV` is replaced at build time, so the development screens are left out of production bundles.
const DesignCatalog = import.meta.env.DEV ? lazy(() => import('../dev/design/DesignCatalog')) : null
const StaffCalendarMock = import.meta.env.DEV ? lazy(() => import('../dev/staff-calendar/StaffCalendarMock')) : null
const TenantStateMock = import.meta.env.DEV ? lazy(() => import('../dev/tenant-state/TenantStateMock')) : null

const tenantApi = createHttpTenantApi()

export function App() {
  const route = resolveRoute(window.location.pathname, import.meta.env.DEV)
  const DevelopmentScreen =
    (route === 'design-catalog' && DesignCatalog) ||
    (route === 'staff-calendar-mock' && StaffCalendarMock) ||
    (route === 'tenant-state-mock' && TenantStateMock) ||
    null

  if (DevelopmentScreen) {
    return (
      <Suspense fallback={null}>
        <DevelopmentScreen />
      </Suspense>
    )
  }
  return (
    <TenantGate api={tenantApi} platformUrl={resolvePlatformUrl(import.meta.env.VITE_PLATFORM_URL, window.location)}>
      <HomePage />
    </TenantGate>
  )
}
