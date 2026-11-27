import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TENANT_API_BASE_URL } from '../../core/api/api-base-url';
import { SUPPORT_SESSION_HEADER } from '../../core/api/api-interceptors';
import { SupportSession } from './support-access-api';

/**
 * Staff operations of contracts/bookings.openapi.yaml as Kuador support calls them (ADR-020): on
 * the host of the complex, with the platform token and the support session header. Every call is
 * a fresh request: nothing of a complex is cached in the backoffice.
 */

export type BookingKind = 'RESERVATION' | 'BLOCK';
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED';

/** `StaffBooking`. */
export interface StaffBooking {
  readonly id: string;
  readonly courtId: string;
  readonly kind: BookingKind;
  readonly status: BookingStatus;
  readonly start: string;
  readonly end: string;
  /** Null for blocks and for pending bookings. */
  readonly holderName: string | null;
  readonly blockReason: string | null;
  /** True on a suspended or cancelled complex: kept, without effect until it is reactivated. */
  readonly inactive: boolean;
}

/** Half-open range `[from, to)` of instants (ISO-8601). */
export interface InstantRange {
  readonly from: string;
  readonly to: string;
}

export interface CreateStaffBookingRequest {
  readonly courtId: string;
  readonly start: string;
  readonly holderName: string;
  readonly holderPhone?: string;
}

export interface BlockSlotRequest {
  readonly courtId: string;
  readonly start: string;
  readonly reason?: string;
}

@Injectable({ providedIn: 'root' })
export class SupportBookingsApi {
  private readonly http = inject(HttpClient);
  private readonly tenantApiBaseUrl = inject(TENANT_API_BASE_URL);

  list(session: SupportSession, range: InstantRange, courtId?: string): Observable<StaffBooking[]> {
    let params = new HttpParams().set('from', range.from).set('to', range.to);
    if (courtId !== undefined) {
      params = params.set('courtId', courtId);
    }
    return this.http.get<StaffBooking[]>(this.url(session, '/staff/bookings'), {
      headers: this.headers(session),
      params,
    });
  }

  create(session: SupportSession, request: CreateStaffBookingRequest): Observable<StaffBooking> {
    return this.http.post<StaffBooking>(this.url(session, '/staff/bookings'), request, {
      headers: this.headers(session),
    });
  }

  block(session: SupportSession, request: BlockSlotRequest): Observable<StaffBooking> {
    return this.http.post<StaffBooking>(this.url(session, '/staff/blocks'), request, {
      headers: this.headers(session),
    });
  }

  /** Cancels a booking or removes a block. */
  cancel(session: SupportSession, bookingId: string): Observable<StaffBooking> {
    return this.http.post<StaffBooking>(
      this.url(session, `/staff/bookings/${encodeURIComponent(bookingId)}/cancel`),
      null,
      { headers: this.headers(session) },
    );
  }

  private url(session: SupportSession, path: string): string {
    return `${this.tenantApiBaseUrl(session.tenantHost)}${path}`;
  }

  private headers(session: SupportSession): HttpHeaders {
    return new HttpHeaders({ [SUPPORT_SESSION_HEADER]: session.sessionId });
  }
}
