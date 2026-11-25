# Cambios a documentos compartidos · PLATODO-33

## docs/architecture.md

En «Arquitectura del backend», ampliar la estructura real del módulo Reservas:

```
com.kuador.platodo.api.bookings/
  domain/event/                  SlotBlocked (nuevo); BookingCreated también para la reserva manual (expiresAt nulo)
  application/                   CreateStaffBooking, BlockSlot, CancelBookingByStaff, GetStaffBooking,
                                 ListStaffBookings; CourtSlots (franja válida y duración de la cancha) y
                                 CalendarRange (rango máximo de 31 días), compartidos con el flujo del jugador;
                                 SlotHasBookingException, StaffNotAuthenticatedException, NotStaffOfTenantException
  application/port/out/          CurrentStaff (nuevo); BookingRepository suma cancelIfActive,
                                 findActiveReservationOverlapping, findActiveOverlapping, findActiveOfCourtOverlapping
  infrastructure/web/            StaffBookingController; BookingProblemHandler (errores comunes),
                                 PlayerBookingProblemHandler y StaffBookingProblemHandler (propios de cada lado)
  infrastructure/failclosed/     UnauthenticatedCurrentStaff
  infrastructure/config/         BookingsConfiguration (repositorio, publicador de eventos y fábrica de listeners
                                 transaccionales, compartidos), PlayerBookingConfiguration, StaffBookingConfiguration
```

- Los endpoints del personal se registran solo con `playtodoo.bookings.staff-api.enabled=true` (por defecto `false`), igual que los del jugador. Sin el adaptador de Identidad, `CurrentStaff` falla cerrado (401). El adaptador real debe exigir un rol de personal **del tenant resuelto** y responder 403 (`NotStaffOfTenantException`) si no lo tiene.
- Las escrituras del personal (reserva manual y bloqueo) toman el mismo candado por esquema y cancha que la creación del jugador (`serializeWritesOfCourt`), hacen vencimiento perezoso y dejan la garantía final a la restricción de exclusión.

En «Catálogo de eventos», agregar y ajustar:

| Evento | Publicador | Carga |
|---|---|---|
| `SlotBlocked` | `BlockSlot` | `bookingId`, `courtId`, `start`, `end` (sin motivo del bloqueo) |
| `BookingCreated` | `CreatePendingBooking`, `CreateStaffBooking` | igual que antes; `expiresAt` es nulo en la reserva manual, que nace confirmada |
| `BookingCancelled` | `CancelOwnPendingBooking`, `CancelBookingByStaff` | igual que antes; también al quitar un bloqueo |

## api/README.md

En la sección de la API de reservas, agregar «Staff booking API»:

- Endpoints de personal de `contracts/bookings.openapi.yaml` bajo `/api/v1/staff`, apagados por defecto (`playtodoo.bookings.staff-api.enabled=false`).
- Pruebas: `StaffBookingFlowIT` (casos de uso contra PostgreSQL: criterios 8, 9, 10 y 13, concurrencia bloqueo contra reserva), `StaffBookingApiIT` (HTTP y Problem Details, con las dos APIs activas), `StaffBookingApiFailClosedIT` (solo la API del personal, adaptadores por defecto).
