import 'package:flutter/foundation.dart';

/// A court the player can book, with the schedule its owner configured.
///
/// Opening hours and slot duration belong to the «Complejos y canchas»
/// module (another card); this flow only consumes them.
@immutable
class BookableCourt {
  const BookableCourt({
    required this.id,
    required this.name,
    required this.slotDurationMinutes,
    required this.opensAt,
    required this.closesAt,
  }) : assert(slotDurationMinutes > 0, 'slotDurationMinutes must be > 0');

  final String id;
  final String name;
  final int slotDurationMinutes;

  /// Time of day, from local midnight, of the first slot start.
  final Duration opensAt;

  /// Time of day, from local midnight, by which the last slot ends.
  final Duration closesAt;

  Duration get slotDuration => Duration(minutes: slotDurationMinutes);
}

/// Local time of the complex. The API speaks UTC and the calendar speaks the
/// local date and time of the complex; this converts between both.
///
/// A fixed offset, because the complex time zone and its rules are still
/// [POR DEFINIR] in PRODUCT.md.
@immutable
class ComplexTimeZone {
  const ComplexTimeZone({required this.utcOffset});

  final Duration utcOffset;

  /// The UTC instant of [time] on [date] (`YYYY-MM-DD`), both local to the
  /// complex.
  DateTime toUtc(String date, Duration time) {
    final [year, month, day] = date.split('-').map(int.parse).toList();
    return DateTime.utc(year, month, day).add(time).subtract(utcOffset);
  }

  /// The local wall-clock of [instant], as a UTC-flagged [DateTime] so the
  /// device time zone never takes part.
  DateTime toLocal(DateTime instant) => instant.toUtc().add(utcOffset);
}
