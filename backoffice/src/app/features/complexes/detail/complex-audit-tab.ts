import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { PagedList } from '../../../core/api/paged-list';
import { formatDateTime } from '../../../core/format/date-format';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { Pagination } from '../../../shared/ui/pagination/pagination';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { StatusTag } from '../../../shared/ui/status-tag/status-tag';
import { AuditEntry, SupportAccessApi } from '../../support/support-access-api';
import { AUDIT_RESULT_LOOKS, auditAction, REDACTED_REASON } from '../../support/support-texts';
import { EMPTY_VALUE } from '../complex-texts';
import { ComplexDetailStore } from './complex-detail-store';

const SKELETON_ROWS = 3;

/**
 * P7 · Auditoría de accesos (CA13), SUPER_ADMIN only: who opened a support session on the
 * complex, why, and every request made with it, allowed or denied. Read only.
 */
@Component({
  selector: 'app-complex-audit-tab',
  imports: [Alert, Button, Dialog, Pagination, Skeleton, StatusTag],
  template: `
    @let current = audit.state();
    <section
      class="audit"
      aria-labelledby="audit-heading"
      [attr.aria-busy]="current.status === 'loading'"
    >
      <h2 id="audit-heading" class="visually-hidden">Auditoría</h2>
      @if (current.status === 'loading') {
        <app-skeleton [rows]="skeletonRows" />
      } @else if (current.status === 'error') {
        <app-alert tone="error">
          {{ current.message }}
          <app-button alertActions label="Reintentar" variant="secondary" (press)="audit.load(0)" />
        </app-alert>
      } @else if (current.page.totalItems === 0) {
        <p>No hay accesos registrados.</p>
      } @else {
        <table class="data-table">
          <caption class="visually-hidden">
            Accesos de soporte a los datos del complejo
          </caption>
          <thead>
            <tr>
              <th scope="col">Fecha y hora</th>
              <th scope="col">Persona</th>
              <th scope="col">Motivo</th>
              <th scope="col">Acción</th>
              <th scope="col">Resultado</th>
            </tr>
          </thead>
          <tbody>
            @for (entry of current.page.items; track entry.id) {
              @let action = actionOf(entry);
              <tr>
                <td data-label="Fecha y hora">{{ formatDateTime(entry.occurredAt) }}</td>
                <td data-label="Persona">{{ entry.actor.displayName }}</td>
                <td data-label="Motivo">
                  @if (entry.reasonRedacted) {
                    <span class="audit__muted">{{ redactedReason }}</span>
                  } @else if (entry.reason; as reason) {
                    <span class="audit__reason">
                      <span class="audit__reason-text">{{ reason }}</span>
                      <app-button
                        variant="text"
                        label="Ver motivo"
                        [accessibleLabel]="'Ver motivo del ' + formatDateTime(entry.occurredAt)"
                        (press)="shownReason.set(reason)"
                      />
                    </span>
                  } @else {
                    {{ emptyValue }}
                  }
                </td>
                <td data-label="Acción">
                  <span class="audit__action">
                    {{ action.label }}
                    @if (action.technicalDetail; as detail) {
                      <code class="audit__muted">{{ detail }}</code>
                    }
                  </span>
                </td>
                <td data-label="Resultado">
                  <app-status-tag [look]="resultLooks[entry.result]" />
                </td>
              </tr>
            }
          </tbody>
        </table>
        <app-pagination
          label="Paginación de la auditoría"
          [page]="current.page.page"
          [size]="current.page.size"
          [totalItems]="current.page.totalItems"
          (pageChange)="audit.load($event)"
        />
      }
      <p class="audit__muted">Se conserva 1 año.</p>
    </section>

    @if (shownReason(); as reason) {
      <app-dialog heading="Motivo del acceso" (closeRequest)="shownReason.set(null)">
        <p class="audit__full-reason">{{ reason }}</p>
        <app-button dialogActions label="Cerrar" (press)="shownReason.set(null)" />
      </app-dialog>
    }
  `,
  styles: `
    .audit__muted {
      margin: var(--space-3) 0 0;
      color: var(--color-fg-muted);
      font-size: var(--font-size-sm);
    }

    .audit__reason,
    .audit__action {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: var(--space-1);
      min-width: 0;
    }

    .audit__reason-text {
      display: -webkit-box;
      max-width: 24rem;
      overflow: hidden;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
      overflow-wrap: anywhere;
    }

    .audit__full-reason {
      margin: 0;
      overflow-wrap: anywhere;
      white-space: pre-wrap;
    }
  `,
})
export class ComplexAuditTab {
  private readonly store = inject(ComplexDetailStore);
  private readonly api = inject(SupportAccessApi);
  /** Only a different complex reloads the list, not every new version of the same one. */
  private readonly complexId = computed(() => this.store.complex()?.id ?? null);

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly emptyValue = EMPTY_VALUE;
  protected readonly redactedReason = REDACTED_REASON;
  protected readonly resultLooks = AUDIT_RESULT_LOOKS;
  protected readonly formatDateTime = formatDateTime;
  protected readonly actionOf = auditAction;
  protected readonly shownReason = signal<string | null>(null);
  protected readonly audit = new PagedList<AuditEntry>((page) =>
    this.api.listAuditEntries(this.complexId() ?? '', page),
  );

  constructor() {
    effect(() => {
      const id = this.complexId();
      if (id) {
        untracked(() => void this.audit.load(0));
      }
    });
  }
}
