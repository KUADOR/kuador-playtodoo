# Cambios a documentos compartidos · PLATODO-54

Baja definitiva con notificación, eliminación del esquema y anonimización (épica PLATODO-26, ADR-013: D8, D10, D11).

## docs/architecture.md

Módulo Tenancy (sumar a la estructura que dejó PLATODO-52):

```
com.kuador.platodo.api.tenancy/
  domain/                        Tenant: startPurge (ARCHIVED → PURGING desde purge_due_at) y markPurged (PURGING → PURGED)
  domain/event/                  TenantPurged (tenantId, purgedAt)
  application/                   PurgeDueTenants: claimDue, pending, purge
  application/port/out/          TenantOwnerNotifier (nuevo); TenantRegistry suma tryLock, lockArchivedDueBy, purging y
                                 retireCurrentSubdomain; TenantSchemas suma exists y dropIfExists; TenantOwnerAccounts
                                 suma ownerEmail
  infrastructure/persistence/    PostgresTenantSchemas (antes MigratingTenantSchemas): provisiona y elimina esquemas
  infrastructure/scheduling/     PurgeDueTenantsJob (tenancy.purge.cron, UTC) y TenancySchedulingConfiguration
                                 (tenancy.purge.enabled)
```

- **Purga en dos pasos:**
  1. `claimDue`: en una transacción toma los `ARCHIVED` vencidos con `FOR UPDATE SKIP LOCKED` y los pasa a `PURGING`.
  2. `purge(id)`: por complejo, una transacción que toma su fila con `SKIP LOCKED` y hace todo junto: encola el correo, `DROP SCHEMA IF EXISTS "<t_uuid validado>" CASCADE`, marca `PURGED` con `purged_at`, retira el subdominio y publica `TenantPurged`.
- **Decisión de implementación (afina el paso 5 de ADR-013):** los pasos 2 a 4 del ADR van en **una sola transacción** por complejo, no en transacciones separadas. PostgreSQL revierte el `DROP SCHEMA` con la transacción. Si algo falla, el complejo queda `PURGING` con su esquema y sin correo encolado, y el siguiente run lo repite desde el principio. Así el correo se encola una sola vez y nunca queda `PURGED` con datos personales. Alternativa descartada: pasos con commit propio, que en un reintento encolaba el correo dos veces.
- Si el esquema ya no existe (borrado a mano) o no hay cuenta de propietario, la purga termina sin correo en lugar de fallar en cada run.
- `tenant_domain`: el subdominio se retira (`is_current = false`, `retired_at`) y no se reutiliza (su clave primaria).

Módulo Notificaciones (nuevo, `com.kuador.platodo.api.notifications`):

- Dueño de `global.notification_outbox` (migración global V5: `id`, `type`, `recipient`, `payload jsonb`, `created_at`, `sent_at`).
- `OutboxTenantOwnerNotifier` implementa el puerto `TenantOwnerNotifier` de Tenancy: inserta el tipo `TENANT_PERMANENTLY_DELETED` con `payload` `{tenantId, displayName, purgedAt}`, en la transacción de la purga. El destinatario va solo en `recipient`, nunca en el `payload`. El envío y el borrado de la fila llegan con la épica de notificaciones.

Módulo Auditoría:

- `TenantPurgedAuditListener` (`@EventListener` síncrono) → `RedactAccessReasonsOfPurgedTenant` → puerto `AccessReasonRedaction` (`JpaAccessReasonRedaction`).
- Reemplaza el motivo de `access_session` y de `access_audit_event` del tenant por `AccessReason.REDACTED` («[anonimizado por baja definitiva]») y marca `reason_redacted_at = purgedAt`. Solo toca filas de ese `tenant_id` y es idempotente.

Catálogo de eventos:

- `TenantPurged` ya se publica. Sus consumidores son Auditoría (en esta tarea) y Planes y suscripciones (pendiente, condición de ADR-013).
- `SpringTenantEventPublisher` y `TenantEventPublisher` documentan la excepción: los consumidores de `TenantPurged` usan `@EventListener`, no `AFTER_COMMIT`.

## docs/glossary.md

- **Lápida (del complejo):** fila de `global.tenant` en estado `PURGED` que queda tras la baja definitiva: id, nombre comercial, estado y fechas, sin datos personales.
- **Outbox de notificaciones:** tabla `global.notification_outbox` donde los módulos encolan mensajes que se enviarán después, desacoplados del proveedor de correo.
