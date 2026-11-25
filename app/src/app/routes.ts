export const DESIGN_CATALOG_PATH = '/dev/design'
export const STAFF_CALENDAR_MOCK_PATH = '/dev/staff-calendar'

export type Route = 'home' | 'design-catalog' | 'staff-calendar-mock'

const DEVELOPMENT_ROUTES: Record<string, Route> = {
  [DESIGN_CATALOG_PATH]: 'design-catalog',
  [STAFF_CALENDAR_MOCK_PATH]: 'staff-calendar-mock',
}

/**
 * The design catalog and the staff calendar on the api mock only exist in development builds. The staff
 * calendar is served for real once the courts contract and authentication exist (PLATODO-36).
 */
export function resolveRoute(pathname: string, isDevelopment: boolean): Route {
  return (isDevelopment && DEVELOPMENT_ROUTES[pathname]) || 'home'
}
