# Cambios a documentos compartidos · PLATODO-87

Purga automática de suspendidos y cancelados y retención de 1 año en la `api` (ADR-015, ADR-016; PLATODO-27 CA10 y la retención de CA5 y CA13).

## docs/architecture.md

En la sección «Módulos», fila «Tenancy», agregar:

```
com.kuador.platodo.api.tenancy/
  domain/                        Tenant.startPurge desde SUSPENDED o ARCHIVED, con purgeDueAt vencido
  application/                   PurgeDueTenants.claimDue toma SUSPENDED y ARCHIVED vencidos
                                 (TenantRegistry.lockInRetentionDueBy, FOR UPDATE SKIP LOCKED) y publica
                                 TenantStatusChanged(PURGING) por cada uno, en la misma transacción
```

En la fila «Acceso de soporte y Auditoría», agregar:

- `InvalidateAccessSessionsOfPurgingTenant` + `TenantPurgingAuditListener` (síncrono, escucha `TenantStatusChanged`): al pasar a `PURGING` cierra (`closed_at`) las sesiones abiertas del complejo en la transacción del paso. Si falla, el complejo no queda `PURGING`. Puerto `AccessSessionInvalidation`.
- `DeleteExpiredAccessAudit` + `AccessAuditRetentionJob`: borra `access_audit_event` con más de `playtodoo.audit.retention-days` (365) y las `access_session` vencidas antes de ese corte que ya no tienen eventos. Puerto `ExpiredAccessAudit`.

En la fila «Planes y suscripciones», agregar:

- `DeleteExpiredSubscriptionHistory` + `SubscriptionHistoryRetentionJob`: borra `subscription_history` con más de `playtodoo.history.retention-days` (365). No toca suscripciones ni pagos. `ListComplexHistory` usa el mismo plazo (antes, una constante de 365 días).

En la fila «Identidad y acceso», agregar:

- `DeleteExpiredCapabilityChanges` + `CapabilityChangeRetentionJob`: borra `capability_change` con más de `playtodoo.history.retention-days` (365). `ListCapabilityChanges` usa el mismo plazo.

En la fila «Complejos y canchas», agregar: `TenantCatalogListener` también escucha `TenantPurged` (síncrono, dentro de la transacción de purga) y borra la fila de `complex_catalog` (`ComplexCatalog.remove`). Si falla, el complejo queda `PURGING` y la tarea reintenta.

En el párrafo de eventos `Tenant*` (excepción a `AFTER_COMMIT`), precisar los consumidores síncronos de `TenantPurged`: Auditoría (redacta motivos) y Complejos y canchas (borra la fila del catálogo). Planes y suscripciones no necesita escucharlo: su listado lee el estado del tenant de `TenantDirectory` y la suscripción no tiene datos personales (`owner_email` vive en `tenant` y lo redacta Tenancy).

En «Catálogo de eventos», fila `TenantStatusChanged`: también lo publica la purga al pasar a `PURGING` (no al pasar a `PURGED`, que se anuncia con `TenantPurged`).

En «Ciclo de vida del tenant», línea «Plazo de conservación»: la propiedad es `tenancy.archive.default-retention-days` (ya lo pide PLATODO-86). Agregar la línea:

- **Retención de 1 año:** `playtodoo.audit.retention-days` y `playtodoo.history.retention-days` (365), con tareas diarias en `playtodoo.retention.cron` (03:30 UTC), desactivables con `playtodoo.retention.enabled=false`. No se usan filas de `platform_parameter` (como en PLATODO-86, ninguna pantalla los cambia). Cada módulo borra sus propias tablas: no hay una tarea compartida que cruce los límites de los módulos.

## docs/adr/ADR-016-alcance-de-borrar-todo.md

Sin cambios de decisión. Nota de implementación para «Decisión», punto 3: los nombres de las propiedades son `playtodoo.audit.retention-days` y `playtodoo.history.retention-days`; la tabla de auditoría es `access_audit_event` (el diseño la llama `audit_entry`) y las sesiones (`access_session`) se borran con ella.

## Pendientes para otras tarjetas

- **PLATODO-88 (soporte):** el diseño menciona `invalidated_at` en la sesión de soporte; esta tarjeta cierra las sesiones con `closed_at`, que ya rechaza la sesión (`AccessSession.isOpenAt`). Si PLATODO-88 agrega la columna, `JpaAccessSessionInvalidation` es el único lugar a ajustar.
- **Índices:** el borrado de `access_audit_event` y `access_session` por fecha no tiene un índice solo por fecha (hay uno por complejo y fecha). Es una tarea diaria sobre una tabla chica hoy; si crece, agregar `access_audit_event (occurred_at)` en una migración global.
