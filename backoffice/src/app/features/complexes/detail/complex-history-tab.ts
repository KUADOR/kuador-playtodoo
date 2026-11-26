import { Component, computed, effect, inject, untracked } from '@angular/core';
import { PagedList } from '../../../core/api/paged-list';
import { formatDateTime } from '../../../core/format/date-format';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Pagination } from '../../../shared/ui/pagination/pagination';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { StatusTag } from '../../../shared/ui/status-tag/status-tag';
import { EMPTY_VALUE, historyStatusLook } from '../complex-texts';
import { ComplexesApi, HistoryEntry } from '../complexes-api';
import { ComplexDetailStore } from './complex-detail-store';

const SKELETON_ROWS = 3;

const KIND_LABELS: Readonly<Record<HistoryEntry['kind'], string>> = {
  PLAN: 'Plan',
  STATUS: 'Estado',
};

/** P6 · Historial (CA5, CA6): plan and status changes of the last year, newest first. */
@Component({
  selector: 'app-complex-history-tab',
  imports: [Alert, Button, Pagination, Skeleton, StatusTag],
  template: `
    @let current = history.state();
    <section
      class="history"
      aria-labelledby="history-heading"
      [attr.aria-busy]="current.status === 'loading'"
    >
      <h2 id="history-heading" class="visually-hidden">Historial</h2>
      @if (current.status === 'loading') {
        <app-skeleton [rows]="skeletonRows" />
      } @else if (current.status === 'error') {
        <app-alert tone="error">
          {{ current.message }}
          <app-button
            alertActions
            label="Reintentar"
            variant="secondary"
            (press)="history.load(0)"
          />
        </app-alert>
      } @else if (current.page.totalItems === 0) {
        <p>No hay cambios en el último año.</p>
      } @else {
        <table class="data-table">
          <caption class="visually-hidden">
            Cambios de plan y de estado de la suscripción
          </caption>
          <thead>
            <tr>
              <th scope="col">Fecha y hora</th>
              <th scope="col">Tipo</th>
              <th scope="col">Antes</th>
              <th scope="col">Después</th>
              <th scope="col">Responsable</th>
            </tr>
          </thead>
          <tbody>
            @for (entry of current.page.items; track entry.id) {
              <tr>
                <td data-label="Fecha y hora">{{ formatDateTime(entry.changedAt) }}</td>
                <td data-label="Tipo">{{ kindLabels[entry.kind] }}</td>
                <td data-label="Antes">
                  @if (entry.before === null) {
                    {{ emptyValue }}
                  } @else if (entry.kind === 'STATUS') {
                    <app-status-tag [look]="statusLook(entry.before)" />
                  } @else {
                    {{ entry.before }}
                  }
                </td>
                <td data-label="Después">
                  @if (entry.kind === 'STATUS') {
                    <app-status-tag [look]="statusLook(entry.after)" />
                  } @else {
                    {{ entry.after }}
                  }
                </td>
                <td data-label="Responsable">{{ entry.changedBy.displayName }}</td>
              </tr>
            }
          </tbody>
        </table>
        <app-pagination
          label="Paginación del historial"
          [page]="current.page.page"
          [size]="current.page.size"
          [totalItems]="current.page.totalItems"
          (pageChange)="history.load($event)"
        />
      }
      <p class="history__retention">Se conserva 1 año.</p>
    </section>
  `,
  styles: `
    .history__retention {
      margin: var(--space-3) 0 0;
      color: var(--color-fg-muted);
      font-size: var(--font-size-sm);
    }
  `,
})
export class ComplexHistoryTab {
  private readonly store = inject(ComplexDetailStore);
  private readonly api = inject(ComplexesApi);
  /** Only a different complex reloads the list, not every new version of the same one. */
  private readonly complexId = computed(() => this.store.complex()?.id ?? null);

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly emptyValue = EMPTY_VALUE;
  protected readonly kindLabels = KIND_LABELS;
  protected readonly formatDateTime = formatDateTime;
  protected readonly statusLook = historyStatusLook;
  protected readonly history = new PagedList<HistoryEntry>((page) =>
    this.api.listHistory(this.complexId() ?? '', page),
  );

  constructor() {
    effect(() => {
      this.store.historyRevision();
      const id = this.complexId();
      if (id) {
        untracked(() => void this.history.load(0));
      }
    });
  }
}
