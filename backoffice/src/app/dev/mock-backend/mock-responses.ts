import { DEFAULT_PAGE_SIZE, Page } from '../../core/api/page';

/** A request as the development mock sees it (see mock-backend.interceptor.ts). */
export interface MockRequest {
  readonly method: string;
  /** Path after the api base URL, for example `/platform/me`. */
  readonly path: string;
  readonly query: URLSearchParams;
  readonly bearerToken: string | null;
  readonly body: unknown;
}

export interface MockResponse {
  readonly status: number;
  readonly body: unknown;
}

export function ok(body: unknown): MockResponse {
  return { status: 200, body };
}

/** Problem Details (RFC 9457) with the stable `code` of the contracts. */
export function problem(status: number, code: string): MockResponse {
  return {
    status,
    body: {
      type: `https://playtodoo.com/problems/${code.toLowerCase()}`,
      title: code,
      status,
      code,
    },
  };
}

/** One page of `items` for the `page` and `size` query parameters of the contract. */
export function pageOf<T>(items: readonly T[], query: URLSearchParams): Page<T> {
  const page = Number(query.get('page') ?? 0);
  const size = Number(query.get('size') ?? DEFAULT_PAGE_SIZE);
  return {
    items: items.slice(page * size, (page + 1) * size),
    page,
    size,
    totalItems: items.length,
  };
}
