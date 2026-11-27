import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PagedList } from '../../../core/api/paged-list';
import { complexDetailUrl, REGISTER_COMPLEX_URL } from '../../../core/app-paths';
import { canAccess, requiresCapability } from '../../../core/auth/access';
import { Session } from '../../../core/auth/session';
import { formatCalendarDate, formatDate } from '../../../core/format/date-format';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Pagination } from '../../../shared/ui/pagination/pagination';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { StatusTag } from '../../../shared/ui/status-tag/status-tag';
import { complexStatusLook, EMPTY_VALUE, formatMoney } from '../complex-texts';
import { ComplexesApi, ComplexSummary } from '../complexes-api';

const SKELETON_ROWS = 5;

/** P2 · Complejos (CA19): every complex with its plan, status and last payment. */
@Component({
  selector: 'app-complexes-page',
  imports: [Alert, Button, Pagination, RouterLink, Skeleton, StatusTag],
  templateUrl: './complexes-page.html',
  styleUrl: './complexes-page.css',
})
export class ComplexesPage {
  private readonly api = inject(ComplexesApi);
  private readonly session = inject(Session);

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly emptyValue = EMPTY_VALUE;
  protected readonly registerUrl = REGISTER_COMPLEX_URL;
  protected readonly complexes = new PagedList<ComplexSummary>((page) => this.api.list(page));
  protected readonly canRegister = computed(() =>
    canAccess(this.session.user(), requiresCapability('COMPLEX_CREATE')),
  );
  protected readonly statusLook = complexStatusLook;
  protected readonly detailUrl = complexDetailUrl;

  constructor() {
    void this.complexes.load();
  }

  protected lastPaymentText(complex: ComplexSummary): string {
    const payment = complex.lastPayment;
    if (payment === null) {
      return 'Sin pagos';
    }
    return `${formatCalendarDate(payment.paidOn)} · ${formatMoney(payment.amount, payment.currency)}`;
  }

  protected purgeText(complex: ComplexSummary): string | null {
    return complex.purgeDueAt === null ? null : `Datos hasta ${formatDate(complex.purgeDueAt)}`;
  }
}
