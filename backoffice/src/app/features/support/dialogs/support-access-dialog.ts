import { Component, computed, inject, input, output, signal } from '@angular/core';
import { errorMessage } from '../../../core/api/error-messages';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { Field } from '../../../shared/ui/field/field';
import { DialogSubmission } from '../../complexes/dialogs/dialog-submission';
import { ActiveSupportAccess, SupportComplex } from '../support-access';
import { SupportAccessApi } from '../support-access-api';
import { ACCESS_ERROR_MESSAGES, ACCESS_TEXTS, REASON_MAX_LENGTH } from '../support-texts';

/**
 * P8 · Acceder con motivo (CA11, CA12): asks for the reason and opens a support session. No data
 * of the complex is loaded until the api accepts the reason. The field always starts empty.
 */
@Component({
  selector: 'app-support-access-dialog',
  imports: [Alert, Button, Dialog, Field],
  template: `
    <app-dialog
      [heading]="'Acceder a datos · ' + complex().displayName"
      [busy]="submission.saving()"
      (closeRequest)="closeRequest.emit()"
    >
      <form class="dialog-form" novalidate (submit)="open($event)">
        <app-field
          data-dialog-initial-focus
          kind="textarea"
          label="Motivo del acceso"
          autocomplete="off"
          [required]="true"
          [maxLength]="maxLength"
          [help]="help()"
          [disabled]="submission.saving()"
          [error]="reasonError() ?? undefined"
          [value]="reason()"
          (valueChange)="changeReason($event)"
        />
        <app-alert tone="info">{{ notice }}</app-alert>
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
        label="Acceder"
        loadingLabel="Abriendo…"
        [loading]="submission.saving()"
        (press)="open()"
      />
    </app-dialog>
  `,
  styleUrl: '../../complexes/dialogs/dialog-form.css',
})
export class SupportAccessDialog {
  private readonly api = inject(SupportAccessApi);

  readonly complex = input.required<SupportComplex>();

  /** The api accepted the reason: the session to use on the host of the complex. */
  readonly opened = output<ActiveSupportAccess>();
  readonly closeRequest = output<void>();

  protected readonly maxLength = REASON_MAX_LENGTH;
  protected readonly notice = ACCESS_TEXTS.notice;
  protected readonly submission = new DialogSubmission();
  protected readonly reason = signal('');
  protected readonly reasonError = signal<string | null>(null);
  protected readonly failure = signal<string | null>(null);

  protected readonly help = computed(
    () => `${ACCESS_TEXTS.reasonHelp} ${this.reason().length}/${REASON_MAX_LENGTH}`,
  );

  protected changeReason(value: string): void {
    this.reason.set(value);
    this.reasonError.set(null);
    this.failure.set(null);
  }

  protected async open(event?: Event): Promise<void> {
    event?.preventDefault();
    const reason = this.reason().trim();
    if (reason.length === 0) {
      this.reasonError.set(ACCESS_TEXTS.reasonRequired);
      return;
    }
    this.failure.set(null);
    const complex = this.complex();
    const result = await this.submission.run(() => this.api.open(complex.id, reason));
    if (result === null) {
      return;
    }
    if (result.ok) {
      this.opened.emit({ complex, reason, session: result.value });
    } else if (result.error.code === 'REASON_REQUIRED') {
      this.reasonError.set(ACCESS_TEXTS.reasonRequired);
    } else {
      this.failure.set(errorMessage(result.error, ACCESS_TEXTS.failed, ACCESS_ERROR_MESSAGES));
    }
  }
}
