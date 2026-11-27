import { InjectionToken, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { BACKOFFICE_HOST_LABEL, HostLocation } from './api-base-url';

/**
 * Root domain under which every complex gets its subdomain (`playtodoo.root-domain` of the api,
 * configured per environment). The backoffice is served on `admin.{root}`, so the root comes from
 * its own host: `admin.playtodoo.com` gives `playtodoo.com`. On any other host (for example
 * `localhost` with the development mock) the host itself is the root, as in `{subdomain}.localhost`.
 */
export function resolveRootDomain(location: Pick<HostLocation, 'hostname'>): string {
  const [firstLabel, ...rootLabels] = location.hostname.split('.');
  if (firstLabel !== BACKOFFICE_HOST_LABEL || rootLabels.length === 0) {
    return location.hostname;
  }
  return rootLabels.join('.');
}

export const ROOT_DOMAIN = new InjectionToken<string>('ROOT_DOMAIN', {
  providedIn: 'root',
  factory: () => resolveRootDomain(inject(DOCUMENT).location),
});

/** Host of a complex, for example `canchas-el-sol.playtodoo.com`. */
export function tenantHost(subdomain: string, rootDomain: string): string {
  return `${subdomain}.${rootDomain}`;
}
