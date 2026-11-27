import { Component, input, output, signal } from '@angular/core';
import { LucideShieldCheck } from '@lucide/angular';

const ICON_SIZE_PX = 20;

/**
 * «Barra de acceso de soporte» of P9: whose data is open, why and for how long. The time left is
 * not announced (only the warning and the expiry are, by the page). Under 720 px the reason is
 * folded behind «Ver motivo».
 */
@Component({
  selector: 'app-support-bar',
  imports: [LucideShieldCheck],
  template: `
    <section class="support-bar" aria-label="Acceso de soporte">
      <p class="support-bar__complex">
        <svg lucideShieldCheck [size]="iconSize" aria-hidden="true"></svg>
        <span>Acceso de soporte a {{ complexName() }}</span>
      </p>
      <p
        class="support-bar__reason"
        [class.support-bar__reason--open]="reasonOpen()"
        id="support-bar-reason"
      >
        <span class="visually-hidden">Motivo: </span>«{{ reason() }}»
      </p>
      <button
        type="button"
        class="support-bar__toggle"
        aria-controls="support-bar-reason"
        [attr.aria-expanded]="reasonOpen()"
        (click)="reasonOpen.set(!reasonOpen())"
      >
        {{ reasonOpen() ? 'Ocultar motivo' : 'Ver motivo' }}
      </button>
      <p class="support-bar__time">{{ timeLeft() }}</p>
      <button type="button" class="support-bar__close" (click)="closeAccess.emit()">
        Cerrar acceso
      </button>
    </section>
  `,
  styles: `
    .support-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--space-2) var(--space-4);
      padding: var(--space-3) var(--space-4);
      border-radius: var(--radius-md);
      background: var(--color-brand-navy);
      color: var(--color-on-primary);
      font-size: var(--font-size-sm);
    }

    .support-bar p {
      margin: 0;
    }

    .support-bar__complex {
      display: inline-flex;
      align-items: center;
      gap: var(--space-2);
      font-weight: var(--font-weight-semibold);
    }

    .support-bar__reason {
      flex: 1 1 12rem;
      min-width: 0;
      overflow-wrap: anywhere;
    }

    .support-bar__time {
      font-weight: var(--font-weight-semibold);
      white-space: nowrap;
    }

    .support-bar__toggle,
    .support-bar__close {
      min-height: var(--size-touch-min);
      padding: 0 var(--space-3);
      border: var(--border-width-sm) solid var(--color-on-primary);
      border-radius: var(--radius-md);
      background: transparent;
      color: var(--color-on-primary);
      font: inherit;
      font-weight: var(--font-weight-semibold);
      cursor: pointer;
    }

    .support-bar__toggle {
      display: none;
    }

    .support-bar__toggle:focus-visible,
    .support-bar__close:focus-visible {
      outline: var(--border-focus-ring) solid var(--color-on-primary);
      outline-offset: var(--border-focus-offset);
    }

    @media (max-width: 719px) {
      .support-bar__reason {
        display: none;
        flex-basis: 100%;
        order: 1;
      }

      .support-bar__reason--open {
        display: block;
      }

      .support-bar__toggle {
        display: inline-flex;
        align-items: center;
      }
    }
  `,
})
export class SupportBar {
  readonly complexName = input.required<string>();
  readonly reason = input.required<string>();
  /** «quedan 12 min». */
  readonly timeLeft = input.required<string>();

  readonly closeAccess = output<void>();

  protected readonly iconSize = ICON_SIZE_PX;
  protected readonly reasonOpen = signal(false);
}
