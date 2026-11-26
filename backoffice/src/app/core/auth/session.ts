import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { asApiError } from '../api/api-error';
import { AccessTokenStore } from './access-token-store';
import { LoginRequest, PlatformAuthApi } from './platform-auth-api';
import { CurrentPlatformUser } from './platform-user';

/** Why the user is back on the login screen without having signed out. */
export type SessionNotice = 'SESSION_EXPIRED' | 'USER_LOAD_FAILED';

/**
 * Who is signed in. Holds the access token and the user of `GET /me`, which the navigation and
 * the guards read to show only what the user may do.
 */
@Injectable({ providedIn: 'root' })
export class Session {
  private readonly api = inject(PlatformAuthApi);
  private readonly tokens = inject(AccessTokenStore);

  private readonly currentUser = signal<CurrentPlatformUser | null>(null);
  private readonly currentNotice = signal<SessionNotice | null>(null);
  private pendingUserLoad: Promise<CurrentPlatformUser | null> | null = null;

  readonly user = this.currentUser.asReadonly();
  readonly notice = this.currentNotice.asReadonly();
  readonly isSignedIn = computed(() => this.currentUser() !== null);

  accessToken(): string | null {
    return this.tokens.read();
  }

  /** Signs in and loads the user. Fails with the `ApiError` of the login or of `GET /me`. */
  async signIn(credentials: LoginRequest): Promise<CurrentPlatformUser> {
    const token = await firstValueFrom(this.api.login(credentials));
    this.tokens.save(token.accessToken, token.expiresIn);
    this.currentNotice.set(null);
    try {
      return await this.fetchUser();
    } catch (error) {
      this.clear();
      throw asApiError(error);
    }
  }

  /**
   * The signed-in user, loading `GET /me` once if there is a token. Null when there is no
   * session or it could not be loaded (the reason is left in `notice`).
   */
  ensureUser(): Promise<CurrentPlatformUser | null> {
    const user = this.currentUser();
    if (user !== null) {
      return Promise.resolve(user);
    }
    if (this.tokens.read() === null) {
      return Promise.resolve(null);
    }
    this.pendingUserLoad ??= this.loadUserOrClear().finally(() => (this.pendingUserLoad = null));
    return this.pendingUserLoad;
  }

  /**
   * Reloads `GET /me`, for example after a capability was revoked, so menu and buttons follow it.
   * A 401 ends the session; any other failure keeps the current user, because the api still
   * checks every request and the next load of `GET /me` corrects the menu.
   */
  async refreshUser(): Promise<void> {
    if (this.tokens.read() === null) {
      return;
    }
    try {
      await this.fetchUser();
    } catch (error) {
      if (asApiError(error).code === 'UNAUTHENTICATED') {
        this.expire();
      }
    }
  }

  signOut(): void {
    this.clear();
    this.currentNotice.set(null);
  }

  /** The api answered 401 to a signed-in request: the session is over. */
  expire(): void {
    this.clear();
    this.currentNotice.set('SESSION_EXPIRED');
  }

  private async loadUserOrClear(): Promise<CurrentPlatformUser | null> {
    try {
      return await this.fetchUser();
    } catch (error) {
      const apiError = asApiError(error);
      if (apiError.code === 'UNAUTHENTICATED') {
        this.expire();
      } else {
        this.clear();
        this.currentNotice.set('USER_LOAD_FAILED');
      }
      return null;
    }
  }

  private async fetchUser(): Promise<CurrentPlatformUser> {
    const user = await firstValueFrom(this.api.getCurrentUser());
    this.currentUser.set(user);
    return user;
  }

  private clear(): void {
    this.tokens.clear();
    this.currentUser.set(null);
  }
}
