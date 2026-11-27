import { Component, ElementRef, computed, input, model, viewChild } from '@angular/core';
import { LucideCircleAlert } from '@lucide/angular';

export type FieldKind = 'text' | 'select';

/** `type` of the native input when `kind` is `text`. */
export type TextInputType = 'text' | 'email' | 'password' | 'date';

/** Native `inputmode`: which on-screen keyboard a phone shows. */
export type InputMode = 'text' | 'decimal' | 'numeric' | 'email';

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
  readonly inputType = input<TextInputType>('text');
  /** Native `autocomplete` hint, for example `username` or `current-password`. */
  readonly autocomplete = input<string>();
  readonly required = input(false);
  readonly help = input<string>();
  readonly error = input<string>();
  readonly disabled = input(false);
  readonly placeholder = input<string>();
  readonly inputMode = input<InputMode>();
  readonly maxLength = input<number>();
  /** Native `max`, for example the latest date allowed. */
  readonly max = input<string>();
  /** Fixed text shown right after the control, for example `.playtodoo.com`. */
  readonly suffix = input<string>();
  readonly options = input<readonly FieldOption[]>([]);
  readonly value = model('');

  protected readonly errorIconSize = ERROR_ICON_SIZE_PX;
  /** Id of the native control, for example to link it from an error summary. */
  readonly controlId = `app-field-${nextFieldId++}`;
  protected readonly helpId = `${this.controlId}-help`;
  protected readonly errorId = `${this.controlId}-error`;
  protected readonly suffixId = `${this.controlId}-suffix`;

  protected readonly hasError = computed(() => Boolean(this.error()));
  protected readonly describedBy = computed(() => {
    const ids = [
      this.suffix() ? this.suffixId : null,
      this.help() ? this.helpId : null,
      this.hasError() ? this.errorId : null,
    ];
    return ids.filter((id) => id !== null).join(' ') || null;
  });

  private readonly control =
    viewChild.required<ElementRef<HTMLInputElement | HTMLSelectElement>>('control');

  /** Moves the focus to the control, for example to the first field with an error. */
  focus(): void {
    this.control().nativeElement.focus();
  }

  protected updateValue(event: Event): void {
    this.value.set((event.target as HTMLInputElement | HTMLSelectElement).value);
  }
}
