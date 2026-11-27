import {
  Component,
  Injector,
  afterNextRender,
  computed,
  inject,
  input,
  linkedSignal,
  output,
  signal,
  viewChildren,
} from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiError, asApiError } from '../../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Checkbox } from '../../../shared/ui/checkbox/checkbox';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { Field, FieldOption } from '../../../shared/ui/field/field';
import { PlanField, PlanFieldType, PlanFieldsApi } from '../plan-fields-api';
import { FIELD_TYPE_LABELS } from '../plan-texts';

const LABEL_MAX_LENGTH = 80;

/** Texts of the «Crear campo» and «Editar campo» dialog of P12. */
export const FIELD_DIALOG_TEXTS = {
  labelRequired: 'Escribe el nombre del campo.',
  labelTooLong: `Usa como máximo ${LABEL_MAX_LENGTH} caracteres.`,
  typeRequired: 'Elige el tipo del campo.',
  nameTaken: 'Ya existe un campo con ese nombre.',
} as const;

const FIELD_TYPE_OPTIONS: readonly FieldOption[] = (
  Object.entries(FIELD_TYPE_LABELS) as [PlanFieldType, string][]
).map(([value, label]) => ({ value, label }));

function labelError(label: string): string | undefined {
  if (label === '') {
    return FIELD_DIALOG_TEXTS.labelRequired;
  }
  return label.length > LABEL_MAX_LENGTH ? FIELD_DIALOG_TEXTS.labelTooLong : undefined;
}

interface FieldDialogErrors {
  readonly label?: string;
  readonly type?: string;
}

/**
 * Dialog of P12 to create a custom plan field or edit one (CA4). The type is chosen only when
 * creating: the api keeps it immutable.
 */
@Component({
  selector: 'app-plan-field-dialog',
  imports: [Alert, Button, Checkbox, Dialog, Field],
  templateUrl: './plan-field-dialog.html',
  styles: `
    p {
      margin: 0;
    }

    .field-dialog__type {
      color: var(--color-fg-muted);
    }
  `,
})
export class PlanFieldDialog {
  private readonly api = inject(PlanFieldsApi);
  private readonly injector = inject(Injector);

  /** The field to edit; null to create one. */
  readonly field = input<PlanField | null>(null);

  readonly saved = output<PlanField>();
  /** Someone else changed the field meanwhile: the page reloads the list. */
  readonly staleState = output<void>();
  readonly closeRequest = output<void>();

  protected readonly typeOptions = FIELD_TYPE_OPTIONS;
  protected readonly typeLabels = FIELD_TYPE_LABELS;

  protected readonly label = linkedSignal(() => this.field()?.label ?? '');
  protected readonly type = signal<string>('');
  protected readonly required = linkedSignal(() => this.field()?.required ?? false);
  protected readonly errors = signal<FieldDialogErrors>({});
  protected readonly saveError = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly heading = computed(() => {
    const field = this.field();
    return field === null ? 'Crear campo' : `Editar campo «${field.label}»`;
  });

  private readonly controls = viewChildren(Field);

  protected close(): void {
    if (!this.saving()) {
      this.closeRequest.emit();
    }
  }

  protected async save(): Promise<void> {
    if (this.saving() || !this.isValid()) {
      return;
    }
    this.saving.set(true);
    this.saveError.set(null);
    try {
      this.saved.emit(await firstValueFrom(this.request()));
    } catch (error) {
      this.handleFailure(asApiError(error));
    } finally {
      this.saving.set(false);
    }
  }

  private request() {
    const field = this.field();
    const label = this.label().trim();
    return field === null
      ? this.api.create({ label, type: this.type() as PlanFieldType, required: this.required() })
      : this.api.update(field.id, { label, required: this.required(), version: field.version });
  }

  private isValid(): boolean {
    const errors: FieldDialogErrors = {
      label: labelError(this.label().trim()),
      type:
        this.field() === null && this.type() === '' ? FIELD_DIALOG_TEXTS.typeRequired : undefined,
    };
    this.showErrors(errors);
    return errors.label === undefined && errors.type === undefined;
  }

  private showErrors(errors: FieldDialogErrors): void {
    this.errors.set(errors);
    const firstError = errors.label !== undefined ? 0 : errors.type !== undefined ? 1 : null;
    if (firstError !== null) {
      afterNextRender(() => this.controls()[firstError]?.focus(), { injector: this.injector });
    }
  }

  private handleFailure(error: ApiError): void {
    if (error.code === 'STALE_STATE') {
      this.staleState.emit();
      return;
    }
    if (error.code === 'FIELD_NAME_TAKEN') {
      this.showErrors({ label: FIELD_DIALOG_TEXTS.nameTaken });
      return;
    }
    const labelError = error.fieldErrors.find((fieldError) => fieldError.field === 'label');
    if (labelError !== undefined) {
      this.showErrors({
        label:
          labelError.code === 'TOO_LONG'
            ? FIELD_DIALOG_TEXTS.labelTooLong
            : FIELD_DIALOG_TEXTS.labelRequired,
      });
      return;
    }
    this.saveError.set(errorMessage(error, COMMON_MESSAGES.saveFailed));
  }
}
