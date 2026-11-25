/// Texts of the complex state screens (Pantallas PLATODO-26 · pantallas 1 y
/// 2), the same as in `app`. They never reveal the name of the complex, the
/// reason (suspension or archive) nor any deadline.
abstract final class TenantText {
  static const String brand = 'Playtodoo';
  static const String goToPlatform = 'Ir a Playtodoo';
  static const String checking = 'Comprobando el complejo';
  static const String checkFailed = 'No pudimos comprobar el complejo.';
  static const String retry = 'Reintentar';
  static const String unavailableTitle =
      'Este complejo no está disponible por ahora';
  static const String unavailableText =
      'Vuelve a intentarlo más tarde o contacta al complejo.';
  static const String notFoundTitle = 'No encontramos este complejo';
  static const String notFoundText =
      'Revisa la dirección o busca otro complejo en Playtodoo.';
}
