import { OPERATOR_USER, SUPER_ADMIN_USER } from '../../../testing/platform-fixtures';
import { SIGNED_IN, SUPER_ADMIN_ONLY, canAccess, requiresCapability } from './access';

describe('canAccess', () => {
  it('denies everything without a user', () => {
    expect(canAccess(null, SIGNED_IN)).toBe(false);
  });

  it('allows the SUPER_ADMIN-only sections to the SUPER_ADMIN only (CA15)', () => {
    expect(canAccess(SUPER_ADMIN_USER, SUPER_ADMIN_ONLY)).toBe(true);
    expect(canAccess(OPERATOR_USER, SUPER_ADMIN_ONLY)).toBe(false);
  });

  it('allows an operator only the capabilities that GET /me lists (CA16)', () => {
    expect(canAccess(OPERATOR_USER, requiresCapability('SUBSCRIPTION_PAYMENT_RECORD'))).toBe(true);
    expect(canAccess(OPERATOR_USER, requiresCapability('COMPLEX_CREATE'))).toBe(false);
    expect(canAccess(SUPER_ADMIN_USER, requiresCapability('COMPLEX_CREATE'))).toBe(true);
  });
});
