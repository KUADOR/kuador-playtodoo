import {
  Component,
  computed,
  effect,
  inject,
  input,
  linkedSignal,
  output,
  signal,
  untracked,
} from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { asApiError } from '../../../core/api/api-error';
import { todayAsCalendarDate } from '../../../core/format/date-format';
import { BookingCalendar } from '../../../shared/ui/booking-calendar/booking-calendar';
import {
  CalendarDay,
  CalendarRange,
  CalendarStatus,
  SlotSelection,
} from '../../../shared/ui/booking-calendar/booking-calendar.types';
import { datesBetween } from '../../../shared/ui/booking-calendar/calendar-dates';
import { Field, FieldOption } from '../../../shared/ui/field/field';
import { StaffBooking, SupportBookingsApi } from '../support-bookings-api';
import { SupportSession } from '../support-access-api';
import { Court } from '../support-courts';
import { courtDay, periodRange } from '../support-day';
import { SESSION_ENDED_CODES } from '../support-texts';

/** The slot a support form acts on. */
export interface ChosenSlot {
  readonly courtId: string;
  readonly date: string;
  readonly start: string;
}

/**
 * Court and slot of «Crear reserva» and «Bloquear horario» (P9): the shared calendar in editable
 * mode shows the slots of the chosen court with the occupancy read now from the complex host.
 */
@Component({
  selector: 'app-slot-picker',
  imports: [BookingCalendar, Field],
  template: `
    <app-field
      kind="select"
      label="Cancha"
      [required]="true"
      [disabled]="disabled()"
      [options]="courtOptions()"
      [value]="courtId()"
      (valueChange)="changeCourt($event)"
    />
    <div class="slot-picker__calendar">
      <p class="slot-picker__label">Horario</p>
      <app-booking-calendar
        mode="editable"
        initialView="day"
        [attr.aria-describedby]="error() ? errorId : null"
        [slotDurationMinutes]="court().slotDurationMinutes"
        [today]="today"
        [days]="days()"
        [status]="status()"
        [selected]="selection()"
        (selectionChange)="choose($event)"
        (rangeChange)="showPeriod($event)"
        (retry)="reload()"
      />
      @if (error(); as message) {
        <p class="slot-picker__error" [id]="errorId">{{ message }}</p>
      }
    </div>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: var(--space-3);
    }

    .slot-picker__calendar {
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
    }

    .slot-picker__label {
      margin: 0;
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
    }

    .slot-picker__error {
      margin: 0;
      color: var(--color-danger);
      font-size: var(--font-size-sm);
    }
  `,
})
export class SlotPicker {
  private readonly api = inject(SupportBookingsApi);

  readonly session = input.required<SupportSession>();
  /** At least one court. */
  readonly courts = input.required<readonly Court[]>();
  readonly disabled = input(false);
  /** Validation message of the form, for example «Elige un horario libre.». */
  readonly error = input<string | null>(null);

  readonly slotChange = output<ChosenSlot | null>();
  /** The api answered that the session can no longer be used. */
  readonly sessionEnded = output<void>();

  protected readonly today = todayAsCalendarDate();
  protected readonly errorId = 'slot-picker-error';
  protected readonly courtId = linkedSignal(() => this.courts()[0]?.id ?? '');
  protected readonly court = computed(
    () => this.courts().find((court) => court.id === this.courtId()) ?? this.courts()[0],
  );
  protected readonly courtOptions = computed<FieldOption[]>(() =>
    this.courts().map((court) => ({ value: court.id, label: court.name })),
  );
  protected readonly selection = signal<SlotSelection | null>(null);
  protected readonly status = signal<CalendarStatus>('loading');
  private readonly period = signal<CalendarRange>({ from: this.today, to: this.today });
  private readonly entries = signal<readonly StaffBooking[]>([]);
  private lastRequest = 0;

  protected readonly days = computed<CalendarDay[]>(() =>
    datesBetween(this.period()).map((date) => courtDay(this.court(), date, this.entries())),
  );

  constructor() {
    effect(() => {
      this.court();
      this.period();
      untracked(() => void this.reload());
    });
  }

  /** Reads the occupancy again, for example after `409 SLOT_UNAVAILABLE`, and clears the slot. */
  async reload(): Promise<void> {
    const request = ++this.lastRequest;
    this.choose(null);
    this.status.set('loading');
    try {
      const entries = await firstValueFrom(
        this.api.list(this.session(), periodRange(this.period()), this.court().id),
      );
      if (request === this.lastRequest) {
        this.entries.set(entries);
        this.status.set('ready');
      }
    } catch (error) {
      if (request !== this.lastRequest) {
        return;
      }
      this.entries.set([]);
      this.status.set('error');
      if (SESSION_ENDED_CODES.includes(asApiError(error).code)) {
        this.sessionEnded.emit();
      }
    }
  }

  protected changeCourt(courtId: string): void {
    this.courtId.set(courtId);
  }

  protected showPeriod(range: CalendarRange): void {
    this.period.set(range);
  }

  protected choose(selection: SlotSelection | null): void {
    this.selection.set(selection);
    this.slotChange.emit(selection === null ? null : { courtId: this.court().id, ...selection });
  }
}
