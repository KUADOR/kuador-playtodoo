import { ComponentFixture, TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { Checkbox } from './checkbox';

describe('Checkbox', () => {
  let fixture: ComponentFixture<Checkbox>;

  async function render(inputs: Record<string, unknown> = {}) {
    fixture = TestBed.createComponent(Checkbox);
    fixture.componentRef.setInput('label', 'Registrar pagos');
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    await fixture.whenStable();
  }

  const control = () => fixture.nativeElement.querySelector('input') as HTMLInputElement;

  it('labels the native checkbox and links its description', async () => {
    await render({ description: 'Registrar pagos de suscripción.' });

    const describedBy = control().getAttribute('aria-describedby');
    expect(control().labels?.[0]?.textContent).toContain('Registrar pagos');
    expect(fixture.nativeElement.querySelector(`#${describedBy}`)?.textContent).toBe(
      'Registrar pagos de suscripción.',
    );
  });

  it('reports the new value when the row is clicked', async () => {
    await render();
    const changes: boolean[] = [];
    fixture.componentInstance.checked.subscribe((value) => changes.push(value));

    (fixture.nativeElement.querySelector('label') as HTMLLabelElement).click();

    expect(control().checked).toBe(true);
    expect(changes).toEqual([true]);
  });

  it('reflects the checked input and can be disabled', async () => {
    await render({ checked: true, disabled: true });

    expect(control().checked).toBe(true);
    expect(control().disabled).toBe(true);
  });

  it('has no axe violations', async () => {
    await render({ description: 'Descripción' });

    await expectNoAxeViolations(fixture.nativeElement);
  });
});
