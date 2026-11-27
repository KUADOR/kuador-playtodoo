import { canAccess, requiresCapability, SUPER_ADMIN_ONLY } from '../../core/auth/access';
import { CurrentPlatformUser } from '../../core/auth/platform-user';
import { TenantStatus } from '../complexes/complexes-api';

/**
 * Whether the user may open a support session on a complex in `tenantStatus` (ADR-020): on an
 * active one with `TENANT_DATA_ACCESS`; on a suspended or cancelled one only a SUPER_ADMIN; never
 * while it is being created or its data is deleted. The api still decides on every request.
 */
export function canOpenSupportSession(
  user: CurrentPlatformUser | null,
  tenantStatus: TenantStatus,
): boolean {
  switch (tenantStatus) {
    case 'ACTIVE':
      return canAccess(user, requiresCapability('TENANT_DATA_ACCESS'));
    case 'SUSPENDED':
    case 'ARCHIVED':
      return canAccess(user, SUPER_ADMIN_ONLY);
    default:
      return false;
  }
}
