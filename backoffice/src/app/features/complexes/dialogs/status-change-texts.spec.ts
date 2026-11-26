import { ACTIVE_COMPLEX, suspendedComplex } from '../../../../testing/complex-fixtures';
import { statusChangeOptions } from './status-change-texts';

describe('statusChangeOptions (P5 «Cambiar estado»)', () => {
  it('offers only the transitions the api allows, in its order', () => {
    expect(statusChangeOptions(ACTIVE_COMPLEX).map((option) => option.target)).toEqual([
      'EXPIRING',
      'SUSPENDED',
      'CANCELLED',
    ]);
    expect(statusChangeOptions({ ...ACTIVE_COMPLEX, allowedTransitions: [] })).toEqual([]);
  });

  it('says what suspending does to access, bookings and data (CA8, CA9)', () => {
    const suspend = statusChangeOptions(ACTIVE_COMPLEX).find((o) => o.target === 'SUSPENDED');

    expect(suspend).toEqual(
      expect.objectContaining({
        confirmLabel: 'Suspender complejo',
        confirmVariant: 'danger',
        effect: expect.stringContaining(
          'Nadie del complejo podrá entrar y los jugadores no lo verán ni podrán reservar. Las reservas futuras quedan inactivas.',
        ),
      }),
    );
  });

  it('reactivates a suspended complex and keeps the running deadline when cancelling it', () => {
    const [reactivate, cancel] = statusChangeOptions(suspendedComplex(10));

    expect(reactivate.confirmLabel).toBe('Reactivar complejo');
    expect(reactivate.effect).toBe(
      'El personal recupera el acceso, el complejo vuelve a mostrarse y las reservas conservadas se activan.',
    );
    expect(cancel).toEqual(
      expect.objectContaining({
        confirmLabel: 'Cancelar suscripción',
        closeLabel: 'Volver',
        effect: expect.stringContaining('Se mantiene el plazo de conservación'),
      }),
    );
  });

  it('says that «Por vencer» does not change the access', () => {
    const expiring = statusChangeOptions(ACTIVE_COMPLEX)[0];

    expect(expiring.effect).toBe('No cambia el acceso del complejo.');
    expect(expiring.confirmVariant).toBe('primary');
  });
});
