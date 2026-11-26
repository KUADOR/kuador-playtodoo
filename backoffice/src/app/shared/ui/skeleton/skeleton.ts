import { Component, computed, input } from '@angular/core';

const LOADING_TEXT = 'Cargando…';

/**
 * Loading placeholder with the shape of a list or table («Reglas comunes» of «Pantallas»):
 * `rows` grey bars. Announces «Cargando…» to screen readers once.
 */
@Component({
  selector: 'app-skeleton',
  template: `
    <span class="visually-hidden">{{ loadingText }}</span>
    @for (row of rowIndexes(); track row) {
      <span class="skeleton__row"></span>
    }
  `,
  styles: `
    :host {
      --skeleton-pulse-duration: 1.5s;

      display: flex;
      flex-direction: column;
      gap: var(--space-2);
    }

    .skeleton__row {
      display: block;
      height: var(--size-touch-min);
      border-radius: var(--radius-md);
      background: var(--color-disabled-bg);
      animation: skeleton-pulse var(--skeleton-pulse-duration) ease-in-out infinite;
    }

    @keyframes skeleton-pulse {
      50% {
        opacity: 0.5;
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .skeleton__row {
        animation: none;
      }
    }
  `,
})
export class Skeleton {
  readonly rows = input(3);

  protected readonly loadingText = LOADING_TEXT;
  protected readonly rowIndexes = computed(() =>
    Array.from({ length: this.rows() }, (_, index) => index),
  );
}
