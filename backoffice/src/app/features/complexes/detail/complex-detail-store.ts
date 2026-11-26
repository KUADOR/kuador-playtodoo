import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { asApiError } from '../../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { ComplexDetail, ComplexesApi } from '../complexes-api';

/** How often P4 asks again while a provisioning retry runs (`202` of the contract). */
export const PROVISIONING_POLL_MS = 2000;

export type ComplexDetailState =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'not-found' }
  | { readonly status: 'ready'; readonly complex: ComplexDetail };

/**
 * The complex P4 shows, shared by the detail page, its tabs and its dialogs. Provided by the
 * detail page, so it lives while that complex is open. After a change the tabs that list
 * payments or history reload through their revision counters.
 */
@Injectable()
export class ComplexDetailStore {
  private readonly api = inject(ComplexesApi);

  private readonly current = signal<ComplexDetailState>({ status: 'loading' });
  private readonly paymentsChanged = signal(0);
  private readonly historyChanged = signal(0);
  private complexId: string | null = null;
  private pollTimer: ReturnType<typeof setTimeout> | null = null;

  readonly state = this.current.asReadonly();
  readonly complex = computed(() => {
    const state = this.current();
    return state.status === 'ready' ? state.complex : null;
  });
  readonly paymentsRevision = this.paymentsChanged.asReadonly();
  readonly historyRevision = this.historyChanged.asReadonly();

  constructor() {
    inject(DestroyRef).onDestroy(() => this.stopPolling());
  }

  async load(complexId: string): Promise<void> {
    if (complexId !== this.complexId) {
      this.complexId = complexId;
      this.current.set({ status: 'loading' });
    }
    try {
      this.show(await firstValueFrom(this.api.get(complexId)));
    } catch (error) {
      const apiError = asApiError(error);
      this.current.set(
        apiError.code === 'COMPLEX_NOT_FOUND'
          ? { status: 'not-found' }
          : { status: 'error', message: errorMessage(apiError, COMMON_MESSAGES.loadFailed) },
      );
    }
  }

  /** Loads the open complex again, for example after `409 STALE_STATE`. */
  reload(): Promise<void> {
    return this.complexId === null ? Promise.resolve() : this.load(this.complexId);
  }

  /** The api answered an operation with the complex after it (for example a provisioning retry). */
  replace(complex: ComplexDetail): void {
    this.show(complex);
  }

  /** The api answered a change of plan, status or retention, which adds a history entry. */
  changed(complex: ComplexDetail): void {
    this.show(complex);
    this.historyChanged.update((revision) => revision + 1);
  }

  paymentRecorded(): void {
    this.paymentsChanged.update((revision) => revision + 1);
  }

  private show(complex: ComplexDetail): void {
    this.stopPolling();
    this.current.set({ status: 'ready', complex });
    if (complex.tenantStatus === 'PROVISIONING') {
      this.pollTimer = setTimeout(() => void this.reload(), PROVISIONING_POLL_MS);
    }
  }

  private stopPolling(): void {
    if (this.pollTimer !== null) {
      clearTimeout(this.pollTimer);
      this.pollTimer = null;
    }
  }
}
