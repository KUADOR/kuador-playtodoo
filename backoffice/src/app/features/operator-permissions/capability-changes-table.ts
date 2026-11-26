import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { asApiError } from '../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../core/api/error-messages';
import { Page } from '../../core/api/page';
import { formatDateTime } from '../../core/format/date-format';
import { Alert } from '../../shared/ui/alert/alert';
import { Button } from '../../shared/ui/button/button';
import { Pagination } from '../../shared/ui/pagination/pagination';
import { Skeleton } from '../../shared/ui/skeleton/skeleton';
import { capabilityText } from './capability-texts';
import { CapabilityChange, OperatorCapabilitiesApi } from './operator-capabilities-api';

const SKELETON_ROWS = 3;

type ChangesState =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'ready'; readonly page: Page<CapabilityChange> };

/** «Cambios de permisos» of P13: who enabled or disabled which capability and when. */
@Component({
  selector: 'app-capability-changes-table',
  imports: [Alert, Button, Pagination, Skeleton],
  templateUrl: './capability-changes-table.html',
})
export class CapabilityChangesTable {
  private readonly api = inject(OperatorCapabilitiesApi);

  /** Changes every time the parent saved: the table goes back to the newest page. */
  readonly refreshKey = input(0);

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly state = signal<ChangesState>({ status: 'loading' });
  protected readonly formatDateTime = formatDateTime;

  constructor() {
    effect(() => {
      this.refreshKey();
      untracked(() => void this.load(0));
    });
  }

  protected label(change: CapabilityChange): string {
    return capabilityText(change.key).label;
  }

  protected async load(page: number): Promise<void> {
    this.state.set({ status: 'loading' });
    try {
      this.state.set({ status: 'ready', page: await firstValueFrom(this.api.listChanges(page)) });
    } catch (error) {
      this.state.set({
        status: 'error',
        message: errorMessage(asApiError(error), COMMON_MESSAGES.loadFailed),
      });
    }
  }
}
