import { signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { asApiError } from './api-error';
import { COMMON_MESSAGES, errorMessage } from './error-messages';

export type RemoteState<T> =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'ready'; readonly value: T };

/**
 * A value read from the api with the common states of «Pantallas»: loading, the common load
 * error (to show with «Reintentar») and the loaded value.
 */
export class RemoteValue<T> {
  private readonly current = signal<RemoteState<T>>({ status: 'loading' });

  readonly state = this.current.asReadonly();

  constructor(private readonly fetch: () => Observable<T>) {}

  async load(): Promise<void> {
    this.current.set({ status: 'loading' });
    try {
      this.current.set({ status: 'ready', value: await firstValueFrom(this.fetch()) });
    } catch (error) {
      this.current.set({
        status: 'error',
        message: errorMessage(asApiError(error), COMMON_MESSAGES.loadFailed),
      });
    }
  }
}
