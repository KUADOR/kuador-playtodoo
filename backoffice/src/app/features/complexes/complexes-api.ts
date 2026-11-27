import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../core/api/api-base-url';
import { DEFAULT_PAGE_SIZE, Page, UserRef } from '../../core/api/page';

/** Types of the «Complexes» and «Subscriptions» tags of contracts/platform-backoffice.openapi.yaml. */

export type SubscriptionStatus = 'ACTIVE' | 'EXPIRING' | 'SUSPENDED' | 'CANCELLED';

export type TenantStatus =
  | 'PROVISIONING'
  | 'PROVISIONING_FAILED'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'ARCHIVED'
  | 'PURGING'
  | 'PURGED';

export type PaymentMethod = 'TRANSFER' | 'INVOICE';

/** Values of custom plan fields by field `key`. */
export type CustomFieldValues = Readonly<Record<string, string | number | boolean>>;

export interface PlanRef {
  readonly id: string;
  readonly name: string;
}

export interface LastPayment {
  readonly amount: string;
  readonly currency: string;
  readonly paidOn: string;
}

export interface ComplexSummary {
  readonly id: string;
  readonly displayName: string;
  readonly subdomain: string | null;
  readonly plan: PlanRef;
  readonly subscriptionStatus: SubscriptionStatus;
  readonly tenantStatus: TenantStatus;
  readonly purgeDueAt: string | null;
  readonly lastPayment: LastPayment | null;
}

/** Conditions of the plan fixed when it was assigned (ADR-018). */
export interface PlanSnapshot {
  readonly planId: string;
  readonly name: string;
  readonly price: string;
  readonly currency: string;
  readonly periodicity: string;
  readonly courtLimit: number;
  readonly userLimit: number;
  readonly customFields: CustomFieldValues;
  readonly assignedAt: string;
}

export interface Subscription {
  readonly status: SubscriptionStatus;
  readonly statusSince: string;
  readonly statusChangedBy: UserRef;
  readonly plan: PlanSnapshot;
}

export interface ComplexDetail {
  readonly id: string;
  readonly displayName: string;
  readonly subdomain: string | null;
  readonly ownerEmail: string | null;
  readonly createdAt: string;
  readonly tenantStatus: TenantStatus;
  readonly suspendedAt: string | null;
  readonly archivedAt: string | null;
  readonly retentionDays: number | null;
  readonly purgeDueAt: string | null;
  readonly purgedAt: string | null;
  readonly subscription: Subscription;
  readonly allowedTransitions: readonly SubscriptionStatus[];
  /** Optimistic concurrency version: send it back in every mutation. */
  readonly version: number;
}

export interface RegisterComplexRequest {
  readonly displayName: string;
  readonly subdomain: string;
  readonly ownerEmail: string;
  readonly planId: string;
}

export interface ChangePlanRequest {
  readonly planId: string;
  readonly version: number;
}

export interface ChangeStatusRequest {
  readonly targetStatus: SubscriptionStatus;
  readonly version: number;
}

export interface ChangeRetentionRequest {
  readonly retentionDays: number;
  readonly version: number;
}

export interface RecordPaymentRequest {
  readonly amount: string;
  readonly paidOn: string;
  readonly method: PaymentMethod;
}

export interface Payment {
  readonly id: string;
  readonly amount: string;
  readonly currency: string;
  readonly paidOn: string;
  readonly method: PaymentMethod;
  readonly recordedBy: UserRef;
  readonly recordedAt: string;
}

export interface HistoryEntry {
  readonly id: string;
  readonly kind: 'PLAN' | 'STATUS';
  /** Plan name or subscription status before the change; null for the first assignment. */
  readonly before: string | null;
  readonly after: string;
  readonly changedBy: UserRef;
  readonly changedAt: string;
}

function pageParams(page: number, size: number): HttpParams {
  return new HttpParams().set('page', page).set('size', size);
}

/** Operations of the «Complexes» and «Subscriptions» tags. The api checks every permission. */
@Injectable({ providedIn: 'root' })
export class ComplexesApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_BASE_URL)}/platform/complexes`;

  list(page: number, size = DEFAULT_PAGE_SIZE): Observable<Page<ComplexSummary>> {
    return this.http.get<Page<ComplexSummary>>(this.baseUrl, { params: pageParams(page, size) });
  }

  get(complexId: string): Observable<ComplexDetail> {
    return this.http.get<ComplexDetail>(this.complexUrl(complexId));
  }

  register(request: RegisterComplexRequest): Observable<ComplexDetail> {
    return this.http.post<ComplexDetail>(this.baseUrl, request);
  }

  retryProvisioning(complexId: string): Observable<ComplexDetail> {
    return this.http.post<ComplexDetail>(
      `${this.complexUrl(complexId)}/provisioning-retries`,
      null,
    );
  }

  changePlan(complexId: string, request: ChangePlanRequest): Observable<ComplexDetail> {
    return this.http.put<ComplexDetail>(`${this.complexUrl(complexId)}/plan`, request);
  }

  changeStatus(complexId: string, request: ChangeStatusRequest): Observable<ComplexDetail> {
    return this.http.post<ComplexDetail>(`${this.complexUrl(complexId)}/status-changes`, request);
  }

  changeRetention(complexId: string, request: ChangeRetentionRequest): Observable<ComplexDetail> {
    return this.http.put<ComplexDetail>(`${this.complexUrl(complexId)}/retention`, request);
  }

  listPayments(
    complexId: string,
    page: number,
    size = DEFAULT_PAGE_SIZE,
  ): Observable<Page<Payment>> {
    return this.http.get<Page<Payment>>(`${this.complexUrl(complexId)}/payments`, {
      params: pageParams(page, size),
    });
  }

  recordPayment(complexId: string, request: RecordPaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.complexUrl(complexId)}/payments`, request);
  }

  listHistory(
    complexId: string,
    page: number,
    size = DEFAULT_PAGE_SIZE,
  ): Observable<Page<HistoryEntry>> {
    return this.http.get<Page<HistoryEntry>>(`${this.complexUrl(complexId)}/history`, {
      params: pageParams(page, size),
    });
  }

  private complexUrl(complexId: string): string {
    return `${this.baseUrl}/${encodeURIComponent(complexId)}`;
  }
}
