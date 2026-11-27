import {
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
  viewChildren,
} from '@angular/core';
import { Location, NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideChevronLeft } from '@lucide/angular';
import { firstValueFrom } from 'rxjs';
import { ApiError, asApiError } from '../../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { LeaveConfirmation } from '../../../core/navigation/unsaved-changes-guard';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { Field } from '../../../shared/ui/field/field';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { StatusBadge } from '../../../shared/ui/status-badge/status-badge';
import { PlanFieldsApi, activeCustomFields } from '../plan-fields-api';
import {
  PlanFormErrors,
  PlanFormField,
  PlanFormValues,
  PlanValidationMode,
  apiFieldErrors,
  planFormFields,
  planFormValues,
  toPlanFields,
  validatePlanForm,
} from '../plan-form';
import { PLANS_URL, PLAN_FIELDS_URL, PLAN_ID_PARAM, planUrl } from '../plan-paths';
import { PLAN_STATUS_BADGES } from '../plan-texts';
import { Plan, PlansApi } from '../plans-api';
import { PlansNotice } from '../plans-notice';
import { PUBLISH_TEXTS, publishDialogHeading, publishedNotice } from '../publish-texts';

/** Texts of P11 «Crear o editar plan» («Pantallas», PLATODO-27). */
const EDITOR_TEXTS = {
  nameTaken: 'Ya existe un plan con ese nombre. Elige otro.',
  staleState: 'Otra persona cambió este plan. Revisa los datos y vuelve a intentarlo.',
  draftSaved: (name: string) => `Guardamos el borrador del plan «${name}».`,
  changesSaved: (name: string) => `Guardamos los cambios del plan «${name}».`,
} as const;

const SKELETON_ROWS = 6;
const BACK_ICON_SIZE_PX = 20;
const CHOICE_PLACEHOLDER = 'Elige una opción';
const NAME_PATH = 'name';

type EditorState =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'not-found' }
  | {
      readonly status: 'ready';
      /** Null while creating, until the first save. */
      readonly plan: Plan | null;
      readonly basic: readonly PlanFormField[];
      readonly custom: readonly PlanFormField[];
    };

type SaveAction = 'draft' | 'publish' | 'changes';

/**
 * P11 · Crear o editar plan (CA3, CA4). «Datos básicos» plus «Otros campos» generated from the
 * plan field definitions (P12) by type. A draft needs only the name; publishing, or saving a
 * published plan, needs every basic and required field. Leaving with unsaved changes asks first.
 */
@Component({
  selector: 'app-plan-editor-page',
  imports: [
    Alert,
    Button,
    Dialog,
    Field,
    LucideChevronLeft,
    NgTemplateOutlet,
    RouterLink,
    Skeleton,
    StatusBadge,
  ],
  templateUrl: './plan-editor-page.html',
  styleUrl: './plan-editor-page.css',
  host: { '(window:beforeunload)': 'warnBeforeUnload($event)' },
})
export class PlanEditorPage implements LeaveConfirmation {
  private readonly plansApi = inject(PlansApi);
  private readonly fieldsApi = inject(PlanFieldsApi);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly injector = inject(Injector);
  private readonly notice = inject(PlansNotice);
  private readonly planId = inject(ActivatedRoute).snapshot.paramMap.get(PLAN_ID_PARAM);

  protected readonly isNew = this.planId === null;
  protected readonly plansUrl = PLANS_URL;
  protected readonly planFieldsUrl = PLAN_FIELDS_URL;
  protected readonly publishTexts = PUBLISH_TEXTS;
  protected readonly publishDialogHeading = publishDialogHeading;
  protected readonly choicePlaceholder = CHOICE_PLACEHOLDER;
  protected readonly backIconSize = BACK_ICON_SIZE_PX;
  protected readonly skeletonRows = SKELETON_ROWS;

  protected readonly state = signal<EditorState>({ status: 'loading' });
  protected readonly values = signal<PlanFormValues>({});
  protected readonly errors = signal<PlanFormErrors>({});
  protected readonly saving = signal<SaveAction | null>(null);
  protected readonly formError = signal<string | null>(null);
  protected readonly confirmingPublish = signal(false);
  protected readonly publishError = signal<string | null>(null);
  protected readonly leaving = signal(false);
  private readonly savedValues = signal<PlanFormValues>({});
  private resolveLeave: ((leave: boolean) => void) | null = null;

  private readonly fieldControls = viewChildren(Field);
  private readonly errorSummary = viewChild<ElementRef<HTMLElement>>('errorSummary');

  protected readonly plan = computed(() => {
    const current = this.state();
    return current.status === 'ready' ? current.plan : null;
  });
  protected readonly isPublished = computed(() => this.plan()?.status === 'PUBLISHED');
  protected readonly heading = computed(() => {
    const plan = this.plan();
    return plan === null ? 'Crear plan' : `Editar plan «${plan.name}»`;
  });
  protected readonly statusBadge = computed(
    () => PLAN_STATUS_BADGES[this.plan()?.status ?? 'DRAFT'],
  );
  protected readonly allFields = computed(() => {
    const current = this.state();
    return current.status === 'ready' ? [...current.basic, ...current.custom] : [];
  });
  /** Error summary in the order of the form, linked to each control. */
  protected readonly summary = computed(() => {
    const errors = this.errors();
    return this.allFields()
      .map((field, index) => ({ field, index, message: errors[field.path] }))
      .filter((entry) => entry.message !== undefined);
  });
  protected readonly dirty = computed(
    () => JSON.stringify(this.values()) !== JSON.stringify(this.savedValues()),
  );

  constructor() {
    void this.load();
  }

  confirmLeave(): boolean | Promise<boolean> {
    if (!this.dirty()) {
      return true;
    }
    this.leaving.set(true);
    return new Promise((resolve) => (this.resolveLeave = resolve));
  }

  protected answerLeave(leave: boolean): void {
    this.leaving.set(false);
    this.resolveLeave?.(leave);
    this.resolveLeave = null;
  }

  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.dirty()) {
      event.preventDefault();
    }
  }

  protected async load(): Promise<void> {
    this.state.set({ status: 'loading' });
    try {
      const [options, definitions, plan] = await Promise.all([
        firstValueFrom(this.plansApi.options()),
        firstValueFrom(this.fieldsApi.list()),
        this.planId === null
          ? Promise.resolve(null)
          : firstValueFrom(this.plansApi.get(this.planId)),
      ]);
      const { basic, custom } = planFormFields(options, activeCustomFields(definitions));
      this.state.set({ status: 'ready', plan, basic, custom });
      this.resetValues(plan);
    } catch (error) {
      const failure = asApiError(error);
      this.state.set(
        failure.code === 'PLAN_NOT_FOUND'
          ? { status: 'not-found' }
          : { status: 'error', message: errorMessage(failure, COMMON_MESSAGES.loadFailed) },
      );
    }
  }

  protected inputModeOf(field: PlanFormField): 'decimal' | 'numeric' | undefined {
    if (field.kind === 'decimal') {
      return 'decimal';
    }
    return field.kind === 'integer' ? 'numeric' : undefined;
  }

  protected helpOf(field: PlanFormField): string | undefined {
    if (field.path === NAME_PATH) {
      return 'Obligatorio.';
    }
    return field.requiredToPublish ? 'Obligatorio para publicar.' : undefined;
  }

  protected update(path: string, value: string): void {
    this.values.update((values) => ({ ...values, [path]: value }));
  }

  protected focusField(event: Event, index: number): void {
    event.preventDefault();
    this.fieldControls()[index]?.focus();
  }

  protected controlIdOf(index: number): string | null {
    return this.fieldControls()[index]?.controlId ?? null;
  }

  protected cancel(): void {
    void this.router.navigateByUrl(PLANS_URL);
  }

  protected async saveDraft(): Promise<void> {
    if (!this.isValid('draft')) {
      return;
    }
    const saved = await this.persist('draft');
    if (saved !== null) {
      this.finish(EDITOR_TEXTS.draftSaved(saved.name));
    }
  }

  protected async saveChanges(): Promise<void> {
    if (!this.isValid('complete')) {
      return;
    }
    const saved = await this.persist('changes');
    if (saved !== null) {
      this.finish(EDITOR_TEXTS.changesSaved(saved.name));
    }
  }

  protected askToPublish(): void {
    if (this.isValid('complete')) {
      this.publishError.set(null);
      this.confirmingPublish.set(true);
    }
  }

  protected closePublish(): void {
    if (this.saving() === null) {
      this.confirmingPublish.set(false);
    }
  }

  protected async publish(): Promise<void> {
    if (this.saving() !== null) {
      return;
    }
    const saved = await this.persist('publish');
    if (saved === null) {
      return;
    }
    this.saving.set('publish');
    try {
      const published = await firstValueFrom(this.plansApi.publish(saved.id));
      this.finish(publishedNotice(published.name));
    } catch (error) {
      this.handleFailure(asApiError(error));
    } finally {
      this.saving.set(null);
    }
  }

  /** Creates or updates the plan; null (with the errors shown) when it failed. */
  private async persist(action: SaveAction): Promise<Plan | null> {
    const current = this.state();
    if (current.status !== 'ready' || this.saving() !== null) {
      return null;
    }
    this.saving.set(action);
    this.formError.set(null);
    const fields = toPlanFields(this.allFields(), this.values());
    try {
      const saved = await firstValueFrom(
        current.plan === null
          ? this.plansApi.create(fields)
          : this.plansApi.update(current.plan.id, { ...fields, version: current.plan.version }),
      );
      this.keepSaved(saved);
      return saved;
    } catch (error) {
      this.handleFailure(asApiError(error));
      return null;
    } finally {
      this.saving.set(null);
    }
  }

  /** From now on the page edits the saved plan (a created draft gets its own address). */
  private keepSaved(plan: Plan): void {
    this.state.update((current) => (current.status === 'ready' ? { ...current, plan } : current));
    this.savedValues.set(this.values());
    this.location.replaceState(planUrl(plan.id));
  }

  private finish(message: string): void {
    this.confirmingPublish.set(false);
    this.notice.leave(message);
    void this.router.navigateByUrl(PLANS_URL);
  }

  private isValid(mode: PlanValidationMode): boolean {
    const errors = validatePlanForm(this.allFields(), this.values(), mode);
    this.showErrors(errors);
    return Object.keys(errors).length === 0;
  }

  private showErrors(errors: PlanFormErrors): void {
    this.errors.set(errors);
    if (Object.keys(errors).length > 0) {
      afterNextRender(() => this.errorSummary()?.nativeElement.focus(), {
        injector: this.injector,
      });
    }
  }

  private handleFailure(error: ApiError): void {
    if (error.code === 'PLAN_NOT_FOUND') {
      this.confirmingPublish.set(false);
      this.state.set({ status: 'not-found' });
      return;
    }
    const fieldErrors =
      error.code === 'PLAN_NAME_TAKEN'
        ? { [NAME_PATH]: EDITOR_TEXTS.nameTaken }
        : apiFieldErrors(this.allFields(), error.fieldErrors);
    if (Object.keys(fieldErrors).length > 0) {
      this.confirmingPublish.set(false);
      this.showErrors(fieldErrors);
      return;
    }
    if (error.code === 'STALE_STATE') {
      this.confirmingPublish.set(false);
      void this.load().then(() => this.formError.set(EDITOR_TEXTS.staleState));
      return;
    }
    const message = errorMessage(error, COMMON_MESSAGES.saveFailed);
    if (this.confirmingPublish()) {
      this.publishError.set(message);
    } else {
      this.formError.set(message);
    }
  }

  private resetValues(plan: Plan | null): void {
    const values = planFormValues(this.allFields(), plan);
    this.values.set(values);
    this.savedValues.set(values);
    this.errors.set({});
  }
}
