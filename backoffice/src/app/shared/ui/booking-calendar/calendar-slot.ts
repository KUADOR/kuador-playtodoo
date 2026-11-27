import { Component, input } from '@angular/core';
import { LucideCheck, LucideLock } from '@lucide/angular';
import { SlotView } from './calendar-model';

const ICON_SIZE_PX = 16;
const OCCUPIED_TEXT = 'Ocupado';

/**
 * One slot, identical in the day and week views. It has no action, hover or focus of its own (in
 * an editable calendar the button around it has them): the visible text is hidden from screen
 * readers, which read the contract's accessible label.
 */
@Component({
  selector: 'app-calendar-slot',
  imports: [LucideCheck, LucideLock],
  template: `
    <span class="slot__line" aria-hidden="true">
      @if (slot().status === 'selected') {
        <svg lucideCheck [size]="iconSize"></svg>
      }
      <span class="slot__label">{{ slot().label }}</span>
    </span>
    @if (slot().status === 'occupied') {
      <span class="slot__occupied" aria-hidden="true">
        <svg lucideLock [size]="iconSize"></svg>{{ occupiedText }}
      </span>
    }
    <span class="visually-hidden">{{ slot().accessibleLabel }}</span>
  `,
  styleUrl: './calendar-slot.css',
  host: { '[attr.data-status]': 'slot().status' },
})
export class CalendarSlot {
  readonly slot = input.required<SlotView>();

  protected readonly iconSize = ICON_SIZE_PX;
  protected readonly occupiedText = OCCUPIED_TEXT;
}
