import {
  Component,
  Injector,
  afterNextRender,
  computed,
  inject,
  input,
  output,
  signal,
  viewChildren,
} from '@angular/core';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { todayAsCalendarDate } from '../../../core/format/date-format';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { Field, FieldOption } from '../../../shared/ui/field/field';
import { PAYMENT_METHOD_LABELS } from '../complex-texts';
import { ComplexDetail, ComplexesApi, Payment, PaymentMethod } from '../complexes-api';
import { DialogSubmission } from './dialog-submission';
import {
  normalizeAmount,
  PAYMENT_FIELD_ORDER,
  PaymentFieldErrors,
  paymentErrorsOfProblem,
  validatePaymentForm,
} from './payment-form';

const METHOD_OPTIONS: FieldOption[] = (
  Object.entries(PAYMENT_METHOD_LABELS) as [PaymentMethod, string][]
).map(([value, label]) => ({ value, label }));

/**
 * «Registrar pago» of P5 (CA7). Only records a payment received outside the platform (ADR-004):
 * it charges nothing and does not change the status of the subscription.
 */
@Component({
  selector: 'app-record-payment-dialog',
  imports: [Alert, Button, Dialog, Field],
  template: `
    <app-dialog
      [heading]="'Registrar pago · ' + complex().displayName"
      [busy]="submission.saving()"
      (closeRequest)="closeRequest.emit()"
    >
      <form id="record-payment-form" class="dialog-form" novalidate (submit)="save($event)">
        <app-field
          data-dialog-initial-focus
          [label]="'Importe (' + currency() + ')'"
          inputMode="decimal"
          autocomplete="off"
          [required]="true"
          [disabled]="submission.saving()"
          [error]="fieldErrors().amount"
          [(value)]="amount"
        />
        <app-field
          label="Fecha del pago"
          inputType="date"
          [required]="true"
          [max]="today"
          [disabled]="submission.saving()"
          [error]="fieldErrors().paidOn"
          [(value)]="paidOn"
        />
        <app-field
          label="Medio"
          kind="select"
          [required]="true"
          [options]="methodOptions"
          [disabled]="submission.saving()"
          [error]="fieldErrors().method"
          [(value)]="method"
        />
        <p class="dialog-form__note">
          Solo registra un pago recibido fuera de la plataforma; no cobra nada.
        </p>
      </form>
      @if (failure(); as message) {
        <app-alert tone="error">{{ message }}</app-alert>
      }
      <app-button
        dialogActions
        label="Cancelar"
        variant="secondary"
        [disabled]="submission.saving()"
        (press)="closeRequest.emit()"
      />
      <app-button
        dialogActions
        label="Registrar pago"
        [loading]="submission.saving()"
        (press)="save()"
      />
    </app-dialog>
  `,
  styleUrl: './dialog-form.css',
})
export class RecordPaymentDialog {
  private readonly api = inject(ComplexesApi);
  private readonly injector = inject(Injector);

  readonly complex = input.required<ComplexDetail>();

  readonly recorded = output<Payment>();
  readonly closeRequest = output<void>();

  protected readonly today = todayAsCalendarDate();
  protected readonly methodOptions = METHOD_OPTIONS;
  protected readonly submission = new DialogSubmission();
  protected readonly amount = signal('');
  protected readonly paidOn = signal(this.today);
  protected readonly method = signal<string>('TRANSFER');
  protected readonly fieldErrors = signal<PaymentFieldErrors>({});
  protected readonly failure = signal<string | null>(null);
  protected readonly currency = computed(() => this.complex().subscription.plan.currency);

  private readonly fields = viewChildren(Field);

  protected async save(event?: Event): Promise<void> {
    event?.preventDefault();
    const values = {
      amount: this.amount(),
      paidOn: this.paidOn(),
      method: this.method() as PaymentMethod | '',
    };
    if (!this.showErrors(validatePaymentForm(values, this.today))) {
      return;
    }
    this.failure.set(null);
    const result = await this.submission.run(() =>
      this.api.recordPayment(this.complex().id, {
        amount: normalizeAmount(values.amount),
        paidOn: values.paidOn,
        method: values.method as PaymentMethod,
      }),
    );
    if (result === null) {
      return;
    }
    if (result.ok) {
      this.recorded.emit(result.value);
    } else if (result.error.code === 'VALIDATION_FAILED' && result.error.fieldErrors.length > 0) {
      this.showErrors(paymentErrorsOfProblem(result.error.fieldErrors));
    } else {
      this.failure.set(errorMessage(result.error, COMMON_MESSAGES.saveFailed));
    }
  }

  private showErrors(errors: PaymentFieldErrors): boolean {
    this.fieldErrors.set(errors);
    const firstIndex = PAYMENT_FIELD_ORDER.findIndex((field) => errors[field] !== undefined);
    if (firstIndex === -1) {
      return true;
    }
    // After the next render: the fields are enabled again once a failed submission ends.
    afterNextRender(() => this.fields()[firstIndex]?.focus(), { injector: this.injector });
    return false;
  }
}
