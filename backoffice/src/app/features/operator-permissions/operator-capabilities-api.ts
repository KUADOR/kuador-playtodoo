import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/api/api-base-url';
import { DEFAULT_PAGE_SIZE, Page, UserRef } from '../../core/api/page';
import { Capability } from '../../core/auth/platform-user';

/** `OperatorCapability` of contracts/platform-backoffice.openapi.yaml. */
export interface OperatorCapability {
  readonly key: Capability;
  /** Developer-facing; the screen shows its own Spanish text by `key`. */
  readonly description: string;
  readonly enabled: boolean;
}

/** `OperatorCapabilities`: the list plus the `version` to send back when changing it. */
export interface OperatorCapabilities {
  readonly version: number;
  readonly items: readonly OperatorCapability[];
}

export interface CapabilityToggle {
  readonly key: Capability;
  readonly enabled: boolean;
}

/** `ChangeOperatorCapabilitiesRequest`. */
export interface ChangeOperatorCapabilitiesRequest {
  readonly version: number;
  readonly changes: readonly CapabilityToggle[];
}

/** `CapabilityChange`: one entry of «Cambios de permisos». */
export interface CapabilityChange {
  readonly id: string;
  readonly key: Capability;
  readonly enabled: boolean;
  readonly changedBy: UserRef;
  readonly changedAt: string;
}

/** Operations of the «Operator capabilities» tag (SUPER_ADMIN only). */
@Injectable({ providedIn: 'root' })
export class OperatorCapabilitiesApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/platform/operator-capabilities`;

  list(): Observable<OperatorCapabilities> {
    return this.http.get<OperatorCapabilities>(this.baseUrl);
  }

  change(request: ChangeOperatorCapabilitiesRequest): Observable<OperatorCapabilities> {
    return this.http.put<OperatorCapabilities>(this.baseUrl, request);
  }

  listChanges(page: number, size = DEFAULT_PAGE_SIZE): Observable<Page<CapabilityChange>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<CapabilityChange>>(`${this.baseUrl}/changes`, { params });
  }
}
