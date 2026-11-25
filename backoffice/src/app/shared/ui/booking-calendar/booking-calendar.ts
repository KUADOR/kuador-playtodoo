import {
  Component,
  computed,
  ElementRef,
  inject,
  input,
  linkedSignal,
  output,
} from '@angular/core';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideCircleAlert,
  LucideEye,
} from '@lucide/angular';
import { Button } from '../button/button';
import {
  CalendarDay,
  CalendarRange,
  CalendarStatus,
  CalendarView,
  IsoDate,
} from './booking-calendar.types';
import { addDays, DAYS_PER_WEEK, isWithin } from './calendar-dates';
import {
  buildDayView,
  buildMonthView,
  buildWeekView,
  CalendarData,
  DayView,
  MonthView,
  periodTitle,
  shiftAnchor,
  visibleRange,
  WeekView,
} from './calendar-model';
import { CalendarSlot } from './calendar-slot';

type CalendarContent =
  | { readonly view: 'day'; readonly day: DayView }
  | { readonly view: 'week'; readonly week: WeekView }
  | { readonly view: 'month'; readonly month: MonthView };

interface ViewOption {
  readonly view: CalendarView;
  readonly label: string;
  readonly previousLabel: string;
  readonly nextLabel: string;
}

const VIEW_OPTIONS: readonly ViewOption[] = [
  { view: 'day', label: 'Día', previousLabel: 'Día anterior', nextLabel: 'Día siguiente' },
  {
    view: 'week',
    label: 'Semana',
    previousLabel: 'Semana anterior',
    nextLabel: 'Semana siguiente',
  },
  { view: 'month', label: 'Mes', previousLabel: 'Mes anterior', nextLabel: 'Mes siguiente' },
];

/** Arrow keys that move the focus between the days of the month grid, in days. */
const MONTH_GRID_STEPS: Readonly<Record<string, number>> = {
  ArrowLeft: -1,
  ArrowRight: 1,
  ArrowUp: -DAYS_PER_WEEK,
  ArrowDown: DAYS_PER_WEEK,
};

const ICON_SIZE_PX = 20;
const SKELETON_LINE_COUNT = 4;

/**
 * Read-only booking calendar of the backoffice (decision D6 of PLATODO-15): Kuador looks at
 * another complex's occupancy and never selects or edits, so the contract's `mode`, `selected`
 * and `selectionChange` do not exist here. It only navigates between views and periods and emits
 * `rangeChange` so the screen loads the data. Contract: design-system/docs/components/calendar.md.
 */
@Component({
  selector: 'app-booking-calendar',
  imports: [
    Button,
    CalendarSlot,
    LucideChevronLeft,
    LucideChevronRight,
    LucideCircleAlert,
    LucideEye,
  ],
  templateUrl: './booking-calendar.html',
  styleUrl: './booking-calendar.css',
})
export class BookingCalendar {
  readonly slotDurationMinutes = input.required<number>();
  readonly today = input.required<IsoDate>();
  readonly days = input<readonly CalendarDay[]>([]);
  readonly initialView = input<CalendarView>('week');
  readonly status = input<CalendarStatus>('ready');

  readonly rangeChange = output<CalendarRange>();
  readonly retry = output<void>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly iconSize = ICON_SIZE_PX;
  protected readonly viewOptions = VIEW_OPTIONS;
  protected readonly skeletonLines = Array.from(
    { length: SKELETON_LINE_COUNT },
    (_, index) => index,
  );

  protected readonly view = linkedSignal(() => this.initialView());
  private readonly anchorDate = linkedSignal(() => this.today());
  private readonly monthFocusCandidate = linkedSignal(() => this.anchorDate());

  protected readonly isLoading = computed(() => this.status() === 'loading');
  protected readonly range = computed(() => visibleRange(this.view(), this.anchorDate()));
  protected readonly title = computed(() => periodTitle(this.view(), this.range()));
  protected readonly currentOption = computed(
    () => VIEW_OPTIONS.find((option) => option.view === this.view()) ?? VIEW_OPTIONS[0],
  );
  /** Roving tabindex of the month grid: the only day reachable with Tab. */
  protected readonly monthFocusDate = computed(() =>
    isWithin(this.monthFocusCandidate(), this.range())
      ? this.monthFocusCandidate()
      : this.range().from,
  );

  private readonly data = computed<CalendarData>(() => ({
    days: this.days(),
    slotDurationMinutes: this.slotDurationMinutes(),
    today: this.today(),
  }));

  protected readonly content = computed<CalendarContent>(() => {
    const range = this.range();
    switch (this.view()) {
      case 'day':
        return { view: 'day', day: buildDayView(range.from, this.data()) };
      case 'week':
        return { view: 'week', week: buildWeekView(range, this.data()) };
      case 'month':
        return { view: 'month', month: buildMonthView(range, this.data()) };
    }
  });

  protected changeView(view: CalendarView): void {
    this.showPeriod(view, this.anchorDate());
  }

  protected goToPreviousPeriod(): void {
    this.showPeriod(this.view(), shiftAnchor(this.view(), this.anchorDate(), -1));
  }

  protected goToNextPeriod(): void {
    this.showPeriod(this.view(), shiftAnchor(this.view(), this.anchorDate(), 1));
  }

  protected openDay(date: IsoDate): void {
    this.showPeriod('day', date);
  }

  protected moveMonthFocus(event: KeyboardEvent, date: IsoDate): void {
    const step = MONTH_GRID_STEPS[event.key];
    if (step === undefined) {
      return;
    }
    event.preventDefault();
    const target = addDays(date, step);
    if (!isWithin(target, this.range())) {
      return;
    }
    this.monthFocusCandidate.set(target);
    this.host.nativeElement.querySelector<HTMLElement>(`[data-date="${target}"]`)?.focus();
  }

  private showPeriod(view: CalendarView, anchor: IsoDate): void {
    const previousRange = this.range();
    this.view.set(view);
    this.anchorDate.set(anchor);
    const range = this.range();
    if (range.from !== previousRange.from || range.to !== previousRange.to) {
      this.rangeChange.emit(range);
    }
  }
}
