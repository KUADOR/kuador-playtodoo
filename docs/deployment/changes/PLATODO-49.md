# Cambios de despliegue · PLATODO-49

## api

La `api` ahora necesita PostgreSQL para arrancar: abre un pool de conexiones (HikariCP) y aplica las migraciones al iniciar.

| Variable | Qué es | Valor local |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de la base de la `api` | `jdbc:postgresql://localhost:5432/platodo` (por defecto, `api/compose.yaml`) |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `platodo` (por defecto) |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base (**secreto**, por entorno; nunca en el repo) | Sin contraseña en local (trust) |

- Tamaño del pool opcional: `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` (por defecto 10). Debe ser **2 o más**: las migraciones de Flyway al arrancar usan más de una conexión.
- Al arrancar, en este orden y deteniendo el arranque ante el primer error (fail fast):
  1. Migraciones globales (`db/migration/global`): crean el esquema `global` (tabla `tenant`, historial de Flyway) y el esquema vacío `no_tenant`.
  2. Migraciones de cada tenant `ACTIVE`, `SUSPENDED` o `ARCHIVED`, de a uno y cada uno en su esquema `t_<uuid>`.
- El usuario de la base necesita permiso para crear esquemas (`CREATE` sobre la base) y debe poder usar el esquema `extensions` (PLATODO-29).
- Toda conexión devuelta al pool queda con `search_path = no_tenant`. No guardar tablas en `no_tenant`.

## Vuelta atrás

Revertir el PR. Los esquemas `global` y `no_tenant` pueden quedar en la base sin efecto: las versiones anteriores no los usan.
