import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/api/api-base-url';
import { DEFAULT_PAGE_SIZE, Page } from '../../core/api/page';

/** `PlanStatus` of contracts/platform-backoffice.openapi.yaml. */
export type PlanStatus = 'DRAFT' | 'PUBLISHED';

/** `CustomFieldValues`: values of the custom fields by field `key`. */
export type CustomFieldValues = Readonly<Record<string, string | number | boolean>>;

/** `PlanFields`: basic and custom fields of a plan. A draft needs only `name`. */
export interface PlanFields {
  readonly name: string;
  /** `Money`: decimal string with up to 15 integer digits and 4 decimals. */
  readonly price: string | null;
  readonly currency: string | null;
  readonly periodicity: string | null;
  readonly courtLimit: number | null;
  readonly userLimit: number | null;
  readonly customFields: CustomFieldValues;
}

/** `Plan`. */
export interface Plan extends PlanFields {
  readonly id: string;
  readonly status: PlanStatus;
  readonly version: number;
}

/** `UpdatePlanRequest`: the fields plus the `version` that was read. */
export interface UpdatePlanRequest extends PlanFields {
  readonly version: number;
}

/** `PlanOptions` (`GET /plan-options`): allowed currencies and periodicities. */
export interface PlanOptions {
  readonly currencies: readonly string[];
  readonly periodicities: readonly string[];
}

/** Operations of the «Plans» tag. */
@Injectable({ providedIn: 'root' })
export class PlansApi {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${inject(API_BASE_URL)}/platform`;
  private readonly plansUrl = `${this.apiUrl}/plans`;

  list(page: number, size = DEFAULT_PAGE_SIZE): Observable<Page<Plan>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Plan>>(this.plansUrl, { params });
  }

  get(planId: string): Observable<Plan> {
    return this.http.get<Plan>(`${this.plansUrl}/${encodeURIComponent(planId)}`);
  }

  create(fields: PlanFields): Observable<Plan> {
    return this.http.post<Plan>(this.plansUrl, fields);
  }

  update(planId: string, request: UpdatePlanRequest): Observable<Plan> {
    return this.http.put<Plan>(`${this.plansUrl}/${encodeURIComponent(planId)}`, request);
  }

  publish(planId: string): Observable<Plan> {
    return this.http.post<Plan>(`${this.plansUrl}/${encodeURIComponent(planId)}/publication`, null);
  }

  options(): Observable<PlanOptions> {
    return this.http.get<PlanOptions>(`${this.apiUrl}/plan-options`);
  }
}
