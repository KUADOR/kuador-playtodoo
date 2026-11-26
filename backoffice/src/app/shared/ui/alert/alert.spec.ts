import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { Alert, AlertTone } from './alert';

@Component({
  imports: [Alert],
  template: `
    <app-alert
      [tone]="tone()"
      [heading]="heading()"
      [dismissible]="dismissible()"
      (dismiss)="dismissed = true"
    >
      Texto del aviso
      <button alertActions type="button">Reintentar</button>
    </app-alert>
  `,
})
class AlertHost {
  readonly tone = signal<AlertTone>('info');
  readonly heading = signal<string | undefined>(undefined);
  readonly dismissible = signal(false);
  dismissed = false;
}

describe('Alert', () => {
  async function render(setup: (host: AlertHost) => void = () => undefined) {
    const fixture = TestBed.createComponent(AlertHost);
    setup(fixture.componentInstance);
    await fixture.whenStable();
    return { fixture, alert: fixture.nativeElement.querySelector('app-alert') as HTMLElement };
  }

  it.each<[AlertTone, string]>([
    ['success', 'status'],
    ['warning', 'status'],
    ['info', 'status'],
    ['error', 'alert'],
  ])('announces the %s tone with role=%s', async (tone, role) => {
    const { alert } = await render((host) => host.tone.set(tone));

    expect(alert.getAttribute('role')).toBe(role);
    expect(alert.dataset['tone']).toBe(tone);
    expect(alert.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('shows the heading, the text and the actions', async () => {
    const { alert } = await render((host) => host.heading.set('Atención'));

    expect(alert.textContent).toContain('Atención');
    expect(alert.textContent).toContain('Texto del aviso');
    expect(alert.querySelector('.alert__actions button')?.textContent).toBe('Reintentar');
  });

  it('is fixed by default and can be dismissed only when it is a result notice', async () => {
    const fixed = await render();
    expect(fixed.alert.querySelector('.alert__dismiss')).toBeNull();

    const result = await render((host) => host.dismissible.set(true));
    const dismiss = result.alert.querySelector<HTMLButtonElement>('.alert__dismiss');
    dismiss?.click();

    expect(dismiss?.getAttribute('aria-label')).toBe('Cerrar aviso');
    expect(result.fixture.componentInstance.dismissed).toBe(true);
  });

  it('has no axe violations', async () => {
    const { fixture } = await render((host) => host.dismissible.set(true));

    await expectNoAxeViolations(fixture.nativeElement);
  });
});
