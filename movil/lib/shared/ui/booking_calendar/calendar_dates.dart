import 'calendar_types.dart';

// Pure calendar arithmetic on `YYYY-MM-DD` strings. Dates are handled in UTC
// only to avoid the device time zone; they always represent the local date of
// the complex. The clock is never read.

const int daysPerWeek = DateTime.daysPerWeek;
const int _minutesPerHour = Duration.minutesPerHour;
const int _minutesPerDay = Duration.minutesPerDay;

class DateParts {
  const DateParts({
    required this.year,
    required this.month,
    required this.dayOfMonth,
    required this.weekdayIndex,
  });

  final int year;

  /// 1 = January.
  final int month;
  final int dayOfMonth;

  /// 0 = Monday … 6 = Sunday.
  final int weekdayIndex;
}

DateTime _toUtc(IsoDate date) {
  final [year, month, day] = date.split('-').map(int.parse).toList();
  return DateTime.utc(year, month, day);
}

String _twoDigits(int value) => value.toString().padLeft(2, '0');

IsoDate _toIsoDate(DateTime date) =>
    '${date.year.toString().padLeft(4, '0')}-'
    '${_twoDigits(date.month)}-${_twoDigits(date.day)}';

DateParts dateParts(IsoDate date) {
  final utc = _toUtc(date);
  return DateParts(
    year: utc.year,
    month: utc.month,
    dayOfMonth: utc.day,
    weekdayIndex: utc.weekday - DateTime.monday,
  );
}

IsoDate addDays(IsoDate date, int days) {
  final utc = _toUtc(date);
  return _toIsoDate(DateTime.utc(utc.year, utc.month, utc.day + days));
}

/// First day of the month [months] away from the month of [date].
IsoDate addMonths(IsoDate date, int months) {
  final utc = _toUtc(date);
  return _toIsoDate(DateTime.utc(utc.year, utc.month + months));
}

IsoDate startOfWeek(IsoDate date) =>
    addDays(date, -dateParts(date).weekdayIndex);

IsoDate endOfWeek(IsoDate date) => addDays(startOfWeek(date), daysPerWeek - 1);

IsoDate startOfMonth(IsoDate date) => addMonths(date, 0);

IsoDate endOfMonth(IsoDate date) => addDays(addMonths(date, 1), -1);

/// Every date from [from] to [to], both included.
List<IsoDate> datesBetween(IsoDate from, IsoDate to) {
  return [
    for (var date = from; date.compareTo(to) <= 0; date = addDays(date, 1))
      date,
  ];
}

/// [start] plus [minutes], wrapping past midnight (`22:30` + 90 → `00:00`).
LocalTime addMinutes(LocalTime start, int minutes) {
  final [hours, mins] = start.split(':').map(int.parse).toList();
  final total = (hours * _minutesPerHour + mins + minutes) % _minutesPerDay;
  return '${_twoDigits(total ~/ _minutesPerHour)}:'
      '${_twoDigits(total % _minutesPerHour)}';
}
