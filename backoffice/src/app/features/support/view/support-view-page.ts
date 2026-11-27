import { Component, OnDestroy, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom, map } from 'rxjs';
import { asApiError } from '../../../core/api/api-error';
import { COMMON_MESSAGES, errorMessage } from '../../../core/api/error-messages';
import { COMPLEX_PATHS, complexDetailUrl } from '../../../core/app-paths';
import { Session } from '../../../core/auth/session';
import { formatDateTime, todayAsCalendarDate } from '../../../core/format/date-format';
import { withResultNotice } from '../../../core/navigation/result-notice';
import { Alert } from '../../../shared/ui/alert/alert';
import { Button } from '../../../shared/ui/button/button';
import { Field, FieldOption } from '../../../shared/ui/field/field';
import { Skeleton } from '../../../shared/ui/skeleton/skeleton';
import { StatusTag } from '../../../shared/ui/status-tag/status-tag';
import { BlockSlotDialog } from '../dialogs/block-slot-dialog';
import { CancelEntryDialog } from '../dialogs/cancel-entry-dialog';
import { CreateReservationDialog } from '../dialogs/create-reservation-dialog';
import { SupportAccessDialog } from '../dialogs/support-access-dialog';
import { ActiveSupportAccess, SupportAccess, SupportComplex } from '../support-access';
import { StaffBooking, SupportBookingsApi } from '../support-bookings-api';
import { minutesLeft, sessionPhase, Ticker } from '../support-clock';
import { Court, SUPPORT_COURTS } from '../support-courts';
import { courtName, dayRange, EntryRow, entryRows, INACTIVE_LOOK } from '../support-day';
import { canOpenSupportSession } from '../support-permissions';
import {
  cancelledNotice,
  minutesLeftText,
  SESSION_ENDED_CODES,
  SUPPORT_TEXTS,
} from '../support-texts';
import { SupportBar } from './support-bar';

const TITLE_SUFFIX = ' · Backoffice de Kuador';
const SKELETON_ROWS = 4;
const ALL_COURTS = '';

type DayState =
  | { readonly status: 'loading' }
  | { readonly status: 'error'; readonly message: string }
  | { readonly status: 'ready'; readonly entries: readonly StaffBooking[] };

type CourtsState =
  { readonly status: 'loading' } | { readonly status: 'ready'; readonly courts: readonly Court[] };

type OpenDialog =
  | { readonly kind: 'create' }
  | { readonly kind: 'block' }
  | { readonly kind: 'cancel'; readonly row: EntryRow }
  | { readonly kind: 'reopen' };

/**
 * P9 · Vista de soporte (CA11 to CA14): bookings and blocks of one complex, read and changed on
 * its host with the support session opened in P8. Its data lives only in this page: it is dropped
 * when the session is closed, expires or is invalidated, and when the person leaves the page or
 * opens another complex. Nothing is cached.
 */
@Component({
  selector: 'app-support-view-page',
  imports: [
    Alert,
    BlockSlotDialog,
    Button,
    CancelEntryDialog,
    CreateReservationDialog,
    Field,
    RouterLink,
    Skeleton,
    StatusTag,
    SupportAccessDialog,
    SupportBar,
  ],
  templateUrl: './support-view-page.html',
  styleUrl: './support-view-page.css',
})
export class SupportViewPage implements OnDestroy {
  private readonly supportAccess = inject(SupportAccess);
  private readonly api = inject(SupportBookingsApi);
  private readonly courtsSource = inject(SUPPORT_COURTS);
  private readonly session = inject(Session);
  private readonly router = inject(Router);
  private readonly title = inject(Title);

  protected readonly skeletonRows = SKELETON_ROWS;
  protected readonly allCourts = ALL_COURTS;
  protected readonly texts = SUPPORT_TEXTS;
  protected readonly inactiveLook = INACTIVE_LOOK;
  private readonly ticker = new Ticker();

  private readonly complexId = toSignal(
    inject(ActivatedRoute).paramMap.pipe(
      map((params) => params.get(COMPLEX_PATHS.complexIdParam) ?? ''),
    ),
    { initialValue: '' },
  );
  protected readonly access = computed(() => this.supportAccess.activeFor(this.complexId()));
  /** The complex and status of the last session, to offer a new one once it ended. */
  protected readonly lastAccess = signal<ActiveSupportAccess | null>(null);
  protected readonly complex = computed<SupportComplex | null>(
    () => this.lastAccess()?.complex ?? null,
  );
  protected readonly detailUrl = computed(() => complexDetailUrl(this.complexId()));

  protected readonly date = signal(todayAsCalendarDate());
  protected readonly courtFilter = signal(ALL_COURTS);
  protected readonly day = signal<DayState>({ status: 'loading' });
  protected readonly courtsState = signal<CourtsState>({ status: 'loading' });
  /** Courts seen in this session that the courts source does not know, for the filter. */
  private readonly seenCourtIds = signal<readonly string[]>([]);
  protected readonly openDialog = signal<OpenDialog | null>(null);
  protected readonly resultNotice = signal<string | null>(null);
  protected readonly pageError = signal<string | null>(null);
  private lastDayRequest = 0;

  protected readonly courts = computed(() => {
    const state = this.courtsState();
    return state.status === 'ready' ? state.courts : [];
  });
  protected readonly courtsLoading = computed(() => this.courtsState().status === 'loading');
  protected readonly canEditSlots = computed(() => this.courts().length > 0);
  protected readonly courtOptions = computed<FieldOption[]>(() => {
    const known = this.courts();
    const unknown = this.seenCourtIds().filter((id) => !known.some((court) => court.id === id));
    return [
      { value: ALL_COURTS, label: 'Todas' },
      ...known.map((court) => ({ value: court.id, label: court.name })),
      ...unknown.map((id) => ({ value: id, label: courtName(id, known) })),
    ];
  });
  protected readonly rows = computed(() => {
    const state = this.day();
    return state.status === 'ready' ? entryRows(state.entries, this.courts()) : [];
  });
  private readonly remainingMs = computed(() => {
    const access = this.access();
    return access === null ? 0 : this.ticker.until(access.session.expiresAt);
  });
  protected readonly phase = computed(() => sessionPhase(this.remainingMs()));
  protected readonly timeLeft = computed(() => minutesLeftText(minutesLeft(this.remainingMs())));
  protected readonly lifecycleNotice = computed(() => {
    const access = this.access();
    switch (access?.session.tenantStatus) {
      case 'SUSPENDED':
        return SUPPORT_TEXTS.suspended;
      case 'ARCHIVED': {
        const purgeDueAt = access.complex.purgeDueAt;
        return cancelledNotice(purgeDueAt === null ? null : formatDateTime(purgeDueAt));
      }
      default:
        return null;
    }
  });
  protected readonly canReopen = computed(() => {
    const last = this.lastAccess();
    return last !== null && canOpenSupportSession(this.session.user(), last.session.tenantStatus);
  });

  constructor() {
    effect(() => {
      const access = this.access();
      untracked(() => this.follow(access));
    });
    effect(() => {
      if (this.access() !== null && this.phase() === 'expired') {
        untracked(() => this.supportAccess.end());
      }
    });
    effect(() => {
      const access = this.access();
      const date = this.date();
      const courtId = this.courtFilter();
      if (access !== null) {
        untracked(() => void this.loadDay(access, date, courtId));
      }
    });
  }

  ngOnDestroy(): void {
    this.supportAccess.end();
  }

  protected changeDate(value: string): void {
    if (value) {
      this.date.set(value);
    }
  }

  protected reloadDay(): void {
    const access = this.access();
    if (access !== null) {
      void this.loadDay(access, this.date(), this.courtFilter());
    }
  }

  protected open(dialog: OpenDialog): void {
    this.pageError.set(null);
    this.openDialog.set(dialog);
  }

  protected closeDialog(): void {
    this.openDialog.set(null);
  }

  protected cancelLabel(row: EntryRow): string {
    const action = row.entry.kind === 'BLOCK' ? 'Quitar bloqueo' : 'Cancelar reserva';
    return `${action} de las ${row.startTime}, ${row.court}`;
  }

  protected changed(notice: string): void {
    this.closeDialog();
    this.resultNotice.set(notice);
    this.reloadDay();
  }

  protected entryGone(message: string): void {
    this.closeDialog();
    this.pageError.set(message);
    this.reloadDay();
  }

  protected cancelledText(entry: StaffBooking): string {
    return entry.kind === 'BLOCK' ? SUPPORT_TEXTS.blockRemoved : SUPPORT_TEXTS.bookingCancelled;
  }

  /** The api rejected the session: everything of the complex is dropped. */
  protected sessionEnded(): void {
    this.supportAccess.end();
  }

  protected reopened(access: ActiveSupportAccess): void {
    this.closeDialog();
    this.supportAccess.start(access);
  }

  protected closeAccess(): void {
    const detailUrl = this.detailUrl();
    this.supportAccess.end();
    void this.router.navigateByUrl(detailUrl, withResultNotice(SUPPORT_TEXTS.closed));
  }

  /** A session started (load its data) or ended (drop everything that came with it). */
  private follow(access: ActiveSupportAccess | null): void {
    this.lastDayRequest++;
    this.day.set({ status: 'loading' });
    this.seenCourtIds.set([]);
    this.resultNotice.set(null);
    this.pageError.set(null);
    if (access === null) {
      this.openDialog.update((dialog) => (dialog?.kind === 'reopen' ? dialog : null));
      this.courtsState.set({ status: 'loading' });
      return;
    }
    this.lastAccess.set(access);
    this.title.setTitle(`Soporte · ${access.complex.displayName}${TITLE_SUFFIX}`);
    void this.loadCourts(access);
  }

  private async loadCourts(access: ActiveSupportAccess): Promise<void> {
    this.courtsState.set({ status: 'loading' });
    let courts: readonly Court[] = [];
    try {
      courts = await firstValueFrom(this.courtsSource.courtsOf(access.session.tenantHost));
    } catch (error) {
      // Without courts the day can still be read and cancelled; only creating is not offered.
      this.pageError.set(errorMessage(asApiError(error), COMMON_MESSAGES.loadFailed));
    }
    if (this.access() === access) {
      this.courtsState.set({ status: 'ready', courts });
    }
  }

  private async loadDay(access: ActiveSupportAccess, date: string, courtId: string): Promise<void> {
    const request = ++this.lastDayRequest;
    this.day.set({ status: 'loading' });
    try {
      const entries = await firstValueFrom(
        this.api.list(access.session, dayRange(date), courtId === ALL_COURTS ? undefined : courtId),
      );
      if (request !== this.lastDayRequest) {
        return;
      }
      this.day.set({ status: 'ready', entries });
      this.seenCourtIds.update((seen) => [
        ...new Set([...seen, ...entries.map((entry) => entry.courtId)]),
      ]);
    } catch (error) {
      if (request !== this.lastDayRequest) {
        return;
      }
      const apiError = asApiError(error);
      if (SESSION_ENDED_CODES.includes(apiError.code)) {
        this.supportAccess.end();
        return;
      }
      this.day.set({
        status: 'error',
        message: errorMessage(apiError, COMMON_MESSAGES.loadFailed),
      });
    }
  }
}
