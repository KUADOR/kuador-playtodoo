import { signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiError, asApiError } from '../../../core/api/api-error';

export type SubmissionResult<T> =
  { readonly ok: true; readonly value: T } | { readonly ok: false; readonly error: ApiError };

/**
 * Saving state of a dialog of P5: «Guardando…» without double submission. The caller decides what
 * each failure means (field error, dialog error or `409 STALE_STATE` for the page).
 */
export class DialogSubmission {
  private readonly running = signal(false);

  readonly saving = this.running.asReadonly();

  async run<T>(request: () => Observable<T>): Promise<SubmissionResult<T> | null> {
    if (this.running()) {
      return null;
    }
    this.running.set(true);
    try {
      return { ok: true, value: await firstValueFrom(request()) };
    } catch (error) {
      return { ok: false, error: asApiError(error) };
    } finally {
      this.running.set(false);
    }
  }
}
