import { HttpTestingController, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../../testing/axe';
import {
  CAPABILITIES_URL,
  CAPABILITY_CHANGES_URL,
  INITIAL_CAPABILITIES,
  problemBody,
} from '../../../testing/platform-fixtures';
import { provideTestApi, settle } from '../../../testing/platform-testing';
import { Page } from '../../core/api/page';
import { CapabilityChange, OperatorCapabilities } from './operator-capabilities-api';
import { OperatorPermissionsPage } from './operator-permissions-page';

const EMPTY_CHANGES: Page<CapabilityChange> = { items: [], page: 0, size: 20, totalItems: 0 };
const PAYMENT_CHANGE: CapabilityChange = {
  id: '5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b',
  key: 'SUBSCRIPTION_PAYMENT_RECORD',
  enabled: true,
  changedBy: { id: '4e5f6a7b-8c9d-4e0f-a1b2-c3d4e5f6a7b8', displayName: 'Sam Example' },
  changedAt: '2027-01-05T08:00:00Z',
};

describe('OperatorPermissionsPage (P13)', () => {
  let fixture: ComponentFixture<OperatorPermissionsPage>;
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
  const checkboxes = () =>
    Array.from(root().querySelectorAll('fieldset input[type="checkbox"]')) as HTMLInputElement[];
  const button = (label: string) =>
    Array.from(root().querySelectorAll('button')).find(
      (element) => element.textContent?.trim() === label,
    ) as HTMLButtonElement | undefined;
  const dialog = () => root().querySelector('[role="dialog"]') as HTMLElement | null;
  const dialogButton = (label: string) =>
    Array.from(dialog()?.querySelectorAll('button') ?? []).find(
      (element) => element.textContent?.trim() === label,
    );

  function expectChanges(): TestRequest {
    return backend.expectOne((request) => request.url === CAPABILITY_CHANGES_URL);
  }

  async function render(
    capabilities: OperatorCapabilities = INITIAL_CAPABILITIES,
    changes: Page<CapabilityChange> = EMPTY_CHANGES,
  ): Promise<void> {
    fixture = TestBed.createComponent(OperatorPermissionsPage);
    document.body.appendChild(fixture.nativeElement);
    await fixture.whenStable();
    backend.expectOne(CAPABILITIES_URL).flush(capabilities);
    expectChanges().flush(changes);
    await settle();
  }

  async function toggle(index: number): Promise<void> {
    checkboxes()[index].click();
    await settle();
  }

  it('shows skeletons while the capabilities load', async () => {
    fixture = TestBed.createComponent(OperatorPermissionsPage);
    await fixture.whenStable();

    expect(root().querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(root().textContent).toContain('Cargando…');
    backend.expectOne(CAPABILITIES_URL).flush(INITIAL_CAPABILITIES);
    expectChanges().flush(EMPTY_CHANGES);
  });

  it('lists every capability of the api, all off at first, in Spanish (CA15)', async () => {
    await render();

    expect(
      checkboxes().map((box) => box.labels?.[0]?.querySelector('.checkbox__label')?.textContent),
    ).toEqual([
      'Dar de alta complejos',
      'Registrar pagos',
      'Cambiar estados',
      'Acceder a datos de complejos con motivo',
    ]);
    expect(checkboxes().every((box) => !box.checked)).toBe(true);
    expect(root().textContent).toContain('Todavía no hay cambios.');
  });

  it('shows a capability added later with the api description until it has a Spanish text', async () => {
    await render({
      version: 3,
      items: [
        {
          key: 'NEW_CAPABILITY' as never,
          description: 'A capability added later.',
          enabled: false,
        },
      ],
    });

    expect(root().querySelector('fieldset')?.textContent).toContain('A capability added later.');
  });

  it('enables «Guardar cambios» and «Descartar» only when something changed', async () => {
    await render();
    expect(button('Guardar cambios')?.disabled).toBe(true);
    expect(button('Descartar')?.disabled).toBe(true);

    await toggle(1);
    expect(button('Guardar cambios')?.disabled).toBe(false);

    button('Descartar')?.click();
    await settle();
    expect(checkboxes()[1].checked).toBe(false);
    expect(button('Guardar cambios')?.disabled).toBe(true);
  });

  it('asks for confirmation with what will be enabled and disabled, starting on «Cancelar»', async () => {
    await render({
      version: 4,
      items: INITIAL_CAPABILITIES.items.map((item) => ({
        ...item,
        enabled: item.key === 'COMPLEX_CREATE',
      })),
    });
    await toggle(0);
    await toggle(1);

    button('Guardar cambios')?.click();
    await settle();

    expect(dialog()?.textContent).toContain('Se activarán:');
    expect(dialog()?.textContent).toContain('Registrar pagos');
    expect(dialog()?.textContent).toContain('Se desactivarán:');
    expect(dialog()?.textContent).toContain('Dar de alta complejos');
    expect(document.activeElement).toBe(dialogButton('Cancelar'));
  });

  it('saves only the changes with the version read, then shows the result and the new history (CA16)', async () => {
    await render();
    await toggle(1);
    button('Guardar cambios')?.click();
    await settle();

    dialogButton('Guardar cambios')?.click();
    await settle();
    const save = backend.expectOne(CAPABILITIES_URL);
    expect(save.request.method).toBe('PUT');
    expect(save.request.body).toEqual({
      version: 0,
      changes: [{ key: 'SUBSCRIPTION_PAYMENT_RECORD', enabled: true }],
    });
    save.flush({
      version: 1,
      items: INITIAL_CAPABILITIES.items.map((item) => ({
        ...item,
        enabled: item.key === 'SUBSCRIPTION_PAYMENT_RECORD',
      })),
    });
    await settle();
    expectChanges().flush({ ...EMPTY_CHANGES, items: [PAYMENT_CHANGE], totalItems: 1 });
    await settle();

    expect(dialog()).toBeNull();
    expect(root().querySelector('[role="status"]')?.textContent).toContain(
      'Guardamos los permisos del operador.',
    );
    expect(checkboxes()[1].checked).toBe(true);
    expect(button('Guardar cambios')?.disabled).toBe(true);
    const row = root().querySelector('tbody tr');
    expect(row?.textContent).toContain('Sam Example');
    expect(row?.textContent).toContain('Registrar pagos');
    expect(row?.textContent).toContain('Activada');
  });

  it('reloads and explains when someone else changed the capabilities (409)', async () => {
    await render();
    await toggle(0);
    button('Guardar cambios')?.click();
    await settle();
    dialogButton('Guardar cambios')?.click();
    await settle();

    backend
      .expectOne(CAPABILITIES_URL)
      .flush(problemBody(409, 'STALE_STATE'), { status: 409, statusText: 'Conflict' });
    await settle();
    backend.expectOne(CAPABILITIES_URL).flush({ ...INITIAL_CAPABILITIES, version: 2 });
    await settle();

    expect(dialog()).toBeNull();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain(
      'Otra persona cambió los permisos del operador. Revisa los datos y vuelve a intentarlo.',
    );
    expect(checkboxes()[0].checked).toBe(false);
  });

  it('keeps the dialog and the choice when saving fails for another reason', async () => {
    await render();
    await toggle(0);
    button('Guardar cambios')?.click();
    await settle();
    dialogButton('Guardar cambios')?.click();
    await settle();

    backend
      .expectOne(CAPABILITIES_URL)
      .flush(problemBody(403, 'FORBIDDEN'), { status: 403, statusText: 'Forbidden' });
    await settle();

    expect(dialog()?.querySelector('[role="alert"]')?.textContent).toContain(
      'Ya no tienes permiso para esta acción.',
    );
    expect(checkboxes()[0].checked).toBe(true);
  });

  it('shows the load error with «Reintentar»', async () => {
    fixture = TestBed.createComponent(OperatorPermissionsPage);
    await fixture.whenStable();
    backend.expectOne(CAPABILITIES_URL).error(new ProgressEvent('error'));
    expectChanges().flush(EMPTY_CHANGES);
    await settle();

    expect(root().querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cargar la información.',
    );
    button('Reintentar')?.click();
    await settle();
    backend.expectOne(CAPABILITIES_URL).flush(INITIAL_CAPABILITIES);
    await settle();
    expect(checkboxes()).toHaveLength(4);
  });

  it('says so when there are no capabilities to configure', async () => {
    await render({ version: 0, items: [] });

    expect(root().textContent).toContain('No hay funciones para configurar.');
  });

  it('has no axe violations with data, history and the confirmation open', async () => {
    await render(INITIAL_CAPABILITIES, {
      ...EMPTY_CHANGES,
      items: [PAYMENT_CHANGE],
      totalItems: 1,
    });
    await expectNoAxeViolations(root());

    await toggle(2);
    button('Guardar cambios')?.click();
    await settle();
    await expectNoAxeViolations(root());
  });
});
