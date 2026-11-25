# Cambios de despliegue · PLATODO-29

## Dependencias

| Servicio | Versión | Para qué | Cómo se levanta | Datos que guarda |
|---|---|---|---|---|
| PostgreSQL | 18 | `api` (esquema global y un esquema por complejo) | Local: `api/compose.yaml`. CI: Testcontainers. Pruebas y producción: [POR DEFINIR: plataforma] | Volumen `postgres-data` en local; en los entornos, según la plataforma |

- Extensión `btree_gist` (contrib estándar de PostgreSQL) en el esquema compartido `extensions`, una vez por base de datos. La exige la restricción de exclusión de reservas (ADR-006). Sin datos de complejos.
- Script único e idempotente: `api/docker/postgres/initdb/01-extensions.sql`. Lo usan el compose local y la prueba de CI.
- Cada conexión de tenant debe usar `search_path = <esquema del tenant>, extensions`.

## CI/CD

| Workflow | Se activa con | Qué hace |
|---|---|---|
| `.github/workflows/api.yml` | PR o push a develop/main que toque `api/` | JDK 25 (Temurin), `./mvnw -B verify`: pruebas unitarias y de integración con Testcontainers (PostgreSQL 18 + `btree_gist`). Usa el Docker que traen los runners `ubuntu-latest` de GitHub; no requiere configuración extra |

- Sin variables ni secretos nuevos. Testcontainers genera credenciales efímeras por prueba.
- Ramas protegidas: no agregar `api` como check obligatorio (está filtrado por ruta); el obligatorio sigue siendo `ci`.

## Qué hacer al desplegar

- **Local:** `cd api && docker compose up -d --wait` (ver `api/README.md`).
- **Pruebas (develop):** cuando exista la base del entorno, ejecutar una vez, como dueño de la base: `psql -d <base> -f api/docker/postgres/initdb/01-extensions.sql`. Verificar con `SELECT extnamespace::regnamespace FROM pg_extension WHERE extname = 'btree_gist'` → `extensions`. Hacerlo antes de desplegar la migración de `booking` (PLATODO-30).
- **Producción (main):** mismo paso, **solo con aprobación de una persona** y en la ventana del release que incluya PLATODO-30. Este PR no toca producción.
- Si el proveedor administrado no permite `CREATE EXTENSION` al dueño de la base, habilitar `btree_gist` desde su panel o pedirlo al soporte. [POR DEFINIR: proveedor de PostgreSQL por entorno]

## Vuelta atrás

- CI: borrar `.github/workflows/api.yml` en un PR a develop.
- Base de datos: `DROP EXTENSION btree_gist;` y `DROP SCHEMA extensions;` solo si ninguna tabla usa la restricción de exclusión (antes de PLATODO-30). Después de PLATODO-30 no se revierte: la restricción depende de la extensión.
