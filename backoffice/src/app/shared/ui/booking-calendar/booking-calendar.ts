import {
  Component,
  computed,
  ElementRef,
  inject,
  input,
  linkedSignal,
  output,
  signal,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideCircleAlert,
  LucideEye,
} from '@lucide/angular';
import { Button } from '../button/button';
import {
  CalendarDay,
  CalendarMode,
  CalendarRange,
  CalendarStatus,
  CalendarView,
  IsoDate,
  SlotSelection,
} from './booking-calendar.types';
import { addDays, DAYS_PER_WEEK, isWithin } from './calendar-dates';
import {
  buildDayView,
  buildMonthView,
  buildWeekView,
  CalendarData,
  DayView,
  effectiveSelection,
  MonthView,
  periodTitle,
  shiftAnchor,
  SlotView,
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

/** Keys that move the focus between the slots of an editable calendar, in reading order. */
const SLOT_STEPS: Readonly<Record<string, number>> = {
  ArrowLeft: -1,
  ArrowUp: -1,
  ArrowRight: 1,
  ArrowDown: 1,
};
const FIRST_SLOT_KEY = 'Home';
const LAST_SLOT_KEY = 'End';
const SLOT_BUTTON_SELECTOR = '[data-slot-key]';

const ICON_SIZE_PX = 20;
const SKELETON_LINE_COUNT = 4;

/**
 * Booking calendar of the backoffice. `readonly` (the default, decision D6 of PLATODO-15) only
 * shows the occupancy; `editable` lets Kuador support choose one free slot of a complex
 * (ADR-020) and emits `selectionChange`. It navigates between views and periods and emits
 * `rangeChange` so the screen loads the data. Contract: design-system/docs/components/calendar.md.
 */
@Component({
  selector: 'app-booking-calendar',
  imports: [
    Button,
    CalendarSlot,
    NgTemplateOutlet,
    LucideChevronLeft,
    LucideChevronRight,
    LucideCircleAlert,
    LucideEye,
  ],
  templateUrl: './booking-calendar.html',
  styleUrls: ['./booking-calendar.css', './calendar-slot-button.css'],
})
export class BookingCalendar {
  readonly slotDurationMinutes = input.required<number>();
  readonly today = input.required<IsoDate>();
  readonly days = input<readonly CalendarDay[]>([]);
  readonly initialView = input<CalendarView>('week');
  readonly status = input<CalendarStatus>('ready');
  readonly mode = input<CalendarMode>('readonly');
  /** The chosen slot in `editable` mode; the screen owns it and updates it on `selectionChange`. */
  readonly selected = input<SlotSelection | null>(null);

  readonly rangeChange = output<CalendarRange>();
  readonly retry = output<void>();
  readonly selectionChange = output<SlotSelection | null>();

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
  protected readonly isEditable = computed(() => this.mode() === 'editable');
  /** Slot the person last moved to with the keyboard (roving tabindex of an editable calendar). */
  private readonly focusedSlotKey = signal<string | null>(null);
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
    mode: this.mode(),
    selected: this.selected(),
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

  /**
   * The only slot reachable with Tab: the one moved to with the keyboard, else the selected one,
   * else the first selectable one of the period.
   */
  protected readonly tabbableSlotKey = computed(() => {
    const keys = this.slotKeysInView();
    const focused = this.focusedSlotKey();
    if (focused !== null && keys.all.includes(focused)) {
      return focused;
    }
    const selection = effectiveSelection(this.data());
    const selectedKey = selection === null ? null : slotKey(selection.date, selection.start);
    if (selectedKey !== null && keys.all.includes(selectedKey)) {
      return selectedKey;
    }
    return keys.selectable[0] ?? keys.all[0] ?? null;
  });

  private readonly slotKeysInView = computed(() => {
    const shown = this.content();
    const slots: { readonly key: string; readonly selectable: boolean }[] = [];
    if (shown.view === 'day') {
      for (const slot of shown.day.slots) {
        slots.push({ key: slotKey(shown.day.date, slot.start), selectable: slot.selectable });
      }
    } else if (shown.view === 'week') {
      shown.week.cells.forEach((row) =>
        row.forEach((cell, column) => {
          if (cell !== null) {
            const date = shown.week.columns[column].date;
            slots.push({ key: slotKey(date, cell.start), selectable: cell.selectable });
          }
        }),
      );
    }
    return {
      all: slots.map((slot) => slot.key),
      selectable: slots.filter((slot) => slot.selectable).map((slot) => slot.key),
    };
  });

  protected slotKeyOf(date: IsoDate, slot: SlotView): string {
    return slotKey(date, slot.start);
  }

  /** Activating a free slot selects it; activating the selected one clears the selection. */
  protected activateSlot(date: IsoDate, slot: SlotView): void {
    if (!slot.selectable) {
      return;
    }
    this.focusedSlotKey.set(slotKey(date, slot.start));
    this.selectionChange.emit(slot.status === 'selected' ? null : { date, start: slot.start });
  }

  protected moveSlotFocus(event: KeyboardEvent): void {
    const buttons = Array.from(
      this.host.nativeElement.querySelectorAll<HTMLElement>(SLOT_BUTTON_SELECTOR),
    );
    const current = buttons.indexOf(event.target as HTMLElement);
    const target = this.slotFocusTarget(event.key, current, buttons.length);
    if (target === null) {
      return;
    }
    event.preventDefault();
    const button = buttons[target];
    this.focusedSlotKey.set(button.dataset['slotKey'] ?? null);
    button.focus();
  }

  private slotFocusTarget(key: string, current: number, count: number): number | null {
    if (key === FIRST_SLOT_KEY) {
      return 0;
    }
    if (key === LAST_SLOT_KEY) {
      return count - 1;
    }
    const step = SLOT_STEPS[key];
    if (step === undefined || current < 0) {
      return null;
    }
    return Math.min(count - 1, Math.max(0, current + step));
  }

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

function slotKey(date: IsoDate, start: string): string {
  return `${date}T${start}`;
}
