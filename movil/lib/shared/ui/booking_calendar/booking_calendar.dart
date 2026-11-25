import 'package:material_ui/material_ui.dart';

import '../../../core/design/tokens.g.dart';
import 'calendar_day_view.dart';
import 'calendar_feedback.dart';
import 'calendar_model.dart';
import 'calendar_month_view.dart';
import 'calendar_text.dart';
import 'calendar_toolbar.dart';
import 'calendar_types.dart';
import 'calendar_week_view.dart';

export 'calendar_types.dart';

/// Presentational booking calendar (design-system/docs/components/calendar.md):
/// it shows the slots it receives and reports the selection. Availability and
/// booking rules belong to the Reservas module.
///
/// The selection is controlled by the screen: pass the last value received in
/// [onSelectionChange] back as [selected].
class BookingCalendar extends StatefulWidget {
  const BookingCalendar({
    super.key,
    required this.slotDurationMinutes,
    required this.days,
    required this.today,
    this.initialView = CalendarView.day,
    this.mode = CalendarMode.editable,
    this.selected,
    this.status = CalendarStatus.ready,
    this.onSelectionChange,
    this.onRangeChange,
    this.onRetry,
  }) : assert(slotDurationMinutes > 0, 'slotDurationMinutes must be > 0');

  /// Slot length of the complex; defines the end time of every slot.
  final int slotDurationMinutes;

  /// Days in the local time of the complex. A missing day, or one without
  /// slots, has no schedule.
  final List<CalendarDay> days;

  /// Injected current date of the complex; the calendar never reads the
  /// clock. It opens on the period containing it.
  final IsoDate today;

  /// View shown first; afterwards the person changes it.
  final CalendarView initialView;
  final CalendarMode mode;

  /// One slot at a time. Ignored when it points to an occupied or missing
  /// slot, and always in readonly.
  final SlotSelection? selected;
  final CalendarStatus status;

  /// A free slot was activated, or the selected one was activated again to
  /// clear it (null). Never called in readonly.
  final ValueChanged<SlotSelection?>? onSelectionChange;

  /// The view or the period changed: the screen should load the days of the
  /// new range.
  final ValueChanged<DateRange>? onRangeChange;

  /// «Reintentar» was pressed in the error state.
  final VoidCallback? onRetry;

  @override
  State<BookingCalendar> createState() => _BookingCalendarState();
}

class _BookingCalendarState extends State<BookingCalendar> {
  late CalendarView _view = widget.initialView;
  late IsoDate _anchor = widget.today;
  late CalendarSource _source = _createSource();

  CalendarSource _createSource() => CalendarSource(
    slotDurationMinutes: widget.slotDurationMinutes,
    days: widget.days,
    mode: widget.mode,
    selected: widget.selected,
    today: widget.today,
  );

  @override
  void didUpdateWidget(BookingCalendar oldWidget) {
    super.didUpdateWidget(oldWidget);
    final sourceChanged =
        widget.slotDurationMinutes != oldWidget.slotDurationMinutes ||
        widget.days != oldWidget.days ||
        widget.mode != oldWidget.mode ||
        widget.selected != oldWidget.selected ||
        widget.today != oldWidget.today;
    if (sourceChanged) {
      _source = _createSource();
    }
  }

  void _navigate(CalendarView view, IsoDate anchor) {
    setState(() {
      _view = view;
      _anchor = anchor;
    });
    widget.onRangeChange?.call(rangeFor(view, anchor));
  }

  void _activateSlot(SlotModel slot) {
    if (!slot.selectable) {
      return;
    }
    widget.onSelectionChange?.call(
      slot.status == SlotStatus.selected
          ? null
          : SlotSelection(date: slot.date, start: slot.start),
    );
  }

  Widget _buildPeriod() => switch (_view) {
    CalendarView.day => CalendarDayView(
      day: buildDay(_source, _anchor),
      onActivateSlot: _activateSlot,
    ),
    CalendarView.week => CalendarWeekView(
      week: buildWeek(_source, _anchor),
      onActivateSlot: _activateSlot,
    ),
    CalendarView.month => CalendarMonthView(
      month: buildMonth(_source, _anchor),
      onOpenDay: (date) => _navigate(CalendarView.day, date),
    ),
  };

  @override
  Widget build(BuildContext context) {
    final isLoading = widget.status == CalendarStatus.loading;
    return Semantics(
      container: true,
      explicitChildNodes: true,
      label: CalendarText.calendar,
      child: FocusTraversalGroup(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            CalendarToolbar(
              title: titleFor(_view, _anchor),
              view: _view,
              isEnabled: !isLoading,
              onShiftPeriod: (step) =>
                  _navigate(_view, shiftAnchor(_view, _anchor, step)),
              onViewChanged: (view) => _navigate(view, _anchor),
            ),
            if (widget.mode == CalendarMode.readonly) ...[
              const SizedBox(height: AppTokens.space3),
              const CalendarReadonlyNotice(),
            ],
            const SizedBox(height: AppTokens.space4),
            switch (widget.status) {
              CalendarStatus.loading => const CalendarSkeleton(),
              CalendarStatus.error => CalendarError(onRetry: widget.onRetry),
              CalendarStatus.ready => _buildPeriod(),
            },
          ],
        ),
      ),
    );
  }
}
