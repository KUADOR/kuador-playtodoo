import { Component, computed, inject, input, output, signal } from '@angular/core';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { StatusTag } from '../../../shared/ui/status-tag/status-tag';
import { SUBSCRIPTION_STATUS_LOOKS } from '../complex-texts';
import { ComplexDetail, ComplexesApi, SubscriptionStatus } from '../complexes-api';
import { DialogSubmission } from './dialog-submission';
import { STATUS_CHANGE_ERROR_MESSAGES, statusChangeOptions } from './status-change-texts';

let nextDialogId = 0;

/**
 * «Cambiar estado» of P5 (CA6, CA8, CA9). Always manual: only the transitions the api allows for
 * this complex are offered, and the api records the date and who made the change.
 */
@Component({
  selector: 'app-change-status-dialog',
  imports: [Alert, Button, Dialog, StatusTag],
  template: `
    <app-dialog
      [heading]="'Cambiar estado · ' + complex().displayName"
      [busy]="submission.saving()"
      (closeRequest)="closeRequest.emit()"
    >
      <p class="dialog-form__current">Estado actual: <app-status-tag [look]="currentLook()" /></p>
      @if (options().length === 0) {
        <p>No hay cambios de estado disponibles.</p>
      } @else {
        <fieldset class="dialog-form__options" data-dialog-initial-focus>
          <legend class="dialog-form__legend">Nuevo estado</legend>
          @for (option of options(); track option.target) {
            <label class="dialog-form__option">
              <input
                type="radio"
                [name]="groupName"
                [value]="option.target"
                [checked]="option.target === target()"
                [disabled]="submission.saving()"
                [attr.aria-describedby]="groupName + '-' + option.target"
                (change)="choose(option.target)"
              />
              <span class="dialog-form__option-text">
                <span class="dialog-form__option-label">{{ option.label }}</span>
                <span
                  class="dialog-form__option-description"
                  [id]="groupName + '-' + option.target"
                  >{{ option.description }}</span
                >
              </span>
            </label>
          }
        </fieldset>
        @if (selected(); as option) {
          <app-alert [tone]="option.effectTone">{{ option.effect }}</app-alert>
        }
        <p class="dialog-form__note">La fecha y tu nombre quedan registrados en el historial.</p>
      }
      @if (failure(); as message) {
        <app-alert tone="error">{{ message }}</app-alert>
      }
      <app-button
        dialogActions
        [label]="selected()?.closeLabel ?? 'Cancelar'"
        variant="secondary"
        [disabled]="submission.saving()"
        (press)="closeRequest.emit()"
      />
      @if (selected(); as option) {
        <app-button
          dialogActions
          [label]="option.confirmLabel"
          [variant]="option.confirmVariant"
          [loading]="submission.saving()"
          (press)="save()"
        />
      }
    </app-dialog>
  `,
  styleUrl: './dialog-form.css',
})
export class ChangeStatusDialog {
  private readonly api = inject(ComplexesApi);

  readonly complex = input.required<ComplexDetail>();

  readonly changed = output<ComplexDetail>();
  /** `409 STALE_STATE`: someone else changed the complex; the page reloads it. */
  readonly stale = output<void>();
  readonly closeRequest = output<void>();

  protected readonly groupName = `change-status-${nextDialogId++}`;
  protected readonly submission = new DialogSubmission();
  protected readonly target = signal<SubscriptionStatus | null>(null);
  protected readonly failure = signal<string | null>(null);
  protected readonly options = computed(() => statusChangeOptions(this.complex()));
  protected readonly selected = computed(
    () => this.options().find((option) => option.target === this.target()) ?? null,
  );
  protected readonly currentLook = computed(
    () => SUBSCRIPTION_STATUS_LOOKS[this.complex().subscription.status],
  );

  protected choose(target: SubscriptionStatus): void {
    this.failure.set(null);
    this.target.set(target);
  }

  protected async save(): Promise<void> {
    const option = this.selected();
    if (option === null) {
      return;
    }
    this.failure.set(null);
    const complex = this.complex();
    const result = await this.submission.run(() =>
      this.api.changeStatus(complex.id, { targetStatus: option.target, version: complex.version }),
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
        errorMessage(result.error, COMMON_MESSAGES.saveFailed, STATUS_CHANGE_ERROR_MESSAGES),
      );
    }
  }
}
