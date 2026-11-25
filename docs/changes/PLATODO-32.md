# Cambios a documentos compartidos · PLATODO-32

## docs/architecture.md

En «Arquitectura del backend», en la estructura real del módulo Reservas (agregada por PLATODO-30), sumar:

```
com.kuador.platodo.api.bookings/
  domain/                        BookingExpired (evento, sin datos personales)
  application/                   ExpirePendingBookings (caso de uso: vencimiento por lotes de 500 por tenant)
  application/port/in/           ExpireDuePendingBookings
  application/port/out/          BookingEventPublisher; BookingRepository.expirePendingDueBy(now, limit)
  infrastructure/scheduling/     ExpirePendingBookingsJob (@Scheduled, cada 30 s, recorre tenants activos)
  infrastructure/events/         SpringBookingEventPublisher (Spring Events, ADR-007)
com.kuador.platodo.api.tenancy/
  application/port/in/           ActiveTenantRunner, TenantId (puerto público para tareas en segundo plano)
```

En «Tenancy» (tabla de módulos, columna de contratos), agregar: «`ActiveTenantRunner`: ejecuta una tarea por cada tenant activo, de a uno, con el esquema de ese tenant activo en el hilo; los suspendidos se omiten. Lo usa la tarea programada de vencimiento de pendientes».

En el catálogo de eventos, fila `BookingExpired`: el publicador es `ExpirePendingBookingsJob` vía `BookingEventPublisher`; un evento por reserva vencida aunque corran varias instancias de la `api` (el `UPDATE … RETURNING` con `FOR UPDATE SKIP LOCKED` entrega cada fila a una sola ejecución).

Pendiente [POR DEFINIR: implementación de `ActiveTenantRunner` en Tenancy (registro global de tenants activos y `search_path` por conexión) y cableado Spring de la tarea con `@EnableScheduling`]. Hasta entonces la tarea está implementada y probada contra PostgreSQL real, pero no corre en la aplicación, igual que el repositorio de PLATODO-30.
