import { Capability, CurrentPlatformUser, PlatformRole } from './platform-user';

/**
 * What a section or action requires. The backoffice only hides what the user cannot do; the api
 * checks every request (ADR-017).
 */
export type AccessRule =
  | { readonly kind: 'signed-in' }
  | { readonly kind: 'role'; readonly role: PlatformRole }
  | { readonly kind: 'capability'; readonly capability: Capability };

export const SIGNED_IN: AccessRule = { kind: 'signed-in' };
export const SUPER_ADMIN_ONLY: AccessRule = { kind: 'role', role: 'SUPER_ADMIN' };

export function requiresCapability(capability: Capability): AccessRule {
  return { kind: 'capability', capability };
}

/** `GET /me` already lists every capability for a SUPER_ADMIN and the enabled ones for an OPERATOR. */
export function canAccess(user: CurrentPlatformUser | null, rule: AccessRule): boolean {
  if (user === null) {
    return false;
  }
  switch (rule.kind) {
    case 'signed-in':
      return true;
    case 'role':
      return user.role === rule.role;
    case 'capability':
      return user.capabilities.includes(rule.capability);
  }
}
