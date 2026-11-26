import { InjectionToken, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';

/** Same-origin prefix used when the backoffice is not served on `admin.{root domain}`. */
export const SAME_ORIGIN_API_BASE_URL = '/api/v1';

const BACKOFFICE_HOST_LABEL = 'admin';
const API_HOST_LABEL = 'api';

export interface HostLocation {
  readonly protocol: string;
  readonly hostname: string;
  readonly port: string;
}

/**
 * Base URL of the global api context. The backoffice is served on `admin.{root}` and the global
 * api on `api.{root}` (infra/proxy/routes.caddy), so `admin.playtodoo.com` resolves to
 * `https://api.playtodoo.com/api/v1`. Any other host (for example `localhost:4200` with the
 * development mock) uses the same origin.
 */
export function resolveApiBaseUrl(location: HostLocation): string {
  const [firstLabel, ...rootLabels] = location.hostname.split('.');
  if (firstLabel !== BACKOFFICE_HOST_LABEL || rootLabels.length === 0) {
    return SAME_ORIGIN_API_BASE_URL;
  }
  const port = location.port ? `:${location.port}` : '';
  return `${location.protocol}//${[API_HOST_LABEL, ...rootLabels].join('.')}${port}${SAME_ORIGIN_API_BASE_URL}`;
}

/** Base URL of the global api context (`…/api/v1`), without a trailing slash. */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => resolveApiBaseUrl(inject(DOCUMENT).location),
});
