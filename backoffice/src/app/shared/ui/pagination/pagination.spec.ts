import { ComponentFixture, TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { Pagination } from './pagination';

describe('Pagination', () => {
  let fixture: ComponentFixture<Pagination>;
  let requested: number[];

  async function render(page: number, totalItems: number) {
    fixture = TestBed.createComponent(Pagination);
    fixture.componentRef.setInput('page', page);
    fixture.componentRef.setInput('size', 20);
    fixture.componentRef.setInput('totalItems', totalItems);
    requested = [];
    fixture.componentInstance.pageChange.subscribe((value) => requested.push(value));
    await fixture.whenStable();
  }

  const buttons = () =>
    Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];

  it('is hidden when everything fits in one page', async () => {
    await render(0, 20);

    expect(fixture.nativeElement.querySelector('nav')).toBeNull();
  });

  it('shows the range and disables «Anterior» on the first page', async () => {
    await render(0, 54);

    expect(fixture.nativeElement.textContent).toContain('Mostrando 1–20 de 54');
    expect(buttons()[0].disabled).toBe(true);
    buttons()[1].click();
    expect(requested).toEqual([1]);
  });

  it('disables «Siguiente» on the last page', async () => {
    await render(2, 54);

    expect(fixture.nativeElement.textContent).toContain('Mostrando 41–54 de 54');
    expect(buttons()[1].disabled).toBe(true);
    buttons()[0].click();
    expect(requested).toEqual([1]);
  });

  it('has no axe violations', async () => {
    await render(1, 54);

    await expectNoAxeViolations(fixture.nativeElement);
  });
});
