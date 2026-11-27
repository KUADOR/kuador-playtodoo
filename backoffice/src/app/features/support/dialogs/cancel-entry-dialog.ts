import { Component, computed, inject, input, output, signal } from '@angular/core';
import { formatCalendarDate } from '../../../core/format/date-format';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { DialogSubmission } from '../../complexes/dialogs/dialog-submission';
import { SupportSession } from '../support-access-api';
import { StaffBooking, SupportBookingsApi } from '../support-bookings-api';
import { EntryRow } from '../support-day';
import { supportFailure } from './support-dialog-outcome';

/** Wording of the confirmation for each kind of entry. */
const CONFIRMATIONS = {
  RESERVATION: { action: 'Cancelar reserva', what: 'cancelar la reserva' },
  BLOCK: { action: 'Quitar bloqueo', what: 'quitar el bloqueo' },
} as const;

/** «Cancelar reserva» and «Quitar bloqueo» of P9: a danger confirmation that names the slot. */
@Component({
  selector: 'app-cancel-entry-dialog',
  imports: [Alert, Button, Dialog],
  template: `
    <app-dialog
      [heading]="confirmation().action + ' · ' + complexName()"
      [busy]="submission.saving()"
      (closeRequest)="closeRequest.emit()"
    >
      <p class="dialog-form__note">{{ question() }}</p>
      @if (failure(); as message) {
        <app-alert tone="error">{{ message }}</app-alert>
      }
      <app-button
        dialogActions
        data-dialog-initial-focus
        label="Volver"
        variant="secondary"
        [disabled]="submission.saving()"
        (press)="closeRequest.emit()"
      />
      <app-button
        dialogActions
        variant="danger"
        [label]="confirmation().action"
        [loading]="submission.saving()"
        (press)="confirm()"
      />
    </app-dialog>
  `,
  styleUrl: '../../complexes/dialogs/dialog-form.css',
})
export class CancelEntryDialog {
  private readonly api = inject(SupportBookingsApi);

  readonly complexName = input.required<string>();
  readonly session = input.required<SupportSession>();
  readonly row = input.required<EntryRow>();

  readonly cancelled = output<StaffBooking>();
  /** `404 BOOKING_NOT_FOUND`: the entry no longer exists; the list is read again. */
  readonly gone = output<string>();
  readonly sessionEnded = output<void>();
  readonly closeRequest = output<void>();

  protected readonly submission = new DialogSubmission();
  protected readonly failure = signal<string | null>(null);
  protected readonly confirmation = computed(() => CONFIRMATIONS[this.row().entry.kind]);
  protected readonly question = computed(() => {
    const row = this.row();
    return `¿Quieres ${this.confirmation().what} de ${row.court} del ${formatCalendarDate(row.date)}, de ${row.time}, en ${this.complexName()}?`;
  });

  protected async confirm(): Promise<void> {
    this.failure.set(null);
    const result = await this.submission.run(() =>
      this.api.cancel(this.session(), this.row().entry.id),
    );
    if (result === null) {
      return;
    }
    if (result.ok) {
      this.cancelled.emit(result.value);
      return;
    }
    const failure = supportFailure(result.error);
    if (failure.kind === 'session-ended') {
      this.sessionEnded.emit();
    } else if (result.error.code === 'BOOKING_NOT_FOUND') {
      this.gone.emit(failure.message);
    } else {
      this.failure.set(failure.message);
    }
  }
}
