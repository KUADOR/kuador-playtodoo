# Pruebas automatizadas · épica PLATODO-27

Corrida local del 2026-11-27 sobre `origin/develop` en `c67e28b` (con PLATODO-81 a PLATODO-92 integradas). JDK 25,
PostgreSQL 18 con Testcontainers y Node 22. Sin pruebas omitidas.

## Totales

| Pieza | Comando | Resultado |
|---|---|---|
| `api` | `./mvnw -B verify` | `BUILD SUCCESS` · surefire 396 · failsafe 298 · 0 fallas, 0 errores, 0 omitidas |
| `backoffice` | `npm test -- --watch=false` | 43 archivos · 391 pruebas · todas en verde |
| `backoffice` | `npm run build` y `npx prettier --check src` | compila; formato sin cambios |

```
surefire (unitarias):   Tests run: 396, Failures: 0, Errors: 0, Skipped: 0
failsafe (integración): Tests run: 298, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
backoffice:             Test Files 43 passed (43) · Tests 391 passed (391)
```

Los registros `ERROR` y `WARN` del log de Maven vienen de casos negativos provocados a propósito (auditoría caída,
purga que falla y se reintenta, nombres duplicados); ninguna prueba falla.

## Evidencia por tema

Extracto del log de Maven (`Tests run: N, Failures: 0, Errors: 0, Skipped: 0 -- in <clase>`).

### Aislamiento entre complejos

| Clase | Pruebas | Qué demuestra | CA |
|---|---|---|---|
| `bookings.infrastructure.persistence.BookingTenantIsolationIT` | 3 | Una reserva de A no se ve ni se cancela desde B, ni bloquea el mismo horario en B | CA14 |
| `identity.infrastructure.web.StaffTenantIsolationIT` | 11 | El personal de un complejo no entra a otro | CA18 |
| `identity.infrastructure.web.PlatformRolesAndCapabilitiesIT` | 10 | Jugadores y personal no llegan a `/platform` (`playersAndComplexStaffDoNotReachThePlatformOperations`) | CA15, CA16, CA18 |
| `audit.infrastructure.web.SupportSessionIT` | 16 | La sesión de soporte de A no abre B; sin motivo no hay datos; operador solo en activos | CA11 a CA14 |
| `subscriptions.infrastructure.web.SubscriptionLifecycleEffectsIT` | 10 | Suspender o cancelar A no toca a B (`theEffectOnOneComplexNeverTouchesAnother`) | CA8, CA9, CA14 |
| `tenancy.infrastructure.web.TenantPurgeIT` | 11 | La purga de A nunca toca otro complejo (`thePurgeOfOneComplexNeverTouchesAnother`) | CA10, CA14 |
| `architecture.PersistenceArchitectureTest` · `architecture.TenantEntityCoverageIT` · `tenancy.application.port.in.TenantScopeTest` | 16 · 2 · 6 | Reglas de arquitectura: el acceso a datos de un complejo pasa siempre por el esquema del tenant resuelto | Regla de AGENTS.md |
| `tenancy.infrastructure.web.TenantResolutionIT` · `ForwardedHostIT` | 20 · 3 | Tenant resuelto solo por subdominio | Regla de AGENTS.md |

### Concurrencia

| Clase | Prueba | CA |
|---|---|---|
| `audit.infrastructure.web.SupportConcurrencyIT` (3) | `supportAndAPlayerNeverBothBookTheSameSlot` | CA17 |
| | `twoSupportSessionsOnASuspendedComplexNeverBothBlockTheSameSlot` | CA17 |
| | `supportRequestsRacingThePurgeAreServedOrRefusedNeverBroken` | CA17 |
| `tenancy.infrastructure.web.TenantPurgeIT` | `aReactivationRacingThePurgeEitherWinsOrIsRejected`, `twoInstancesRunningTogetherPurgeTheComplexOnce`, `aComplexHeldByAnotherInstanceIsSkippedWithoutWaiting` | CA10 |
| `bookings.application.StaffBookingFlowIT` (20) · `PlayerBookingFlowIT` (24) · `PostgresBookingRepositoryIT` (18) | Reservas sin duplicados ni solapes (ADR-006) | CA17 |

### Purga y conservación

| Clase | Pruebas | Qué demuestra | CA |
|---|---|---|---|
| `tenancy.infrastructure.web.TenantPurgeIT` | 11 | Suspendido o archivado con el plazo vencido se borra; antes del plazo no; reintento idempotente; cierra las sesiones de soporte al reclamar la purga | CA10 |
| `subscriptions.infrastructure.web.SubscriptionLifecycleEffectsIT` | 10 | Plazo desde la suspensión o la cancelación; cancelar un suspendido conserva la fecha; reactivar después del plazo da `RETENTION_EXPIRED` | CA9, CA10 |
| `RetentionJobsWiringIT` | 1 | Las tareas programadas de purga y retención quedan registradas | CA10, CA13 |

### Auditoría y retención de 1 año

| Clase | Pruebas | Qué demuestra | CA |
|---|---|---|---|
| `audit.infrastructure.persistence.AccessAuditRetentionIT` | 2 | Se borra la auditoría de más de un año y se conserva el resto | CA13 |
| `audit.application.DeleteExpiredAccessAuditTest` · `AuthorizeSupportAccessTest` · `OpenSupportSessionTest` | 2 · 13 · 17 | Se audita lo permitido y lo denegado; si la auditoría falla, se deniega | CA12, CA13 |
| `subscriptions.infrastructure.web.SubscriptionsApiIT` | 16 | Historial de plan y estado; lo de más de un año no se muestra y la tarea lo borra | CA5, CA6, CA7 |
| `identity.infrastructure.web.PlatformRolesAndCapabilitiesIT` | 10 | Registro de cambios de funciones del operador y su retención de un año | CA16 |

### Alta, planes y listado

| Clase | Pruebas | CA |
|---|---|---|
| `subscriptions.infrastructure.web.ComplexesApiIT` · `ComplexProvisioningRetryIT` | 11 · 2 | CA1, CA2, CA19 |
| `subscriptions.infrastructure.web.PlansApiIT` | 17 | CA3, CA4 |
| `subscriptions.domain.*` (`SubscriptionTest`, `PlanTest`, `PaymentDraftTest`…) | 57 | CA3 a CA7 |
