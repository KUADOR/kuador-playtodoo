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

export type StatusBadgeTone = 'success' | 'warning' | 'danger' | 'neutral';

/** Icons of «Etiqueta de estado» (v3): the status is never told by colour alone. */
export type StatusBadgeIcon = 'check' | 'clock' | 'pause' | 'cross' | 'pencil' | 'trash' | 'dash';

export interface StatusBadgeSpec {
  readonly label: string;
  readonly tone: StatusBadgeTone;
  readonly icon: StatusBadgeIcon;
}

const ICON_SIZE_PX = 16;

/**
 * Status label of «Identidad visual» v3 («Etiqueta de estado»): a pill with icon and text in the
 * colour of its tone. Not interactive; the icon is decorative because the text says it all.
 */
@Component({
  selector: 'app-status-badge',
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
    <span class="status-badge__icon" aria-hidden="true">
      @switch (status().icon) {
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
    <span>{{ status().label }}</span>
  `,
  styleUrl: './status-badge.css',
  host: { '[attr.data-tone]': 'status().tone' },
})
export class StatusBadge {
  readonly status = input.required<StatusBadgeSpec>();

  protected readonly iconSize = ICON_SIZE_PX;
}
