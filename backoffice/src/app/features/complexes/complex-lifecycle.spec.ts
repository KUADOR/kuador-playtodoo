import {
  ACTIVE_COMPLEX,
  PROVISIONING_FAILED_COMPLEX,
  PURGED_COMPLEX,
  suspendedComplex,
} from '../../../testing/complex-fixtures';
import { OPERATOR_USER, SUPER_ADMIN_USER } from '../../../testing/platform-fixtures';
import { complexActions, lifecycleNotices } from './complex-lifecycle';

const NOW = new Date('2027-02-01T12:00:00Z');

describe('lifecycleNotices (P4 fixed notices)', () => {
  it('has none for an active complex', () => {
    expect(lifecycleNotices(ACTIVE_COMPLEX, NOW)).toEqual([]);
  });

  it('says a suspended complex blocks access, keeps bookings and when its data is deleted', () => {
    const notices = lifecycleNotices(suspendedComplex(20, NOW), NOW);

    expect(notices.map((notice) => notice.kind)).toEqual(['suspended', 'purge-scheduled']);
    expect(notices[0].text).toMatch(
      /^Complejo suspendido desde el \d{2}\/\d{2}\/2027\. Ni su personal ni los jugadores pueden acceder\. Las reservas futuras se conservan inactivas\.$/,
    );
    expect(notices[1]).toEqual(
      expect.objectContaining({
        tone: 'warning',
        text: expect.stringMatching(
          /^Si no se reactiva, todos sus datos se borrarán a partir del \d{2}\/\d{2}\/2027 \d{2}:\d{2} \(faltan 20 días\)\. No se puede deshacer\.$/,
        ),
      }),
    );
  });

  it('turns the purge notice into an error with 7 days or fewer left', () => {
    expect(lifecycleNotices(suspendedComplex(7, NOW), NOW)[1].tone).toBe('error');
    expect(lifecycleNotices(suspendedComplex(1, NOW), NOW)[1].text).toContain('(falta 1 día)');
  });

  it('says a cancelled complex cannot be reactivated, without «Si no se reactiva»', () => {
    const cancelled = {
      ...suspendedComplex(30, NOW),
      tenantStatus: 'ARCHIVED' as const,
      archivedAt: NOW.toISOString(),
    };

    const notices = lifecycleNotices(cancelled, NOW);

    expect(notices[0].text).toContain('ya no se puede reactivar');
    expect(notices[1].text).toMatch(/^Todos sus datos se borrarán/);
  });

  it('shows only «Borrado de datos en curso» once the deadline passed or while purging', () => {
    expect(lifecycleNotices(suspendedComplex(-1, NOW), NOW)).toEqual([
      expect.objectContaining({
        kind: 'purging',
        text: 'Borrado de datos en curso. Ya no se puede reactivar.',
      }),
    ]);
    expect(
      lifecycleNotices({ ...ACTIVE_COMPLEX, tenantStatus: 'PURGING' }, NOW).map((n) => n.kind),
    ).toEqual(['purging']);
  });

  it('says when the data of a purged complex was deleted', () => {
    expect(lifecycleNotices(PURGED_COMPLEX, NOW)).toEqual([
      expect.objectContaining({
        kind: 'purged',
        text: expect.stringMatching(
          /^Los datos de este complejo se borraron el \d{2}\/02\/2027\.$/,
        ),
      }),
    ]);
  });

  it('marks an incomplete registration', () => {
    expect(lifecycleNotices(PROVISIONING_FAILED_COMPLEX, NOW)).toEqual([
      expect.objectContaining({
        kind: 'provisioning-failed',
        text: 'No se terminó de crear el complejo.',
      }),
    ]);
  });
});

describe('complexActions (what P4 shows)', () => {
  it('gives the super admin every action that fits the status', () => {
    expect(complexActions(SUPER_ADMIN_USER, ACTIVE_COMPLEX, NOW)).toEqual({
      recordPayment: true,
      changeStatus: true,
      changePlan: true,
      changeRetention: false,
      retryRegistration: false,
    });
    expect(complexActions(SUPER_ADMIN_USER, suspendedComplex(10, NOW), NOW).changeRetention).toBe(
      true,
    );
    expect(
      complexActions(SUPER_ADMIN_USER, PROVISIONING_FAILED_COMPLEX, NOW).retryRegistration,
    ).toBe(true);
  });

  it('gives an operator only the enabled capabilities and never plan or retention (CA15)', () => {
    expect(complexActions(OPERATOR_USER, suspendedComplex(10, NOW), NOW)).toEqual({
      recordPayment: true,
      changeStatus: false,
      changePlan: false,
      changeRetention: false,
      retryRegistration: false,
    });
  });

  it('allows nothing once the data is being deleted or was deleted', () => {
    for (const complex of [PURGED_COMPLEX, suspendedComplex(-1, NOW)]) {
      expect(Object.values(complexActions(SUPER_ADMIN_USER, complex, NOW))).not.toContain(true);
    }
  });

  it('does not offer «Cambiar plan» on a cancelled subscription', () => {
    const cancelled = {
      ...ACTIVE_COMPLEX,
      tenantStatus: 'ARCHIVED' as const,
      subscription: { ...ACTIVE_COMPLEX.subscription, status: 'CANCELLED' as const },
    };

    expect(complexActions(SUPER_ADMIN_USER, cancelled, NOW).changePlan).toBe(false);
  });
});
