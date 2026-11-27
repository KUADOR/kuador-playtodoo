import { DestroyRef, InjectionToken, Signal, inject, signal } from '@angular/core';

/** Time source of the support session countdown, replaceable in tests. */
export interface SupportClock {
  now(): number;
  /** How often the countdown is recomputed, in milliseconds. */
  readonly tickMs: number;
}

const TICK_MS = 1000;

export const SUPPORT_CLOCK = new InjectionToken<SupportClock>('SUPPORT_CLOCK', {
  providedIn: 'root',
  factory: () => ({ now: () => Date.now(), tickMs: TICK_MS }),
});

/** With this little time left the support bar warns that the access is about to expire. */
export const SESSION_WARNING_MS = 5 * 60_000;
const MILLISECONDS_PER_MINUTE = 60_000;

export type SessionPhase = 'active' | 'expiring' | 'expired';

/** Phase of a session with `remainingMs` left. */
export function sessionPhase(remainingMs: number): SessionPhase {
  if (remainingMs <= 0) {
    return 'expired';
  }
  return remainingMs <= SESSION_WARNING_MS ? 'expiring' : 'active';
}

/** Whole minutes left, rounded up, for «quedan {n} min». */
export function minutesLeft(remainingMs: number): number {
  return Math.ceil(Math.max(0, remainingMs) / MILLISECONDS_PER_MINUTE);
}

/**
 * The current time as a signal, updated every `tickMs` of the support clock. Create it in an
 * injection context: it ticks until that context is destroyed.
 */
export class Ticker {
  private readonly clock = inject(SUPPORT_CLOCK);
  private readonly current = signal(this.clock.now());

  readonly now: Signal<number> = this.current.asReadonly();

  constructor() {
    const timer = setInterval(() => this.current.set(this.clock.now()), this.clock.tickMs);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  /** Milliseconds from now until `instant` (ISO-8601); negative once it passed. */
  until(instant: string): number {
    return new Date(instant).getTime() - this.now();
  }
}
