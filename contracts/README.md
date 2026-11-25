# API contracts

OpenAPI 3.1 contracts of the `api`. Clients (`movil`, `app`, `backoffice`) and the backend build against these files.

| Contract | Module | Decision |
|---|---|---|
| `bookings.openapi.yaml` | Bookings | ADR-006 (`docs/adr/ADR-006-prevencion-de-reservas-duplicadas.md`) |
| `tenancy.openapi.yaml` | Tenancy, Identity and access, Audit | ADR-002, ADR-003, ADR-009 to ADR-013 (`docs/adr/`) |

Tenant-host operations (`{subdomain}.playtodoo.com/api/v1`) of every contract also answer `404 TENANT_NOT_FOUND` and `403 TENANT_UNAVAILABLE`, as described in `tenancy.openapi.yaml`.

## Lint

```bash
cd contracts
npx --yes @redocly/cli@2.83.0 lint *.openapi.yaml
```

Rules live in `redocly.yaml`. CI runs the same command (`.github/workflows/contracts.yml`).

## Mock

```bash
cd contracts
npx --yes @stoplight/prism-cli@5 mock -p 4010 bookings.openapi.yaml
```

Send any bearer token (`Authorization: Bearer dev`). Pick a specific response with the `Prefer` header, for example:

```bash
curl -X POST http://127.0.0.1:4010/bookings/0b8f3c1e-2d4a-4b6c-8e0f-1a2b3c4d5e6f/confirm \
  -H 'Authorization: Bearer dev' -H 'Prefer: code=409, example=expired'
```

Tenancy mock (tenant and global operations on the same port, because Prism ignores the host):

```bash
cd contracts
npx --yes @stoplight/prism-cli@5 mock -p 4011 tenancy.openapi.yaml

# Screens of the complex web and movil
curl http://127.0.0.1:4011/tenant                                  # 200 ACTIVE
curl http://127.0.0.1:4011/tenant -H 'Prefer: code=403'            # TENANT_UNAVAILABLE
curl http://127.0.0.1:4011/tenant -H 'Prefer: code=404'            # TENANT_NOT_FOUND

# Subdomain rules
curl http://127.0.0.1:4011/subdomains/admin/availability -H 'Prefer: example=reserved'
```

Note: Prism answers invalid request bodies with `422`; the real `api` answers them with `400 INVALID_REQUEST`.
