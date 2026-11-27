import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/api/api-base-url';
import { DEFAULT_PAGE_SIZE, Page, UserRef } from '../../core/api/page';
import { TenantStatus } from '../complexes/complexes-api';

/** Types of the «Support access» tag of contracts/platform-backoffice.openapi.yaml (ADR-020). */

/** `SupportSession`: valid on `tenantHost` with the header `X-Support-Session: sessionId`. */
export interface SupportSession {
  readonly sessionId: string;
  readonly expiresAt: string;
  readonly tenantHost: string;
  readonly tenantStatus: TenantStatus;
}

export interface AuditEntry {
  readonly id: string;
  readonly sessionId: string | null;
  readonly actor: UserRef;
  /** Null when `reasonRedacted` is true (the complex was purged). */
  readonly reason: string | null;
  readonly reasonRedacted: boolean;
  /** operationId of the contract operation; null if the route is unknown. */
  readonly operationId: string | null;
  readonly httpMethod: string;
  readonly pathTemplate: string;
  readonly result: 'ALLOWED' | 'DENIED';
  readonly denialCode: string | null;
  readonly occurredAt: string;
}

/** Operations of the «Support access» tag. The api checks every permission. */
@Injectable({ providedIn: 'root' })
export class SupportAccessApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/platform`;

  open(complexId: string, reason: string): Observable<SupportSession> {
    return this.http.post<SupportSession>(`${this.complexUrl(complexId)}/support-sessions`, {
      reason,
    });
  }

  close(sessionId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/support-sessions/${encodeURIComponent(sessionId)}`,
    );
  }

  listAuditEntries(
    complexId: string,
    page: number,
    size = DEFAULT_PAGE_SIZE,
  ): Observable<Page<AuditEntry>> {
    return this.http.get<Page<AuditEntry>>(`${this.complexUrl(complexId)}/audit-entries`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  private complexUrl(complexId: string): string {
    return `${this.baseUrl}/complexes/${encodeURIComponent(complexId)}`;
  }
}
