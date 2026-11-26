import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/api/api-base-url';

/** `SubdomainAvailability.reason` of contracts/tenancy.openapi.yaml. */
export type SubdomainUnavailableReason = 'INVALID' | 'RESERVED' | 'TAKEN';

export interface SubdomainAvailability {
  readonly subdomain: string;
  readonly available: boolean;
  readonly reason: SubdomainUnavailableReason | null;
}

interface SubdomainSuggestion {
  readonly subdomain: string;
}

/** Public «Subdomains» operations of contracts/tenancy.openapi.yaml (global host). */
@Injectable({ providedIn: 'root' })
export class SubdomainsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/subdomains`;

  suggest(complexName: string): Observable<SubdomainSuggestion> {
    const params = new HttpParams().set('name', complexName);
    return this.http.get<SubdomainSuggestion>(`${this.baseUrl}/suggestion`, { params });
  }

  availability(subdomain: string): Observable<SubdomainAvailability> {
    return this.http.get<SubdomainAvailability>(
      `${this.baseUrl}/${encodeURIComponent(subdomain)}/availability`,
    );
  }
}
