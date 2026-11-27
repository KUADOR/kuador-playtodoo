import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronLeft } from '@lucide/angular';
import { firstValueFrom } from 'rxjs';
import { asApiError } from '../../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';
import { PlanField, PlanFieldsApi, activeCustomFields } from '../plan-fields-api';
import { PLANS_URL } from '../plan-paths';
import { FIELD_TYPE_LABELS, RETIRED_FIELD_BADGE, planFieldLabel } from '../plan-texts';
import { PlanFieldDialog } from './plan-field-dialog';

/** Texts of P12 «Campos de plan» («Pantallas», PLATODO-27). */
const FIELDS_TEXTS = {
  created: (label: string) => `Creamos el campo «${label}».`,
  updated: (label: string) => `Guardamos los cambios del campo «${label}».`,
  retired: (label: string) => `Retiramos el campo «${label}».`,
  staleState: 'Otra persona cambió este campo. Revisa los datos y vuelve a intentarlo.',
} as const;

const SKELETON_ROWS = 6;
const BACK_ICON_SIZE_PX = 20;

type FieldsState =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'ready'; readonly fields: readonly PlanField[] };

/** Which dialog is open: create, edit or retire. */
type FieldAction =
  | { readonly kind: 'create' }
  | { readonly kind: 'edit'; readonly field: PlanField }
  | { readonly kind: 'retire'; readonly field: PlanField; readonly error: string | null };

/**
 * P12 · Campos de plan (CA4): create, edit and retire the custom fields of the plans without
 * changing code. Basic fields are listed read-only. SUPER_ADMIN only.
 */
@Component({
  selector: 'app-plan-fields-page',
  imports: [
    Alert,
    Button,
    Dialog,
    LucideChevronLeft,
    PlanFieldDialog,
    RouterLink,
    Skeleton,
    StatusBadge,
  ],
  templateUrl: './plan-fields-page.html',
  styleUrl: './plan-fields-page.css',
})
export class PlanFieldsPage {
  private readonly api = inject(PlanFieldsApi);

  protected readonly plansUrl = PLANS_URL;
  protected readonly typeLabels = FIELD_TYPE_LABELS;
  protected readonly retiredBadge = RETIRED_FIELD_BADGE;
  protected readonly fieldLabel = planFieldLabel;
  protected readonly backIconSize = BACK_ICON_SIZE_PX;
  protected readonly skeletonRows = SKELETON_ROWS;

  protected readonly state = signal<FieldsState>({ status: 'loading' });
  protected readonly action = signal<FieldAction | null>(null);
  protected readonly retiring = signal(false);
  protected readonly notice = signal<string | null>(null);
  protected readonly pageError = signal<string | null>(null);

  protected readonly creating = computed(() => this.action()?.kind === 'create');
  protected readonly editing = computed(() => {
    const current = this.action();
    return current?.kind === 'edit' ? current.field : null;
  });
  protected readonly retireAction = computed(() => {
    const current = this.action();
    return current?.kind === 'retire' ? current : null;
  });
  protected readonly hasCustomFields = computed(() => {
    const current = this.state();
    return current.status === 'ready' && activeCustomFields(current.fields).length > 0;
  });

  constructor() {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.state.set({ status: 'loading' });
    try {
      this.state.set({ status: 'ready', fields: await firstValueFrom(this.api.list()) });
    } catch (error) {
      this.state.set({
        status: 'error',
        message: errorMessage(asApiError(error), COMMON_MESSAGES.loadFailed),
      });
    }
  }

  protected canChange(field: PlanField): boolean {
    return field.origin === 'CUSTOM' && field.retiredAt === null;
  }

  protected open(action: FieldAction): void {
    this.notice.set(null);
    this.pageError.set(null);
    this.action.set(action);
  }

  protected openRetire(field: PlanField): void {
    this.open({ kind: 'retire', field, error: null });
  }

  protected closeDialog(): void {
    if (!this.retiring()) {
      this.action.set(null);
    }
  }

  protected onSaved(field: PlanField): void {
    const created = this.action()?.kind === 'create';
    this.action.set(null);
    this.notice.set(
      created ? FIELDS_TEXTS.created(field.label) : FIELDS_TEXTS.updated(field.label),
    );
    void this.load();
  }

  protected onStaleState(): void {
    this.action.set(null);
    this.pageError.set(FIELDS_TEXTS.staleState);
    void this.load();
  }

  protected async retire(): Promise<void> {
    const current = this.action();
    if (current?.kind !== 'retire' || this.retiring()) {
      return;
    }
    this.retiring.set(true);
    try {
      const retired = await firstValueFrom(this.api.retire(current.field.id));
      this.action.set(null);
      this.notice.set(FIELDS_TEXTS.retired(retired.label));
      void this.load();
    } catch (error) {
      this.action.set({
        ...current,
        error: errorMessage(asApiError(error), COMMON_MESSAGES.saveFailed),
      });
    } finally {
      this.retiring.set(false);
    }
  }
}
