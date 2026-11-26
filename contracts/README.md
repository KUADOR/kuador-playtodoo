# API contracts

OpenAPI 3.1 contracts of the `api`. Clients (`movil`, `app`, `backoffice`) and the backend build against these files.

| Contract | Module | Decision |
|---|---|---|
| `bookings.openapi.yaml` | Bookings | ADR-006 (`docs/adr/ADR-006-prevencion-de-reservas-duplicadas.md`) |
| `tenancy.openapi.yaml` | Tenancy, Identity and access, Audit | ADR-002, ADR-003, ADR-009 to ADR-013 (`docs/adr/`) |
| `platform-backoffice.openapi.yaml` | Plans and subscriptions, Identity and access, Support access, Audit, Tenancy (global host, `backoffice`) | ADR-014 to ADR-020; adjusts ADR-011 and ADR-013 (`docs/adr/`) |

The platform tenant and access-session operations of `tenancy.openapi.yaml` are `deprecated`: `platform-backoffice.openapi.yaml` replaces them (see `x-replaced-by` on each operation).

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

Platform backoffice mock (complexes, plans, subscriptions, capabilities and support sessions):

```bash
cd contracts
npx --yes @stoplight/prism-cli@5 mock -p 4012 platform-backoffice.openapi.yaml

curl http://127.0.0.1:4012/me -H 'Authorization: Bearer dev' -H 'Prefer: example=operator'
curl http://127.0.0.1:4012/complexes/5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c \
  -H 'Authorization: Bearer dev' -H 'Prefer: example=suspended'
curl -X POST http://127.0.0.1:4012/complexes/5f1c2a3b-4d5e-4f6a-8b7c-9d0e1f2a3b4c/status-changes \
  -H 'Authorization: Bearer dev' -H 'Content-Type: application/json' \
  -H 'Prefer: code=422, example=transitionNotAllowed' -d '{"targetStatus":"ACTIVE","version":3}'
```

Support views on the complex host use the staff operations of `bookings.openapi.yaml` with
`X-Support-Session`; `Prefer: example=suspendedSupport` on `GET /staff/bookings` returns entries
with `inactive: true`.

Note: Prism answers invalid request bodies with `422`; the real `api` answers them with `400 INVALID_REQUEST`.
