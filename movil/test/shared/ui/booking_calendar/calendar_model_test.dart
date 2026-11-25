import 'package:flutter_test/flutter_test.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_dates.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_model.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_text.dart';
import 'package:playtodoo/shared/ui/booking_calendar/calendar_types.dart';

import 'calendar_fixtures.dart';

// The presentation rules must produce exactly `expected` of every shared
// fixture, like the app and backoffice implementations.

Map<String, Object?>? _selectionJson(SlotSelection? selection) =>
    selection == null
    ? null
    : {'date': selection.date, 'start': selection.start};

Map<String, Object?> _slotJson(SlotModel slot) => {
  'start': slot.start,
  'end': slot.end,
  'label': slot.label,
  'status': slot.status.name,
  'accessibleLabel': slot.accessibleLabel,
  'selectable': slot.selectable,
};

Map<String, Object?> _dayJson(DayModel day) => {
  'date': day.date,
  'isToday': day.isToday,
  'availableCount': day.counts.availableCount,
  'occupiedCount': day.counts.occupiedCount,
  'summary': day.counts.summary,
  'emptyMessage': day.emptyMessage,
  'slots': [for (final slot in day.slots) _slotJson(slot)],
};

Map<String, Object?> _weekJson(WeekModel week) => {
  'columns': [
    for (final column in week.columns)
      {
        'date': column.date,
        'header': column.header,
        'isToday': column.isToday,
        'summary': column.summary,
      },
  ],
  'rows': week.rows,
  'cells': [
    for (final row in week.cells) [for (final cell in row) cell?.status.name],
  ],
};

Map<String, Object?> _monthJson(MonthModel month) => {
  'weekdayHeaders': month.weekdayHeaders,
  'weeks': [
    for (final week in month.weeks)
      [
        for (final day in week)
          day == null
              ? null
              : {
                  'date': day.date,
                  'dayOfMonth': day.dayOfMonth,
                  'isToday': day.isToday,
                  'availableCount': day.counts.availableCount,
                  'occupiedCount': day.counts.occupiedCount,
                  'summary': day.counts.summary,
                },
      ],
  ],
};

Map<String, Object?> _render(CalendarFixture fixture) {
  final source = CalendarSource(
    slotDurationMinutes: fixture.slotDurationMinutes,
    days: fixture.days,
    mode: fixture.mode,
    selected: fixture.selected,
    today: fixture.today,
  );
  final view = fixture.view;
  final anchor = fixture.today;
  final range = rangeFor(view, anchor);
  return {
    'range': {'from': range.from, 'to': range.to},
    'selected': _selectionJson(source.selection),
    'title': titleFor(view, anchor),
    switch (view) {
      CalendarView.day => 'day',
      CalendarView.week => 'week',
      CalendarView.month => 'month',
    }: switch (view) {
      CalendarView.day => _dayJson(buildDay(source, anchor)),
      CalendarView.week => _weekJson(buildWeek(source, anchor)),
      CalendarView.month => _monthJson(buildMonth(source, anchor)),
    },
  };
}

void main() {
  group('shared fixtures', () {
    for (final name in CalendarFixture.names) {
      test(name, () {
        final fixture = CalendarFixture.load(name);
        expect(_render(fixture), fixture.expected);
      });
    }
  });

  group('dates', () {
    test('slot end wraps past midnight', () {
      expect(addMinutes('22:30', 90), '00:00');
      expect(addMinutes('23:30', 60), '00:30');
    });

    test('weeks start on Monday across a year change', () {
      expect(startOfWeek('2027-01-01'), '2026-12-28');
      expect(endOfWeek('2026-12-28'), '2027-01-03');
      expect(
        formatWeekTitle('2026-12-28', '2027-01-03'),
        '28 dic 2026 – 3 ene 2027',
      );
    });

    test('month shift lands on the first day of the month', () {
      expect(shiftAnchor(CalendarView.month, '2026-01-31', 1), '2026-02-01');
      expect(endOfMonth('2028-02-10'), '2028-02-29');
    });

    test('week title inside one month repeats the month once', () {
      expect(formatWeekTitle('2026-12-07', '2026-12-13'), '7 – 13 dic 2026');
    });
  });

  test('a missing day has no slots and the empty message', () {
    final source = CalendarSource(
      slotDurationMinutes: 60,
      days: const [],
      mode: CalendarMode.editable,
      selected: null,
      today: '2026-12-04',
    );
    final day = buildDay(source, '2026-12-04');
    expect(day.slots, isEmpty);
    expect(day.emptyMessage, CalendarText.emptyDay);
    expect(day.counts.summary, CalendarText.noSlots);
  });
}
