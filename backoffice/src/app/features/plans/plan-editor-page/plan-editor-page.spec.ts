import { Location } from '@angular/common';
import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import {
  DRAFT_PLAN,
  PLANS_URL,
  PLAN_FIELDS_URL,
  PLAN_OPTIONS,
  PLAN_OPTIONS_URL,
  PRICE_FIELD,
  PUBLISHED_PLAN,
  RETIRED_FIELD,
  SUPER_ADMIN_USER,
  SUPPORT_HOURS_FIELD,
  TOURNAMENTS_FIELD,
  problemBody,
  validationProblem,
} from '../../../../testing/platform-fixtures';
import { provideTestApi, settle, signInAs } from '../../../../testing/platform-testing';
import { unsavedChangesGuard } from '../../../core/navigation/unsaved-changes-guard';
import { PlanField } from '../plan-fields-api';
import { Plan } from '../plans-api';
import { PlansNotice } from '../plans-notice';
import { PlanEditorPage } from './plan-editor-page';

@Component({ template: '<h1>Planes</h1>' })
class PlansStub {}

const DEFINITIONS: readonly PlanField[] = [
  PRICE_FIELD,
  SUPPORT_HOURS_FIELD,
  TOURNAMENTS_FIELD,
  RETIRED_FIELD,
];

describe('PlanEditorPage (P11)', () => {
  let harness: RouterTestingHarness;
  let backend: HttpTestingController;

  beforeEach(async () => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideTestApi(),
        provideRouter([
          { path: 'plans', component: PlansStub },
          { path: 'plans/new', component: PlanEditorPage, canDeactivate: [unsavedChangesGuard] },
          {
            path: 'plans/:planId',
            component: PlanEditorPage,
            canDeactivate: [unsavedChangesGuard],
          },
        ]),
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    await signInAs(SUPER_ADMIN_USER);
    harness = await RouterTestingHarness.create();
    document.body.appendChild(harness.fixture.nativeElement);
  });

  afterEach(() => {
    backend.verify();
    document.body.replaceChildren();
  });

  const root = (): HTMLElement => harness.fixture.nativeElement;
  const heading = () => root().querySelector('h1')?.textContent?.trim();
  const controlLabelled = (label: string) => {
    const element = Array.from(root().querySelectorAll('label')).find(
      (candidate) => candidate.textContent?.trim() === label,
    );
    return document.getElementById(element?.htmlFor ?? '') as
      HTMLInputElement | HTMLSelectElement | null;
  };
  const button = (label: string, scope: ParentNode = root()) =>
    Array.from(scope.querySelectorAll('button')).find(
      (element) => element.textContent?.trim() === label,
    ) as HTMLButtonElement | undefined;
  const dialog = () => root().querySelector('[role="dialog"]') as HTMLElement | null;

  async function open(url: string, plan: Plan | null = null): Promise<void> {
    const navigation = harness.navigateByUrl(url);
    await vi.waitFor(() => backend.expectOne(PLAN_OPTIONS_URL).flush(PLAN_OPTIONS));
    backend.expectOne(PLAN_FIELDS_URL).flush(DEFINITIONS);
    if (plan !== null) {
      backend.expectOne(`${PLANS_URL}/${plan.id}`).flush(plan);
    }
    await navigation;
    await settle();
  }

  async function type(label: string, value: string): Promise<void> {
    const control = controlLabelled(label)!;
    control.value = value;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
    await settle();
  }

  async function press(label: string, scope?: ParentNode): Promise<void> {
    button(label, scope)?.click();
    await settle();
  }

  async function fillComplete(): Promise<void> {
    await type('Nombre', 'Premium');
    await type('Precio', '120,00');
    await type('Moneda', 'USD');
    await type('Periodicidad', 'MONTHLY');
    await type('Límite de canchas', '10');
    await type('Límite de usuarios', '25');
    await type('Horario de soporte', 'Lunes a viernes');
  }

  function expectCreate(): TestRequest {
    return backend.expectOne((request) => request.url === PLANS_URL && request.method === 'POST');
  }

  it('creates a draft with only the name and goes back to «Planes» with a notice', async () => {
    await open('/plans/new');
    expect(heading()).toBe('Crear plan');
    expect(controlLabelled('Precio')?.value).toBe('');
    expect(controlLabelled('Precio')?.getAttribute('inputmode')).toBe('decimal');
    await expectNoAxeViolations(root());

    await type('Nombre', 'Inicial');
    await press('Guardar borrador');
    const request = expectCreate();
    expect(request.request.body).toEqual({
      name: 'Inicial',
      price: null,
      currency: null,
      periodicity: null,
      courtLimit: null,
      userLimit: null,
      customFields: {},
    });
    request.flush(DRAFT_PLAN);
    await settle();

    expect(TestBed.inject(Router).url).toBe('/plans');
    expect(TestBed.inject(PlansNotice).take()).toBe('Guardamos el borrador del plan «Inicial».');
  });

  it('generates «Otros campos» by type and leaves out retired fields (CA4)', async () => {
    await open('/plans/new');

    const others = root().querySelectorAll('fieldset')[1];
    expect(
      Array.from(others.querySelectorAll('label')).map((label) => label.textContent?.trim()),
    ).toEqual(['Horario de soporte', 'Torneos incluidos']);
    expect(controlLabelled('Torneos incluidos')?.tagName).toBe('SELECT');
    expect(others.textContent).toContain('Obligatorio para publicar.');
    expect(root().textContent).not.toContain('Descuento anterior');
  });

  it('says there are no custom fields and links to «Campos de plan»', async () => {
    const navigation = harness.navigateByUrl('/plans/new');
    await vi.waitFor(() => backend.expectOne(PLAN_OPTIONS_URL).flush(PLAN_OPTIONS));
    backend.expectOne(PLAN_FIELDS_URL).flush([PRICE_FIELD, RETIRED_FIELD]);
    await navigation;
    await settle();

    expect(root().textContent).toContain('No hay campos adicionales.');
    expect(root().querySelector('a[href="/plans/fields"]')).not.toBeNull();
  });

  it('shows a focusable error summary linked to each field before publishing', async () => {
    await open('/plans/new');
    await type('Nombre', 'Premium');
    await type('Límite de canchas', 'diez');

    await press('Publicar');

    const summary = root().querySelector('.editor__summary') as HTMLElement;
    expect(document.activeElement).toBe(summary);
    expect(summary.textContent).toContain('Revisa los campos marcados');
    expect(summary.textContent).toContain(
      'Límite de canchas: Escribe un número entero igual o mayor que 0.',
    );
    expect(summary.textContent).toContain('Horario de soporte: Completa «Horario de soporte».');
    expect(controlLabelled('Precio')?.getAttribute('aria-invalid')).toBe('true');
    expect(dialog()).toBeNull();
    await expectNoAxeViolations(root());

    (summary.querySelector('a') as HTMLAnchorElement).click();
    expect(document.activeElement).toBe(controlLabelled('Precio'));
  });

  it('creates and publishes a complete plan after confirming (CA3)', async () => {
    await open('/plans/new');
    await fillComplete();

    await press('Publicar');
    expect(dialog()?.textContent).toContain('Se podrá asignar a complejos.');
    await press('Publicar', dialog()!);

    const create = expectCreate();
    expect(create.request.body).toEqual(
      expect.objectContaining({
        price: '120.00',
        courtLimit: 10,
        customFields: { 'support-hours': 'Lunes a viernes' },
      }),
    );
    create.flush({ ...PUBLISHED_PLAN, status: 'DRAFT', version: 0 });
    await settle();
    backend
      .expectOne(`${PLANS_URL}/${PUBLISHED_PLAN.id}/publication`)
      .flush({ ...PUBLISHED_PLAN, version: 1 });
    await settle();

    expect(TestBed.inject(Router).url).toBe('/plans');
    expect(TestBed.inject(PlansNotice).take()).toBe(
      'Publicamos el plan «Premium». Ya se puede asignar a complejos.',
    );
  });

  it('keeps the created draft when publishing fails, so the next try updates it', async () => {
    await open('/plans/new');
    await fillComplete();
    await press('Publicar');
    await press('Publicar', dialog()!);
    expectCreate().flush({ ...PUBLISHED_PLAN, status: 'DRAFT', version: 0 });
    await settle();

    backend
      .expectOne(`${PLANS_URL}/${PUBLISHED_PLAN.id}/publication`)
      .flush(
        validationProblem(422, 'PLAN_INCOMPLETE', [
          { field: 'customFields.support-hours', code: 'REQUIRED' },
        ]),
        { status: 422, statusText: 'Unprocessable' },
      );
    await settle();

    expect(dialog()).toBeNull();
    expect(root().querySelector('.editor__summary')?.textContent).toContain(
      'Completa «Horario de soporte».',
    );
    expect(TestBed.inject(Location).path()).toBe(`/plans/${PUBLISHED_PLAN.id}`);

    await press('Guardar borrador');
    const update = backend.expectOne(`${PLANS_URL}/${PUBLISHED_PLAN.id}`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body.version).toBe(0);
    update.flush({ ...PUBLISHED_PLAN, status: 'DRAFT', version: 1 });
    await settle();
  });

  it('marks the name when another plan already has it', async () => {
    await open('/plans/new');
    await type('Nombre', 'Premium');
    await press('Guardar borrador');

    expectCreate().flush(problemBody(409, 'PLAN_NAME_TAKEN'), {
      status: 409,
      statusText: 'Conflict',
    });
    await settle();

    expect(root().textContent).toContain('Ya existe un plan con ese nombre. Elige otro.');
    expect(controlLabelled('Nombre')?.value).toBe('Premium');
  });

  it('warns that editing a published plan only affects new assignments', async () => {
    await open(`/plans/${PUBLISHED_PLAN.id}`, PUBLISHED_PLAN);

    expect(heading()).toBe('Editar plan «Premium»');
    expect(root().textContent).toContain('Publicado');
    expect(root().querySelector('[role="status"]')?.textContent).toContain(
      'Los cambios solo se aplican a las nuevas asignaciones',
    );
    expect(button('Guardar borrador')).toBeUndefined();
    expect(controlLabelled('Precio')?.value).toBe('120.00');
    await expectNoAxeViolations(root());

    await type('Precio', '150');
    await press('Guardar cambios');
    const update = backend.expectOne(`${PLANS_URL}/${PUBLISHED_PLAN.id}`);
    expect(update.request.body).toEqual(expect.objectContaining({ price: '150', version: 2 }));
    update.flush({ ...PUBLISHED_PLAN, price: '150', version: 3 });
    await settle();

    expect(TestBed.inject(PlansNotice).take()).toBe('Guardamos los cambios del plan «Premium».');
  });

  it('does not let a published plan lose a required value', async () => {
    await open(`/plans/${PUBLISHED_PLAN.id}`, PUBLISHED_PLAN);
    await type('Precio', '');

    await press('Guardar cambios');

    expect(root().querySelector('.editor__summary')?.textContent).toContain('Completa «Precio».');
  });

  it('reloads the plan when someone else changed it', async () => {
    await open(`/plans/${DRAFT_PLAN.id}`, DRAFT_PLAN);
    await type('Precio', '10');
    await press('Guardar borrador');

    backend
      .expectOne(`${PLANS_URL}/${DRAFT_PLAN.id}`)
      .flush(problemBody(409, 'STALE_STATE'), { status: 409, statusText: 'Conflict' });
    await vi.waitFor(() => backend.expectOne(PLAN_OPTIONS_URL).flush(PLAN_OPTIONS));
    backend.expectOne(PLAN_FIELDS_URL).flush(DEFINITIONS);
    backend.expectOne(`${PLANS_URL}/${DRAFT_PLAN.id}`).flush({ ...DRAFT_PLAN, version: 1 });
    await settle();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain(
      'Otra persona cambió este plan.',
    );
    expect(controlLabelled('Precio')?.value).toBe('');
  });

  it('says so when the plan does not exist', async () => {
    const navigation = harness.navigateByUrl('/plans/unknown');
    await vi.waitFor(() => backend.expectOne(PLAN_OPTIONS_URL).flush(PLAN_OPTIONS));
    backend.expectOne(PLAN_FIELDS_URL).flush(DEFINITIONS);
    backend
      .expectOne(`${PLANS_URL}/unknown`)
      .flush(problemBody(404, 'PLAN_NOT_FOUND'), { status: 404, statusText: 'Not Found' });
    await navigation;
    await settle();

    expect(root().textContent).toContain('No encontramos este plan.');
    expect(root().querySelector('main a[href="/plans"], [role="alert"] a')?.textContent).toBe(
      'Ir a Planes',
    );
  });

  it('asks before leaving with unsaved changes and stays if the person keeps editing', async () => {
    await open('/plans/new');
    await type('Nombre', 'Borrador sin guardar');

    const router = TestBed.inject(Router);
    const leaving = router.navigateByUrl('/plans');
    // The pending navigation keeps the app unstable: wait for the dialog instead of `settle`.
    await vi.waitFor(() => expect(dialog()).not.toBeNull());
    expect(dialog()?.textContent).toContain('Los cambios que hiciste en el plan se perderán.');
    await vi.waitFor(() =>
      expect(document.activeElement?.textContent?.trim()).toBe('Seguir editando'),
    );

    button('Seguir editando', dialog()!)?.click();
    expect(await leaving).toBe(false);
    expect(router.url).toBe('/plans/new');
    await settle();
    expect(dialog()).toBeNull();

    const leavingAgain = router.navigateByUrl('/plans');
    await vi.waitFor(() => expect(dialog()).not.toBeNull());
    button('Salir sin guardar', dialog()!)?.click();
    expect(await leavingAgain).toBe(true);
    expect(router.url).toBe('/plans');
  });

  it('leaves without asking when nothing changed', async () => {
    await open('/plans/new');

    await harness.navigateByUrl('/plans');

    expect(TestBed.inject(Router).url).toBe('/plans');
  });
});
