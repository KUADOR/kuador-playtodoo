import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, delay, dematerialize, materialize, of, throwError } from 'rxjs';
import { API_BASE_URL, SAME_ORIGIN_API_BASE_URL } from '../../core/api/api-base-url';
import { SUPPORT_SESSION_HEADER } from '../../core/api/api-interceptors';
import { MockPlatformBackend, MockRequest, MockResponse } from './mock-platform-backend';

/** Simulated network latency, so loading states can be seen in the browser. */
const MOCK_LATENCY_MS = 400;
const BEARER_PREFIX = 'Bearer ';
const SUCCESS_LIMIT = 400;

const backend = new MockPlatformBackend();

/**
 * Answers the api calls from `MockPlatformBackend` instead of the network: the global context and,
 * for Kuador support, the staff operations on the host of a complex. Registered only in
 * development builds (mock-backend.development.ts); `npm run start:api` talks to a real api.
 */
export const mockBackendInterceptor: HttpInterceptorFn = (request, next) => {
  const baseUrl = inject(API_BASE_URL);
  if (request.url.startsWith(baseUrl)) {
    return respond(
      request,
      backend.handle(mockRequestOf(request, request.url.slice(baseUrl.length))),
    );
  }
  const tenantCall = tenantCallOf(request);
  if (tenantCall !== null) {
    const { subdomain, path } = tenantCall;
    return respond(
      request,
      backend.handleTenant(
        subdomain,
        mockRequestOf(request, path),
        request.headers.get(SUPPORT_SESSION_HEADER),
      ),
    );
  }
  return next(request);
};

/** `https://{subdomain}.{root}/api/v1/...` with a support session, as the support view calls it. */
function tenantCallOf(request: HttpRequest<unknown>): { subdomain: string; path: string } | null {
  if (!request.headers.has(SUPPORT_SESSION_HEADER) || !URL.canParse(request.url)) {
    return null;
  }
  const url = new URL(request.url);
  if (!url.pathname.startsWith(SAME_ORIGIN_API_BASE_URL)) {
    return null;
  }
  return {
    subdomain: url.hostname.split('.')[0],
    path: url.pathname.slice(SAME_ORIGIN_API_BASE_URL.length),
  };
}

function mockRequestOf(request: HttpRequest<unknown>, path: string): MockRequest {
  const authorization = request.headers.get('Authorization');
  return {
    method: request.method,
    path,
    query: new URLSearchParams(request.params.toString()),
    bearerToken: authorization?.startsWith(BEARER_PREFIX)
      ? authorization.slice(BEARER_PREFIX.length)
      : null,
    body: request.body,
  };
}

function respond(
  request: HttpRequest<unknown>,
  response: MockResponse,
): Observable<HttpResponse<unknown>> {
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
}
