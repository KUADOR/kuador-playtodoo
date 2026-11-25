# Cambios a documentos compartidos · PLATODO-30

## docs/architecture.md

En «Datos», reemplazar la línea de la herramienta de migraciones por:

- **Herramienta de migraciones:** Flyway (ADR-009). Migraciones de tenant en `api/src/main/resources/db/migration/tenant` (la primera, `V1__create_booking.sql`); `TenantSchemaMigrator` (Tenancy, infraestructura) las aplica a un esquema con su propio `flyway_schema_history`. Migraciones globales: [POR DEFINIR: aún no hay ninguna].

En «Arquitectura del backend», debajo del ejemplo ilustrativo de capas, agregar la estructura real del módulo Reservas:

```
com.kuador.platodo.api.bookings/
  domain/                        Booking, BookingKind, BookingStatus, TimePeriod, BookingHolder,
                                 BookingCancellation, BookingOverlapException (sin Spring)
  application/port/out/          BookingRepository (puerto de salida)
  infrastructure/persistence/    PostgresBookingRepository (JdbcClient; 23P01 → BookingOverlapException)
com.kuador.platodo.api.tenancy/
  infrastructure/persistence/    TenantSchemaMigrator (Flyway por esquema de tenant)
```

Los adaptadores usan nombres de tabla sin esquema: el esquema del tenant lo pone el `search_path` de la conexión (`<tenant>, extensions`), que fijará Tenancy.

## api/README.md

Agregar una sección «Tenant migrations»: las migraciones de tenant viven en `src/main/resources/db/migration/tenant`; las pruebas `*IT` comparten un contenedor PostgreSQL (`support/TenantDatabase`) y migran cada esquema con `TenantSchemaMigrator`.
