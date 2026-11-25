import { Component, input } from '@angular/core';
import { LucideLock } from '@lucide/angular';
import { SlotView } from './calendar-model';

const LOCK_ICON_SIZE_PX = 16;
const OCCUPIED_TEXT = 'Ocupado';

/**
 * One read-only slot, identical in the day and week views. It has no action, hover or focus:
 * the visible text is hidden from screen readers, which read the contract's accessible label.
 */
@Component({
  selector: 'app-calendar-slot',
  imports: [LucideLock],
  template: `
    <span class="slot__label" aria-hidden="true">{{ slot().label }}</span>
    @if (slot().status === 'occupied') {
      <span class="slot__occupied" aria-hidden="true">
        <svg lucideLock [size]="lockIconSize"></svg>{{ occupiedText }}
      </span>
    }
    <span class="visually-hidden">{{ slot().accessibleLabel }}</span>
  `,
  styleUrl: './calendar-slot.css',
  host: { '[attr.data-status]': 'slot().status' },
})
export class CalendarSlot {
  readonly slot = input.required<SlotView>();

  protected readonly lockIconSize = LOCK_ICON_SIZE_PX;
  protected readonly occupiedText = OCCUPIED_TEXT;
}
