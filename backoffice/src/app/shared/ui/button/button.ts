import { Component, computed, input, output } from '@angular/core';
import { LucideLoaderCircle } from '@lucide/angular';

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'text';
export type ButtonType = 'button' | 'submit' | 'reset';

const DEFAULT_LOADING_LABEL = 'Guardando…';
const SPINNER_SIZE_PX = 18;

/**
 * Base action button. Follows the shared contract in design-system/docs/components/button.md.
 * `press` is emitted only when the button is neither loading nor disabled.
 */
@Component({
  selector: 'app-button',
  imports: [LucideLoaderCircle],
  templateUrl: './button.html',
  styleUrl: './button.css',
})
export class Button {
  readonly label = input.required<string>();
  readonly variant = input<ButtonVariant>('primary');
  readonly type = input<ButtonType>('button');
  readonly loading = input(false);
  readonly disabled = input(false);
  readonly loadingLabel = input(DEFAULT_LOADING_LABEL);

  readonly press = output<void>();

  protected readonly spinnerSize = SPINNER_SIZE_PX;
  protected readonly visibleLabel = computed(() =>
    this.loading() ? this.loadingLabel() : this.label(),
  );

  protected handleClick(event: MouseEvent): void {
    if (this.loading() || this.disabled()) {
      event.preventDefault();
      return;
    }
    this.press.emit();
  }
}
