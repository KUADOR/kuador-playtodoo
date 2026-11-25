# End-to-end walkthrough · tenancy

Checks the tenancy epic (PLATODO-26) end to end on the local environment, against the real api
through the subdomain proxy. Nothing is mocked.

| Script | What it does |
|---|---|
| `tenancy-walkthrough.sh` | Seeds a Kuador owner, registers complexes A and B, and walks them through: active, staff of A rejected on B, suspended, archived, reactivated, unknown and malformed subdomain, and subdomain change. Prints a Markdown table; exits non-zero if any step fails |
| `capture-screens.sh` | Hook for `ON_STATE`: screenshots of `app` (Chrome, desktop and mobile widths) and, with `MOVIL_DEVICE`, of `movil` on a simulator or emulator, which also asserts the expected screen |

## Run

```bash
# 1. Local environment (infra/local/README.md): PostgreSQL, proxy, api and app running.
# 2. Steps only:
infra/local/e2e/tenancy-walkthrough.sh

# 3. Steps and screenshots (movil needs a booted simulator or emulator: `flutter devices`):
CAPTURE_DIR=docs/evidence/<card> MOVIL_DEVICE=<device id> \
  ON_STATE=infra/local/e2e/capture-screens.sh \
  infra/local/e2e/tenancy-walkthrough.sh > docs/evidence/<card>/recorrido.md
```

Requirements: `curl`, `jq`, `openssl`, `htpasswd` (Apache utils), `docker`, Google Chrome (or
`CHROME_BIN`) and, for `movil`, the Flutter version pinned in `.github/workflows/movil.yml`.

## Data

Local development only. Every run creates its own complexes (`e2ea<run>`, `e2eb<run>`) and accounts
under `example.test`. The password is random, kept in memory and stored only as a BCrypt hash in the
local database; it is never printed. The Kuador owner and the staff passwords are written straight
to the local PostgreSQL because no endpoint creates them yet. Remove the data with
`docker compose -f infra/local/compose.yaml down -v`.
