import '../../../shared/ui/booking_calendar/calendar_text.dart';
import '../domain/hold_countdown.dart';

// Every text of the player booking flow, in Spanish with tuteo
// (docs/screens/platodo-25-reservas-sin-conflictos-ni-duplicados.md,
// pantallas 1–5).

abstract final class BookingText {
  // Pantalla 1 · calendario.
  static const String reserve = 'Reservar cancha';
  static const String reserving = 'Reservando…';
  static const String chooseFreeSlot = 'Elige una franja libre';
  static const String reserveFailed =
      'No pudimos reservar. Revisa tu conexión e inténtalo de nuevo.';
  static const String reserveRejected =
      'No pudimos reservar este horario. Elige otro.';

  // Pantalla 2 · conflicto.
  static const String slotUnavailable =
      'Ese horario ya no está disponible. Elige otro.';
  static const String slotUnavailableTitle = 'Horario no disponible';
  static const String chooseAnotherSlot = 'Elegir otro horario';

  // Pantalla 3 · resumen con cuenta regresiva.
  static const String pendingTitle = 'Confirma tu reserva';
  static const String holdHelp =
      'Te guardamos este horario mientras confirmas. '
      'Si el tiempo termina, se libera.';
  static const String confirm = 'Confirmar reserva';
  static const String confirming = 'Confirmando…';
  static const String cancel = 'Cancelar';
  static const String cancelling = 'Cancelando…';
  static const String confirmFailed =
      'No pudimos confirmar. Revisa tu conexión e inténtalo de nuevo.';
  static const String cancelFailed =
      'No pudimos cancelar. Revisa tu conexión e inténtalo de nuevo.';
  static const String retry = CalendarText.retry;
  static const String confirmedTitle = 'Reserva confirmada';
  static const String backToCalendar = 'Volver al calendario';

  // Pantalla 4 · tiempo terminado.
  static const String expiredTitle = 'Tu tiempo terminó';
  static const String expiredBody =
      'No confirmaste a tiempo y liberamos el horario. '
      'Puedes elegirlo de nuevo si sigue disponible.';
  static const String chooseSlot = 'Elegir un horario';

  // Pantalla 5 · varias reservas.
  static const String ownOverlapTitle = 'Ya tienes una reserva';
  static const String ownOverlapBody =
      'Ya tienes una reserva en este horario. '
      '¿Quieres reservar otra cancha para tus compañeros?';
  static const String continueBooking = 'Continuar con la reserva';
  static const String goBack = 'Volver';

  static const String courtLabel = 'Cancha';
  static const String dateLabel = 'Día';
  static const String timeLabel = 'Hora';
  static const String durationLabel = 'Duración';
}

/// `Tienes 4:32 para confirmar`.
String formatHoldRemaining(Duration remaining) =>
    'Tienes ${formatRemaining(remaining)} para confirmar';

/// `60 minutos`.
String formatDuration(Duration duration) => '${duration.inMinutes} minutos';

/// What screen readers hear at each [HoldMilestone].
String holdMilestoneAnnouncement(HoldMilestone milestone) =>
    switch (milestone) {
      HoldMilestone.oneMinuteLeft => 'Te queda 1 minuto para confirmar',
      HoldMilestone.thirtySecondsLeft => 'Te quedan 30 segundos para confirmar',
      HoldMilestone.expired => BookingText.expiredTitle,
    };
