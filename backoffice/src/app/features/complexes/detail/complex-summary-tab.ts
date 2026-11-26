import { Component, computed, effect, inject, untracked } from '@angular/core';
import { map } from 'rxjs';
import { RemoteValue } from '../../../core/api/remote-value';
import { formatCalendarDate, formatDateTime } from '../../../core/format/date-format';
import { StatusTag } from '../../../shared/ui/status-tag/status-tag';
import {
  EMPTY_VALUE,
  formatMoney,
  planFieldRows,
  SUBSCRIPTION_STATUS_LOOKS,
} from '../complex-texts';
import { ComplexesApi, Payment } from '../complexes-api';
import { ComplexDetailStore } from './complex-detail-store';

/** The detail does not carry the last payment: the newest one is the first of its list. */
const LAST_PAYMENT_PAGE_SIZE = 1;

/** «Resumen» tab of P4: the current plan with all its fields and the subscription status. */
@Component({
  selector: 'app-complex-summary-tab',
  imports: [StatusTag],
  template: `
    @if (complex(); as complex) {
      <div class="summary">
        <section class="summary__card" aria-labelledby="summary-plan-heading">
          <h2 id="summary-plan-heading">Plan · {{ complex.subscription.plan.name }}</h2>
          <dl class="summary__list">
            @for (row of planRows(); track row.label) {
              <div class="summary__row">
                <dt>{{ row.label }}</dt>
                <dd>{{ row.value }}</dd>
              </div>
            }
            <div class="summary__row">
              <dt>Asignado el</dt>
              <dd>{{ formatDateTime(complex.subscription.plan.assignedAt) }}</dd>
            </div>
          </dl>
        </section>
        <section class="summary__card" aria-labelledby="summary-subscription-heading">
          <h2 id="summary-subscription-heading">Suscripción</h2>
          <dl class="summary__list">
            <div class="summary__row">
              <dt>Estado</dt>
              <dd><app-status-tag [look]="statusLook()" /></dd>
            </div>
            <div class="summary__row">
              <dt>Desde</dt>
              <dd>{{ formatDateTime(complex.subscription.statusSince) }}</dd>
            </div>
            <div class="summary__row">
              <dt>Responsable</dt>
              <dd>{{ complex.subscription.statusChangedBy.displayName }}</dd>
            </div>
            <div class="summary__row">
              <dt>Último pago</dt>
              <dd [attr.aria-busy]="lastPayment.state().status === 'loading'">
                {{ lastPaymentText() }}
              </dd>
            </div>
            <div class="summary__row">
              <dt>Correo del propietario</dt>
              <dd>{{ complex.ownerEmail ?? emptyValue }}</dd>
            </div>
            <div class="summary__row">
              <dt>Alta</dt>
              <dd>{{ formatDateTime(complex.createdAt) }}</dd>
            </div>
          </dl>
        </section>
      </div>
    }
  `,
  styleUrl: './complex-summary-tab.css',
})
export class ComplexSummaryTab {
  private readonly store = inject(ComplexDetailStore);
  private readonly api = inject(ComplexesApi);
  /** Only a different complex reloads the list, not every new version of the same one. */
  private readonly complexId = computed(() => this.store.complex()?.id ?? null);

  protected readonly emptyValue = EMPTY_VALUE;
  protected readonly formatDateTime = formatDateTime;
  protected readonly complex = this.store.complex;
  protected readonly lastPayment = new RemoteValue<Payment | null>(() =>
    this.api
      .listPayments(this.complexId() ?? '', 0, LAST_PAYMENT_PAGE_SIZE)
      .pipe(map((page) => page.items[0] ?? null)),
  );
  protected readonly planRows = computed(() => {
    const complex = this.complex();
    return complex === null ? [] : planFieldRows(complex.subscription.plan);
  });
  protected readonly statusLook = computed(
    () => SUBSCRIPTION_STATUS_LOOKS[this.complex()?.subscription.status ?? 'ACTIVE'],
  );
  protected readonly lastPaymentText = computed(() => {
    const state = this.lastPayment.state();
    switch (state.status) {
      case 'loading':
        return 'Cargando…';
      case 'error':
        return state.message;
      case 'ready':
        return state.value === null
          ? 'Sin pagos'
          : `${formatCalendarDate(state.value.paidOn)} · ${formatMoney(state.value.amount, state.value.currency)}`;
    }
  });

  constructor() {
    effect(() => {
      this.store.paymentsRevision();
      const id = this.complexId();
      if (id) {
        untracked(() => void this.lastPayment.load());
      }
    });
  }
}
