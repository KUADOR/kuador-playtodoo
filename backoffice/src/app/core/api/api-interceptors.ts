import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { LOGIN_URL } from '../app-paths';
import { Session } from '../auth/session';
import { API_BASE_URL } from './api-base-url';
import { toApiError } from './api-error';

const AUTHORIZATION_HEADER = 'Authorization';

/**
 * Header with the id of a support session (ADR-020). Requests to the host of a complex carry it
 * together with the platform token; the host they go to is the `tenantHost` the api returned.
 */
export const SUPPORT_SESSION_HEADER = 'X-Support-Session';

/**
 * Sends the platform access token only to the api: the global context, and the host of a complex
 * when the request carries a support session.
 */
export const accessTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(Session).accessToken();
  const toApi =
    request.url.startsWith(inject(API_BASE_URL)) || request.headers.has(SUPPORT_SESSION_HEADER);
  if (token === null || !toApi) {
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
