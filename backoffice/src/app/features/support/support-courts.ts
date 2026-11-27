import { InjectionToken } from '@angular/core';
import { Observable, of } from 'rxjs';

/** Local time of the complex, `HH:mm`. */
type LocalTime = string;

/** What support needs to know about a court to offer its slots. */
export interface Court {
  readonly id: string;
  readonly name: string;
  /** Slot length configured by the owner, in minutes (greater than 0). */
  readonly slotDurationMinutes: number;
  /** First slot start. */
  readonly opensAt: LocalTime;
  /** No slot ends after this time. */
  readonly closesAt: LocalTime;
}

/** Port to the courts of a complex, read on its host like the rest of the support data. */
export interface SupportCourts {
  courtsOf(tenantHost: string): Observable<readonly Court[]>;
}

/**
 * No contract lists the courts of a complex or their valid slots yet: the Courts module does not
 * exist (ADR-020, point 6). [POR DEFINIR: endpoint de canchas, duración de franja y horario.]
 * Until then the default source has no courts, so P9 cannot offer «Crear reserva» or «Bloquear
 * horario»; the development mock provides example courts (mock-backend.development.ts).
 */
export const SUPPORT_COURTS = new InjectionToken<SupportCourts>('SUPPORT_COURTS', {
  providedIn: 'root',
  factory: () => ({ courtsOf: () => of([]) }),
});
