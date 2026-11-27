import { Component, computed, inject, input, linkedSignal, output, signal } from '@angular/core';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { addDays, formatDateTime } from '../../../core/format/date-format';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { Field } from '../../../shared/ui/field/field';
import { ComplexDetail, ComplexesApi } from '../complexes-api';
import { DialogSubmission } from './dialog-submission';

const WHOLE_NUMBER_PATTERN = /^\d+$/;
const MIN_RETENTION_DAYS = 1;

const RETENTION_TEXTS = {
  daysInvalid: 'Escribe un número entero de días, 1 o más.',
  deadlineInPast: 'La nueva fecha de borrado debe ser posterior a hoy.',
} as const;

const RETENTION_ERROR_MESSAGES = {
  RETENTION_DEADLINE_IN_PAST: RETENTION_TEXTS.deadlineInPast,
  NOT_IN_RETENTION: 'El complejo ya no está suspendido ni cancelado. Revisa su estado.',
} as const;

/**
 * «Cambiar plazo de conservación» of P4. SUPER_ADMIN only, while the complex is suspended or
 * cancelled. The new purge date counts from the day retention started (ADR-014) and must be in
 * the future, so this never triggers a purge.
 */
@Component({
  selector: 'app-change-retention-dialog',
  imports: [Alert, Button, Dialog, Field],
  template: `
    <app-dialog
      [heading]="'Cambiar plazo de conservación · ' + complex().displayName"
      [busy]="submission.saving()"
      (closeRequest)="closeRequest.emit()"
    >
      <form class="dialog-form" novalidate (submit)="save($event)">
        <app-field
          data-dialog-initial-focus
          label="Días de conservación"
          inputMode="numeric"
          autocomplete="off"
          [required]="true"
          [help]="'Se cuentan desde el ' + retentionStartText() + '.'"
          [disabled]="submission.saving()"
          [error]="daysError() ?? undefined"
          [value]="days()"
          (valueChange)="changeDays($event)"
        />
        <p class="dialog-form__note" role="status">
          @if (previewText(); as preview) {
            Nueva fecha de borrado: {{ preview }}
          }
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
        label="Cambiar plazo"
        [loading]="submission.saving()"
        (press)="save()"
      />
    </app-dialog>
  `,
  styleUrl: './dialog-form.css',
})
export class ChangeRetentionDialog {
  private readonly api = inject(ComplexesApi);

  readonly complex = input.required<ComplexDetail>();

  readonly changed = output<ComplexDetail>();
  /** `409 STALE_STATE`: someone else changed the complex; the page reloads it. */
  readonly stale = output<void>();
  readonly closeRequest = output<void>();

  protected readonly submission = new DialogSubmission();
  protected readonly days = linkedSignal(() => String(this.complex().retentionDays ?? ''));
  protected readonly daysError = signal<string | null>(null);
  protected readonly failure = signal<string | null>(null);

  /** Retention starts at the suspension, or at the cancellation of an active complex. */
  private readonly retentionStart = computed(
    () => this.complex().suspendedAt ?? this.complex().archivedAt ?? this.complex().createdAt,
  );
  protected readonly retentionStartText = computed(() => formatDateTime(this.retentionStart()));
  private readonly parsedDays = computed(() => {
    const value = this.days().trim();
    const days = WHOLE_NUMBER_PATTERN.test(value) ? Number(value) : NaN;
    return days >= MIN_RETENTION_DAYS ? days : null;
  });
  private readonly newDeadline = computed(() => {
    const days = this.parsedDays();
    return days === null ? null : addDays(this.retentionStart(), days);
  });
  protected readonly previewText = computed(() => {
    const deadline = this.newDeadline();
    return deadline === null ? null : formatDateTime(deadline.toISOString());
  });

  protected changeDays(value: string): void {
    this.days.set(value);
    this.daysError.set(null);
    this.failure.set(null);
  }

  protected async save(event?: Event): Promise<void> {
    event?.preventDefault();
    const days = this.parsedDays();
    const deadline = this.newDeadline();
    if (days === null || deadline === null) {
      this.daysError.set(RETENTION_TEXTS.daysInvalid);
      return;
    }
    if (deadline.getTime() <= Date.now()) {
      this.daysError.set(RETENTION_TEXTS.deadlineInPast);
      return;
    }
    this.failure.set(null);
    const complex = this.complex();
    const result = await this.submission.run(() =>
      this.api.changeRetention(complex.id, { retentionDays: days, version: complex.version }),
    );
    if (result === null) {
      return;
    }
    if (result.ok) {
      this.changed.emit(result.value);
    } else if (result.error.code === 'STALE_STATE') {
      this.stale.emit();
    } else {
      this.failure.set(
        errorMessage(result.error, COMMON_MESSAGES.saveFailed, RETENTION_ERROR_MESSAGES),
      );
    }
  }
}
