# Cambios de despliegue · PLATODO-30

## Dependencias

| Dependencia | Versión | Para qué |
|---|---|---|
| Flyway (`flyway-core`, `flyway-database-postgresql`) | La fija el BOM de Spring Boot 4.2.0 (12.11.0) | Migraciones SQL versionadas de cada esquema de tenant (ADR-009) |
| Driver JDBC de PostgreSQL | La fija el BOM (42.7.14) | Pasa de alcance `test` a `runtime`: lo usa la `api` |
| `spring-jdbc` | La fija el BOM | Adaptador de persistencia de Reservas (`JdbcClient`) |

- Sin servicios, puertos, variables ni secretos nuevos.
- Primera migración de tenant: `api/src/main/resources/db/migration/tenant/V1__create_booking.sql` (tabla `booking`, restricción `booking_no_overlap` e índices). Cada esquema guarda su propio `flyway_schema_history`.

## Qué hacer al desplegar

- **Requisito previo en cada base:** la extensión `btree_gist` en el esquema `extensions` (nota de PLATODO-29). Sin ella la migración falla al crear `booking_no_overlap`.
- La conexión que migra un tenant debe tener `extensions` en su `search_path` (por ejemplo, `currentSchema=<tenant>,extensions` en la URL JDBC o el `search_path` por defecto del rol).
- **Todavía no se ejecuta sola:** esta tarea deja `TenantSchemaMigrator` (aplica las migraciones a un esquema). Recorrer los tenants activos al arrancar y migrar al crear un complejo es responsabilidad de Tenancy, que aún no existe: [POR DEFINIR: tarjeta de Tenancy que lo conecte]. Hasta entonces no hay cambio en ningún entorno desplegado.
- **Producción (main):** sin acción en este PR.

## Vuelta atrás

- Antes de que existan datos: `DROP TABLE <tenant>.booking; DROP TABLE <tenant>."flyway_schema_history";` en cada esquema de tenant migrado, y revertir el PR.
- Con datos reales no se revierte la tabla; se corrige con una migración nueva.
