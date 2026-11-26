# Salida de `./mvnw -B verify` · api

Corrida local del 2026-11-25 sobre `origin/develop` en `e126da2` (JDK 25, PostgreSQL 18 con Testcontainers). Extracto del log de Maven; resultado `BUILD SUCCESS`.

## Totales

```
surefire (unitarias):   Tests run: 235, Failures: 0, Errors: 0, Skipped: 0
failsafe (integración): Tests run: 224, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Pruebas de aislamiento entre complejos

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.tenancy.application.port.in.TenantScopeTest
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.architecture.PersistenceArchitectureTest
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.identity.infrastructure.web.StaffTenantIsolationIT
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.identity.infrastructure.web.PlayerGlobalAccountIT
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.bookings.infrastructure.web.PlayerBookingApiFailClosedIT
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.bookings.infrastructure.web.StaffBookingApiFailClosedIT
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.bookings.infrastructure.persistence.BookingTenantIsolationIT
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.tenancy.infrastructure.web.ForwardedHostIT
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.tenancy.infrastructure.web.TenantResolutionIT
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.tenancy.infrastructure.web.TenantPurgeIT
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.tenancy.infrastructure.persistence.TenantPersistenceIT
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.architecture.TenantEntityCoverageIT
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.audit.infrastructure.web.PlatformAccessSessionIT
```

## Pruebas de concurrencia

Clases que incluyen pruebas concurrentes (dos o más solicitudes a la vez sobre el mismo horario o subdominio: un solo ganador):

```
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.bookings.application.StaffBookingFlowIT
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.bookings.application.PlayerBookingFlowIT
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.bookings.infrastructure.persistence.PostgresBookingRepositoryIT
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.tenancy.infrastructure.web.TenantLifecycleIT
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- in com.kuador.platodo.api.tenancy.infrastructure.web.SubdomainIT
```

| Prueba concurrente | Clase |
|---|---|
| `concurrentPlayersRequestingTheSameSlotGetExactlyOneBooking_criteria1And3` | `PlayerBookingFlowIT` |
| `concurrentConfirmationsConfirmOnceAndPublishOneEvent` | `PlayerBookingFlowIT` |
| `concurrentManualBookingsOfTheSameSlotGetExactlyOneBooking_criterion8` | `StaffBookingFlowIT` |
| `concurrentRequestsForTheSameSlotCreateExactlyOneBooking_criteria1And3` | `PostgresBookingRepositoryIT` |
| `concurrentChangesToTheSameSubdomainHaveOneWinner` | `SubdomainIT` |
| `concurrentRegistrationsWithTheSameSubdomainHaveOneWinner` | `TenantLifecycleIT` |

Los mensajes `ERROR` del log corresponden a pruebas que provocan fallas a propósito (auditoría no disponible, purga fallida, expiración en un tenant roto) y verifican que el sistema deniega o reintenta.
