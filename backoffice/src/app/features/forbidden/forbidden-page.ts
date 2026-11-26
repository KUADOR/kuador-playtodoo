import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HOME_URL } from '../../core/app-paths';
import { Alert } from '../../shared/ui/alert/alert';

/** Direct address to a section the user may not open («Pantallas», «Navegación y permisos»). */
@Component({
  selector: 'app-forbidden-page',
  imports: [Alert, RouterLink],
  template: `
    <h1>Sin permiso</h1>
    <app-alert tone="error">
      No tienes permiso para ver esta sección.
      <a alertActions class="forbidden__link" [routerLink]="homeUrl">Ir a Complejos</a>
    </app-alert>
  `,
  styles: `
    .forbidden__link {
      display: inline-flex;
      align-items: center;
      min-height: var(--size-touch-min);
      color: var(--color-primary);
      font-weight: var(--font-weight-semibold);
    }

    .forbidden__link:focus-visible {
      outline: var(--border-focus-ring) solid var(--color-focus);
      outline-offset: var(--border-focus-offset);
    }
  `,
})
export class ForbiddenPage {
  protected readonly homeUrl = HOME_URL;
}
