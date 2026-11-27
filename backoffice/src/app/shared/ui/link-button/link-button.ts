import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonVariant } from '../button/button';

/**
 * A link that looks like a button («Crear plan», «Campos de plan»): it navigates, so it is an
 * `<a>` for screen readers and the browser, with the look of `app-button` (same stylesheet).
 */
@Component({
  selector: 'app-link-button',
  imports: [RouterLink],
  template: `<a class="button link-button" [class]="'button--' + variant()" [routerLink]="url()">{{
    label()
  }}</a>`,
  styleUrls: ['../button/button.css', './link-button.css'],
})
export class LinkButton {
  readonly label = input.required<string>();
  readonly url = input.required<string>();
  readonly variant = input<Exclude<ButtonVariant, 'danger'>>('primary');
}
