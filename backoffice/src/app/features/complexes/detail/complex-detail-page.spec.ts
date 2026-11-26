import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Routes, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import {
  ACTIVE_COMPLEX,
  BASIC_PLAN,
  COMPLEX_URL,
  EMPTY_PAGE,
  HISTORY_ENTRIES,
  PLANS_URL,
  PREMIUM_PLAN,
  PROVISIONING_FAILED_COMPLEX,
  PURGED_COMPLEX,
  RECORDED_PAYMENT,
  pageWith,
  suspendedComplex,
} from '../../../../testing/complex-fixtures';
import {
  OPERATOR_USER,
  SUPER_ADMIN_USER,
  problemBody,
} from '../../../../testing/platform-fixtures';
import { provideTestApi, settle, signInAs } from '../../../../testing/platform-testing';
import { ROOT_DOMAIN } from '../../../core/api/root-domain';
import { CurrentPlatformUser } from '../../../core/auth/platform-user';
import { todayAsCalendarDate } from '../../../core/format/date-format';
import { ComplexDetail, Payment } from '../complexes-api';
import { ComplexDetailPage } from './complex-detail-page';
import { PROVISIONING_POLL_MS } from './complex-detail-store';
import { ComplexHistoryTab } from './complex-history-tab';
import { ComplexPaymentsTab } from './complex-payments-tab';
import { ComplexSummaryTab } from './complex-summary-tab';

const ROUTES: Routes = [
  {
    path: 'complexes/:complexId',
    component: ComplexDetailPage,
    children: [
      { path: '', component: ComplexSummaryTab },
      { path: 'payments', component: ComplexPaymentsTab },
      { path: 'history', component: ComplexHistoryTab },
    ],
  },
];

const PAYMENTS_URL = `${COMPLEX_URL}/payments`;
const HISTORY_URL = `${COMPLEX_URL}/history`;

describe('ComplexDetailPage (P4, P5, P6)', () => {
  let harness: RouterTestingHarness;
  let backend: HttpTestingController;

  beforeEach(async () => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideTestApi(),
        provideRouter(ROUTES),
        { provide: ROOT_DOMAIN, useValue: 'playtodoo.com' },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
    document.body.appendChild(harness.fixture.nativeElement);
  });

  afterEach(() => {
    backend.verify();
    document.body.replaceChildren();
  });

  const root = (): HTMLElement => harness.fixture.nativeElement;
  const text = (element: Element | null | undefined) =>
    element?.textContent?.replace(/\s+/g, ' ').trim();
  /** Text of each child joined with spaces, for rows of «label value» cells. */
  const joined = (element: Element) => Array.from(element.children).map(text).join(' ');
  const summaryRows = () => Array.from(root().querySelectorAll('.summary__row')).map(joined);
  const buttons = (container: Element = root()) =>
    Array.from(container.querySelectorAll('button')).map((button) => text(button));
  const button = (label: string, container: Element = root()) =>
    Array.from(container.querySelectorAll('button')).find(
      (candidate) => text(candidate) === label,
    ) as HTMLButtonElement;
  const dialog = () => root().querySelector('[role="dialog"]') as HTMLElement | null;
  const control = (label: string) => {
    const element = Array.from(root().querySelectorAll('label')).find(
      (candidate) => text(candidate) === label,
    );
    return root().querySelector(`#${element?.htmlFor}`) as HTMLInputElement | HTMLSelectElement;
  };

  async function type(label: string, value: string): Promise<void> {
    const element = control(label);
    element.value = value;
    element.dispatchEvent(new Event(element instanceof HTMLSelectElement ? 'change' : 'input'));
    await settle();
  }

  function expectLastPayment(): TestRequest {
    const request = backend.expectOne((candidate) => candidate.url === PAYMENTS_URL);
    expect(request.request.params.get('size')).toBe('1');
    return request;
  }

  /** Opens P4 on `tab` as `user`, answering the detail with `complex`. */
  async function open(
    complex: ComplexDetail,
    user: CurrentPlatformUser = SUPER_ADMIN_USER,
    tab = '',
  ): Promise<void> {
    await signInAs(user);
    await harness.navigateByUrl(`/complexes/${complex.id}${tab ? `/${tab}` : ''}`);
    backend.expectOne(COMPLEX_URL).flush(complex);
    await settle();
  }

  async function openSummary(
    complex: ComplexDetail,
    user: CurrentPlatformUser = SUPER_ADMIN_USER,
    lastPayment: Payment | null = RECORDED_PAYMENT,
  ): Promise<void> {
    await open(complex, user);
    expectLastPayment().flush(pageWith(lastPayment ? [lastPayment] : []));
    await settle();
  }

  describe('P4 · Detalle', () => {
    it('shows a skeleton, then the complex with its tag, host and summary', async () => {
      await signInAs(SUPER_ADMIN_USER);
      await harness.navigateByUrl(`/complexes/${ACTIVE_COMPLEX.id}`);
      expect(root().querySelector('[aria-busy="true"]')?.textContent).toContain('Cargando…');
      backend.expectOne(COMPLEX_URL).flush(ACTIVE_COMPLEX);
      await settle();
      expectLastPayment().flush(pageWith([RECORDED_PAYMENT]));
      await settle();

      expect(text(root().querySelector('h1'))).toBe('Canchas El Sol');
      expect(text(root().querySelector('app-status-tag'))).toBe('Activa');
      expect(text(root().querySelector('.detail__host'))).toBe('canchas-el-sol.playtodoo.com');
      expect(document.title).toBe('Canchas El Sol · Backoffice de Kuador');
      expect(text(root().querySelector('#summary-plan-heading'))).toBe('Plan · Premium');
      expect(summaryRows()).toEqual([
        'Precio 120.00 USD',
        'Periodicidad Mensual',
        'Límite de canchas 10',
        'Límite de usuarios 25',
        'support-hours Weekdays 9 to 18',
        expect.stringMatching(/^Asignado el \d{2}\/12\/2026 \d{2}:\d{2}$/),
        'Estado Activa',
        expect.stringMatching(/^Desde \d{2}\/12\/2026/),
        'Responsable Sam Example',
        'Último pago 01/12/2026 · 120.00 USD',
        'Correo del propietario owner@example.com',
        expect.stringMatching(/^Alta \d{2}\/12\/2026/),
      ]);
    });

    it('shows every action to the super admin', async () => {
      await openSummary(ACTIVE_COMPLEX);

      expect(buttons(root().querySelector('.detail__actions') as Element)).toEqual([
        'Registrar pago',
        'Cambiar estado',
        'Cambiar plan',
      ]);
    });

    it('shows an operator only what is enabled for them (CA15)', async () => {
      await openSummary(ACTIVE_COMPLEX, OPERATOR_USER, null);

      expect(buttons(root().querySelector('.detail__actions') as Element)).toEqual([
        'Registrar pago',
      ]);
      expect(summaryRows()).toContain('Último pago Sin pagos');
    });

    it('warns about a suspended complex and its purge date, as an error with 7 days or less', async () => {
      await openSummary(suspendedComplex(5));

      const alerts = Array.from(root().querySelectorAll('app-alert')).map(text);
      expect(alerts[0]).toMatch(
        /^Complejo suspendido desde el \d{2}\/\d{2}\/\d{4}\. Ni su personal ni los jugadores pueden acceder\. Las reservas futuras se conservan inactivas\.$/,
      );
      expect(alerts[1]).toMatch(
        /^Si no se reactiva, todos sus datos se borrarán a partir del .+ \(faltan 5 días\)\. No se puede deshacer\. Cambiar plazo de conservación$/,
      );
      expect(root().querySelectorAll('app-alert')[1].getAttribute('role')).toBe('alert');
    });

    it('does not offer «Cambiar plazo de conservación» to an operator', async () => {
      await openSummary(suspendedComplex(20), OPERATOR_USER);

      expect(root().querySelectorAll('app-alert')[1].getAttribute('role')).toBe('status');
      expect(button('Cambiar plazo de conservación')).toBeUndefined();
    });

    it('shows a purged complex without management actions', async () => {
      await openSummary(PURGED_COMPLEX, SUPER_ADMIN_USER, null);

      expect(text(root().querySelector('app-status-tag'))).toBe('Datos eliminados');
      expect(text(root().querySelector('.detail__host'))).toBe('—');
      expect(root().textContent).toMatch(
        /Los datos de este complejo se borraron el \d{2}\/02\/2027\./,
      );
      expect(root().querySelector('.detail__actions')).toBeNull();
      expect(summaryRows()).toContain('Correo del propietario —');
    });

    it('says when the complex does not exist', async () => {
      await signInAs(SUPER_ADMIN_USER);
      await harness.navigateByUrl(`/complexes/${ACTIVE_COMPLEX.id}`);
      backend
        .expectOne(COMPLEX_URL)
        .flush(problemBody(404, 'COMPLEX_NOT_FOUND'), { status: 404, statusText: 'Not Found' });
      await settle();

      expect(text(root().querySelector('[role="alert"]'))).toBe(
        'No encontramos este complejo. Ir a Complejos',
      );
    });

    it('retries a failed load', async () => {
      await signInAs(SUPER_ADMIN_USER);
      await harness.navigateByUrl(`/complexes/${ACTIVE_COMPLEX.id}`);
      backend.expectOne(COMPLEX_URL).error(new ProgressEvent('error'));
      await settle();
      expect(text(root().querySelector('[role="alert"]'))).toContain(
        'No pudimos cargar la información.',
      );

      button('Reintentar').click();
      await settle();
      backend.expectOne(COMPLEX_URL).flush(ACTIVE_COMPLEX);
      await settle();
      expectLastPayment().flush(EMPTY_PAGE);
      await settle();
      expect(text(root().querySelector('h1'))).toBe('Canchas El Sol');
    });

    it('retries an incomplete registration and follows it until it is active (CA1)', async () => {
      await openSummary(PROVISIONING_FAILED_COMPLEX, SUPER_ADMIN_USER, null);
      expect(text(root().querySelector('app-status-tag'))).toBe('Alta incompleta');
      expect(root().textContent).toContain('No se terminó de crear el complejo.');

      button('Reintentar alta').click();
      await settle();
      backend
        .expectOne({ method: 'POST', url: `${COMPLEX_URL}/provisioning-retries` })
        .flush({ ...PROVISIONING_FAILED_COMPLEX, tenantStatus: 'PROVISIONING', version: 2 });
      await settle();
      expect(root().textContent).toContain('Creando el espacio del complejo…');

      await new Promise((resolve) => setTimeout(resolve, PROVISIONING_POLL_MS + 100));
      backend.expectOne(COMPLEX_URL).flush(ACTIVE_COMPLEX);
      await settle();
      expect(text(root().querySelector('app-status-tag'))).toBe('Activa');
      expect(root().textContent).not.toContain('Creando el espacio del complejo…');
    });

    it('has no axe violations', async () => {
      await openSummary(suspendedComplex(5));

      await expectNoAxeViolations(root());
    });
  });

  describe('P5 · Registrar pago', () => {
    it('validates, records the payment and refreshes the last payment (CA7)', async () => {
      await openSummary(ACTIVE_COMPLEX, SUPER_ADMIN_USER, null);
      button('Registrar pago').click();
      await settle();

      expect(text(dialog()?.querySelector('h2'))).toBe('Registrar pago · Canchas El Sol');
      expect(document.activeElement).toBe(control('Importe (USD)'));
      expect(dialog()?.textContent).toContain(
        'Solo registra un pago recibido fuera de la plataforma; no cobra nada.',
      );
      button('Registrar pago', dialog() as Element).click();
      await settle();
      expect(dialog()?.textContent).toContain('Escribe el importe.');
      backend.expectNone(PAYMENTS_URL);

      await type('Importe (USD)', '100,5');
      await type('Medio', 'INVOICE');
      button('Registrar pago', dialog() as Element).click();
      await settle();
      const request = backend.expectOne({ method: 'POST', url: PAYMENTS_URL });
      expect(request.request.body).toEqual({
        amount: '100.5',
        paidOn: todayAsCalendarDate(),
        method: 'INVOICE',
      });
      request.flush(
        { ...RECORDED_PAYMENT, amount: '100.5' },
        { status: 201, statusText: 'Created' },
      );
      await settle();

      expect(dialog()).toBeNull();
      expect(text(root().querySelector('app-alert[data-tone="success"]'))).toContain(
        'Registramos el pago de 100.5 USD.',
      );
      expectLastPayment().flush(pageWith([RECORDED_PAYMENT]));
    });

    it('shows the api field errors inside the dialog and keeps what was typed', async () => {
      await openSummary(ACTIVE_COMPLEX, SUPER_ADMIN_USER, null);
      button('Registrar pago').click();
      await settle();
      await type('Importe (USD)', '50');

      button('Registrar pago', dialog() as Element).click();
      await settle();
      backend.expectOne({ method: 'POST', url: PAYMENTS_URL }).flush(
        {
          ...problemBody(422, 'VALIDATION_FAILED'),
          fieldErrors: [{ field: 'paidOn', code: 'DATE_IN_FUTURE' }],
        },
        { status: 422, statusText: 'Unprocessable' },
      );
      await settle();

      expect(dialog()?.textContent).toContain('La fecha del pago no puede ser futura.');
      expect(control('Importe (USD)').value).toBe('50');
    });
  });

  describe('P5 · Cambiar estado', () => {
    it('suspends with the effect explained and shows the result (CA6, CA8, CA9)', async () => {
      await openSummary(ACTIVE_COMPLEX);
      button('Cambiar estado').click();
      await settle();

      expect(text(dialog()?.querySelector('.dialog-form__current'))).toBe('Estado actual: Activa');
      const options = Array.from(dialog()?.querySelectorAll('.dialog-form__option-label') ?? []);
      expect(options.map(text)).toEqual(['Por vencer', 'Suspendida', 'Cancelada']);
      expect(dialog()?.textContent).toContain(
        'La fecha y tu nombre quedan registrados en el historial.',
      );

      (dialog()?.querySelectorAll('input[type="radio"]')[1] as HTMLInputElement).click();
      await settle();
      expect(dialog()?.textContent).toContain(
        'Nadie del complejo podrá entrar y los jugadores no lo verán ni podrán reservar. Las reservas futuras quedan inactivas.',
      );
      button('Suspender complejo', dialog() as Element).click();
      await settle();
      const request = backend.expectOne({ method: 'POST', url: `${COMPLEX_URL}/status-changes` });
      expect(request.request.body).toEqual({ targetStatus: 'SUSPENDED', version: 3 });
      request.flush(suspendedComplex(45));
      await settle();

      expect(dialog()).toBeNull();
      expect(root().textContent).toContain('La suscripción quedó «Suspendida».');
      expect(root().textContent).toContain('Complejo suspendido desde el');
      expect(text(root().querySelector('app-status-tag'))).toBe('Suspendida');
    });

    it('labels the close button «Volver» when cancelling', async () => {
      await openSummary(ACTIVE_COMPLEX);
      button('Cambiar estado').click();
      await settle();

      (dialog()?.querySelectorAll('input[type="radio"]')[2] as HTMLInputElement).click();
      await settle();

      expect(buttons(dialog() as Element)).toEqual(['Volver', 'Cancelar suscripción']);
      expect(dialog()?.textContent).toContain('La suscripción no se podrá reactivar.');
    });

    it('says when there is no status change available', async () => {
      await openSummary({ ...ACTIVE_COMPLEX, allowedTransitions: [] });
      button('Cambiar estado').click();
      await settle();

      expect(dialog()?.textContent).toContain('No hay cambios de estado disponibles.');
      expect(buttons(dialog() as Element)).toEqual(['Cancelar']);
    });

    it('reloads the complex when someone else changed it (409)', async () => {
      await openSummary(ACTIVE_COMPLEX);
      button('Cambiar estado').click();
      await settle();
      (dialog()?.querySelector('input[type="radio"]') as HTMLInputElement).click();
      await settle();

      button('Marcar por vencer', dialog() as Element).click();
      await settle();
      backend
        .expectOne({ method: 'POST', url: `${COMPLEX_URL}/status-changes` })
        .flush(problemBody(409, 'STALE_STATE'), { status: 409, statusText: 'Conflict' });
      await settle();

      expect(dialog()).toBeNull();
      expect(text(root().querySelector('[role="alert"]'))).toBe(
        'El estado del complejo cambió. Revisa los datos y vuelve a intentarlo.',
      );
      backend.expectOne(COMPLEX_URL).flush({ ...ACTIVE_COMPLEX, version: 4 });
    });

    it('says the retention expired when reactivation is refused', async () => {
      await openSummary(suspendedComplex(3));
      button('Cambiar estado').click();
      await settle();
      (dialog()?.querySelector('input[type="radio"]') as HTMLInputElement).click();
      await settle();

      button('Reactivar complejo', dialog() as Element).click();
      await settle();
      backend
        .expectOne({ method: 'POST', url: `${COMPLEX_URL}/status-changes` })
        .flush(problemBody(422, 'RETENTION_EXPIRED'), { status: 422, statusText: 'Unprocessable' });
      await settle();

      expect(dialog()?.textContent).toContain(
        'El plazo de conservación venció. Ya no se puede reactivar.',
      );
    });

    it('has no axe violations with the dialog open', async () => {
      await openSummary(ACTIVE_COMPLEX);
      button('Cambiar estado').click();
      await settle();
      (dialog()?.querySelectorAll('input[type="radio"]')[1] as HTMLInputElement).click();
      await settle();

      await expectNoAxeViolations(root());
    });
  });

  describe('P5 · Cambiar plan', () => {
    it('compares the plans and changes it (CA5)', async () => {
      await openSummary(ACTIVE_COMPLEX);
      button('Cambiar plan').click();
      await settle();
      backend
        .expectOne((candidate) => candidate.url === PLANS_URL)
        .flush(pageWith([BASIC_PLAN, PREMIUM_PLAN]));
      await settle();

      const options = Array.from(control('Nuevo plan').querySelectorAll('option:not([disabled])'));
      expect(options.map(text)).toEqual(['Basic']);
      await type('Nuevo plan', BASIC_PLAN.id);
      const rows = Array.from(dialog()?.querySelectorAll('tbody tr') ?? []).map(joined);
      expect(rows).toEqual([
        'Precio 120.00 USD 60.00 USD',
        'Periodicidad Mensual Mensual',
        'Límite de canchas 10 4',
        'Límite de usuarios 25 8',
        'support-hours Weekdays 9 to 18 —',
      ]);

      button('Cambiar plan', dialog() as Element).click();
      await settle();
      const request = backend.expectOne({ method: 'PUT', url: `${COMPLEX_URL}/plan` });
      expect(request.request.body).toEqual({ planId: BASIC_PLAN.id, version: 3 });
      request.flush({
        ...ACTIVE_COMPLEX,
        subscription: {
          ...ACTIVE_COMPLEX.subscription,
          plan: { ...ACTIVE_COMPLEX.subscription.plan, planId: BASIC_PLAN.id, name: 'Basic' },
        },
        version: 4,
      });
      await settle();

      expect(dialog()).toBeNull();
      expect(root().textContent).toContain('El plan del complejo ahora es Basic.');
    });

    it('says when there is no other published plan', async () => {
      await openSummary(ACTIVE_COMPLEX);
      button('Cambiar plan').click();
      await settle();
      backend.expectOne((candidate) => candidate.url === PLANS_URL).flush(pageWith([PREMIUM_PLAN]));
      await settle();

      expect(dialog()?.textContent).toContain('No hay otro plan publicado.');
      expect(buttons(dialog() as Element)).toEqual(['Cancelar']);
    });
  });

  describe('P4 · Cambiar plazo de conservación', () => {
    it('previews the new purge date, validates and saves it', async () => {
      const suspended = suspendedComplex(5);
      await openSummary(suspended);
      button('Cambiar plazo de conservación').click();
      await settle();

      expect(control('Días de conservación').value).toBe('45');
      expect(dialog()?.textContent).toContain('Nueva fecha de borrado:');
      await type('Días de conservación', '0');
      button('Cambiar plazo', dialog() as Element).click();
      await settle();
      expect(dialog()?.textContent).toContain('Escribe un número entero de días, 1 o más.');

      await type('Días de conservación', '30');
      button('Cambiar plazo', dialog() as Element).click();
      await settle();
      expect(dialog()?.textContent).toContain(
        'La nueva fecha de borrado debe ser posterior a hoy.',
      );

      await type('Días de conservación', '60');
      button('Cambiar plazo', dialog() as Element).click();
      await settle();
      const request = backend.expectOne({ method: 'PUT', url: `${COMPLEX_URL}/retention` });
      expect(request.request.body).toEqual({ retentionDays: 60, version: 4 });
      request.flush(suspendedComplex(20));
      await settle();

      expect(dialog()).toBeNull();
      expect(root().textContent).toContain('Cambiamos el plazo de conservación');
    });
  });

  describe('Pestañas Pagos e Historial', () => {
    it('lists the payments of the complex, newest first (CA7)', async () => {
      await open(ACTIVE_COMPLEX, SUPER_ADMIN_USER, 'payments');
      const request = backend.expectOne((candidate) => candidate.url === PAYMENTS_URL);
      expect(request.request.params.get('size')).toBe('20');
      request.flush(pageWith([RECORDED_PAYMENT]));
      await settle();

      const cells = Array.from(root().querySelectorAll('tbody td')).map(text);
      expect(cells).toEqual([
        '01/12/2026',
        '120.00 USD',
        'Transferencia',
        'Robin Example',
        expect.stringMatching(/^02\/12\/2026 \d{2}:\d{2}$/),
      ]);
      const selected = root().querySelector('[role="tab"][aria-selected="true"]');
      expect(text(selected)).toBe('Pagos');
      expect(root().querySelector('[role="tabpanel"]')?.getAttribute('aria-labelledby')).toBe(
        selected?.id,
      );
      await expectNoAxeViolations(root());
    });

    it('says when there are no payments', async () => {
      await open(ACTIVE_COMPLEX, SUPER_ADMIN_USER, 'payments');
      backend.expectOne((candidate) => candidate.url === PAYMENTS_URL).flush(EMPTY_PAGE);
      await settle();

      expect(root().textContent).toContain('Todavía no hay pagos registrados.');
    });

    it('lists plan and status changes with who made them (CA5, CA6)', async () => {
      await open(ACTIVE_COMPLEX, OPERATOR_USER, 'history');
      backend
        .expectOne((candidate) => candidate.url === HISTORY_URL)
        .flush(pageWith(HISTORY_ENTRIES));
      await settle();

      const rows = Array.from(root().querySelectorAll('tbody tr')).map((row) =>
        Array.from(row.querySelectorAll('td')).map(text),
      );
      expect(rows).toEqual([
        [expect.stringMatching(/^10\/01\/2027/), 'Estado', 'Activa', 'Suspendida', 'Sam Example'],
        [expect.stringMatching(/^20\/12\/2026/), 'Plan', 'Basic', 'Premium', 'Sam Example'],
      ]);
      expect(root().textContent).toContain('Se conserva 1 año.');
      await expectNoAxeViolations(root());
    });

    it('says when there were no changes in the last year', async () => {
      await open(ACTIVE_COMPLEX, SUPER_ADMIN_USER, 'history');
      backend.expectOne((candidate) => candidate.url === HISTORY_URL).flush(EMPTY_PAGE);
      await settle();

      expect(root().textContent).toContain('No hay cambios en el último año.');
    });

    it('moves between tabs with the arrow keys', async () => {
      await openSummary(ACTIVE_COMPLEX);
      const tabs = Array.from(root().querySelectorAll<HTMLAnchorElement>('[role="tab"]'));
      expect(tabs.map(text)).toEqual(['Resumen', 'Pagos', 'Historial']);
      expect(tabs.map((tab) => tab.getAttribute('tabindex'))).toEqual(['0', '-1', '-1']);

      tabs[0].focus();
      tabs[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
      expect(document.activeElement).toBe(tabs[1]);
      tabs[1].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
      tabs[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft', bubbles: true }));
      expect(document.activeElement).toBe(tabs[2]);
      expect(tabs[1].getAttribute('href')).toBe(`/complexes/${ACTIVE_COMPLEX.id}/payments`);
    });
  });
});
