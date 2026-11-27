import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../../../testing/axe';
import {
  DRAFT_PLAN,
  PLANS_URL,
  PUBLISHED_PLAN,
  pageOf,
  problemBody,
  validationProblem,
} from '../../../../testing/platform-fixtures';
import { provideTestApi, settle } from '../../../../testing/platform-testing';
import { Page } from '../../../core/api/page';
import { Plan } from '../plans-api';
import { PlansNotice } from '../plans-notice';
import { PlansPage } from './plans-page';

describe('PlansPage (P10)', () => {
  let fixture: ComponentFixture<PlansPage>;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideTestApi(), provideRouter([])] });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
    document.body.replaceChildren();
  });

  const root = (): HTMLElement => fixture.nativeElement;
  const buttonNamed = (name: string, scope: ParentNode = root()) =>
    Array.from(scope.querySelectorAll('button')).find(
      (element) => (element.getAttribute('aria-label') ?? element.textContent?.trim()) === name,
    ) as HTMLButtonElement | undefined;
  const dialog = () => root().querySelector('[role="dialog"]') as HTMLElement | null;
  const rows = () => Array.from(root().querySelectorAll('tbody tr')) as HTMLElement[];
  const cells = (row: HTMLElement) =>
    Array.from(row.querySelectorAll('td')).map((cell) => cell.textContent?.trim());

  function expectList(): TestRequest {
    return backend.expectOne((request) => request.url === PLANS_URL && request.method === 'GET');
  }

  async function render(page: Page<Plan> = pageOf([DRAFT_PLAN, PUBLISHED_PLAN])): Promise<void> {
    fixture = TestBed.createComponent(PlansPage);
    document.body.appendChild(fixture.nativeElement);
    await fixture.whenStable();
    expectList().flush(page);
    await settle();
  }

  async function openPublish(): Promise<void> {
    buttonNamed('Publicar plan Inicial')?.click();
    await settle();
  }

  async function confirmPublish(): Promise<TestRequest> {
    buttonNamed('Publicar', dialog()!)?.click();
    await settle();
    return backend.expectOne(`${PLANS_URL}/${DRAFT_PLAN.id}/publication`);
  }

  it('shows skeletons while the plans load', async () => {
    fixture = TestBed.createComponent(PlansPage);
    await fixture.whenStable();

    expect(root().querySelector('[aria-busy="true"]')?.textContent).toContain('Cargando…');
    expectList().flush(pageOf([]));
  });

  it('lists the plans with price and currency, periodicity, limits and status', async () => {
    await render();

    expect(rows().map(cells)).toEqual([
      ['Inicial', '—', '—', '—', 'Borrador', 'Publicar'],
      [
        'Premium',
        '120.00 USD',
        'Mensual',
        '10 canchas · 25 usuarios',
        'Publicado',
        '—Sin acciones',
      ],
    ]);
    expect(rows()[1].querySelector('a')?.getAttribute('href')).toBe(`/plans/${PUBLISHED_PLAN.id}`);
    expect(root().querySelector('a[href="/plans/new"]')?.textContent?.trim()).toBe('Crear plan');
    expect(root().querySelector('a[href="/plans/fields"]')?.textContent?.trim()).toBe(
      'Campos de plan',
    );
    await expectNoAxeViolations(root());
  });

  it('invites to create the first plan when there are none', async () => {
    await render(pageOf([]));

    expect(root().textContent).toContain('Todavía no hay planes.');
    expect(root().querySelectorAll('a[href="/plans/new"]')).toHaveLength(2);
    await expectNoAxeViolations(root());
  });

  it('offers to retry when the plans cannot be loaded', async () => {
    fixture = TestBed.createComponent(PlansPage);
    await fixture.whenStable();
    expectList().flush(problemBody(500, 'UNEXPECTED_RESPONSE'), {
      status: 500,
      statusText: 'Server Error',
    });
    await settle();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cargar la información.',
    );
    buttonNamed('Reintentar')?.click();
    await settle();
    expectList().flush(pageOf([PUBLISHED_PLAN]));
    await settle();
    expect(rows()).toHaveLength(1);
  });

  it('shows the result notice left by the editor once', async () => {
    TestBed.inject(PlansNotice).leave('Guardamos el borrador del plan «Inicial».');
    await render();

    expect(root().querySelector('[role="status"]')?.textContent).toContain(
      'Guardamos el borrador del plan «Inicial».',
    );
    expect(TestBed.inject(PlansNotice).take()).toBeNull();
  });

  it('publishes a draft after confirming it can be assigned to complexes (CA3)', async () => {
    await render();
    await openPublish();

    expect(dialog()?.textContent).toContain('Publicar plan «Inicial»');
    expect(dialog()?.textContent).toContain('Se podrá asignar a complejos.');
    expect(document.activeElement?.textContent?.trim()).toBe('Cancelar');
    await expectNoAxeViolations(root());

    (await confirmPublish()).flush({ ...DRAFT_PLAN, status: 'PUBLISHED', version: 1 });
    await settle();

    expect(dialog()).toBeNull();
    expect(cells(rows()[0])[4]).toBe('Publicado');
    expect(root().querySelector('[role="status"]')?.textContent).toContain(
      'Publicamos el plan «Inicial». Ya se puede asignar a complejos.',
    );
  });

  it('explains an incomplete plan inside the dialog and links to edit it', async () => {
    await render();
    await openPublish();

    (await confirmPublish()).flush(
      validationProblem(422, 'PLAN_INCOMPLETE', [{ field: 'price', code: 'REQUIRED' }]),
      { status: 422, statusText: 'Unprocessable' },
    );
    await settle();

    expect(dialog()?.querySelector('[role="alert"]')?.textContent).toContain(
      'Al plan le faltan datos obligatorios para publicarse.',
    );
    expect(dialog()?.querySelector(`a[href="/plans/${DRAFT_PLAN.id}"]`)?.textContent).toBe(
      'Editar plan',
    );
  });

  it('closes the dialog without publishing when cancelled', async () => {
    await render();
    await openPublish();

    buttonNamed('Cancelar', dialog()!)?.click();
    await settle();

    expect(dialog()).toBeNull();
  });
});
