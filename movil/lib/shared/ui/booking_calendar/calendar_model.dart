import 'calendar_dates.dart';
import 'calendar_text.dart';
import 'calendar_types.dart';

// Presentation rules of calendar.md turned into plain data. The shapes match
// `expected` in the shared fixtures; the widgets only paint them.

class SlotModel {
  const SlotModel({
    required this.date,
    required this.start,
    required this.end,
    required this.label,
    required this.status,
    required this.accessibleLabel,
    required this.selectable,
  });

  final IsoDate date;
  final LocalTime start;
  final LocalTime end;
  final String label;
  final SlotStatus status;
  final String accessibleLabel;
  final bool selectable;
}

/// Slot counts of a day and their summary (`3 libres`, `Sin horarios`).
class DayCounts {
  DayCounts({required this.availableCount, required this.occupiedCount})
    : summary = formatSummary(availableCount);

  final int availableCount;
  final int occupiedCount;
  final String summary;
}

class DayModel {
  const DayModel({
    required this.date,
    required this.isToday,
    required this.counts,
    required this.emptyMessage,
    required this.slots,
  });

  final IsoDate date;
  final bool isToday;
  final DayCounts counts;
  final String? emptyMessage;
  final List<SlotModel> slots;
}

class WeekColumnModel {
  const WeekColumnModel({
    required this.date,
    required this.header,
    required this.isToday,
    required this.summary,
  });

  final IsoDate date;
  final String header;
  final bool isToday;
  final String summary;
}

class WeekModel {
  const WeekModel({
    required this.columns,
    required this.rows,
    required this.cells,
  });

  final List<WeekColumnModel> columns;

  /// Every start time of the week, sorted.
  final List<LocalTime> rows;

  /// `cells[row][column]`, null when that day has no slot at that time.
  final List<List<SlotModel?>> cells;
}

class MonthDayModel {
  const MonthDayModel({
    required this.date,
    required this.dayOfMonth,
    required this.isToday,
    required this.counts,
  });

  final IsoDate date;
  final int dayOfMonth;
  final bool isToday;
  final DayCounts counts;
}

class MonthModel {
  const MonthModel({required this.weekdayHeaders, required this.weeks});

  final List<String> weekdayHeaders;

  /// Monday-to-Sunday weeks; days outside the month are null.
  final List<List<MonthDayModel?>> weeks;
}

/// The inputs indexed once per change, ready to build any view.
class CalendarSource {
  CalendarSource._(
    this._slotsByDate, {
    required this.slotDurationMinutes,
    required this.mode,
    required this.today,
    required this.selection,
  });

  factory CalendarSource({
    required int slotDurationMinutes,
    required List<CalendarDay> days,
    required CalendarMode mode,
    required SlotSelection? selected,
    required IsoDate today,
  }) {
    final slotsByDate = {
      for (final day in days)
        day.date: [...day.slots]..sort((a, b) => a.start.compareTo(b.start)),
    };
    return CalendarSource._(
      slotsByDate,
      slotDurationMinutes: slotDurationMinutes,
      mode: mode,
      today: today,
      selection: _resolveSelection(selected, mode, slotsByDate),
    );
  }

  final int slotDurationMinutes;
  final CalendarMode mode;
  final IsoDate today;

  /// The selection that is shown: null when it points to an occupied or
  /// missing slot, or in readonly.
  final SlotSelection? selection;
  final Map<IsoDate, List<CalendarSlot>> _slotsByDate;

  List<CalendarSlot> slotsOf(IsoDate date) => _slotsByDate[date] ?? const [];

  static SlotSelection? _resolveSelection(
    SlotSelection? selected,
    CalendarMode mode,
    Map<IsoDate, List<CalendarSlot>> slotsByDate,
  ) {
    if (selected == null || mode == CalendarMode.readonly) {
      return null;
    }
    final isAvailable = (slotsByDate[selected.date] ?? const []).any(
      (slot) =>
          slot.start == selected.start &&
          slot.availability == SlotAvailability.available,
    );
    return isAvailable ? selected : null;
  }
}

DateRange rangeFor(CalendarView view, IsoDate anchor) => switch (view) {
  CalendarView.day => DateRange(from: anchor, to: anchor),
  CalendarView.week => DateRange(
    from: startOfWeek(anchor),
    to: endOfWeek(anchor),
  ),
  CalendarView.month => DateRange(
    from: startOfMonth(anchor),
    to: endOfMonth(anchor),
  ),
};

String titleFor(CalendarView view, IsoDate anchor) => switch (view) {
  CalendarView.day => formatLongDate(anchor),
  CalendarView.week => formatWeekTitle(startOfWeek(anchor), endOfWeek(anchor)),
  CalendarView.month => formatMonthTitle(anchor),
};

/// The anchor date of the previous (`-1`) or next (`1`) period.
IsoDate shiftAnchor(CalendarView view, IsoDate anchor, int step) =>
    switch (view) {
      CalendarView.day => addDays(anchor, step),
      CalendarView.week => addDays(anchor, step * daysPerWeek),
      CalendarView.month => addMonths(anchor, step),
    };

SlotModel _buildSlot(CalendarSource source, IsoDate date, CalendarSlot slot) {
  final end = addMinutes(slot.start, source.slotDurationMinutes);
  final isSelected =
      source.selection == SlotSelection(date: date, start: slot.start);
  final status = isSelected
      ? SlotStatus.selected
      : switch (slot.availability) {
          SlotAvailability.available => SlotStatus.available,
          SlotAvailability.occupied => SlotStatus.occupied,
        };
  return SlotModel(
    date: date,
    start: slot.start,
    end: end,
    label: formatSlotLabel(slot.start, end),
    status: status,
    accessibleLabel: formatSlotAccessibleLabel(slot.start, end, status),
    selectable:
        source.mode == CalendarMode.editable && status != SlotStatus.occupied,
  );
}

DayCounts _countSlots(CalendarSource source, IsoDate date) {
  final slots = source.slotsOf(date);
  final occupiedCount = slots
      .where((slot) => slot.availability == SlotAvailability.occupied)
      .length;
  return DayCounts(
    availableCount: slots.length - occupiedCount,
    occupiedCount: occupiedCount,
  );
}

DayModel buildDay(CalendarSource source, IsoDate date) {
  final slots = [
    for (final slot in source.slotsOf(date)) _buildSlot(source, date, slot),
  ];
  return DayModel(
    date: date,
    isToday: date == source.today,
    counts: _countSlots(source, date),
    emptyMessage: slots.isEmpty ? CalendarText.emptyDay : null,
    slots: slots,
  );
}

WeekModel buildWeek(CalendarSource source, IsoDate anchor) {
  final dates = datesBetween(startOfWeek(anchor), endOfWeek(anchor));
  final columns = [
    for (final date in dates)
      WeekColumnModel(
        date: date,
        header: formatWeekdayHeader(date),
        isToday: date == source.today,
        summary: _countSlots(source, date).summary,
      ),
  ];
  final slotsByStart = [
    for (final date in dates)
      {
        for (final slot in source.slotsOf(date))
          slot.start: _buildSlot(source, date, slot),
      },
  ];
  final rows = {for (final slots in slotsByStart) ...slots.keys}.toList()
    ..sort();
  final cells = [
    for (final start in rows) [for (final slots in slotsByStart) slots[start]],
  ];
  return WeekModel(columns: columns, rows: rows, cells: cells);
}

MonthModel buildMonth(CalendarSource source, IsoDate anchor) {
  final first = startOfMonth(anchor);
  final last = endOfMonth(anchor);
  bool isInMonth(IsoDate date) =>
      date.compareTo(first) >= 0 && date.compareTo(last) <= 0;

  final days = [
    for (final date in datesBetween(startOfWeek(first), endOfWeek(last)))
      isInMonth(date)
          ? MonthDayModel(
              date: date,
              dayOfMonth: dateParts(date).dayOfMonth,
              isToday: date == source.today,
              counts: _countSlots(source, date),
            )
          : null,
  ];
  final weeks = [
    for (var start = 0; start < days.length; start += daysPerWeek)
      days.sublist(start, start + daysPerWeek),
  ];
  return MonthModel(weekdayHeaders: weekdayHeaders, weeks: weeks);
}
