import { Injectable, signal } from '@angular/core';

/**
 * Result notice that P11 and P12 leave for P10 («Éxito: aviso de resultado en la pantalla de
 * destino»). P10 takes it once, so it does not show up again on a later visit.
 */
@Injectable({ providedIn: 'root' })
export class PlansNotice {
  private readonly pending = signal<string | null>(null);

  leave(message: string): void {
    this.pending.set(message);
  }

  take(): string | null {
    const message = this.pending();
    this.pending.set(null);
    return message;
  }
}
