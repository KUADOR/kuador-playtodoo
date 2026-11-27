# Cambios a documentos compartidos · PLATODO-88

Acceso de soporte con motivo, edición y auditoría en la `api` (ADR-017, ADR-020; PLATODO-27 CA11 a CA14 y CA17).

## docs/architecture.md

En la sección «Módulos», fila «Auditoría», reemplazar la descripción de las sesiones de acceso por:

```
com.kuador.platodo.api.audit/
  domain/                        AccessSession (sesión de soporte: persona, tenant, motivo, 30 min), AccessReason
                                 (1 a 500, no solo espacios), AccessAuditEvent (+ operationId y denialCode),
                                 AuditedRequest (método, plantilla de ruta, operationId), DenialCode,
                                 SupportCaller.refusalOn(estado): ACTIVE para SUPER_ADMIN u OPERATOR con
                                 TENANT_DATA_ACCESS; SUSPENDED y ARCHIVED solo SUPER_ADMIN; otros estados nadie
  application/                   OpenSupportSession (audita la apertura, permitida o rechazada), CloseSupportSession
                                 (audita el cierre), ListAuditEntries (solo SUPER_ADMIN, últimos 365 días, nombres en
                                 una consulta), AuthorizeSupportAccess (implementa SupportAccessAuthorization de
                                 Tenancy), CloseSupportSessionsOfPurgedTenant, CurrentSupportCaller (CapabilityGuard)
  infrastructure/web/            SupportAccessController: POST /platform/complexes/{complexId}/support-sessions,
                                 DELETE /platform/support-sessions/{sessionId},
                                 GET /platform/complexes/{complexId}/audit-entries
```

`AuthorizeSupportAccess` decide con el estado del complejo leído con `TenantProfileLock` (fila bloqueada dentro de su transacción propia): una purga o un cambio de ciclo de vida en curso se espera y su resultado decide.

En la fila «Tenancy»:

- `TenantResolutionFilter`: un complejo `SUSPENDED` o `ARCHIVED` sigue respondiendo `403 TENANT_UNAVAILABLE`, salvo que la solicitud anuncie una sesión de soporte (`X-Support-Session` o el preflight CORS que la pide); entonces corre en el alcance del complejo y decide `SupportAccessGuard`.
- `SupportAccessGuard` (reemplaza a `PlatformAccessGuard`): toda solicitud `PLATFORM` en un host de complejo pasa por `SupportAccessAuthorization` (auditada antes de ejecutarse) o recibe `403 SUPPORT_SESSION_INVALID`; cualquier otra solicitud en un complejo suspendido o archivado recibe `403 TENANT_UNAVAILABLE`, con o sin la cabecera. Registra el permiso en `RequestSupportGrants`.
- `SupportGrants` (puerto público, `application/port/in`): dice si la solicitud en curso corre con una sesión de soporte ya validada para esa persona y ese tenant.
- `TenantHostOperations`: tabla método + plantilla de ruta → `operationId` de los contratos del host del complejo; `TenantHostOperationsContractTest` la mantiene igual a `bookings.openapi.yaml` y `tenancy.openapi.yaml`.
- `GET /tenant` responde `403 TENANT_UNAVAILABLE` también a soporte en un complejo suspendido o archivado (el estado público solo informa `ACTIVE`).

En la fila «Reservas»: `PrincipalCurrentStaff` acepta como personal a una cuenta `PLATFORM` solo si `SupportGrants` lo confirma; soporte reutiliza los casos de uso del personal y su id queda en `created_by` y `cancelled_by`.

En «Esquema global», precisar: `access_audit_event` suma `operation_id` y `denial_code` (migración V11; las denegaciones anteriores quedan con `SUPPORT_SESSION_INVALID`).

En «Decisiones», ADR-011: la cabecera es `X-Support-Session` y el error `SUPPORT_SESSION_INVALID` (ajustes de ADR-020 ya implementados).

## docs/adr/ADR-020-soporte-en-el-host-del-complejo.md

Punto 7 («Reemplazo»): queda hecho en PLATODO-88. Las operaciones `access-sessions`, `access-audit` y la cabecera `X-Platform-Access-Session` se retiraron de la `api` y de `contracts/tenancy.openapi.yaml`.

Punto 5 («Purga»): desde `PURGING` el host del complejo responde «no encontrado» y ninguna solicitud llega; las sesiones abiertas se cierran en la transacción de la purga (`TenantPurged`), porque `claimDue` no publica un evento al pasar a `PURGING`.

Punto 6 (canchas y horarios): sigue igual; crear reservas o bloqueos desde soporte responde `404 COURT_NOT_FOUND` hasta que exista el módulo de canchas.
