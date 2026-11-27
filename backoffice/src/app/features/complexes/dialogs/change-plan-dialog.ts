import { Component, computed, inject, input, output, signal } from '@angular/core';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { RemoteValue } from '../../../core/api/remote-value';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Dialog } from '../../../shared/ui/dialog/dialog';
import { Field, FieldOption } from '../../../shared/ui/field/field';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { EMPTY_VALUE, planFieldRows } from '../complex-texts';
import { ComplexDetail, ComplexesApi } from '../complexes-api';
import { PublishedPlansApi } from '../published-plans-api';
import { DialogSubmission } from './dialog-submission';

const PLAN_ERROR_MESSAGES = {
  PLAN_NOT_PUBLISHED: 'Ese plan ya no está publicado. Elige otro.',
  SAME_PLAN: 'Ese ya es el plan del complejo. Elige otro.',
  SUBSCRIPTION_CANCELLED: 'La suscripción está cancelada: ya no se puede cambiar el plan.',
} as const;

const PLAN_REQUIRED = 'Elige un plan.';

interface ComparisonRow {
  readonly label: string;
  readonly current: string;
  readonly next: string;
}

/**
 * «Cambiar plan» of P5 (CA5). SUPER_ADMIN only. Lists the other published plans and compares
 * their fields with the current one; the api records the change in the history.
 */
@Component({
  selector: 'app-change-plan-dialog',
  imports: [Alert, Button, Dialog, Field, Skeleton],
  template: `
    @let plansState = plans.state();
    <app-dialog
      [heading]="'Cambiar plan · ' + complex().displayName"
      [busy]="submission.saving()"
      (closeRequest)="closeRequest.emit()"
    >
      <p class="dialog-form__current">Plan actual: {{ complex().subscription.plan.name }}</p>
      <div [attr.aria-busy]="plansState.status === 'loading'">
        @if (plansState.status === 'loading') {
          <app-skeleton [rows]="1" />
        } @else if (plansState.status === 'error') {
          <app-alert tone="error">
            {{ plansState.message }}
            <app-button
              alertActions
              data-dialog-initial-focus
              label="Reintentar"
              variant="secondary"
              (press)="plans.load()"
            />
          </app-alert>
        } @else if (planOptions().length === 0) {
          <p>No hay otro plan publicado.</p>
        } @else {
          <app-field
            label="Nuevo plan"
            kind="select"
            placeholder="Elige un plan"
            [required]="true"
            [options]="planOptions()"
            [disabled]="submission.saving()"
            [error]="planError() ?? undefined"
            [value]="planId()"
            (valueChange)="choose($event)"
          />
        }
      </div>
      @if (comparison().length > 0) {
        <table class="dialog-form__comparison">
          <caption class="visually-hidden">
            Comparación del plan actual con el nuevo
          </caption>
          <thead>
            <tr>
              <th scope="col">Campo</th>
              <th scope="col">Actual</th>
              <th scope="col">Nuevo</th>
            </tr>
          </thead>
          <tbody>
            @for (row of comparison(); track row.label) {
              <tr>
                <th scope="row">{{ row.label }}</th>
                <td>{{ row.current }}</td>
                <td>{{ row.next }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
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
      @if (planOptions().length > 0) {
        <app-button
          dialogActions
          label="Cambiar plan"
          [loading]="submission.saving()"
          (press)="save()"
        />
      }
    </app-dialog>
  `,
  styleUrl: './dialog-form.css',
})
export class ChangePlanDialog {
  private readonly api = inject(ComplexesApi);
  private readonly plansApi = inject(PublishedPlansApi);

  readonly complex = input.required<ComplexDetail>();

  readonly changed = output<ComplexDetail>();
  /** `409 STALE_STATE`: someone else changed the complex; the page reloads it. */
  readonly stale = output<void>();
  readonly closeRequest = output<void>();

  protected readonly submission = new DialogSubmission();
  protected readonly plans = new RemoteValue(() => this.plansApi.list());
  protected readonly planId = signal('');
  protected readonly planError = signal<string | null>(null);
  protected readonly failure = signal<string | null>(null);

  private readonly otherPlans = computed(() => {
    const state = this.plans.state();
    const currentId = this.complex().subscription.plan.planId;
    return state.status === 'ready' ? state.value.filter((plan) => plan.id !== currentId) : [];
  });
  protected readonly planOptions = computed<FieldOption[]>(() =>
    this.otherPlans().map((plan) => ({ value: plan.id, label: plan.name })),
  );
  protected readonly comparison = computed<ComparisonRow[]>(() => {
    const next = this.otherPlans().find((plan) => plan.id === this.planId());
    if (!next) {
      return [];
    }
    const currentRows = planFieldRows(this.complex().subscription.plan);
    const nextRows = planFieldRows(next);
    const labels = [...new Set([...currentRows, ...nextRows].map((row) => row.label))];
    const valueOf = (rows: typeof currentRows, label: string) =>
      rows.find((row) => row.label === label)?.value ?? EMPTY_VALUE;
    return labels.map((label) => ({
      label,
      current: valueOf(currentRows, label),
      next: valueOf(nextRows, label),
    }));
  });

  constructor() {
    void this.plans.load();
  }

  protected choose(planId: string): void {
    this.planId.set(planId);
    this.planError.set(null);
    this.failure.set(null);
  }

  protected async save(): Promise<void> {
    if (!this.planId()) {
      this.planError.set(PLAN_REQUIRED);
      return;
    }
    this.failure.set(null);
    const complex = this.complex();
    const result = await this.submission.run(() =>
      this.api.changePlan(complex.id, { planId: this.planId(), version: complex.version }),
    );
    if (result === null) {
      return;
    }
    if (result.ok) {
      this.changed.emit(result.value);
    } else if (result.error.code === 'STALE_STATE') {
      this.stale.emit();
    } else {
      if (result.error.code === 'PLAN_NOT_PUBLISHED') {
        this.planId.set('');
        void this.plans.load();
      }
      this.failure.set(errorMessage(result.error, COMMON_MESSAGES.saveFailed, PLAN_ERROR_MESSAGES));
    }
  }
}
