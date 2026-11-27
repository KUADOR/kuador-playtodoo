import {
  Component,
  Injector,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChildren,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  debounceTime,
  filter,
  firstValueFrom,
  map,
  of,
  switchMap,
} from 'rxjs';
import { ApiError, asApiError } from '../../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { RemoteValue } from '../../../core/api/remote-value';
import { ROOT_DOMAIN } from '../../../core/api/root-domain';
import { complexDetailUrl, HOME_URL, PLANS_URL } from '../../../core/app-paths';
import { canAccess, SUPER_ADMIN_ONLY } from '../../../core/auth/access';
import { Session } from '../../../core/auth/session';
import { withResultNotice } from '../../../core/navigation/result-notice';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Field, FieldOption } from '../../../shared/ui/field/field';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { planSummary } from '../complex-texts';
import { ComplexDetail, ComplexesApi } from '../complexes-api';
import { PublishedPlansApi } from '../published-plans-api';
import { SubdomainsApi } from '../subdomains-api';
import {
  DISPLAY_NAME_MAX_LENGTH,
  fieldErrorsOfProblem,
  OWNER_EMAIL_MAX_LENGTH,
  REGISTER_FIELD_ORDER,
  REGISTER_TEXTS,
  RegisterComplexValues,
  RegisterFieldErrors,
  validateRegisterForm,
} from './register-complex-form';
import {
  hasValidSubdomainFormat,
  SUBDOMAIN_CHECK_TEXTS,
  SubdomainCheck,
  subdomainRejectionText,
  UNAVAILABLE_CHECKS,
} from './subdomain-check';

/** Wait after the last keystroke before asking the api (suggestion and availability). */
export const TYPING_DEBOUNCE_MS = 400;

const SUBDOMAIN_PROBLEM_CHECKS: Partial<Record<ApiError['code'], SubdomainCheck>> = {
  SUBDOMAIN_TAKEN: 'taken',
  SUBDOMAIN_INVALID: 'invalid',
  SUBDOMAIN_RESERVED: 'reserved',
};

/**
 * P3 · Dar de alta complejo (CA1, CA2). Creates the tenant with its subscription in «Activa». The
 * subdomain is suggested from the name until it is edited by hand and its availability is
 * checked while typing; the api checks it again when registering.
 */
@Component({
  selector: 'app-register-complex-page',
  imports: [Alert, Button, Field, RouterLink, Skeleton],
  templateUrl: './register-complex-page.html',
  styleUrl: './register-complex-page.css',
})
export class RegisterComplexPage {
  private readonly complexesApi = inject(ComplexesApi);
  private readonly subdomainsApi = inject(SubdomainsApi);
  private readonly router = inject(Router);
  private readonly session = inject(Session);
  private readonly plansApi = inject(PublishedPlansApi);
  private readonly injector = inject(Injector);

  protected readonly rootDomainSuffix = `.${inject(ROOT_DOMAIN)}`;
  protected readonly displayNameMaxLength = DISPLAY_NAME_MAX_LENGTH;
  protected readonly ownerEmailMaxLength = OWNER_EMAIL_MAX_LENGTH;
  protected readonly plansUrl = PLANS_URL;
  protected readonly complexesUrl = HOME_URL;
  protected readonly plans = new RemoteValue(() => this.plansApi.list());

  protected readonly displayName = signal('');
  protected readonly subdomain = signal('');
  protected readonly ownerEmail = signal('');
  protected readonly planId = signal('');
  protected readonly subdomainCheck = signal<SubdomainCheck>('idle');
  protected readonly fieldErrors = signal<RegisterFieldErrors>({});
  protected readonly failure = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected readonly canManagePlans = computed(() =>
    canAccess(this.session.user(), SUPER_ADMIN_ONLY),
  );
  protected readonly publishedPlans = computed(() => {
    const state = this.plans.state();
    return state.status === 'ready' ? state.value : [];
  });
  protected readonly hasNoPublishedPlans = computed(
    () => this.plans.state().status === 'ready' && this.publishedPlans().length === 0,
  );
  protected readonly planOptions = computed<FieldOption[]>(() =>
    this.publishedPlans().map((plan) => ({ value: plan.id, label: plan.name })),
  );
  protected readonly selectedPlanSummary = computed(() => {
    const plan = this.publishedPlans().find((candidate) => candidate.id === this.planId());
    return plan ? planSummary(plan) : undefined;
  });
  /** «Comprobando…» or «Disponible», announced with role="status"; rejections go as field error. */
  protected readonly subdomainStatusText = computed(() => {
    const check = this.subdomainCheck();
    return check === 'checking' || check === 'available' || check === 'unverified'
      ? SUBDOMAIN_CHECK_TEXTS[check]
      : '';
  });
  protected readonly subdomainError = computed(
    () => this.fieldErrors().subdomain ?? subdomainRejectionText(this.subdomainCheck()),
  );

  private readonly fields = viewChildren(Field);
  private readonly nameChanges = new Subject<string>();
  private readonly subdomainChanges = new Subject<string>();
  private subdomainEditedByHand = false;

  constructor() {
    void this.plans.load();
    this.nameChanges
      .pipe(
        debounceTime(TYPING_DEBOUNCE_MS),
        filter((name) => !this.subdomainEditedByHand && name.trim() !== ''),
        switchMap((name) => this.suggestSubdomain(name)),
        takeUntilDestroyed(),
      )
      .subscribe((suggestion) => this.applySubdomain(suggestion));
    this.subdomainChanges
      .pipe(
        debounceTime(TYPING_DEBOUNCE_MS),
        switchMap((candidate) => this.checkSubdomain(candidate)),
        takeUntilDestroyed(),
      )
      .subscribe((check) => this.subdomainCheck.set(check));
  }

  protected changeDisplayName(value: string): void {
    this.displayName.set(value);
    this.clearFieldError('displayName');
    this.nameChanges.next(value);
  }

  protected changeSubdomain(value: string): void {
    this.subdomainEditedByHand = value !== '';
    this.applySubdomain(value);
  }

  protected changeOwnerEmail(value: string): void {
    this.ownerEmail.set(value);
    this.clearFieldError('ownerEmail');
  }

  protected changePlan(value: string): void {
    this.planId.set(value);
    this.clearFieldError('planId');
  }

  protected cancel(): void {
    void this.router.navigateByUrl(HOME_URL);
  }

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    if (this.submitting() || this.hasNoPublishedPlans()) {
      return;
    }
    const values = this.values();
    if (!this.showErrors(validateRegisterForm(values, this.subdomainCheck()))) {
      return;
    }
    this.submitting.set(true);
    this.failure.set(null);
    try {
      const complex = await firstValueFrom(this.complexesApi.register(values));
      await this.openRegistered(complex);
    } catch (error) {
      this.handleRegisterFailure(asApiError(error));
    } finally {
      this.submitting.set(false);
    }
  }

  private values(): RegisterComplexValues {
    return {
      displayName: this.displayName().trim(),
      subdomain: this.subdomain(),
      ownerEmail: this.ownerEmail().trim(),
      planId: this.planId(),
    };
  }

  private applySubdomain(value: string): void {
    const candidate = value.trim().toLowerCase();
    this.subdomain.set(candidate);
    this.clearFieldError('subdomain');
    this.subdomainCheck.set(candidate ? 'checking' : 'idle');
    this.subdomainChanges.next(candidate);
  }

  private suggestSubdomain(name: string): Observable<string> {
    return this.subdomainsApi.suggest(name).pipe(
      map((suggestion) => suggestion.subdomain),
      // The suggestion only saves typing: if it fails, the subdomain is written by hand.
      catchError(() => EMPTY),
    );
  }

  private checkSubdomain(candidate: string): Observable<SubdomainCheck> {
    if (!candidate) {
      return of('idle');
    }
    if (!hasValidSubdomainFormat(candidate)) {
      return of('invalid');
    }
    return this.subdomainsApi.availability(candidate).pipe(
      map((availability) =>
        availability.available || availability.reason === null
          ? 'available'
          : UNAVAILABLE_CHECKS[availability.reason],
      ),
      catchError(() => of<SubdomainCheck>('unverified')),
    );
  }

  private async openRegistered(complex: ComplexDetail): Promise<void> {
    const url = complexDetailUrl(complex.id);
    if (complex.tenantStatus === 'ACTIVE') {
      await this.router.navigateByUrl(
        url,
        withResultNotice(`Dimos de alta ${complex.displayName}. Su suscripción quedó activa.`),
      );
      return;
    }
    // Registered but its schema could not be created: P4 shows «Alta incompleta».
    await this.router.navigateByUrl(url);
  }

  private handleRegisterFailure(error: ApiError): void {
    const subdomainCheck = SUBDOMAIN_PROBLEM_CHECKS[error.code];
    if (subdomainCheck) {
      this.subdomainCheck.set(subdomainCheck);
      this.showErrors({ subdomain: subdomainRejectionText(subdomainCheck) });
      return;
    }
    if (error.code === 'PLAN_NOT_PUBLISHED') {
      this.planId.set('');
      void this.plans.load();
      this.showErrors({ planId: REGISTER_TEXTS.planNotPublished });
      return;
    }
    if (error.code === 'VALIDATION_FAILED' && error.fieldErrors.length > 0) {
      this.showErrors(fieldErrorsOfProblem(error.fieldErrors));
      return;
    }
    this.failure.set(errorMessage(error, COMMON_MESSAGES.saveFailed));
  }

  /** Shows `errors` and focuses the first field with one. True when there is none. */
  private showErrors(errors: RegisterFieldErrors): boolean {
    this.fieldErrors.set(errors);
    const firstIndex = REGISTER_FIELD_ORDER.findIndex((field) => errors[field] !== undefined);
    if (firstIndex === -1) {
      return true;
    }
    // After the next render: the fields are enabled again once a failed submission ends.
    afterNextRender(() => this.fields()[firstIndex]?.focus(), { injector: this.injector });
    return false;
  }

  private clearFieldError(field: keyof RegisterFieldErrors): void {
    if (this.fieldErrors()[field] !== undefined) {
      this.fieldErrors.update((errors) => ({ ...errors, [field]: undefined }));
    }
  }
}
