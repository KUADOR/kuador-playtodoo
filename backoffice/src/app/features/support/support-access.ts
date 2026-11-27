import { ErrorHandler, Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ComplexDetail } from '../complexes/complexes-api';
import { SupportAccessApi, SupportSession } from './support-access-api';

/** The complex a support session was opened for, as P9 needs it. */
export type SupportComplex = Pick<ComplexDetail, 'id' | 'displayName' | 'purgeDueAt'>;

/** A support session opened with a reason for one complex (P8), used by P9. */
export interface ActiveSupportAccess {
  readonly complex: SupportComplex;
  readonly reason: string;
  readonly session: SupportSession;
}

/**
 * The only support session open in this tab, at most one and for one complex. It lives in
 * memory only (never in storage) and carries no data of the complex: P9 loads that data with the
 * session and drops it when the session ends.
 */
@Injectable({ providedIn: 'root' })
export class SupportAccess {
  private readonly api = inject(SupportAccessApi);
  private readonly errorHandler = inject(ErrorHandler);
  private readonly current = signal<ActiveSupportAccess | null>(null);

  readonly active = this.current.asReadonly();

  /** The open access for `complexId`, or null. */
  activeFor(complexId: string): ActiveSupportAccess | null {
    const access = this.current();
    return access?.complex.id === complexId ? access : null;
  }

  /** Keeps a newly opened session, closing the one of any other complex first. */
  start(access: ActiveSupportAccess): void {
    this.end();
    this.current.set(access);
  }

  /**
   * Forgets the session and asks the api to close it. Closing an expired or invalidated session
   * is a no-op for the api; if the request fails, the session still expires on its own, so the
   * failure is only reported.
   */
  end(): void {
    const access = this.current();
    if (access === null) {
      return;
    }
    this.current.set(null);
    firstValueFrom(this.api.close(access.session.sessionId)).catch((error: unknown) =>
      this.errorHandler.handleError(error),
    );
  }
}
