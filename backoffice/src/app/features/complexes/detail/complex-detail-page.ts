import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink, RouterOutlet } from '@angular/router';
import { firstValueFrom, map } from 'rxjs';
import { asApiError } from '../../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { ROOT_DOMAIN, tenantHost } from '../../../core/api/root-domain';
import {
  complexAuditUrl,
  complexDetailUrl,
  complexHistoryUrl,
  complexPaymentsUrl,
  complexSupportUrl,
  COMPLEX_PATHS,
  HOME_URL,
} from '../../../core/app-paths';
import { canAccess, SUPER_ADMIN_ONLY } from '../../../core/auth/access';
import { Session } from '../../../core/auth/session';
import { formatDateTime } from '../../../core/format/date-format';
import { resultNoticeOf } from '../../../core/navigation/result-notice';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { StatusTag } from '../../../shared/ui/status-tag/status-tag';
import { TabLink, TabNav } from '../../../shared/ui/tab-nav/tab-nav';
import { complexActions, lifecycleNotices } from '../complex-lifecycle';
import {
  complexStatusLook,
  EMPTY_VALUE,
  formatMoney,
  SUBSCRIPTION_STATUS_LOOKS,
} from '../complex-texts';
import { ComplexDetail, ComplexesApi, Payment } from '../complexes-api';
import { ChangePlanDialog } from '../dialogs/change-plan-dialog';
import { ChangeRetentionDialog } from '../dialogs/change-retention-dialog';
import { ChangeStatusDialog } from '../dialogs/change-status-dialog';
import { RecordPaymentDialog } from '../dialogs/record-payment-dialog';
import { SupportAccessDialog } from '../../support/dialogs/support-access-dialog';
import { ActiveSupportAccess, SupportAccess } from '../../support/support-access';
import { ComplexDetailStore } from './complex-detail-store';

const TITLE_SUFFIX = ' · Backoffice de Kuador';
const SKELETON_ROWS = 4;

type OpenDialog = 'payment' | 'status' | 'plan' | 'retention' | 'access';

const RETRY_ERROR_MESSAGES = {
  NOT_PROVISIONING_FAILED: 'El alta ya no está incompleta. Revisa el estado del complejo.',
} as const;

/**
 * P4 · Detalle del complejo: name, subdomain and tag, the fixed notices of its lifecycle, the
 * actions the user may take (including «Acceder a datos del complejo», P8) and the tabs
 * «Resumen», «Pagos», «Historial» (P6) and, for the SUPER_ADMIN, «Auditoría» (P7).
 */
@Component({
  selector: 'app-complex-detail-page',
  imports: [
    Alert,
    Button,
    ChangePlanDialog,
    ChangeRetentionDialog,
    ChangeStatusDialog,
    RecordPaymentDialog,
    RouterLink,
    RouterOutlet,
    Skeleton,
    StatusTag,
    SupportAccessDialog,
    TabNav,
  ],
  providers: [ComplexDetailStore],
  templateUrl: './complex-detail-page.html',
  styleUrl: './complex-detail-page.css',
})
export class ComplexDetailPage {
  private readonly store = inject(ComplexDetailStore);
  private readonly api = inject(ComplexesApi);
  private readonly session = inject(Session);
  private readonly supportAccess = inject(SupportAccess);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly rootDomain = inject(ROOT_DOMAIN);

  protected readonly complexesUrl = HOME_URL;
  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly state = this.store.state;
  protected readonly complex = this.store.complex;
  protected readonly resultNotice = signal(resultNoticeOf(this.router));
  protected readonly pageError = signal<string | null>(null);
  protected readonly openDialog = signal<OpenDialog | null>(null);
  protected readonly retrying = signal(false);

  private readonly complexId = toSignal(
    inject(ActivatedRoute).paramMap.pipe(
      map((params) => params.get(COMPLEX_PATHS.complexIdParam) ?? ''),
    ),
    { initialValue: '' },
  );

  protected readonly host = computed(() => {
    const subdomain = this.complex()?.subdomain;
    return subdomain ? tenantHost(subdomain, this.rootDomain) : EMPTY_VALUE;
  });
  protected readonly statusLook = computed(() => {
    const complex = this.complex();
    return complex === null
      ? null
      : complexStatusLook({
          subscriptionStatus: complex.subscription.status,
          tenantStatus: complex.tenantStatus,
        });
  });
  protected readonly notices = computed(() => {
    const complex = this.complex();
    return complex === null ? [] : lifecycleNotices(complex);
  });
  protected readonly actions = computed(() => {
    const complex = this.complex();
    return complex === null ? null : complexActions(this.session.user(), complex);
  });
  protected readonly hasActions = computed(() => {
    const actions = this.actions();
    return (
      actions !== null &&
      (actions.recordPayment || actions.changeStatus || actions.changePlan || actions.accessData)
    );
  });
  protected readonly tabs = computed<TabLink[]>(() => {
    const id = this.complexId();
    const tabs: TabLink[] = [
      { label: 'Resumen', link: complexDetailUrl(id) },
      { label: 'Pagos', link: complexPaymentsUrl(id) },
      { label: 'Historial', link: complexHistoryUrl(id) },
    ];
    return canAccess(this.session.user(), SUPER_ADMIN_ONLY)
      ? [...tabs, { label: 'Auditoría', link: complexAuditUrl(id) }]
      : tabs;
  });

  constructor() {
    effect(() => {
      const id = this.complexId();
      if (id) {
        void this.store.load(id);
      }
    });
    effect(() => {
      const complex = this.complex();
      if (complex !== null) {
        this.title.setTitle(`${complex.displayName}${TITLE_SUFFIX}`);
      }
    });
  }

  protected reload(): void {
    void this.store.reload();
  }

  protected open(dialog: OpenDialog): void {
    this.pageError.set(null);
    this.openDialog.set(dialog);
  }

  protected closeDialog(): void {
    this.openDialog.set(null);
  }

  protected paymentRecorded(payment: Payment): void {
    this.closeDialog();
    this.store.paymentRecorded();
    this.resultNotice.set(
      `Registramos el pago de ${formatMoney(payment.amount, payment.currency)}.`,
    );
  }

  protected statusChanged(complex: ComplexDetail): void {
    this.applyChange(
      complex,
      `La suscripción quedó «${SUBSCRIPTION_STATUS_LOOKS[complex.subscription.status].label}».`,
    );
  }

  protected planChanged(complex: ComplexDetail): void {
    this.applyChange(complex, `El plan del complejo ahora es ${complex.subscription.plan.name}.`);
  }

  protected retentionChanged(complex: ComplexDetail): void {
    const deadline =
      complex.purgeDueAt === null ? '' : ` a partir del ${formatDateTime(complex.purgeDueAt)}`;
    this.applyChange(
      complex,
      `Cambiamos el plazo de conservación: los datos se borrarán${deadline}.`,
    );
  }

  /** P8 accepted the reason: P9 opens with that session. */
  protected supportOpened(access: ActiveSupportAccess): void {
    this.closeDialog();
    this.supportAccess.start(access);
    void this.router.navigateByUrl(complexSupportUrl(access.complex.id));
  }

  /** `409 STALE_STATE` from a dialog: show the current data before trying again. */
  protected staleState(): void {
    this.closeDialog();
    this.pageError.set(COMMON_MESSAGES.staleState);
    void this.store.reload();
  }

  protected async retryRegistration(): Promise<void> {
    const complex = this.complex();
    if (complex === null || this.retrying()) {
      return;
    }
    this.retrying.set(true);
    this.pageError.set(null);
    try {
      this.store.replace(await firstValueFrom(this.api.retryProvisioning(complex.id)));
    } catch (error) {
      const apiError = asApiError(error);
      this.pageError.set(errorMessage(apiError, COMMON_MESSAGES.saveFailed, RETRY_ERROR_MESSAGES));
      if (apiError.code === 'NOT_PROVISIONING_FAILED') {
        void this.store.reload();
      }
    } finally {
      this.retrying.set(false);
    }
  }

  private applyChange(complex: ComplexDetail, notice: string): void {
    this.closeDialog();
    this.store.changed(complex);
    this.resultNotice.set(notice);
  }
}
