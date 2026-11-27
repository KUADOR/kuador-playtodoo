import { Component, inject, input, output, signal, viewChild } from '@angular/core';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { Field } from '../../../shared/ui/field/field';
import { DialogSubmission } from '../../complexes/dialogs/dialog-submission';
import { SupportSession } from '../support-access-api';
import { StaffBooking, SupportBookingsApi } from '../support-bookings-api';
import { Court } from '../support-courts';
import { localInstant } from '../support-day';
import { SUPPORT_TEXTS } from '../support-texts';
import { supportFailure } from './support-dialog-outcome';
import { ChosenSlot, SlotPicker } from './slot-picker';

const BLOCK_REASON_MAX_LENGTH = 200;

/** «Bloquear horario» of P9: makes a free slot unavailable, with an optional internal reason. */
@Component({
  selector: 'app-block-slot-dialog',
  imports: [Alert, Button, Dialog, Field, SlotPicker],
  template: `
    <app-dialog
      [heading]="'Bloquear horario · ' + complexName()"
      [busy]="submission.saving()"
      (closeRequest)="closeRequest.emit()"
    >
      <form class="dialog-form" novalidate (submit)="save($event)">
        <app-slot-picker
          data-dialog-initial-focus
          [session]="session()"
          [courts]="courts()"
          [disabled]="submission.saving()"
          [error]="slotError()"
          (slotChange)="changeSlot($event)"
          (sessionEnded)="sessionEnded.emit()"
        />
        <app-field
          label="Motivo del bloqueo (opcional)"
          autocomplete="off"
          help="Solo lo ve el personal del complejo."
          [maxLength]="reasonMaxLength"
          [disabled]="submission.saving()"
          [value]="reason()"
          (valueChange)="reason.set($event)"
        />
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
        label="Bloquear horario"
        [loading]="submission.saving()"
        (press)="save()"
      />
    </app-dialog>
  `,
  styleUrl: '../../complexes/dialogs/dialog-form.css',
})
export class BlockSlotDialog {
  private readonly api = inject(SupportBookingsApi);

  readonly complexName = input.required<string>();
  readonly session = input.required<SupportSession>();
  readonly courts = input.required<readonly Court[]>();

  readonly blocked = output<StaffBooking>();
  readonly sessionEnded = output<void>();
  readonly closeRequest = output<void>();

  protected readonly reasonMaxLength = BLOCK_REASON_MAX_LENGTH;
  protected readonly submission = new DialogSubmission();
  protected readonly slot = signal<ChosenSlot | null>(null);
  protected readonly reason = signal('');
  protected readonly slotError = signal<string | null>(null);
  protected readonly failure = signal<string | null>(null);
  private readonly picker = viewChild.required(SlotPicker);

  protected changeSlot(slot: ChosenSlot | null): void {
    this.slot.set(slot);
    if (slot !== null) {
      this.slotError.set(null);
    }
  }

  protected async save(event?: Event): Promise<void> {
    event?.preventDefault();
    const slot = this.slot();
    if (slot === null) {
      this.slotError.set(SUPPORT_TEXTS.slotRequired);
      return;
    }
    this.failure.set(null);
    const reason = this.reason().trim();
    const result = await this.submission.run(() =>
      this.api.block(this.session(), {
        courtId: slot.courtId,
        start: localInstant(slot.date, slot.start),
        ...(reason ? { reason } : {}),
      }),
    );
    if (result === null) {
      return;
    }
    if (result.ok) {
      this.blocked.emit(result.value);
      return;
    }
    const failure = supportFailure(result.error);
    if (failure.kind === 'session-ended') {
      this.sessionEnded.emit();
      return;
    }
    this.failure.set(failure.message);
    if (failure.kind === 'slot-taken' || result.error.code === 'SLOT_HAS_BOOKING') {
      void this.picker().reload();
    }
  }
}
