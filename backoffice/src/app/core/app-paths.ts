/** Router paths of the backoffice sections (without the leading slash). */
export const APP_PATHS = {
  login: 'login',
  complexes: 'complexes',
  plans: 'plans',
  newPlan: 'plans/new',
  planFields: 'plans/fields',
  operatorPermissions: 'operator-permissions',
  forbidden: 'forbidden',
} as const;

/** Absolute URL of a section, for `navigateByUrl`, `parseUrl` and `routerLink`. */
export function urlOf(path: (typeof APP_PATHS)[keyof typeof APP_PATHS]): string {
  return `/${path}`;
}

/** Where a signed-in user lands: «Complejos» (P2). */
export const HOME_URL = urlOf(APP_PATHS.complexes);
export const LOGIN_URL = urlOf(APP_PATHS.login);
export const FORBIDDEN_URL = urlOf(APP_PATHS.forbidden);
