# Playtodoo Kuador · api

Pieza **api** del proyecto PLATODO. Stack: Java 25 · Spring Boot 4.

| Compilar | Probar | Levantar |
|---|---|---|
| `./mvnw -B package` | `./mvnw -B verify` | `./mvnw spring-boot:run` |

## Requirements

- JDK 25.
- Docker running: `./mvnw -B verify` starts PostgreSQL with Testcontainers for the integration tests (`*IT`).
  `./mvnw -B package` runs only unit tests and does not need Docker.

## Local PostgreSQL

`compose.yaml` starts PostgreSQL 18 with the shared `extensions` schema and the `btree_gist` extension
(script: `docker/postgres/initdb/01-extensions.sql`). It is for development only: it listens on
`127.0.0.1` and uses trust authentication, so there is no password to configure.

```bash
cd api
docker compose up -d --wait                  # start (POSTGRES_PORT=55432 to use another host port)
docker compose exec postgres psql -U platodo -d platodo \
  -c "SELECT extname, extnamespace::regnamespace FROM pg_extension WHERE extname = 'btree_gist'"
docker compose down                          # stop and keep data
docker compose down -v                       # stop and delete data (init scripts run again on next start)
```

Connection: `jdbc:postgresql://localhost:5432/platodo`, user `platodo`.

## Database extensions

- Extensions live in the shared `extensions` schema, installed once per database. It holds no tenant data.
- `btree_gist` backs the booking no-overlap `EXCLUDE USING gist` constraint (ADR-006).
- Every tenant connection must use `search_path = <tenant_schema>, extensions`; otherwise `gist(uuid)`
  does not resolve and the constraint cannot be created.
- The init script is idempotent. On an existing database (non-production environments), run it as the
  database owner: `psql -d <database> -f docker/postgres/initdb/01-extensions.sql`. `btree_gist` is a
  trusted extension since PostgreSQL 13, so no superuser is needed.
- `BtreeGistExtensionIT` runs the same script in Testcontainers and checks the extension, overlap rejection
  (`SQLSTATE 23P01`), adjacent periods and that two tenant schemas do not conflict.
