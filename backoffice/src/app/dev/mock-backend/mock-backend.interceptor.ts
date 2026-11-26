import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { delay, dematerialize, materialize, of, throwError } from 'rxjs';
import { API_BASE_URL } from '../../core/api/api-base-url';
import { MockPlatformBackend } from './mock-platform-backend';

/** Simulated network latency, so loading states can be seen in the browser. */
const MOCK_LATENCY_MS = 400;
const BEARER_PREFIX = 'Bearer ';
const SUCCESS_LIMIT = 400;

const backend = new MockPlatformBackend();

/**
 * Answers the api calls from `MockPlatformBackend` instead of the network. Registered only in
 * development builds (mock-backend.development.ts); `npm run start:api` talks to a real api.
 */
export const mockBackendInterceptor: HttpInterceptorFn = (request, next) => {
  const baseUrl = inject(API_BASE_URL);
  if (!request.url.startsWith(baseUrl)) {
    return next(request);
  }
  const authorization = request.headers.get('Authorization');
  const response = backend.handle({
    method: request.method,
    path: request.url.slice(baseUrl.length),
    query: new URLSearchParams(request.params.toString()),
    bearerToken: authorization?.startsWith(BEARER_PREFIX)
      ? authorization.slice(BEARER_PREFIX.length)
      : null,
    body: request.body,
  });
  const outcome =
    response.status < SUCCESS_LIMIT
      ? of(new HttpResponse({ status: response.status, body: response.body, url: request.url }))
      : throwError(
          () =>
            new HttpErrorResponse({
              status: response.status,
              error: response.body,
              url: request.url,
            }),
        );
  // materialize/dematerialize lets the error be delayed like a successful response.
  return outcome.pipe(materialize(), delay(MOCK_LATENCY_MS), dematerialize());
};
