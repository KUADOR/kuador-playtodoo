/** Router paths of the backoffice sections (without the leading slash). */
export const APP_PATHS = {
  login: 'login',
  complexes: 'complexes',
  plans: 'plans',
  operatorPermissions: 'operator-permissions',
  forbidden: 'forbidden',
} as const;

/** Paths under «Complejos» (P3 and the tabs of P4), relative to `APP_PATHS.complexes`. */
export const COMPLEX_PATHS = {
  register: 'new',
  complexIdParam: 'complexId',
  payments: 'payments',
  history: 'history',
} as const;

/** Absolute URL of a section, for `navigateByUrl`, `parseUrl` and `routerLink`. */
export function urlOf(path: (typeof APP_PATHS)[keyof typeof APP_PATHS]): string {
  return `/${path}`;
}

/** Where a signed-in user lands: «Complejos» (P2). */
export const HOME_URL = urlOf(APP_PATHS.complexes);
export const LOGIN_URL = urlOf(APP_PATHS.login);
export const FORBIDDEN_URL = urlOf(APP_PATHS.forbidden);
export const PLANS_URL = urlOf(APP_PATHS.plans);
export const REGISTER_COMPLEX_URL = `${HOME_URL}/${COMPLEX_PATHS.register}`;

/** P4 «Detalle del complejo» (its «Resumen» tab). */
export function complexDetailUrl(complexId: string): string {
  return `${HOME_URL}/${encodeURIComponent(complexId)}`;
}

export function complexPaymentsUrl(complexId: string): string {
  return `${complexDetailUrl(complexId)}/${COMPLEX_PATHS.payments}`;
}

export function complexHistoryUrl(complexId: string): string {
  return `${complexDetailUrl(complexId)}/${COMPLEX_PATHS.history}`;
}
