# Cambios a documentos compartidos · PLATODO-53

## docs/architecture.md

Agregar una sección «Auditoría (acceso de Kuador)»:

```
com.kuador.platodo.api.audit/
  domain/                        AccessReason (obligatorio, ≤ 500), AccessSession (grants: abierta, del usuario y del
                                 tenant), AccessAuditEvent (allowed / denied), AccessOutcome
  application/                   OpenAccessSession, CloseAccessSession, ListAccessAudit,
                                 AuthorizePlatformAccess (RecordAccess: valida y registra en transacción propia)
  application/port/out/          AccessSessions, AccessAuditLog, CurrentPlatformOwner
  infrastructure/persistence/global/  JpaAccessSessions, JpaAccessAuditLog (unidad global)
  infrastructure/identity/       PrincipalCurrentPlatformOwner
  infrastructure/web/            PlatformAccessController, AuditConfiguration, AuditProblemHandler
```

- **Flujo:** el dueño abre la sesión en el host global (`POST /api/v1/platform/tenants/{id}/access-sessions`, motivo obligatorio, 30 min por defecto) y llama al host del complejo con su token y `X-Platform-Access-Session`.
- **Identidad y acceso:** `AuthenticateRequest` acepta ahora el token `PLATFORM` en cualquier host con la cuenta activa (antes respondía 403 en los hosts de complejo). Se quitó `Reason.ACCESS_SESSION_INVALID` de Identidad: ese 403 lo responde Tenancy.
- **Tenancy · `PlatformAccessGuard`:** `HandlerInterceptor` en `/api/**` (no filtro, porque necesita la plantilla de ruta del handler elegido). Si hay tenant resuelto y el principal es `PLATFORM`, pide permiso a `PlatformAccessAuthorization` (puerto de salida de Tenancy, implementado por Auditoría con `AuthorizePlatformAccess`); si lo niega, 403 `ACCESS_SESSION_INVALID` y el handler no corre.
- **RecordAccess (D6):** valida la sesión (abierta, del mismo usuario, del tenant resuelto, rol `PLATFORM_OWNER`) y escribe el evento en una transacción `REQUIRES_NEW` de la unidad global, con `flush` inmediato, antes del handler. Los denegados también se registran, sin sesión ni motivo (el motivo de otra sesión no se copia a la auditoría de otro complejo). Si la escritura falla, se deniega y se registra un `ERROR` en el log.
- **Plantilla de ruta (D12):** `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE`; nunca la ruta concreta ni el query string. Sin handler, `[unmatched]`.
- **Tenancy expone `TenantDirectory`** (`application/port/in`): busca un complejo por id en cualquier estado y devuelve su host actual (vacío tras la purga). Lo usan las operaciones de plataforma; implementación `JdbcTenantDirectory`.
- **CORS (`BackofficeCors`):** `/api/**` admite solo `playtodoo.cors.backoffice-origin`, con `Authorization`, `Content-Type` y `X-Platform-Access-Session`.
- **Pruebas:** `TenantDatabase` arranca PostgreSQL con `max_connections=300`, porque cada contexto de Spring cacheado mantiene su propio pool y la suite ya superaba el límite de 100. Reemplaza la nota de PLATODO-51 sobre compartir un solo contexto para no llegar al límite.

## docs/architecture.md · Módulos

- Fila «Auditoría»: `OpenAccessSession`, `CloseAccessSession`, `ListAccessAudit` y `AuthorizePlatformAccess` (RecordAccess). El listener de `TenantPurged` (anonimizar `reason` y marcar `reason_redacted_at`) llega con PLATODO-54.

## Para las tareas siguientes de la épica PLATODO-26

- **PLATODO-54 (purga):** el listener síncrono de `TenantPurged` en Auditoría debe actualizar `reason` y `reason_redacted_at` en `access_session` y `access_audit_event` del tenant purgado. `AccessAuditEvent.reasonRedacted` ya se lee de `reason_redacted_at`.
- **PLATODO-52 (ciclo de vida):** puede reutilizar `TenantDirectory` y `AuthenticatedPrincipal.isPlatformOwner()` para el 403 `FORBIDDEN` de sus operaciones de plataforma. Si agrega una migración global, que use `V5` o posterior.
- **Módulos operativos (otras épicas):** hoy un dueño con sesión válida pasa el guard, pero los endpoints de personal exigen un principal `STAFF` y responden su propio rechazo. Cada módulo que deba admitir el acceso de Kuador tiene que aceptar explícitamente el principal `PLATFORM` (ya auditado por el guard).
