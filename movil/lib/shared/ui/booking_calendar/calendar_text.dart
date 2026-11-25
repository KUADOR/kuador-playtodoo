import 'calendar_dates.dart';
import 'calendar_types.dart';

// Every text the calendar shows, in Spanish regardless of the device language
// (design-system/docs/components/calendar.md · Títulos).

const List<String> _weekdays = [
  'lunes',
  'martes',
  'miércoles',
  'jueves',
  'viernes',
  'sábado',
  'domingo',
];
const List<String> _months = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
];
const List<String> _monthsShort = [
  'ene',
  'feb',
  'mar',
  'abr',
  'may',
  'jun',
  'jul',
  'ago',
  'sep',
  'oct',
  'nov',
  'dic',
];

const List<String> weekdayHeaders = [
  'lun',
  'mar',
  'mié',
  'jue',
  'vie',
  'sáb',
  'dom',
];

abstract final class CalendarText {
  static const String calendar = 'Calendario de reservas';
  static const String noSlots = 'Sin horarios';
  static const String emptyDay =
      'Este día no tiene horarios. Prueba con otro día.';
  static const String loadError = 'No pudimos cargar los horarios.';
  static const String retry = 'Reintentar';
  static const String loading = 'Cargando horarios…';
  static const String readonly = 'Solo lectura';
  static const String today = 'Hoy';
  static const String time = 'Hora';
}

const Map<SlotStatus, String> slotStatusText = {
  SlotStatus.available: 'Libre',
  SlotStatus.occupied: 'Ocupado',
  SlotStatus.selected: 'Seleccionado',
};

const Map<CalendarView, String> viewNames = {
  CalendarView.day: 'Día',
  CalendarView.week: 'Semana',
  CalendarView.month: 'Mes',
};

typedef PeriodNavigationText = ({String previous, String next});

const Map<CalendarView, PeriodNavigationText> periodNavigation = {
  CalendarView.day: (previous: 'Día anterior', next: 'Día siguiente'),
  CalendarView.week: (previous: 'Semana anterior', next: 'Semana siguiente'),
  CalendarView.month: (previous: 'Mes anterior', next: 'Mes siguiente'),
};

/// `N libres`, `1 libre` or `Sin horarios`.
String formatSummary(int freeCount) {
  if (freeCount == 0) {
    return CalendarText.noSlots;
  }
  return freeCount == 1 ? '1 libre' : '$freeCount libres';
}

/// `18:00–19:00` with an en dash (U+2013).
String formatSlotLabel(LocalTime start, LocalTime end) => '$start–$end';

/// `18:00 a 19:00, Libre`.
String formatSlotAccessibleLabel(
  LocalTime start,
  LocalTime end,
  SlotStatus status,
) => '$start a $end, ${slotStatusText[status]}';

/// `viernes 4 de diciembre de 2026`.
String formatLongDate(IsoDate date) {
  final parts = dateParts(date);
  return '${_weekdays[parts.weekdayIndex]} ${parts.dayOfMonth} '
      'de ${_months[parts.month - 1]} de ${parts.year}';
}

/// `lun 30`.
String formatWeekdayHeader(IsoDate date) {
  final parts = dateParts(date);
  return '${weekdayHeaders[parts.weekdayIndex]} ${parts.dayOfMonth}';
}

/// `30 nov – 6 dic 2026`; the first month or year is repeated only when it
/// differs.
String formatWeekTitle(IsoDate from, IsoDate to) {
  final start = dateParts(from);
  final end = dateParts(to);
  final endText =
      '${end.dayOfMonth} ${_monthsShort[end.month - 1]} ${end.year}';
  if (start.year != end.year) {
    return '${start.dayOfMonth} ${_monthsShort[start.month - 1]} '
        '${start.year} – $endText';
  }
  if (start.month != end.month) {
    return '${start.dayOfMonth} ${_monthsShort[start.month - 1]} – $endText';
  }
  return '${start.dayOfMonth} – $endText';
}

/// `diciembre 2026`.
String formatMonthTitle(IsoDate date) {
  final parts = dateParts(date);
  return '${_months[parts.month - 1]} ${parts.year}';
}
