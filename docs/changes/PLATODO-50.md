# Cambios a documentos compartidos · PLATODO-50

## docs/architecture.md

En la sección «Persistencia multi-tenant (ADR-010)» (nota de PLATODO-49), agregar la resolución de tenant:

```
com.kuador.platodo.api.tenancy/
  domain/                        Subdomain (formato de ADR-012), TenantStatus
  application/                   RequestHosts (clasifica el host: global, tenant u otro), ResolveTenant,
                                 TenantResolution; port/out/TenantDomains
  infrastructure/persistence/    JdbcTenantDomains (global.tenant_domain + global.tenant)
  infrastructure/web/            TenantResolutionFilter, TenantStateController (GET /api/v1/tenant),
                                 TenancyProblemHandler, TenancyWebConfiguration
```

- **TenantResolutionFilter** es el primer filtro de `/api/*` (orden `HIGHEST_PRECEDENCE + 10`): clasifica el host, valida el formato del subdominio antes de consultar, busca el subdominio vigente en `global.tenant_domain` y ejecuta el resto de la solicitud dentro del `TenantScope` del complejo. Respuestas: 403 `TENANT_UNAVAILABLE` (suspendido o archivado), 404 `TENANT_NOT_FOUND` (mal formado, inexistente, retirado, `PROVISIONING`, `PURGING`, `PURGED` u host fuera del dominio raíz) y 404 `RESOURCE_NOT_FOUND` (operación de tenant en el host global u operación global en un host de tenant). Ningún rechazo abre un `TenantScope`.
- Operaciones del host global: `/api/v1/auth/players/**`, `/api/v1/auth/platform/**`, `/api/v1/subdomains/**` y `/api/v1/platform/**` (`TenancyApiPaths`). Cualquier otra ruta de `/api` es de tenant.
- El host es `request.getServerName()`. Tomcat lo toma de `X-Forwarded-Host` solo si la solicitud viene de un proxy de `server.tomcat.remoteip.internal-proxies` (`server.request.header.forwarded.strategy=web-server`).
- Sin caché: una búsqueda por clave primaria por solicitud.
- Migración global `V2__create_tenant_domain.sql`: tabla `global.tenant_domain` (subdominio como PK, índice único parcial de un subdominio vigente por tenant).
- Pruebas: `support/FixedTenantScopeFilter` se reemplazó por `support/TenantHostRequests` (MockMvc envía cada solicitud al host del tenant y la resuelve el filtro real). `TestTenants.register` también registra el subdominio `test-<uuid sin guiones>`.
