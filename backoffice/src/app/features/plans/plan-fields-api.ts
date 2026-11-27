import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/api/api-base-url';

/** `PlanFieldType`: immutable after creation. */
export type PlanFieldType = 'TEXT' | 'NUMBER' | 'BOOLEAN';

/** `BASIC` fields are seeded by the api, read-only and never retired. */
export type PlanFieldOrigin = 'BASIC' | 'CUSTOM';

/** `PlanField`: definition of a field a plan can have. */
export interface PlanField {
  readonly id: string;
  /** Key in `customFields`; basic fields use the property names of `Plan`. */
  readonly key: string;
  readonly label: string;
  readonly type: PlanFieldType;
  readonly required: boolean;
  readonly origin: PlanFieldOrigin;
  readonly retiredAt: string | null;
  readonly version: number;
}

/** `CreatePlanFieldRequest`. */
export interface CreatePlanFieldRequest {
  readonly label: string;
  readonly type: PlanFieldType;
  readonly required: boolean;
}

/** `UpdatePlanFieldRequest`. */
export interface UpdatePlanFieldRequest {
  readonly label: string;
  readonly required: boolean;
  readonly version: number;
}

/** Operations of the «Plan fields» tag (SUPER_ADMIN only). */
@Injectable({ providedIn: 'root' })
export class PlanFieldsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/platform/plan-fields`;

  /** Basic fields first, then custom fields by label, retired ones included. */
  list(): Observable<PlanField[]> {
    return this.http.get<PlanField[]>(this.baseUrl);
  }

  create(request: CreatePlanFieldRequest): Observable<PlanField> {
    return this.http.post<PlanField>(this.baseUrl, request);
  }

  update(fieldId: string, request: UpdatePlanFieldRequest): Observable<PlanField> {
    return this.http.put<PlanField>(`${this.baseUrl}/${encodeURIComponent(fieldId)}`, request);
  }

  retire(fieldId: string): Observable<PlanField> {
    return this.http.post<PlanField>(
      `${this.baseUrl}/${encodeURIComponent(fieldId)}/retirement`,
      null,
    );
  }
}

/** Custom fields a plan can still be given: not retired. */
export function activeCustomFields(fields: readonly PlanField[]): PlanField[] {
  return fields.filter((field) => field.origin === 'CUSTOM' && field.retiredAt === null);
}
