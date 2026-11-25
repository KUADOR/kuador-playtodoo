// Destination of «Ir a Playtodoo»: the root domain of the environment (`playtodoo.root-domain` in the api).

export interface HostLocation {
  protocol: string
  hostname: string
  port: string
}

/**
 * The URL configured in `VITE_PLATFORM_URL` or, when it is not set, the current host without its first label
 * (`elsol.playtodoo.com` → `playtodoo.com`, `elsol.localhost:5173` → `localhost:5173`), which is the root
 * domain because tenant hosts are always `{subdomain}.{root domain}`.
 */
export function resolvePlatformUrl(configuredUrl: string | undefined, location: HostLocation): string {
  if (configuredUrl) return configuredUrl
  const labels = location.hostname.split('.')
  const rootHostname = labels.length > 1 ? labels.slice(1).join('.') : location.hostname
  const port = location.port ? `:${location.port}` : ''
  return `${location.protocol}//${rootHostname}${port}/`
}
