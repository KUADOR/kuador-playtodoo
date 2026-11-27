import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../../core/api/api-base-url';
import { Page } from '../../core/api/page';
import { CustomFieldValues } from './complexes-api';

/** A published `Plan` of the contract: every basic field is filled (ADR-018). */
export interface PublishedPlan {
  readonly id: string;
  readonly name: string;
  readonly price: string;
  readonly currency: string;
  readonly periodicity: string;
  readonly courtLimit: number;
  readonly userLimit: number;
  readonly customFields: CustomFieldValues;
}

/** Largest page of the contract: the published catalogue fits in one page. */
const PUBLISHED_PLANS_PAGE_SIZE = 100;

/**
 * Published plans, the only ones that can be assigned to a complex (CA3). Any platform user may
 * read them (`GET /plans?status=PUBLISHED`); managing plans is P10 to P12 (PLATODO-91).
 */
@Injectable({ providedIn: 'root' })
export class PublishedPlansApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${inject(API_BASE_URL)}/platform/plans`;

  list(): Observable<readonly PublishedPlan[]> {
    const params = new HttpParams()
      .set('status', 'PUBLISHED')
      .set('page', 0)
      .set('size', PUBLISHED_PLANS_PAGE_SIZE);
    return this.http.get<Page<PublishedPlan>>(this.url, { params }).pipe(map((page) => page.items));
  }
}
