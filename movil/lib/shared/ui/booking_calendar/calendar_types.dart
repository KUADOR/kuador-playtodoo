import 'package:flutter/foundation.dart';

/// A local date of the complex as `YYYY-MM-DD`, without time zone.
typedef IsoDate = String;

/// A local time of the complex as `HH:mm`, without time zone.
typedef LocalTime = String;

enum CalendarView { day, week, month }

enum CalendarMode { editable, readonly }

enum CalendarStatus { loading, error, ready }

/// Availability of a slot as the screen receives it.
enum SlotAvailability { available, occupied }

/// State of a slot as the calendar shows it.
enum SlotStatus { available, occupied, selected }

@immutable
class CalendarSlot {
  const CalendarSlot({required this.start, required this.availability});

  final LocalTime start;
  final SlotAvailability availability;
}

@immutable
class CalendarDay {
  const CalendarDay({required this.date, required this.slots});

  final IsoDate date;
  final List<CalendarSlot> slots;
}

@immutable
class SlotSelection {
  const SlotSelection({required this.date, required this.start});

  final IsoDate date;
  final LocalTime start;

  @override
  bool operator ==(Object other) =>
      other is SlotSelection && other.date == date && other.start == start;

  @override
  int get hashCode => Object.hash(date, start);

  @override
  String toString() => 'SlotSelection($date $start)';
}

/// Inclusive range of dates shown by a view.
@immutable
class DateRange {
  const DateRange({required this.from, required this.to});

  final IsoDate from;
  final IsoDate to;

  @override
  bool operator ==(Object other) =>
      other is DateRange && other.from == from && other.to == to;

  @override
  int get hashCode => Object.hash(from, to);

  @override
  String toString() => 'DateRange($from – $to)';
}
