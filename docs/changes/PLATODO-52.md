# Cambios a documentos compartidos · PLATODO-52

Ciclo de vida del complejo y cambio de subdominio (épica PLATODO-26, ADR-012 y ADR-013).

## docs/architecture.md

Agregar en la sección del módulo Tenancy (o en una nueva «Tenancy: ciclo de vida y subdominios»):

```
com.kuador.platodo.api.tenancy/
  domain/                        Tenant (máquina de estados), SubdomainRules (formato + reservados),
                                 SubdomainCandidates (sugerencia desde el nombre), excepciones de transición
  domain/event/                  TenantEvent, TenantActivated, TenantSuspended, TenantArchived, TenantSubdomainChanged
  application/                   RegisterTenant, TenantLifecycle, ChangeSubdomain, SuggestSubdomain,
                                 CheckSubdomainAvailability; TenancyUseCases (fábrica: comparten TenantTransitions y
                                 TenantProvisioning, internos del paquete)
  application/port/out/          TenantRegistry, SubdomainUsage, TenantSchemas, TenantOwnerAccounts,
                                 TenantEventPublisher, GlobalTransaction, Callers
  infrastructure/persistence/    JdbcTenantRegistry, JdbcSubdomainUsage, MigratingTenantSchemas,
                                 SpringGlobalTransaction (gestor de transacciones global)
  infrastructure/identity/       PrincipalCallers (lee CurrentPrincipal de Identidad y acceso)
  infrastructure/events/         SpringTenantEventPublisher
  infrastructure/config/         TenancyProperties (tenancy.*), TenancyLifecycleConfiguration
  infrastructure/web/            PlatformTenantController, SubdomainController, TenantSubdomainController,
                                 TenancyOperationsProblemHandler
```

- **Transiciones:** cada una bloquea la fila de `global.tenant` (`SELECT … FOR UPDATE`), valida la transición en el dominio, guarda y publica su evento en la misma transacción global. Una transición que el estado no permite responde `409 INVALID_TRANSITION`.
- **Al volver a `ACTIVE`** (activar o reactivar) se limpian `suspended_at` y los campos del archivado (`archived_at`, `retention_days`, `purge_due_at`): los campos siempre describen el estado actual.
- **Alta:** registra el tenant `PROVISIONING` y su subdominio en una transacción (si el subdominio está tomado no queda nada registrado), luego crea el esquema con las migraciones de tenant, crea el propietario y activa. Si la provisión falla, queda `PROVISIONING` y `POST /platform/tenants/{id}/activate` la reintenta.
- **Propietario:** Identidad y acceso implementa el puerto `TenantOwnerAccounts` (`identity/infrastructure/persistence/tenant/JdbcTenantOwnerAccounts`): crea el `staff_user` con rol `OWNER`, el correo normalizado y una contraseña aleatoria que nadie conoce (la fija la invitación de su épica). `PasswordHashes` suma `hash(rawPassword)`.
- **Concurrencia de subdominios:** la clave primaria de `tenant_domain` serializa a dos altas o dos cambios por el mismo subdominio: uno gana y el otro recibe `409 SUBDOMAIN_TAKEN` (`DuplicateKeyException` traducida en el adaptador).
- **Cambio de subdominio:** retira el actual (`is_current = false`, `retired_at`) e inserta el nuevo en la misma transacción. El token del personal está ligado al `tid`, no al subdominio: sigue sirviendo en el host nuevo.
- **Sugerencia:** si el nombre no tiene letras ni dígitos se usa «complejo»; si queda con menos de 3 caracteres se le agrega «-complejo» (decisión técnica, ver PR). Los candidatos se consultan de a 20 por consulta (`IN`), sin N+1.
- **Reservados:** `tenancy.subdomains.reserved`; `api` (etiqueta del host global) queda reservado siempre, aunque falte en la configuración.
- **Catálogo de eventos:** los cuatro eventos `TenantActivated`, `TenantSuspended`, `TenantArchived` y `TenantSubdomainChanged` ya se publican (`tenancy/domain/event`). Cambiar el plazo de retención no publica evento. Todavía no hay consumidores.
- **Pruebas:** `TenantLifecycleIT` y `SubdomainIT` comparten un contexto. En `src/test/resources/config/application.properties` se agregó `spring.datasource.hikari.minimum-idle=1`: cada contexto cacheado mantenía 10 conexiones ociosas y la suite llegaba al límite de 100 conexiones del PostgreSQL de Testcontainers.

## contracts/tenancy.openapi.yaml (para el arquitecto; no se cambió en esta tarea)

- `POST /platform/tenants/{tenantId}/activate` sobre un tenant `PROVISIONING` cuyo propietario no llegó a crearse responde `409 INVALID_TRANSITION` con un `detail` propio: el correo del propietario no se guarda en `global`, así que el reintento no puede recrearlo. Si hace falta recuperar ese caso, el contrato necesitaría un cuerpo opcional con `ownerEmail` en `activate`.
- `PUT /platform/tenants/{tenantId}/retention` acepta hasta 36 500 días como tope técnico (para que la fecha de purga entre en un `timestamptz`); el máximo de negocio sigue [POR DEFINIR].
- Un `tenantId` que no es un UUID canónico responde `404 TENANT_NOT_FOUND`, igual que uno inexistente.
- Recordatorio de PLATODO-51: un token `STAFF` en el host global responde `401 UNAUTHENTICATED` (lo rechaza el filtro de tokens antes de llegar a `403 FORBIDDEN`). Conviene documentarlo en las operaciones de plataforma.
