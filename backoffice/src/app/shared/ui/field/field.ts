import { Component, computed, input, model } from '@angular/core';
import { LucideCircleAlert } from '@lucide/angular';

export type FieldKind = 'text' | 'select';

export interface FieldOption {
  readonly value: string;
  readonly label: string;
}

const ERROR_ICON_SIZE_PX = 16;

let nextFieldId = 0;

/**
 * Labelled form field (text input or select). Follows the shared contract in
 * design-system/docs/components/field.md. Help and error texts are linked to the
 * control with aria-describedby; an error also sets aria-invalid.
 */
@Component({
  selector: 'app-field',
  imports: [LucideCircleAlert],
  templateUrl: './field.html',
  styleUrl: './field.css',
})
export class Field {
  readonly label = input.required<string>();
  readonly kind = input<FieldKind>('text');
  readonly help = input<string>();
  readonly error = input<string>();
  readonly disabled = input(false);
  readonly placeholder = input<string>();
  readonly options = input<readonly FieldOption[]>([]);
  readonly value = model('');

  protected readonly errorIconSize = ERROR_ICON_SIZE_PX;
  protected readonly controlId = `app-field-${nextFieldId++}`;
  protected readonly helpId = `${this.controlId}-help`;
  protected readonly errorId = `${this.controlId}-error`;

  protected readonly hasError = computed(() => Boolean(this.error()));
  protected readonly describedBy = computed(() => {
    const ids = [this.help() ? this.helpId : null, this.hasError() ? this.errorId : null];
    return ids.filter((id) => id !== null).join(' ') || null;
  });

  protected updateValue(event: Event): void {
    this.value.set((event.target as HTMLInputElement | HTMLSelectElement).value);
  }
}
