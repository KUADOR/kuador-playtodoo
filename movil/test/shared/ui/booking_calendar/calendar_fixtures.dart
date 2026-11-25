import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:playtodoo/shared/ui/booking_calendar/booking_calendar.dart';

/// Shared calendar fixtures (design-system/fixtures/calendar), copied here by
/// `npm run build` in design-system.
class CalendarFixture {
  CalendarFixture._(this.name, this.input, this.expected);

  factory CalendarFixture.load(String name) {
    final file = File('test/shared/ui/booking_calendar/fixtures/$name.json');
    final json = jsonDecode(file.readAsStringSync()) as Map<String, dynamic>;
    return CalendarFixture._(
      name,
      json['input'] as Map<String, dynamic>,
      json['expected'] as Map<String, dynamic>,
    );
  }

  static const List<String> names = [
    'day-30min',
    'day-60min',
    'day-90min',
    'day-empty',
    'day-all-occupied',
    'week-mixed',
    'week-readonly',
    'month-mixed',
  ];

  final String name;
  final Map<String, dynamic> input;
  final Map<String, dynamic> expected;

  int get slotDurationMinutes => input['slotDurationMinutes'] as int;
  CalendarView get view => CalendarView.values.byName(input['view'] as String);
  CalendarMode get mode => CalendarMode.values.byName(input['mode'] as String);
  IsoDate get today => input['today'] as String;
  SlotSelection? get selected => parseSelection(input['selected']);
  String get expectedTitle => expected['title'] as String;
  SlotSelection? get expectedSelected => parseSelection(expected['selected']);

  List<CalendarDay> get days => [
    for (final day in input['days'] as List<dynamic>)
      CalendarDay(
        date: day['date'] as String,
        slots: [
          for (final slot in day['slots'] as List<dynamic>)
            CalendarSlot(
              start: slot['start'] as String,
              availability: SlotAvailability.values.byName(
                slot['status'] as String,
              ),
            ),
        ],
      ),
  ];

  DateRange get expectedRange {
    final range = expected['range'] as Map<String, dynamic>;
    return DateRange(from: range['from'] as String, to: range['to'] as String);
  }

  /// `expected.day.slots`, the rendered slots of a day fixture.
  List<Map<String, dynamic>> get expectedDaySlots => [
    for (final slot
        in (expected['day'] as Map<String, dynamic>)['slots'] as List<dynamic>)
      slot as Map<String, dynamic>,
  ];

  static SlotSelection? parseSelection(Object? json) {
    if (json == null) {
      return null;
    }
    final map = json as Map<String, dynamic>;
    return SlotSelection(
      date: map['date'] as String,
      start: map['start'] as String,
    );
  }
}

/// Mounts [BookingCalendar] with the fixture input.
BookingCalendar calendarFrom(
  CalendarFixture fixture, {
  CalendarStatus status = CalendarStatus.ready,
  ValueChanged<SlotSelection?>? onSelectionChange,
  ValueChanged<DateRange>? onRangeChange,
}) {
  return BookingCalendar(
    slotDurationMinutes: fixture.slotDurationMinutes,
    days: fixture.days,
    today: fixture.today,
    initialView: fixture.view,
    mode: fixture.mode,
    selected: fixture.selected,
    status: status,
    onSelectionChange: onSelectionChange,
    onRangeChange: onRangeChange,
  );
}
