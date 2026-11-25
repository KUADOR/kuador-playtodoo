export const DESIGN_CATALOG_PATH = '/dev/design'

export type Route = 'home' | 'design-catalog'

/** The design catalog only exists in development builds. */
export function resolveRoute(pathname: string, isDevelopment: boolean): Route {
  return isDevelopment && pathname === DESIGN_CATALOG_PATH ? 'design-catalog' : 'home'
}
