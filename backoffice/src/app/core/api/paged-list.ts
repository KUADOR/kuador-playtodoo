import { signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { asApiError } from './api-error';
import { COMMON_MESSAGES, errorMessage } from './error-messages';
import { Page } from './page';

export type PagedState<T> =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'ready'; readonly page: Page<T> };

/**
 * One paginated list of the api with the common states of «Pantallas»: loading, the common
 * load error (to show with «Reintentar») and the loaded page.
 */
export class PagedList<T> {
  private readonly current = signal<PagedState<T>>({ status: 'loading' });
  private lastRequested = 0;

  readonly state = this.current.asReadonly();

  constructor(private readonly fetchPage: (page: number) => Observable<Page<T>>) {}

  /** Loads `page` (zero-based); an answer to an older request is ignored. */
  async load(page = 0): Promise<void> {
    const request = ++this.lastRequested;
    this.current.set({ status: 'loading' });
    try {
      const loaded = await firstValueFrom(this.fetchPage(page));
      if (request === this.lastRequested) {
        this.current.set({ status: 'ready', page: loaded });
      }
    } catch (error) {
      if (request === this.lastRequested) {
        this.current.set({
          status: 'error',
          message: errorMessage(asApiError(error), COMMON_MESSAGES.loadFailed),
        });
      }
    }
  }
}
