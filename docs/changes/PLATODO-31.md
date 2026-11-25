# Cambios a documentos compartidos · PLATODO-31

## docs/architecture.md

En «Arquitectura del backend», ampliar la estructura real del módulo Reservas:

```
com.kuador.platodo.api.bookings/
  domain/                        Booking (+ estado efectivo: asSeenAt, isOverdueAt), TimePeriod, ...
  domain/event/                  BookingEvent, BookingCreated, BookingConfirmed, BookingCancelled
  application/                   CreatePendingBooking, ConfirmBooking, CancelOwnPendingBooking,
                                 GetBooking, GetCourtOccupancy y sus excepciones
  application/port/out/          BookingRepository, CourtSlotRules, CurrentPlayer, BookingEventPublisher
  infrastructure/persistence/    PostgresBookingRepository
  infrastructure/web/            PlayerBookingController, CourtOccupancyController, Problem Details
  infrastructure/events/         SpringBookingEventPublisher (ApplicationEventPublisher)
  infrastructure/failclosed/     UnconfiguredCourtSlotRules, UnauthenticatedCurrentPlayer
  infrastructure/config/         PlayerBookingConfiguration
```

- Los endpoints de jugador se registran solo con `playtodoo.bookings.player-api.enabled=true` (por defecto `false`). Se activan cuando existan Tenancy por solicitud, Identidad (JWT del jugador) y Canchas (`CourtSlotRules`). Mientras falten, los adaptadores por defecto fallan cerrado: `CurrentPlayer` responde 401 y `CourtSlotRules` informa «cancha no encontrada».
- `PlayerBookingConfiguration` necesita un bean `DataSource` cuyas conexiones usen el `search_path` del tenant resuelto (lo aporta Tenancy).

En «Catálogo de eventos», ajustar la carga de los eventos de jugador a lo implementado (D4 del diseño de PLATODO-31):

| Evento | Carga |
|---|---|
| `BookingCreated` | `bookingId`, `courtId`, `start`, `end`, `expiresAt` |
| `BookingConfirmed` | `bookingId`, `courtId`, `start`, `end` |
| `BookingCancelled` | `bookingId`, `courtId`, `start`, `end` |

Todavía no llevan `tenantId` ni `kind`. Se publican dentro de la transacción; los consumidores usan `@TransactionalEventListener(AFTER_COMMIT)` en el mismo hilo hasta que el evento lleve el `tenantId`. `PlayerBookingConfiguration` registra la `TransactionalEventListenerFactory` porque la app no tiene autoconfiguración de transacciones.

## docs/adr/ADR-006-prevencion-de-reservas-duplicadas.md

Agregar en «Consecuencias»:

- Con inserciones concurrentes que chocan en la restricción de exclusión, PostgreSQL puede abortar una con `deadlock detected` (40P01) en vez de `23P01`. Para dar siempre una respuesta limpia (`409 SLOT_UNAVAILABLE`), `CreatePendingBooking` toma en su transacción un candado de transacción por esquema y cancha (`pg_advisory_xact_lock`, `BookingRepository.serializeWritesOfCourt`) antes del vencimiento perezoso y el `INSERT`. El candado no es la garantía: la restricción sigue siéndolo (criterio 14). Las escrituras del personal (PLATODO-33) deben tomar el mismo candado.

## api/README.md

Agregar una sección «Player booking API»:

- Endpoints de jugador de `contracts/bookings.openapi.yaml` bajo `/api/v1`, apagados por defecto (`playtodoo.bookings.player-api.enabled=false`).
- No activarlos en un entorno real hasta integrar Tenancy por solicitud, Identidad y Canchas.
- Pruebas: `PlayerBookingFlowIT` (casos de uso contra PostgreSQL, concurrencia y aislamiento), `PlayerBookingApiIT` (HTTP y Problem Details), `PlayerBookingApiFailClosedIT` (adaptadores por defecto).
