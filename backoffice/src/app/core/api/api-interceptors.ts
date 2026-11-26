import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { LOGIN_URL } from '../app-paths';
import { Session } from '../auth/session';
import { API_BASE_URL } from './api-base-url';
import { toApiError } from './api-error';

const AUTHORIZATION_HEADER = 'Authorization';

/** Sends the platform access token, only to the api. */
export const accessTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(Session).accessToken();
  if (token === null || !request.url.startsWith(inject(API_BASE_URL))) {
    return next(request);
  }
  return next(request.clone({ setHeaders: { [AUTHORIZATION_HEADER]: `Bearer ${token}` } }));
};

/**
 * Turns every failure into an `ApiError` (RFC 9457 `code`) and applies the common rules of
 * «Pantallas»:
 * - 401 on a signed-in request: the session expired; back to the login with that notice.
 * - 403 `CAPABILITY_REVOKED`: reload `GET /me` so the menu and the buttons follow the change.
 * Screens still get the error to show their own text (for example on 409 `STALE_STATE`).
 */
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(Session);
  const router = inject(Router);
  return next(request).pipe(
    catchError((failure: unknown) => {
      if (!(failure instanceof HttpErrorResponse)) {
        return throwError(() => failure);
      }
      const error = toApiError(failure);
      if (error.code === 'UNAUTHENTICATED' && request.headers.has(AUTHORIZATION_HEADER)) {
        session.expire();
        void router.navigateByUrl(LOGIN_URL);
      } else if (error.code === 'CAPABILITY_REVOKED') {
        void session.refreshUser();
      }
      return throwError(() => error);
    }),
  );
};
