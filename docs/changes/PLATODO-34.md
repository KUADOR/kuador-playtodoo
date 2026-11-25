# PLATODO-34 · Cambios para documentos compartidos

## AGENTS.md

En «Comandos del proyecto», reemplazar el `[POR DEFINIR]` de `movil` por los comandos que usa la CI (`.github/workflows/movil.yml`, Flutter 3.50.1):

```bash
# movil (movil/) · jugadores
flutter pub get
flutter analyze            # análisis estático
flutter test               # probar
```

## docs/architecture.md

En la tabla de piezas, fila `movil` (organización de capas y gestión de estado, hoy `[POR DEFINIR]`), proponer lo que aplica esta tarea:

- Organización por funcionalidad: `lib/features/<funcionalidad>/{domain,data,presentation}` y componentes compartidos en `lib/shared/ui`.
- `domain` define el puerto hacia la `api` (p. ej. `BookingsApi`); `data` tiene sus implementaciones (hoy, el mock del contrato `InMemoryBookingsApi`; el adaptador HTTP llega con la tarea de integración PLATODO-36).
- Estado con `StatefulWidget` del SDK, sin librería de gestión de estado; se revisa si el estado compartido entre pantallas crece.
- El reloj se inyecta (`DateTime Function() now`) para poder probar la cuenta regresiva.

## DESIGN.md

Cuando una persona apruebe los componentes nuevos de «Pantallas» (épica PLATODO-25), sumarlos a «Componentes base». En `movil` ya existen así:

- **Aviso** (`lib/shared/ui/app_notice.dart`): tonos advertencia y error, con icono, título corto, mensaje y acción; borde de 1,5 px del color del tono sobre blanco y `radius-lg`; en móvil, hoja inferior con botones apilados de ancho completo.
- **Cuenta regresiva de reserva pendiente** (`lib/features/booking/presentation/hold_countdown_banner.dart`): `color-primary`; en el último minuto, `color-warning` con icono de alerta y texto en negrita; sin barra con movimiento reducido. Se anuncia solo al empezar, a 1:00, a 0:30 y al terminar.
- **Estado «Reservando»**: el contrato (`GET /courts/{courtId}/occupancy`) no distingue pendientes de confirmadas, así que el jugador ve toda franja tomada como «Ocupado» (lo que Pantallas pide en lo visual). La etiqueta para lectores «Ocupado, en proceso de reserva» no se puede mostrar sin un cambio de contrato.
