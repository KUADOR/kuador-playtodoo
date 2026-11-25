import { ComponentFixture, TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { Button, ButtonVariant } from './button';

describe('Button', () => {
  let fixture: ComponentFixture<Button>;
  let pressCount: number;

  async function render(
    inputs: Partial<Record<'label' | 'variant' | 'loading' | 'disabled' | 'type', unknown>> = {},
  ) {
    fixture = TestBed.createComponent(Button);
    fixture.componentRef.setInput('label', 'Guardar cambios');
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    pressCount = 0;
    fixture.componentInstance.press.subscribe(() => pressCount++);
    await fixture.whenStable();
  }

  function nativeButton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button');
  }

  it('renders the label as the accessible name and defaults to a primary, non-submitting button', async () => {
    await render();

    expect(nativeButton().textContent?.trim()).toBe('Guardar cambios');
    expect(nativeButton().classList).toContain('button--primary');
    expect(nativeButton().type).toBe('button');
  });

  it.each<ButtonVariant>(['primary', 'secondary', 'danger', 'text'])(
    'applies the %s variant class',
    async (variant) => {
      await render({ variant });

      expect(nativeButton().classList).toContain(`button--${variant}`);
    },
  );

  it('emits press when clicked', async () => {
    await render();

    nativeButton().click();

    expect(pressCount).toBe(1);
  });

  it('shows the loading text, announces busy and does not emit press while loading', async () => {
    await render({ loading: true });

    nativeButton().click();

    expect(pressCount).toBe(0);
    expect(nativeButton().textContent?.trim()).toBe('Guardando…');
    expect(nativeButton().getAttribute('aria-busy')).toBe('true');
    expect(nativeButton().getAttribute('aria-disabled')).toBe('true');
    expect(nativeButton().querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('prevents a submit button from submitting its form while loading', async () => {
    await render({ loading: true, type: 'submit' });
    const click = new MouseEvent('click', { bubbles: true, cancelable: true });

    nativeButton().dispatchEvent(click);

    expect(click.defaultPrevented).toBe(true);
  });

  it('is disabled natively and does not emit press when disabled', async () => {
    await render({ disabled: true });

    nativeButton().click();

    expect(nativeButton().disabled).toBe(true);
    expect(pressCount).toBe(0);
  });

  it.each([
    ['normal', {}],
    ['loading', { loading: true }],
    ['disabled', { disabled: true }],
  ])('has no axe violations in the %s state', async (_state, inputs) => {
    await render(inputs);

    await expectNoAxeViolations(fixture.nativeElement);
  });
});
