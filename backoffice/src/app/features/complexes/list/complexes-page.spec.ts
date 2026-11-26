import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../../../testing/axe';
import {
  COMPLEX_SUMMARIES,
  COMPLEXES_URL,
  EMPTY_PAGE,
  pageWith,
} from '../../../../testing/complex-fixtures';
import { OPERATOR_USER, SUPER_ADMIN_USER } from '../../../../testing/platform-fixtures';
import { provideTestApi, settle, signInAs } from '../../../../testing/platform-testing';
import { CurrentPlatformUser } from '../../../core/auth/platform-user';
import { ComplexSummary } from '../complexes-api';
import { ComplexesPage } from './complexes-page';

const PURGED: ComplexSummary = {
  id: '1f2a3b4c-5d6e-4f7a-8b9c-0d1e2f3a4b5c',
  displayName: 'Club Ribera',
  subdomain: null,
  plan: { id: 'p', name: 'Basic' },
  subscriptionStatus: 'CANCELLED',
  tenantStatus: 'PURGED',
  purgeDueAt: null,
  lastPayment: null,
};

describe('ComplexesPage (P2)', () => {
  let fixture: ComponentFixture<ComplexesPage>;
  let backend: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideTestApi(), provideRouter([])] });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  const root = (): HTMLElement => fixture.nativeElement;
  const rows = () =>
    Array.from(root().querySelectorAll('tbody tr')).map((row) =>
      Array.from(row.querySelectorAll('td')).map((cell) =>
        cell.textContent?.replace(/\s+/g, ' ').trim(),
      ),
    );

  function expectList(page = 0): TestRequest {
    const request = backend.expectOne((candidate) => candidate.url === COMPLEXES_URL);
    expect(request.request.params.get('page')).toBe(String(page));
    expect(request.request.params.get('size')).toBe('20');
    return request;
  }

  async function render(user: CurrentPlatformUser = SUPER_ADMIN_USER): Promise<void> {
    await signInAs(user);
    fixture = TestBed.createComponent(ComplexesPage);
    await fixture.whenStable();
  }

  it('shows a skeleton while loading', async () => {
    await render();

    expect(root().querySelector('[aria-busy="true"]')?.textContent).toContain('Cargando…');
    expectList().flush(EMPTY_PAGE);
  });

  it('lists every complex with subdomain, plan, status and last payment (CA19)', async () => {
    await render();
    expectList().flush(pageWith([...COMPLEX_SUMMARIES, PURGED]));
    await settle();

    expect(rows()).toEqual([
      ['Canchas El Sol', 'canchas-el-sol', 'Premium', 'Activa', '01/12/2026 · 120.00 USD'],
      [
        'Complejo La Esquina',
        'la-esquina',
        'Basic',
        expect.stringMatching(/^SuspendidaDatos hasta \d{2}\/02\/2027$/),
        'Sin pagos',
      ],
      ['Club Ribera', '—', 'Basic', 'Datos eliminados', 'Sin pagos'],
    ]);
    expect(root().querySelector('tbody a')?.getAttribute('href')).toBe(
      `/complexes/${COMPLEX_SUMMARIES[0].id}`,
    );
    expect(root().querySelector('td')?.dataset['label']).toBe('Complejo');
  });

  it('shows «Dar de alta complejo» only with COMPLEX_CREATE', async () => {
    await render(OPERATOR_USER);
    expectList().flush(EMPTY_PAGE);
    await settle();

    expect(root().textContent).toContain('Todavía no hay complejos.');
    expect(root().textContent).not.toContain('Dar de alta complejo');
  });

  it('links «Dar de alta complejo» to P3 for the super admin', async () => {
    await render();
    expectList().flush(pageWith(COMPLEX_SUMMARIES));
    await settle();

    expect(root().querySelector('a.link-button')?.getAttribute('href')).toBe('/complexes/new');
  });

  it('shows the load error with «Reintentar» and pages through the list', async () => {
    await render();
    expectList().error(new ProgressEvent('error'));
    await settle();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cargar la información.',
    );

    (root().querySelector('[role="alert"] button') as HTMLButtonElement).click();
    await settle();
    expectList().flush(pageWith(COMPLEX_SUMMARIES, 21));
    await settle();
    const next = Array.from(root().querySelectorAll('button')).find(
      (button) => button.textContent?.trim() === 'Siguiente',
    ) as HTMLButtonElement;
    next.click();
    await settle();
    expectList(1).flush(pageWith([PURGED], 21, 1));
    await settle();

    expect(root().textContent).toContain('Mostrando 21–21 de 21');
  });

  it('has no axe violations', async () => {
    await render();
    expectList().flush(pageWith([...COMPLEX_SUMMARIES, PURGED]));
    await settle();

    await expectNoAxeViolations(root());
  });
});
