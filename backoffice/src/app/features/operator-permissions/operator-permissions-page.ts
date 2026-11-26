import { Component, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiError, asApiError } from '../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../core/api/error-messages';
import { Alert } from '../../shared/ui/alert/alert';
import { Button } from '../../shared/ui/button/button';
import { Checkbox } from '../../shared/ui/checkbox/checkbox';
import { Dialog } from '../../shared/ui/dialog/dialog';
import { Skeleton } from '../../shared/ui/skeleton/skeleton';
import { CapabilityChangesTable } from './capability-changes-table';
import { CapabilityDraft, draftOf, pendingChanges } from './capability-draft';
import { capabilityText } from './capability-texts';
import { OperatorCapabilities, OperatorCapabilitiesApi } from './operator-capabilities-api';

/** Texts of P13 «Permisos del operador» («Pantallas», PLATODO-27). */
const PERMISSIONS_TEXTS = {
  saved: 'Guardamos los permisos del operador.',
  staleState:
    'Otra persona cambió los permisos del operador. Revisa los datos y vuelve a intentarlo.',
} as const;

const SKELETON_ROWS = 4;

type CapabilitiesState =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'ready'; readonly capabilities: OperatorCapabilities };

/**
 * P13 · Permisos del operador (CA15, CA16). Configured per role, not per person: the
 * SUPER_ADMIN enables or disables the capabilities that exist in the api; new ones show up
 * disabled without redesigning the screen.
 */
@Component({
  selector: 'app-operator-permissions-page',
  imports: [Alert, Button, CapabilityChangesTable, Checkbox, Dialog, Skeleton],
  templateUrl: './operator-permissions-page.html',
  styleUrl: './operator-permissions-page.css',
})
export class OperatorPermissionsPage {
  private readonly api = inject(OperatorCapabilitiesApi);

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly state = signal<CapabilitiesState>({ status: 'loading' });
  protected readonly draft = signal<CapabilityDraft>({});
  protected readonly confirming = signal(false);
  protected readonly saving = signal(false);
  protected readonly dialogError = signal<string | null>(null);
  protected readonly pageError = signal<string | null>(null);
  protected readonly savedNotice = signal<string | null>(null);
  protected readonly historyRefreshKey = signal(0);

  protected readonly items = computed(() => {
    const current = this.state();
    if (current.status !== 'ready') {
      return [];
    }
    return current.capabilities.items.map((capability) => ({
      key: capability.key,
      text: capabilityText(capability.key, capability.description),
      enabled: this.draft()[capability.key] ?? capability.enabled,
    }));
  });
  protected readonly changes = computed(() => {
    const current = this.state();
    return current.status === 'ready'
      ? pendingChanges(current.capabilities.items, this.draft())
      : [];
  });
  protected readonly toEnable = computed(() => this.labelsOf(true));
  protected readonly toDisable = computed(() => this.labelsOf(false));

  constructor() {
    void this.load();
  }

  protected async load(): Promise<void> {
    this.state.set({ status: 'loading' });
    try {
      this.show(await firstValueFrom(this.api.list()));
    } catch (error) {
      this.state.set({
        status: 'error',
        message: errorMessage(asApiError(error), COMMON_MESSAGES.loadFailed),
      });
    }
  }

  protected toggle(key: string, enabled: boolean): void {
    this.savedNotice.set(null);
    this.draft.update((draft) => ({ ...draft, [key]: enabled }));
  }

  protected discard(): void {
    const current = this.state();
    if (current.status === 'ready') {
      this.draft.set(draftOf(current.capabilities.items));
    }
  }

  protected askConfirmation(): void {
    this.pageError.set(null);
    this.dialogError.set(null);
    this.confirming.set(true);
  }

  protected closeConfirmation(): void {
    if (!this.saving()) {
      this.confirming.set(false);
    }
  }

  protected async save(): Promise<void> {
    const current = this.state();
    if (current.status !== 'ready' || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.dialogError.set(null);
    try {
      const saved = await firstValueFrom(
        this.api.change({ version: current.capabilities.version, changes: this.changes() }),
      );
      this.show(saved);
      this.confirming.set(false);
      this.savedNotice.set(PERMISSIONS_TEXTS.saved);
      this.historyRefreshKey.update((key) => key + 1);
    } catch (error) {
      this.handleSaveFailure(asApiError(error));
    } finally {
      this.saving.set(false);
    }
  }

  private handleSaveFailure(error: ApiError): void {
    if (error.code !== 'STALE_STATE') {
      this.dialogError.set(errorMessage(error, COMMON_MESSAGES.saveFailed));
      return;
    }
    // Someone else changed the capabilities: show the current state before trying again.
    this.confirming.set(false);
    this.pageError.set(PERMISSIONS_TEXTS.staleState);
    void this.load();
  }

  private show(capabilities: OperatorCapabilities): void {
    this.state.set({ status: 'ready', capabilities });
    this.draft.set(draftOf(capabilities.items));
  }

  private labelsOf(enabled: boolean): string[] {
    return this.changes()
      .filter((change) => change.enabled === enabled)
      .map((change) => capabilityText(change.key).label);
  }
}
