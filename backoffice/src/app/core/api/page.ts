/** Paginated response of contracts/platform-backoffice.openapi.yaml (`page` is zero-based). */
export interface Page<T> {
  readonly items: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
}

/** Default page size of the contract. */
export const DEFAULT_PAGE_SIZE = 20;

/** Reference to a platform user (`UserRef` of the contract). */
export interface UserRef {
  readonly id: string;
  readonly displayName: string;
}
