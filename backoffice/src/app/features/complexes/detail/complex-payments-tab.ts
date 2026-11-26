import { Component, computed, effect, inject, untracked } from '@angular/core';
import { PagedList } from '../../../core/api/paged-list';
import { formatCalendarDate, formatDateTime } from '../../../core/format/date-format';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Pagination } from '../../../shared/ui/pagination/pagination';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { formatMoney, PAYMENT_METHOD_LABELS } from '../complex-texts';
import { ComplexesApi, Payment } from '../complexes-api';
import { ComplexDetailStore } from './complex-detail-store';

const SKELETON_ROWS = 3;

/** «Pagos» tab of P4 (CA7): newest first, read only (payments are immutable). */
@Component({
  selector: 'app-complex-payments-tab',
  imports: [Alert, Button, Pagination, Skeleton],
  template: `
    @let current = payments.state();
    <section aria-labelledby="payments-heading" [attr.aria-busy]="current.status === 'loading'">
      <h2 id="payments-heading" class="visually-hidden">Pagos</h2>
      @if (current.status === 'loading') {
        <app-skeleton [rows]="skeletonRows" />
      } @else if (current.status === 'error') {
        <app-alert tone="error">
          {{ current.message }}
          <app-button
            alertActions
            label="Reintentar"
            variant="secondary"
            (press)="payments.load(0)"
          />
        </app-alert>
      } @else if (current.page.totalItems === 0) {
        <p>Todavía no hay pagos registrados.</p>
      } @else {
        <table class="data-table">
          <caption class="visually-hidden">
            Pagos de suscripción registrados
          </caption>
          <thead>
            <tr>
              <th scope="col">Fecha</th>
              <th scope="col" class="data-table__number">Importe</th>
              <th scope="col">Medio</th>
              <th scope="col">Registrado por</th>
              <th scope="col">Fecha de registro</th>
            </tr>
          </thead>
          <tbody>
            @for (payment of current.page.items; track payment.id) {
              <tr>
                <td data-label="Fecha">{{ formatCalendarDate(payment.paidOn) }}</td>
                <td data-label="Importe" class="data-table__number">
                  {{ formatMoney(payment.amount, payment.currency) }}
                </td>
                <td data-label="Medio">{{ methodLabels[payment.method] }}</td>
                <td data-label="Registrado por">{{ payment.recordedBy.displayName }}</td>
                <td data-label="Fecha de registro">{{ formatDateTime(payment.recordedAt) }}</td>
              </tr>
            }
          </tbody>
        </table>
        <app-pagination
          label="Paginación de pagos"
          [page]="current.page.page"
          [size]="current.page.size"
          [totalItems]="current.page.totalItems"
          (pageChange)="payments.load($event)"
        />
      }
    </section>
  `,
})
export class ComplexPaymentsTab {
  private readonly store = inject(ComplexDetailStore);
  private readonly api = inject(ComplexesApi);
  /** Only a different complex reloads the list, not every new version of the same one. */
  private readonly complexId = computed(() => this.store.complex()?.id ?? null);

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly methodLabels = PAYMENT_METHOD_LABELS;
  protected readonly formatMoney = formatMoney;
  protected readonly formatCalendarDate = formatCalendarDate;
  protected readonly formatDateTime = formatDateTime;
  protected readonly payments = new PagedList<Payment>((page) =>
    this.api.listPayments(this.complexId() ?? '', page),
  );

  constructor() {
    effect(() => {
      this.store.paymentsRevision();
      const id = this.complexId();
      if (id) {
        untracked(() => void this.payments.load(0));
      }
    });
  }
}
