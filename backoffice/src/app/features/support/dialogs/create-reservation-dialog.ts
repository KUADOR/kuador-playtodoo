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

const HOLDER_NAME_MAX_LENGTH = 120;
const HOLDER_PHONE_MAX_LENGTH = 32;

/** «Crear reserva» of P9: a confirmed manual booking for a holder, on a free slot. */
@Component({
  selector: 'app-create-reservation-dialog',
  imports: [Alert, Button, Dialog, Field, SlotPicker],
  template: `
    <app-dialog
      [heading]="'Crear reserva · ' + complexName()"
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
          label="Nombre del titular"
          autocomplete="off"
          [required]="true"
          [maxLength]="holderNameMaxLength"
          [disabled]="submission.saving()"
          [error]="holderError() ?? undefined"
          [value]="holderName()"
          (valueChange)="changeHolderName($event)"
        />
        <app-field
          label="Teléfono del titular (opcional)"
          autocomplete="off"
          [maxLength]="holderPhoneMaxLength"
          [disabled]="submission.saving()"
          [value]="holderPhone()"
          (valueChange)="holderPhone.set($event)"
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
        label="Crear reserva"
        [loading]="submission.saving()"
        (press)="save()"
      />
    </app-dialog>
  `,
  styleUrl: '../../complexes/dialogs/dialog-form.css',
})
export class CreateReservationDialog {
  private readonly api = inject(SupportBookingsApi);

  readonly complexName = input.required<string>();
  readonly session = input.required<SupportSession>();
  readonly courts = input.required<readonly Court[]>();

  readonly created = output<StaffBooking>();
  readonly sessionEnded = output<void>();
  readonly closeRequest = output<void>();

  protected readonly holderNameMaxLength = HOLDER_NAME_MAX_LENGTH;
  protected readonly holderPhoneMaxLength = HOLDER_PHONE_MAX_LENGTH;
  protected readonly submission = new DialogSubmission();
  protected readonly slot = signal<ChosenSlot | null>(null);
  protected readonly holderName = signal('');
  protected readonly holderPhone = signal('');
  protected readonly slotError = signal<string | null>(null);
  protected readonly holderError = signal<string | null>(null);
  protected readonly failure = signal<string | null>(null);
  private readonly picker = viewChild.required(SlotPicker);

  protected changeSlot(slot: ChosenSlot | null): void {
    this.slot.set(slot);
    if (slot !== null) {
      this.slotError.set(null);
    }
  }

  protected changeHolderName(value: string): void {
    this.holderName.set(value);
    this.holderError.set(null);
  }

  protected async save(event?: Event): Promise<void> {
    event?.preventDefault();
    const slot = this.slot();
    const holderName = this.holderName().trim();
    this.slotError.set(slot === null ? SUPPORT_TEXTS.slotRequired : null);
    this.holderError.set(holderName ? null : SUPPORT_TEXTS.holderRequired);
    if (slot === null || !holderName) {
      return;
    }
    this.failure.set(null);
    const holderPhone = this.holderPhone().trim();
    const result = await this.submission.run(() =>
      this.api.create(this.session(), {
        courtId: slot.courtId,
        start: localInstant(slot.date, slot.start),
        holderName,
        ...(holderPhone ? { holderPhone } : {}),
      }),
    );
    if (result === null) {
      return;
    }
    if (result.ok) {
      this.created.emit(result.value);
      return;
    }
    const failure = supportFailure(result.error);
    if (failure.kind === 'session-ended') {
      this.sessionEnded.emit();
      return;
    }
    this.failure.set(failure.message);
    if (failure.kind === 'slot-taken') {
      void this.picker().reload();
    }
  }
}
