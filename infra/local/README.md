# Local environment by subdomain

Runs PostgreSQL and the subdomain proxy in Docker; the api, app and backoffice run on the host with
their usual dev commands. Every complex is reached at `http://{subdomain}.localhost:8000`, the same
host layout as the deployed environments (ADR-002):

| URL | Goes to | Context |
|---|---|---|
| `http://{sub}.localhost:8000/api/**` | api (`:8080`) | Tenant: the api resolves the complex from `Host` |
| `http://{sub}.localhost:8000/**` | app (`:5173`) | Web of the complex |
| `http://api.localhost:8000` | api (`:8080`) | Global (no tenant) |
| `http://admin.localhost:8000` | backoffice (`:4200`) | Kuador team |
| `localhost`, `www`, nested subdomains | proxy | `404` |

Browsers and curl resolve `*.localhost` to the loopback address: no hosts file is needed.
Whether a subdomain is well formed and belongs to a complex is decided by the api (`404 TENANT_NOT_FOUND`).

## Start

```bash
# 1. PostgreSQL (localhost:5432) and the proxy (localhost:8000)
docker compose -f infra/local/compose.yaml up -d --wait

# 2. Each piece in its own terminal. Bind the web dev servers to 127.0.0.1: by default they listen
#    on ::1 only, which the proxy container cannot reach.
(cd api && ./mvnw spring-boot:run)
(cd app && pnpm install && pnpm dev --host 127.0.0.1)
(cd backoffice && npm ci && npm start -- --host 127.0.0.1)

# 3. Check
curl -i http://canchaselsol.localhost:8000/api/v1/tenant
```

On Linux Docker Engine, `host.docker.internal` points to the Docker bridge, not to loopback: start the
dev servers with `--host 0.0.0.0` instead (only on a trusted network).

## Stop

```bash
docker compose -f infra/local/compose.yaml down      # keep data
docker compose -f infra/local/compose.yaml down -v   # delete data
```

This project uses its own volume (`platodo-local_postgres-data`), separate from the one created by
`api/compose.yaml` alone. Use one or the other: both publish port 5432.

## Ports

Override with environment variables: `PROXY_PORT` (8000), `POSTGRES_PORT` (5432), and the upstream
ports `API_PORT` (8080), `APP_PORT` (5173), `BACKOFFICE_PORT` (4200).

## Proxy

Routing rules, trusted proxies and the deployed configuration: [`infra/proxy/README.md`](../proxy/README.md).
