import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../../../testing/axe';
import {
  PLAN_FIELDS_URL,
  PRICE_FIELD,
  RETIRED_FIELD,
  SUPPORT_HOURS_FIELD,
  problemBody,
} from '../../../../testing/platform-fixtures';
import { provideTestApi, settle } from '../../../../testing/platform-testing';
import { PlanField } from '../plan-fields-api';
import { PlanFieldsPage } from './plan-fields-page';

const FIELDS: readonly PlanField[] = [PRICE_FIELD, SUPPORT_HOURS_FIELD, RETIRED_FIELD];

describe('PlanFieldsPage (P12)', () => {
  let fixture: ComponentFixture<PlanFieldsPage>;
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
    Array.from(row.querySelectorAll('td')).map((cell) =>
      cell.textContent?.replace(/\s+/g, ' ').trim(),
    );
  const dialogControl = (label: string) => {
    const element = Array.from(dialog()?.querySelectorAll('label') ?? []).find(
      (candidate) => candidate.textContent?.trim() === label,
    );
    return document.getElementById(element?.htmlFor ?? '') as
      HTMLInputElement | HTMLSelectElement | null;
  };

  function expectList(): TestRequest {
    return backend.expectOne(
      (request) => request.url === PLAN_FIELDS_URL && request.method === 'GET',
    );
  }

  async function render(fields: readonly PlanField[] = FIELDS): Promise<void> {
    fixture = TestBed.createComponent(PlanFieldsPage);
    document.body.appendChild(fixture.nativeElement);
    await fixture.whenStable();
    expectList().flush(fields);
    await settle();
  }

  async function press(name: string, scope?: ParentNode): Promise<void> {
    buttonNamed(name, scope)?.click();
    await settle();
  }

  async function type(label: string, value: string): Promise<void> {
    const control = dialogControl(label)!;
    control.value = value;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
    await settle();
  }

  it('lists basic fields read-only and custom ones with their actions', async () => {
    await render();

    expect(rows().map(cells)).toEqual([
      ['Precio', 'Número', 'Sí', 'Básico', '—Sin acciones'],
      ['Horario de soporte', 'Texto', 'Sí', 'Adicional', 'EditarRetirar'],
      ['Descuento anterior Retirado', 'Número', 'No', 'Adicional', '—Sin acciones'],
    ]);
    expect(buttonNamed('Retirar campo Horario de soporte')).toBeDefined();
    expect(root().textContent).not.toContain('Todavía no hay campos adicionales.');
    await expectNoAxeViolations(root());
  });

  it('says there are no custom fields yet', async () => {
    await render([PRICE_FIELD, RETIRED_FIELD]);

    expect(root().textContent).toContain('Todavía no hay campos adicionales.');
  });

  it('offers to retry when the fields cannot be loaded', async () => {
    fixture = TestBed.createComponent(PlanFieldsPage);
    await fixture.whenStable();
    expectList().flush(problemBody(503, 'UNEXPECTED_RESPONSE'), {
      status: 503,
      statusText: 'Unavailable',
    });
    await settle();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cargar la información.',
    );
    await press('Reintentar');
    expectList().flush(FIELDS);
  });

  it('creates a field with its type and required flag (CA4)', async () => {
    await render();
    await press('Crear campo');

    expect(dialog()?.textContent).toContain('no activa funciones ni límites nuevos por sí solo');
    expect(document.activeElement).toBe(dialogControl('Nombre'));
    await expectNoAxeViolations(root());

    await press('Crear campo', dialog()!);
    expect(dialog()?.textContent).toContain('Escribe el nombre del campo.');
    expect(dialog()?.textContent).toContain('Elige el tipo del campo.');
    expect(document.activeElement).toBe(dialogControl('Nombre'));

    await type('Nombre', 'Torneos incluidos');
    await type('Tipo', 'BOOLEAN');
    (dialog()!.querySelector('input[type="checkbox"]') as HTMLInputElement).click();
    await press('Crear campo', dialog()!);

    const request = backend.expectOne(
      (candidate) => candidate.url === PLAN_FIELDS_URL && candidate.method === 'POST',
    );
    expect(request.request.body).toEqual({
      label: 'Torneos incluidos',
      type: 'BOOLEAN',
      required: true,
    });
    request.flush({ ...SUPPORT_HOURS_FIELD, id: 'new', label: 'Torneos incluidos' });
    await settle();
    expectList().flush(FIELDS);
    await settle();

    expect(dialog()).toBeNull();
    expect(root().querySelector('[role="status"]')?.textContent).toContain(
      'Creamos el campo «Torneos incluidos».',
    );
  });

  it('marks the name when another field already has it', async () => {
    await render();
    await press('Crear campo');
    await type('Nombre', 'Horario de soporte');
    await type('Tipo', 'TEXT');
    await press('Crear campo', dialog()!);

    backend
      .expectOne((candidate) => candidate.method === 'POST')
      .flush(problemBody(409, 'FIELD_NAME_TAKEN'), { status: 409, statusText: 'Conflict' });
    await settle();

    expect(dialog()?.textContent).toContain('Ya existe un campo con ese nombre.');
    expect(dialogControl('Nombre')?.value).toBe('Horario de soporte');
  });

  it('edits the label and the required flag but not the type', async () => {
    await render();
    await press('Editar campo Horario de soporte');

    expect(dialog()?.textContent).toContain('Editar campo «Horario de soporte»');
    expect(dialog()?.textContent).toContain('Tipo: Texto. No se puede cambiar');
    expect(dialogControl('Tipo')).toBeNull();
    expect(dialogControl('Nombre')?.value).toBe('Horario de soporte');

    await type('Nombre', 'Horario de atención');
    await press('Guardar cambios', dialog()!);
    const request = backend.expectOne(`${PLAN_FIELDS_URL}/${SUPPORT_HOURS_FIELD.id}`);
    expect(request.request.body).toEqual({
      label: 'Horario de atención',
      required: true,
      version: 0,
    });
    request.flush({ ...SUPPORT_HOURS_FIELD, label: 'Horario de atención', version: 1 });
    await settle();
    expectList().flush(FIELDS);
    await settle();

    expect(root().textContent).toContain('Guardamos los cambios del campo «Horario de atención».');
  });

  it('reloads the list when someone else changed the field', async () => {
    await render();
    await press('Editar campo Horario de soporte');
    await press('Guardar cambios', dialog()!);

    backend
      .expectOne(`${PLAN_FIELDS_URL}/${SUPPORT_HOURS_FIELD.id}`)
      .flush(problemBody(409, 'STALE_STATE'), { status: 409, statusText: 'Conflict' });
    await settle();
    expectList().flush(FIELDS);
    await settle();

    expect(dialog()).toBeNull();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain(
      'Otra persona cambió este campo.',
    );
  });

  it('retires a field after explaining that saved values are kept', async () => {
    await render();
    await press('Retirar campo Horario de soporte');

    expect(dialog()?.textContent).toContain(
      'El campo dejará de pedirse en los planes. Los valores ya guardados y el historial no se borran.',
    );
    expect(document.activeElement?.textContent?.trim()).toBe('Cancelar');
    await expectNoAxeViolations(root());

    await press('Retirar campo', dialog()!);
    backend
      .expectOne(`${PLAN_FIELDS_URL}/${SUPPORT_HOURS_FIELD.id}/retirement`)
      .flush({ ...SUPPORT_HOURS_FIELD, retiredAt: '2027-02-01T10:00:00Z', version: 1 });
    await settle();
    expectList().flush(FIELDS);
    await settle();

    expect(root().textContent).toContain('Retiramos el campo «Horario de soporte».');
  });

  it('keeps the retire dialog open with the error when it fails', async () => {
    await render();
    await press('Retirar campo Horario de soporte');
    await press('Retirar campo', dialog()!);

    backend
      .expectOne(`${PLAN_FIELDS_URL}/${SUPPORT_HOURS_FIELD.id}/retirement`)
      .flush(problemBody(500, 'UNEXPECTED_RESPONSE'), { status: 500, statusText: 'Error' });
    await settle();

    expect(dialog()?.querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos guardar los cambios.',
    );
  });
});
