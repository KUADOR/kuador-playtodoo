/** `PlatformRole` of contracts/platform-backoffice.openapi.yaml. */
export type PlatformRole = 'SUPER_ADMIN' | 'OPERATOR';

/** `Capability` of contracts/platform-backoffice.openapi.yaml (ADR-017). */
export type Capability =
  | 'COMPLEX_CREATE'
  | 'SUBSCRIPTION_PAYMENT_RECORD'
  | 'SUBSCRIPTION_STATUS_CHANGE'
  | 'TENANT_DATA_ACCESS';

/** `CurrentPlatformUser` (`GET /platform/me`): who is signed in and what they may do. */
export interface CurrentPlatformUser {
  readonly id: string;
  readonly displayName: string;
  readonly role: PlatformRole;
  readonly capabilities: readonly Capability[];
}

/** Spanish names of the roles, as «Pantallas» shows them. */
export const ROLE_LABELS: Record<PlatformRole, string> = {
  SUPER_ADMIN: 'Super administrador',
  OPERATOR: 'Operador',
};
