import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { expectNoAxeViolations } from '../../../../testing/axe';
import { Dialog } from './dialog';

@Component({
  imports: [Dialog],
  template: `
    <button type="button" id="trigger" (click)="open.set(true)">Abrir</button>
    @if (open()) {
      <app-dialog heading="Suspender complejo" [busy]="busy()" (closeRequest)="open.set(false)">
        <p>Texto</p>
        <button
          dialogActions
          type="button"
          id="cancel"
          [attr.data-dialog-initial-focus]="markCancel() ? '' : null"
        >
          Cancelar
        </button>
        <button dialogActions type="button" id="confirm">Suspender complejo</button>
      </app-dialog>
    }
  `,
})
class DialogHost {
  readonly open = signal(false);
  readonly busy = signal(false);
  readonly markCancel = signal(true);
}

describe('Dialog', () => {
  async function openDialog(setup: (host: DialogHost) => void = () => undefined) {
    const fixture = TestBed.createComponent(DialogHost);
    document.body.appendChild(fixture.nativeElement);
    setup(fixture.componentInstance);
    await fixture.whenStable();
    const trigger = fixture.nativeElement.querySelector('#trigger') as HTMLButtonElement;
    trigger.focus();
    trigger.click();
    await fixture.whenStable();
    return {
      fixture,
      trigger,
      panel: fixture.nativeElement.querySelector('[role="dialog"]') as HTMLElement,
    };
  }

  function pressKey(target: HTMLElement, key: string, shiftKey = false): KeyboardEvent {
    const event = new KeyboardEvent('keydown', { key, shiftKey, bubbles: true, cancelable: true });
    target.dispatchEvent(event);
    return event;
  }

  afterEach(() => document.body.replaceChildren());

  it('is a labelled modal dialog', async () => {
    const { panel } = await openDialog();

    expect(panel.getAttribute('aria-modal')).toBe('true');
    expect(document.getElementById(panel.getAttribute('aria-labelledby') ?? '')?.textContent).toBe(
      'Suspender complejo',
    );
  });

  it('starts on the marked element, or on the first focusable one', async () => {
    await openDialog();
    expect(document.activeElement?.id).toBe('cancel');

    document.body.replaceChildren();
    await openDialog((host) => host.markCancel.set(false));
    expect(document.activeElement?.id).toBe('cancel');
  });

  it('keeps Tab inside the dialog', async () => {
    const { panel } = await openDialog();
    const confirm = panel.querySelector<HTMLButtonElement>('#confirm');
    confirm?.focus();

    const forward = pressKey(confirm as HTMLElement, 'Tab');
    expect(forward.defaultPrevented).toBe(true);
    expect(document.activeElement?.id).toBe('cancel');

    const backward = pressKey(document.activeElement as HTMLElement, 'Tab', true);
    expect(backward.defaultPrevented).toBe(true);
    expect(document.activeElement?.id).toBe('confirm');
  });

  it('closes with Escape and gives the focus back to the trigger', async () => {
    const { fixture, panel, trigger } = await openDialog();

    pressKey(panel, 'Escape');
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeNull();
    expect(document.activeElement).toBe(trigger);
  });

  it('cannot be closed while busy', async () => {
    const { fixture, panel } = await openDialog((host) => host.busy.set(true));

    pressKey(panel, 'Escape');
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[role="dialog"]')).not.toBeNull();
    expect(panel.getAttribute('aria-busy')).toBe('true');
  });

  it('has no axe violations', async () => {
    const { fixture } = await openDialog();

    await expectNoAxeViolations(fixture.nativeElement);
  });
});
