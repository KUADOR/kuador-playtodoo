import { Component, input, model } from '@angular/core';

let nextCheckboxId = 0;

/**
 * Checkbox of «Identidad visual» v3 («Casilla»): a native checkbox in a row of 44 px or more
 * where the whole row toggles it. The description is linked with aria-describedby.
 */
@Component({
  selector: 'app-checkbox',
  templateUrl: './checkbox.html',
  styleUrl: './checkbox.css',
})
export class Checkbox {
  readonly label = input.required<string>();
  readonly description = input<string>();
  readonly disabled = input(false);
  readonly checked = model(false);

  protected readonly controlId = `app-checkbox-${nextCheckboxId++}`;
  protected readonly descriptionId = `${this.controlId}-description`;

  protected toggle(event: Event): void {
    this.checked.set((event.target as HTMLInputElement).checked);
  }
}
