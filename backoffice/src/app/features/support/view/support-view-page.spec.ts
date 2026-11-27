import { Component } from '@angular/core';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, Routes, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { COMPLEX_ID } from '../../../../testing/complex-fixtures';
import { SUPER_ADMIN_USER, problemBody } from '../../../../testing/platform-fixtures';
import { provideTestApi, settle, signInAs } from '../../../../testing/platform-testing';
import {
  CLOSE_SESSION_URL,
  COURTS,
  REASON,
  STAFF_BLOCKS_URL,
  STAFF_BOOKINGS_URL,
  SUPPORT_SESSIONS_URL,
  dayEntries,
  supportAccess,
  supportSession,
  tenantApiBaseUrl,
} from '../../../../testing/support-fixtures';
import { TENANT_API_BASE_URL } from '../../../core/api/api-base-url';
import { Court, SUPPORT_COURTS } from '../support-courts';
import { SUPPORT_CLOCK } from '../support-clock';
import { SupportAccess } from '../support-access';
import { supportAccessGuard } from '../support-access.guard';
import { SupportViewPage } from './support-view-page';

@Component({ template: '<h1>Detalle</h1>' })
class DetailStub {}

const ROUTES: Routes = [
  {
    path: 'complexes/:complexId/support',
    canActivate: [supportAccessGuard],
    component: SupportViewPage,
  },
  { path: 'complexes/:complexId', component: DetailStub },
];

const MINUTE_MS = 60_000;
const SUPPORT_URL = `/complexes/${COMPLEX_ID}/support`;

describe('SupportViewPage (P9)', () => {
  let harness: RouterTestingHarness;
  let backend: HttpTestingController;
  let now: number;
  let courts: readonly Court[];

  beforeEach(async () => {
    sessionStorage.clear();
    now = Date.now();
    courts = COURTS;
    TestBed.configureTestingModule({
      providers: [
        provideTestApi(),
        provideRouter(ROUTES),
        { provide: TENANT_API_BASE_URL, useValue: tenantApiBaseUrl },
        { provide: SUPPORT_COURTS, useValue: { courtsOf: () => of(courts) } },
        { provide: SUPPORT_CLOCK, useValue: { now: () => now, tickMs: 5 } },
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
  const alerts = () => Array.from(root().querySelectorAll('app-alert')).map(text);
  const button = (label: string, container: Element = root()) =>
    Array.from(container.querySelectorAll('button')).find(
      (candidate) => text(candidate) === label || candidate.getAttribute('aria-label') === label,
    ) as HTMLButtonElement;
  const dialog = () => root().querySelector('[role="dialog"]') as HTMLElement | null;
  /** A slot of the editable calendar by its accessible label, for example «19:00 a 20:00, Libre». */
  const slot = (label: string) =>
    Array.from(dialog()?.querySelectorAll<HTMLButtonElement>('.calendar__slot-button') ?? []).find(
      (candidate) => candidate.textContent?.includes(label),
    ) as HTMLButtonElement;
  const rows = () =>
    Array.from(root().querySelectorAll('.support__day tbody tr')).map((row) =>
      Array.from(row.children).map(text),
    );

  function dayRequest(): TestRequest {
    return backend.expectOne(
      (request) => request.url === STAFF_BOOKINGS_URL && !request.params.has('courtId'),
    );
  }

  async function click(target: HTMLElement): Promise<void> {
    target.click();
    await settle();
  }

  /** Opens P9 with a session started in P8 and answers the day with `entries`. */
  async function openView(
    access = supportAccess(supportSession(30, now)),
    entries = dayEntries(access.session.tenantStatus !== 'ACTIVE'),
  ): Promise<void> {
    await signInAs(SUPER_ADMIN_USER);
    TestBed.inject(SupportAccess).start(access);
    await harness.navigateByUrl(SUPPORT_URL);
    dayRequest().flush(entries);
    await settle();
  }

  async function expectSessionClosed(): Promise<void> {
    const close = backend.expectOne(CLOSE_SESSION_URL);
    expect(close.request.method).toBe('DELETE');
    close.flush(null, { status: 204, statusText: 'No Content' });
    await settle();
  }

  describe('access', () => {
    it('reads the day on the complex host with the platform token and the session header', async () => {
      await signInAs(SUPER_ADMIN_USER);
      TestBed.inject(SupportAccess).start(supportAccess(supportSession(30, now)));
      await harness.navigateByUrl(SUPPORT_URL);

      expect(root().querySelector('.support__day')?.getAttribute('aria-busy')).toBe('true');
      const request = dayRequest();
      expect(request.request.headers.get('X-Support-Session')).toBe(supportSession().sessionId);
      expect(request.request.headers.get('Authorization')).toBe('Bearer test-token');
      request.flush(dayEntries());
      await settle();

      expect(text(root().querySelector('app-support-bar'))).toContain(
        'Acceso de soporte a Canchas El Sol',
      );
      expect(text(root().querySelector('app-support-bar'))).toContain(`«${REASON}»`);
      expect(text(root().querySelector('.support-bar__time'))).toBe('quedan 30 min');
      expect(document.title).toBe('Soporte · Canchas El Sol · Backoffice de Kuador');
    });

    it('goes back to the complex without a session for it (direct address or reload)', async () => {
      await signInAs(SUPER_ADMIN_USER);

      await harness.navigateByUrl(SUPPORT_URL);

      expect(TestBed.inject(Router).url).toBe(`/complexes/${COMPLEX_ID}`);
    });
  });

  describe('day list', () => {
    it('lists bookings and blocks with a pending booking without holder', async () => {
      await openView();

      expect(rows()).toEqual([
        ['13:00–14:00', 'Cancha 2', 'Bloqueo', '—', 'Maintenance', 'Quitar bloqueo'],
        ['19:00–20:00', 'Cancha 1', 'Reserva', 'Pendiente', '—', 'Cancelar reserva'],
        ['20:00–21:00', 'Cancha 1', 'Reserva', 'Confirmada', 'Alex Example', 'Cancelar reserva'],
      ]);
      expect(button('Cancelar reserva de las 20:00, Cancha 1')).toBeDefined();
      await expectNoAxeViolations(root());
    });

    it('filters by court and day, asking the api each time', async () => {
      await openView();

      const court = root().querySelectorAll('select')[0] as HTMLSelectElement;
      court.value = COURTS[1].id;
      court.dispatchEvent(new Event('change'));
      await settle();
      const filtered = backend.expectOne(
        (request) => request.params.get('courtId') === COURTS[1].id,
      );
      filtered.flush([dayEntries()[2]]);
      await settle();
      expect(rows()).toHaveLength(1);

      const day = root().querySelector('input[type="date"]') as HTMLInputElement;
      day.value = '2027-01-11';
      day.dispatchEvent(new Event('input'));
      await settle();
      const other = backend.expectOne((request) => request.url === STAFF_BOOKINGS_URL);
      expect(new Date(other.request.params.get('from') ?? '').getDate()).toBe(11);
      other.flush([]);
      await settle();

      expect(text(root().querySelector('.support__day'))).toContain(
        'No hay reservas ni bloqueos este día.',
      );
    });

    it('shows the load error with «Reintentar» and never stale data', async () => {
      await openView();

      const day = root().querySelector('input[type="date"]') as HTMLInputElement;
      day.value = '2027-01-12';
      day.dispatchEvent(new Event('input'));
      await settle();
      dayRequest().flush(problemBody(500, 'INVALID_REQUEST'), { status: 500, statusText: 'Error' });
      await settle();

      expect(rows()).toEqual([]);
      expect(alerts()).toContain('No pudimos cargar la información. Reintentar');
      await click(button('Reintentar'));
      dayRequest().flush([]);
      await settle();
      expect(text(root().querySelector('.support__day'))).toContain('No hay reservas');
    });

    it('marks entries of a suspended complex as inactive under a fixed notice', async () => {
      await openView(supportAccess(supportSession(30, now, 'SUSPENDED')));

      expect(alerts()).toContain(
        'Complejo suspendido. Los cambios que hagas quedan inactivos hasta que se reactive.',
      );
      expect(rows()[2][3]).toMatch(/^Confirmada ?Inactiva$/);
      expect(button('Crear reserva').disabled).toBe(false);
    });

    it('says a cancelled complex never activates the changes and when its data is deleted', async () => {
      await openView(supportAccess(supportSession(30, now, 'ARCHIVED')));

      expect(alerts().join(' ')).toMatch(
        /Suscripción cancelada\. Los cambios no se activarán y los datos se borrarán a partir del \d{2}\/02\/2027 \d{2}:\d{2}\./,
      );
    });

    it('cannot create or block without the courts of the complex', async () => {
      courts = [];
      await openView();

      expect(button('Crear reserva').disabled).toBe(true);
      expect(button('Bloquear horario').disabled).toBe(true);
      expect(alerts().join(' ')).toContain('no se pueden crear reservas ni bloquear horarios');
    });
  });

  describe('session end (CA12)', () => {
    it('warns once with 5 minutes left and drops data and dialogs when it expires', async () => {
      await openView();
      await click(button('Cancelar reserva de las 20:00, Cancha 1'));
      expect(dialog()).not.toBeNull();

      now += 25 * MINUTE_MS + 1;
      await settle();
      await new Promise((resolve) => setTimeout(resolve, 20));
      await settle();
      expect(alerts()).toContain('El acceso está por vencer');
      expect(root().querySelectorAll('app-alert[role="status"]').length).toBeGreaterThan(0);

      now += 5 * MINUTE_MS;
      await new Promise((resolve) => setTimeout(resolve, 20));
      await settle();
      await expectSessionClosed();

      expect(dialog()).toBeNull();
      expect(rows()).toEqual([]);
      expect(root().querySelector('app-support-bar')).toBeNull();
      expect(alerts()[0]).toContain('El acceso venció. Para continuar, registra un nuevo motivo.');
      expect(button('Acceder de nuevo')).toBeDefined();
      await expectNoAxeViolations(root());
    });

    it('drops everything when the api invalidates the session', async () => {
      await openView();
      const day = root().querySelector('input[type="date"]') as HTMLInputElement;
      day.value = '2027-01-12';
      day.dispatchEvent(new Event('input'));
      await settle();

      dayRequest().flush(problemBody(403, 'SUPPORT_SESSION_INVALID'), {
        status: 403,
        statusText: 'Forbidden',
      });
      await settle();
      await expectSessionClosed();

      expect(rows()).toEqual([]);
      expect(alerts()[0]).toContain('El acceso venció.');
    });

    it('opens a new session with a new reason and reads the data again', async () => {
      await openView();
      now += 31 * MINUTE_MS;
      await new Promise((resolve) => setTimeout(resolve, 20));
      await settle();
      await expectSessionClosed();

      await click(button('Acceder de nuevo'));
      const reason = dialog()?.querySelector('textarea') as HTMLTextAreaElement;
      expect(reason.value).toBe('');
      reason.value = 'Follow-up of ticket 1234';
      reason.dispatchEvent(new Event('input'));
      await click(button('Acceder', dialog() as HTMLElement));
      backend.expectOne(SUPPORT_SESSIONS_URL).flush(supportSession(30, now));
      await settle();

      dayRequest().flush(dayEntries());
      await settle();
      expect(rows()).toHaveLength(3);
    });

    it('closes the session from «Cerrar acceso» and goes back to the complex', async () => {
      await openView();

      await click(button('Cerrar acceso'));
      await expectSessionClosed();

      expect(TestBed.inject(Router).url).toBe(`/complexes/${COMPLEX_ID}`);
      expect(TestBed.inject(SupportAccess).active()).toBeNull();
    });

    it('closes the session and drops the data when the person leaves the view', async () => {
      await openView();

      await harness.navigateByUrl(`/complexes/${COMPLEX_ID}`);
      await expectSessionClosed();

      expect(TestBed.inject(SupportAccess).active()).toBeNull();
    });
  });

  describe('changes', () => {
    it('cancels a booking after a danger confirmation that names the slot', async () => {
      await openView();

      await click(button('Cancelar reserva de las 20:00, Cancha 1'));
      expect(text(dialog())).toMatch(
        /¿Quieres cancelar la reserva de Cancha 1 del \d{2}\/\d{2}\/\d{4}, de 20:00–21:00, en Canchas El Sol\?/,
      );
      await click(button('Cancelar reserva', dialog() as HTMLElement));
      const cancel = backend.expectOne(`${STAFF_BOOKINGS_URL}/${dayEntries()[0].id}/cancel`);
      expect(cancel.request.headers.get('X-Support-Session')).toBe(supportSession().sessionId);
      cancel.flush({ ...dayEntries()[0], status: 'CANCELLED' });
      await settle();
      dayRequest().flush(dayEntries().slice(1));
      await settle();

      expect(dialog()).toBeNull();
      expect(alerts()).toContain('Cancelamos la reserva.');
      expect(rows()).toHaveLength(2);
    });

    it('creates a booking on a slot chosen in the shared calendar', async () => {
      await openView();
      await click(button('Crear reserva'));
      backend.expectOne((request) => request.params.get('courtId') === COURTS[0].id).flush([]);
      await settle();
      await click(button('Día siguiente', dialog() as HTMLElement));
      backend.expectOne((request) => request.params.get('courtId') === COURTS[0].id).flush([]);
      await settle();

      await click(button('Crear reserva', dialog() as HTMLElement));
      expect(text(dialog())).toContain('Elige un horario libre.');
      expect(text(dialog())).toContain('Escribe el nombre del titular.');

      await click(slot('19:00 a 20:00, Libre'));
      const holder = Array.from(dialog()?.querySelectorAll('input') ?? []).find(
        (input) => input.getAttribute('maxlength') === '120',
      ) as HTMLInputElement;
      holder.value = 'Alex Example';
      holder.dispatchEvent(new Event('input'));
      await click(button('Crear reserva', dialog() as HTMLElement));

      const create = backend.expectOne(
        (request) => request.url === STAFF_BOOKINGS_URL && request.method === 'POST',
      );
      expect(create.request.body).toEqual({
        courtId: COURTS[0].id,
        start: expect.any(String),
        holderName: 'Alex Example',
      });
      expect(new Date(create.request.body.start).getHours()).toBe(19);
      create.flush(dayEntries()[0], { status: 201, statusText: 'Created' });
      await settle();
      dayRequest().flush(dayEntries());
      await settle();

      expect(alerts()).toContain('Creamos la reserva.');
    });

    it('reads the availability again when the slot was just taken', async () => {
      await openView();
      await click(button('Crear reserva'));
      backend.expectOne((request) => request.params.get('courtId') === COURTS[0].id).flush([]);
      await settle();
      await click(button('Día siguiente', dialog() as HTMLElement));
      backend.expectOne((request) => request.params.get('courtId') === COURTS[0].id).flush([]);
      await settle();
      await click(slot('19:00 a 20:00, Libre'));
      const holder = Array.from(dialog()?.querySelectorAll('input') ?? []).find(
        (input) => input.getAttribute('maxlength') === '120',
      ) as HTMLInputElement;
      holder.value = 'Alex Example';
      holder.dispatchEvent(new Event('input'));
      await click(button('Crear reserva', dialog() as HTMLElement));

      backend
        .expectOne((request) => request.method === 'POST')
        .flush(problemBody(409, 'SLOT_UNAVAILABLE'), { status: 409, statusText: 'Conflict' });
      await settle();

      expect(text(dialog())).toContain('Ese horario ya fue reservado. Elige otro disponible.');
      backend.expectOne((request) => request.params.get('courtId') === COURTS[0].id).flush([]);
      await settle();
    });

    it('explains that a booked slot cannot be blocked', async () => {
      await openView();
      await click(button('Bloquear horario'));
      backend.expectOne((request) => request.params.get('courtId') === COURTS[0].id).flush([]);
      await settle();
      await click(button('Día siguiente', dialog() as HTMLElement));
      backend.expectOne((request) => request.params.get('courtId') === COURTS[0].id).flush([]);
      await settle();
      await click(slot('18:00 a 19:00, Libre'));
      await expectNoAxeViolations(dialog() as HTMLElement);

      await click(button('Bloquear horario', dialog() as HTMLElement));
      const block = backend.expectOne(STAFF_BLOCKS_URL);
      expect(block.request.body).toEqual({ courtId: COURTS[0].id, start: expect.any(String) });
      block.flush(problemBody(409, 'SLOT_HAS_BOOKING'), { status: 409, statusText: 'Conflict' });
      await settle();

      expect(text(dialog())).toContain(
        'Ese horario tiene una reserva. Cancélala antes de bloquearlo.',
      );
      backend.expectOne((request) => request.params.get('courtId') === COURTS[0].id).flush([]);
      await settle();
    });
  });
});
