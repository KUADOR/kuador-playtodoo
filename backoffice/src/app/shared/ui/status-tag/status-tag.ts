import { Component, input } from '@angular/core';
import {
  LucideCircleCheck,
  LucideCirclePause,
  LucideCircleX,
  LucideClock,
  LucideMinus,
  LucidePencil,
  LucideTrash,
} from '@lucide/angular';

export type StatusTagTone = 'success' | 'warning' | 'danger' | 'neutral';

/** Icons of «Etiqueta de estado» (Identidad visual v3). */
export type StatusTagIcon = 'check' | 'clock' | 'pause' | 'cross' | 'pencil' | 'trash' | 'dash';

/** What a tag shows: always an icon and a text, never only a colour. */
export interface StatusTagLook {
  readonly label: string;
  readonly tone: StatusTagTone;
  readonly icon: StatusTagIcon;
}

const ICON_SIZE_PX = 16;

/**
 * «Etiqueta de estado» of «Identidad visual» v3: a non-interactive pill with an icon and a
 * text in the colour of its tone.
 */
@Component({
  selector: 'app-status-tag',
  imports: [
    LucideCircleCheck,
    LucideCirclePause,
    LucideCircleX,
    LucideClock,
    LucideMinus,
    LucidePencil,
    LucideTrash,
  ],
  template: `
    @let current = look();
    <span class="status-tag__icon" aria-hidden="true">
      @switch (current.icon) {
        @case ('check') {
          <svg lucideCircleCheck [size]="iconSize"></svg>
        }
        @case ('clock') {
          <svg lucideClock [size]="iconSize"></svg>
        }
        @case ('pause') {
          <svg lucideCirclePause [size]="iconSize"></svg>
        }
        @case ('cross') {
          <svg lucideCircleX [size]="iconSize"></svg>
        }
        @case ('pencil') {
          <svg lucidePencil [size]="iconSize"></svg>
        }
        @case ('trash') {
          <svg lucideTrash [size]="iconSize"></svg>
        }
        @default {
          <svg lucideMinus [size]="iconSize"></svg>
        }
      }
    </span>
    <span>{{ current.label }}</span>
  `,
  styles: `
    :host {
      --status-tag-color: var(--color-fg-muted);

      display: inline-flex;
      align-items: center;
      gap: var(--space-1);
      padding: 0 var(--space-2);
      border: var(--border-width-sm) solid var(--status-tag-color);
      border-radius: var(--radius-pill);
      background: var(--color-surface);
      color: var(--status-tag-color);
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      line-height: var(--line-height-body);
      white-space: nowrap;
    }

    :host([data-tone='success']) {
      --status-tag-color: var(--color-success);
    }

    :host([data-tone='warning']) {
      --status-tag-color: var(--color-warning);
    }

    :host([data-tone='danger']) {
      --status-tag-color: var(--color-danger);
    }

    .status-tag__icon {
      display: inline-flex;
    }
  `,
  host: { '[attr.data-tone]': 'look().tone' },
})
export class StatusTag {
  readonly look = input.required<StatusTagLook>();

  protected readonly iconSize = ICON_SIZE_PX;
}
