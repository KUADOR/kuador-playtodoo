# Local rehearsal of the Dokploy manual (PLATODO-96)

Files used to rehearse `docs/despliegue-dokploy-develop.md` on a workstation, without a VPS. They are a record of
what was run, not a supported environment: day-to-day local work uses `infra/local`.

| File | What it is |
|---|---|
| `railpack-build.sh` | Builds one piece the way Dokploy does: `railpack prepare` plus `docker buildx build` with the Railpack frontend |
| `compose.yaml` | PostgreSQL 18, the three built images and Traefik v3.6 on one network, with the variables of section 4 of the manual |
| `domains.yml` | Traefik routers equivalent to the Dokploy domains of sections 5.4 to 5.6 |
| `check.cjs` | Opens `app` and `backoffice` in Chrome with the real hostnames mapped to the local Traefik |

## Run

```bash
# 1. Build (Railpack 0.44.0 on PATH or in $RAILPACK)
./railpack-build.sh ../../../../api platodo96-api RAILPACK_JDK_VERSION=25 \
  "RAILPACK_BUILD_CMD=./mvnw -B -DskipTests -DskipITs clean package"
./railpack-build.sh ../../../../app platodo96-app RAILPACK_NODE_VERSION=22
./railpack-build.sh ../../../../backoffice platodo96-backoffice RAILPACK_NODE_VERSION=22 \
  RAILPACK_SPA_OUTPUT_DIR=dist/backoffice/browser

# 2. Secrets of the rehearsal, generated on the spot and never committed
printf 'DB_PASSWORD=%s\nSIGNING_KEY=%s\n' "$(openssl rand -hex 16)" "$(openssl rand -base64 32)" > .env
mkdir -p dynamic && cp domains.yml dynamic/

# 3. Database first (section 5.2 of the manual), then the rest
docker compose up -d --wait postgres
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < ../../../../api/docker/postgres/initdb/01-extensions.sql
docker compose --profile apps up -d

# 4. Checks of section 10 of the manual, for example
curl -sk --resolve api.develop.play.kuador.com:41001:127.0.0.1 \
  https://api.develop.play.kuador.com:41001/api/v1/subdomains/www/availability

# 5. Stop and delete the data
docker compose --profile apps down -v
```

Traefik serves its self-signed default certificate on `127.0.0.1:41001` and redirects `127.0.0.1:41000` to HTTPS.
`PLAYTODOO_BOOKINGS_STAFF_API_ENABLED=true` is set only to check data isolation between two complexes.
