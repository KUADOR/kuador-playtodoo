import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { expectNoAxeViolations } from '../../../testing/axe';
import { CAPABILITY_CHANGES_URL } from '../../../testing/platform-fixtures';
import { provideTestApi, settle } from '../../../testing/platform-testing';
import { CapabilityChangesTable } from './capability-changes-table';
import { CapabilityChange } from './operator-capabilities-api';

function change(index: number, enabled: boolean): CapabilityChange {
  return {
    id: `change-${index}`,
    key: 'TENANT_DATA_ACCESS',
    enabled,
    changedBy: { id: 'u1', displayName: 'Sam Example' },
    changedAt: '2027-01-05T08:00:00Z',
  };
}

describe('CapabilityChangesTable', () => {
  let fixture: ComponentFixture<CapabilityChangesTable>;
  let backend: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({ providers: [provideTestApi(), provideRouter([])] });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CapabilityChangesTable);
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  const root = (): HTMLElement => fixture.nativeElement;

  function answer(page: number, items: CapabilityChange[], totalItems: number): void {
    const request = backend.expectOne((candidate) => candidate.url === CAPABILITY_CHANGES_URL);
    expect(request.request.params.get('page')).toBe(String(page));
    expect(request.request.params.get('size')).toBe('20');
    request.flush({ items, page, size: 20, totalItems });
  }

  it('lists the changes with date, person, capability and result in a captioned table', async () => {
    answer(0, [change(1, true), change(2, false)], 2);
    await settle();

    const rows = Array.from(root().querySelectorAll('tbody tr')).map((row) =>
      Array.from(row.querySelectorAll('td')).map((cell) => cell.textContent?.trim()),
    );
    expect(root().querySelector('caption')?.textContent?.trim()).toBe(
      'Cambios de permisos del operador',
    );
    expect(rows[0]).toEqual([
      expect.stringMatching(/^\d{2}\/\d{2}\/2027 \d{2}:\d{2}$/),
      'Sam Example',
      'Acceder a datos de complejos con motivo',
      'Activada',
    ]);
    expect(rows[1][3]).toBe('Desactivada');
    expect(root().querySelector('td')?.dataset['label']).toBe('Fecha');
  });

  it('pages through the history', async () => {
    answer(0, [change(1, true)], 21);
    await settle();

    (
      Array.from(root().querySelectorAll('button')).find(
        (b) => b.textContent?.trim() === 'Siguiente',
      ) as HTMLButtonElement
    ).click();
    await settle();
    answer(1, [change(21, false)], 21);
    await settle();

    expect(root().textContent).toContain('Mostrando 21–21 de 21');
  });

  it('shows the empty and error states', async () => {
    backend
      .expectOne((candidate) => candidate.url === CAPABILITY_CHANGES_URL)
      .error(new ProgressEvent('error'));
    await settle();
    expect(root().querySelector('[role="alert"]')?.textContent).toContain(
      'No pudimos cargar la información.',
    );

    (root().querySelector('[role="alert"] button') as HTMLButtonElement).click();
    await settle();
    answer(0, [], 0);
    await settle();
    expect(root().textContent).toContain('Todavía no hay cambios.');
  });

  it('has no axe violations', async () => {
    answer(0, [change(1, true)], 30);
    await settle();

    await expectNoAxeViolations(root());
  });
});
