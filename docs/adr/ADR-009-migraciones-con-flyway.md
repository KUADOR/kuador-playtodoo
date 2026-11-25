# ADR-009 · Migraciones de base de datos con Flyway

- **Estado:** propuesto (lo aprueba una persona en la puerta del tablero).
- **Fecha:** 2026-11-25
- **Origen:** PLATODO-28 (decisión D12 del diseño de PLATODO-25).

## Contexto

`docs/architecture.md` deja la herramienta de migraciones como [POR DEFINIR]. La pieza `api` todavía no tiene código (solo `api/README.md`), así que no hay ninguna herramienta configurada. La tabla `booking` (ADR-006) es la primera que debe existir en **cada esquema de tenant**, incluidos los que ya existan cuando se despliegue. La estrategia Schema per Tenant (ADR-001) exige:

- Migraciones del **esquema global** (tenants, planes, auditoría, catálogo de complejos).
- Migraciones de **cada esquema de tenant**, aplicadas al crear un complejo y a todos los existentes en cada despliegue.
- Versionado en el repositorio, repetible y aplicable sobre datos reales.

## Decisión

Usar **Flyway** (integración nativa de Spring Boot) con dos conjuntos de migraciones SQL versionadas:

| Conjunto | Carpeta | Se aplica |
|---|---|---|
| Global | `api/src/main/resources/db/migration/global` | Al arrancar, sobre el esquema `global` (autoconfiguración de Spring Boot) |
| Tenant | `api/src/main/resources/db/migration/tenant` | Por cada esquema de tenant: al crearlo (Tenancy) y al arrancar, recorriendo los tenants activos de a uno con una instancia de Flyway configurada con `schemas` = esquema del tenant |

- Cada esquema guarda su propio historial (`flyway_schema_history`), así un tenant nunca ve el historial de otro.
- Las migraciones son SQL plano, revisables en el PR; `ddl-auto` queda en `validate` o `none`.
- Una migración de tenant que falla detiene el arranque y deja indicado el esquema; no se sigue con el resto en un estado mixto sin aviso.
- La extensión `btree_gist` no la crea una migración de tenant: la instala la infraestructura (PLATODO-29) una vez por base.
- La versión exacta de Flyway la fija el BOM de Spring Boot 4 al crear el proyecto de la `api`; quien lo implemente consulta su documentación para esa versión.

## Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Liquibase | Más potente (changelogs en XML/YAML, rollback), pero agrega complejidad que hoy no se necesita; SQL plano basta y lo revisa cualquiera |
| `spring.jpa.hibernate.ddl-auto=update` | No versiona, no revierte y no sirve para evolucionar cientos de esquemas con datos reales |
| Scripts manuales | No son repetibles ni verificables en CI |

## Actualización · PLATODO-26 (2026-11-25)

Decisión D5 del diseño de la épica de aislamiento, que confirma Flyway y precisa:

- **Global:** el conjunto global crea las tablas de `global` (`tenant`, `tenant_domain`, cuentas, auditoría, `notification_outbox`) y el esquema vacío `no_tenant` (ADR-010).
- **Tenant:** se aplica al provisionar un complejo y, al arrancar, a cada tenant en `ACTIVE`, `SUSPENDED` o `ARCHIVED` (no a `PROVISIONING`, `PURGING` ni `PURGED`), de a uno y cada uno en su propio esquema, abriendo su ámbito de tenant. Si un tenant falla, el arranque se detiene.
- La primera migración de tenant de la épica crea `staff_user` (personal del complejo).

## Consecuencias

- Nueva dependencia de la `api` (`flyway-core` y `flyway-database-postgresql`); la agrega la primera tarea que cree tablas (PLATODO-30).
- El tiempo de arranque crece con el número de tenants; si se vuelve un problema se mide y se mueve la migración de tenants a un paso de despliegue separado.
- Cambia el despliegue: la nota va en `docs/deployment/changes/` de la tarea que agregue la dependencia.
