import { Injectable, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';

const STORAGE_KEY = 'playtodoo.backoffice.access-token';
const MILLISECONDS_PER_SECOND = 1000;

interface StoredToken {
  readonly accessToken: string;
  readonly expiresAt: number;
}

/**
 * Keeps the platform access token for the browser tab. `sessionStorage` survives a reload but not
 * closing the tab, and is never shared with other tabs or sent automatically like a cookie.
 */
@Injectable({ providedIn: 'root' })
export class AccessTokenStore {
  private readonly storage = inject(DOCUMENT).defaultView?.sessionStorage ?? null;

  /** The token, or null when there is none or it already expired. */
  read(): string | null {
    const stored = this.readStored();
    if (stored === null) {
      return null;
    }
    if (stored.expiresAt <= Date.now()) {
      this.clear();
      return null;
    }
    return stored.accessToken;
  }

  save(accessToken: string, expiresInSeconds: number): void {
    const stored: StoredToken = {
      accessToken,
      expiresAt: Date.now() + expiresInSeconds * MILLISECONDS_PER_SECOND,
    };
    this.storage?.setItem(STORAGE_KEY, JSON.stringify(stored));
  }

  clear(): void {
    this.storage?.removeItem(STORAGE_KEY);
  }

  private readStored(): StoredToken | null {
    const raw = this.storage?.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as StoredToken;
    } catch {
      // A value that is not ours (or corrupted) is treated as no session.
      this.clear();
      return null;
    }
  }
}
