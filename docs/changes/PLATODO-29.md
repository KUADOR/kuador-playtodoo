# Cambios a documentos compartidos · PLATODO-29

## AGENTS.md

En «Comandos del proyecto», reemplazar el bloque «Levantar en local» por:

```bash
# Levantar en local
cd api && docker compose up -d --wait   # PostgreSQL 18 con el esquema extensions y btree_gist
./mvnw spring-boot:run                  # api (JDK 25)
# [POR DEFINIR: comandos para levantar backoffice, app y movil juntos]
```

Agregar bajo «# api (api/)»: `./mvnw -B verify` necesita Docker en marcha (Testcontainers).

## docs/architecture.md

En «Base de datos», agregar:

- **Esquema compartido de extensiones:** `extensions`, una vez por base de datos, con `btree_gist` (restricción de exclusión de reservas, ADR-006). No contiene datos de complejos. Cada conexión de tenant usa `search_path = <esquema del tenant>, extensions`. Script: `api/docker/postgres/initdb/01-extensions.sql`.
- **Pruebas de persistencia:** contra PostgreSQL 18 real con Testcontainers (D11 de PLATODO-25); se nombran `*IT` y corren en `./mvnw -B verify`.

## README.md

En la tabla «Piezas», nota para `api`: requiere JDK 25 y Docker para `./mvnw -B verify`; PostgreSQL local con `api/compose.yaml` (ver `api/README.md`).
