import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { FORBIDDEN_URL, HOME_URL, LOGIN_URL } from '../app-paths';
import { AccessRule, canAccess } from './access';
import { Session } from './session';

// Every dependency is injected before the first `await`: the injection context ends there.

/** Lets a signed-in user through; anyone else goes to the login. */
export const signedInGuard: CanActivateFn = async () => {
  const router = inject(Router);
  const user = await inject(Session).ensureUser();
  return user !== null || router.parseUrl(LOGIN_URL);
};

/** The login is only for people without a session: a signed-in user goes home. */
export const signedOutGuard: CanActivateFn = async () => {
  const router = inject(Router);
  const user = await inject(Session).ensureUser();
  return user === null || router.parseUrl(HOME_URL);
};

/**
 * Lets the user through only if `GET /me` allows `rule`. A direct address without permission
 * goes to the page «No tienes permiso para ver esta sección.».
 */
export function accessGuard(rule: AccessRule): CanActivateFn {
  return async () => {
    const router = inject(Router);
    const user = await inject(Session).ensureUser();
    if (user === null) {
      return router.parseUrl(LOGIN_URL);
    }
    return canAccess(user, rule) || router.parseUrl(FORBIDDEN_URL);
  };
}
