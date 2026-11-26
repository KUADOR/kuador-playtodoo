import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';
import { Alert } from '../../shared/ui/alert/alert';
import { SECTION_HEADING_KEY } from './section-heading';

/**
 * Placeholder of a menu section whose screens belong to another task of PLATODO-27
 * («Complejos»: PLATODO-90). That task replaces it on its route.
 */
@Component({
  selector: 'app-upcoming-section-page',
  imports: [Alert],
  template: `
    <h1>{{ heading() }}</h1>
    <app-alert tone="info">Esta sección todavía está en construcción.</app-alert>
  `,
})
export class UpcomingSectionPage {
  protected readonly heading = toSignal(
    inject(ActivatedRoute).data.pipe(map((data) => String(data[SECTION_HEADING_KEY] ?? ''))),
    { initialValue: '' },
  );
}
