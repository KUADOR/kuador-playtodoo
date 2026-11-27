import { ComponentFixture, TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { Field, FieldOption } from './field';

const PLAN_OPTIONS: readonly FieldOption[] = [
  { value: 'basic', label: 'Básico' },
  { value: 'pro', label: 'Profesional' },
];
const ERROR_TEXT = 'Falta el plan. Elige uno para continuar.';

describe('Field', () => {
  let fixture: ComponentFixture<Field>;

  async function render(inputs: Record<string, unknown> = {}) {
    fixture = TestBed.createComponent(Field);
    fixture.componentRef.setInput('label', 'Plan');
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    await fixture.whenStable();
  }

  function control(): HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement {
    return fixture.nativeElement.querySelector('input, textarea, select');
  }

  function describedTexts(): string[] {
    const ids = control().getAttribute('aria-describedby')?.split(' ') ?? [];
    return ids.map((id) => document.getElementById(id)?.textContent?.trim() ?? '');
  }

  describe('text kind', () => {
    it('shows a suffix linked to the control and passes the native hints', async () => {
      await render({
        suffix: '.playtodoo.com',
        inputMode: 'decimal',
        maxLength: 120,
        inputType: 'date',
        max: '2026-12-31',
      });
      document.body.appendChild(fixture.nativeElement);

      expect(fixture.nativeElement.querySelector('.field__suffix')?.textContent).toBe(
        '.playtodoo.com',
      );
      expect(describedTexts()).toEqual(['.playtodoo.com']);
      expect(control().getAttribute('inputmode')).toBe('decimal');
      expect(control().getAttribute('maxlength')).toBe('120');
      expect(control().getAttribute('type')).toBe('date');
      expect(control().getAttribute('max')).toBe('2026-12-31');
      fixture.nativeElement.remove();
    });

    it('defaults to a plain text input without autocomplete or required hints', async () => {
      await render();

      expect(control().getAttribute('type')).toBe('text');
      expect(control().hasAttribute('autocomplete')).toBe(false);
      expect(control().hasAttribute('aria-required')).toBe(false);
    });

    it('renders e-mail and password inputs with their autocomplete and required hints', async () => {
      await render({ inputType: 'password', autocomplete: 'current-password', required: true });

      expect(control().getAttribute('type')).toBe('password');
      expect(control().getAttribute('autocomplete')).toBe('current-password');
      expect(control().getAttribute('aria-required')).toBe('true');
    });

    it('moves the focus to its control on request', async () => {
      await render();
      document.body.appendChild(fixture.nativeElement);

      fixture.componentInstance.focus();

      expect(document.activeElement).toBe(control());
      fixture.nativeElement.remove();
    });

    it('labels the input with a visible label', async () => {
      await render();

      const label: HTMLLabelElement = fixture.nativeElement.querySelector('label');
      expect(control().tagName).toBe('INPUT');
      expect(label.textContent?.trim()).toBe('Plan');
      expect(label.htmlFor).toBe(control().id);
    });

    it('updates the value model as the person types', async () => {
      await render();
      const input = control() as HTMLInputElement;

      input.value = 'Canchas El Sol';
      input.dispatchEvent(new Event('input'));

      expect(fixture.componentInstance.value()).toBe('Canchas El Sol');
    });

    it('links the help text without marking the input invalid', async () => {
      await render({ help: 'Solo letras minúsculas.' });

      expect(describedTexts()).toEqual(['Solo letras minúsculas.']);
      expect(control().hasAttribute('aria-invalid')).toBe(false);
    });

    it('shows the error with an icon, marks the input invalid and links both texts', async () => {
      await render({ help: 'Solo letras minúsculas.', error: ERROR_TEXT });

      const error: HTMLElement = fixture.nativeElement.querySelector('.field__error');
      expect(control().getAttribute('aria-invalid')).toBe('true');
      expect(control().classList).toContain('field__control--error');
      expect(error.querySelector('svg')).not.toBeNull();
      expect(describedTexts()).toEqual(['Solo letras minúsculas.', ERROR_TEXT]);
    });

    it('is not editable when disabled', async () => {
      await render({ disabled: true });

      expect(control().disabled).toBe(true);
    });
  });

  describe('textarea kind', () => {
    it('renders a labelled multi-line control with its length limit and hints', async () => {
      await render({ kind: 'textarea', rows: 5, maxLength: 500, required: true, help: 'Ayuda.' });

      const textarea = control() as HTMLTextAreaElement;
      expect(textarea.tagName).toBe('TEXTAREA');
      expect(textarea.rows).toBe(5);
      expect(textarea.getAttribute('maxlength')).toBe('500');
      expect(textarea.getAttribute('aria-required')).toBe('true');
      expect(describedTexts()).toEqual(['Ayuda.']);
      await expectNoAxeViolations(fixture.nativeElement);
    });

    it('updates the value model as the person types', async () => {
      await render({ kind: 'textarea' });

      control().value = 'Reclamo por una reserva duplicada';
      control().dispatchEvent(new Event('input'));

      expect(fixture.componentInstance.value()).toBe('Reclamo por una reserva duplicada');
    });
  });

  describe('select kind', () => {
    it('renders the options and selects the current value', async () => {
      await render({ kind: 'select', options: PLAN_OPTIONS, value: 'pro' });

      const select = control() as HTMLSelectElement;
      expect(select.tagName).toBe('SELECT');
      expect(Array.from(select.options).map((option) => option.text)).toEqual([
        'Básico',
        'Profesional',
      ]);
      expect(select.value).toBe('pro');
    });

    it('shows the placeholder while there is no value', async () => {
      await render({ kind: 'select', options: PLAN_OPTIONS, placeholder: 'Elige un plan' });

      const select = control() as HTMLSelectElement;
      expect(select.value).toBe('');
      expect(select.selectedOptions[0].text).toBe('Elige un plan');
    });

    it('updates the value model when an option is chosen', async () => {
      await render({ kind: 'select', options: PLAN_OPTIONS, placeholder: 'Elige un plan' });
      const select = control() as HTMLSelectElement;

      select.value = 'basic';
      select.dispatchEvent(new Event('change'));

      expect(fixture.componentInstance.value()).toBe('basic');
    });

    it('marks the select invalid and links the error message', async () => {
      await render({ kind: 'select', options: PLAN_OPTIONS, error: ERROR_TEXT });

      expect(control().getAttribute('aria-invalid')).toBe('true');
      expect(describedTexts()).toEqual([ERROR_TEXT]);
    });
  });

  it('gives each field its own control id', async () => {
    await render();
    const firstId = control().id;

    await render();

    expect(control().id).not.toBe(firstId);
  });

  it.each([
    ['text, empty', {}],
    ['text, with help', { help: 'Solo letras minúsculas.' }],
    ['text, error', { error: ERROR_TEXT }],
    ['text, disabled', { disabled: true, value: 'CES-001' }],
    ['select', { kind: 'select', options: PLAN_OPTIONS }],
    ['select, error', { kind: 'select', options: PLAN_OPTIONS, error: ERROR_TEXT }],
    ['select, disabled', { kind: 'select', options: PLAN_OPTIONS, disabled: true }],
  ])('has no axe violations (%s)', async (_state, inputs) => {
    await render(inputs);

    await expectNoAxeViolations(fixture.nativeElement);
  });
});
