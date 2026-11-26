import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api/api-base-url';
import { CurrentPlatformUser } from './platform-user';

/** `LoginRequest` of contracts/tenancy.openapi.yaml. */
export interface LoginRequest {
  readonly email: string;
  readonly password: string;
}

/** `AccessToken` of contracts/tenancy.openapi.yaml. */
export interface AccessToken {
  readonly accessToken: string;
  readonly tokenType: 'Bearer';
  readonly expiresIn: number;
}

/** Login (`loginPlatformUser`) and current user (`getCurrentPlatformUser`) on the global host. */
@Injectable({ providedIn: 'root' })
export class PlatformAuthApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  login(request: LoginRequest): Observable<AccessToken> {
    return this.http.post<AccessToken>(`${this.baseUrl}/auth/platform/login`, request);
  }

  getCurrentUser(): Observable<CurrentPlatformUser> {
    return this.http.get<CurrentPlatformUser>(`${this.baseUrl}/platform/me`);
  }
}
