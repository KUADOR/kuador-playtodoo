# End-to-end walkthroughs

Check the epics end to end on the local environment, against the real api through the subdomain
proxy. Nothing is mocked.

| Script | What it does |
|---|---|
| `tenancy-walkthrough.sh` | Seeds a Kuador owner, registers complexes A and B, and walks them through: active, staff of A rejected on B, suspended, archived, reactivated, unknown and malformed subdomain, and subdomain change. Prints a Markdown table; exits non-zero if any step fails |
| `subscriptions-walkthrough.sh` | Subscriptions and the Kuador backoffice (PLATODO-27): plans and plan fields, registration, plan and status changes, payments, history, operator capabilities, support access with a reason and its audit, suspension, reactivation, cancellation and the automatic purge. One row per step, tagged with its acceptance criterion (CA1 to CA19) |
| `backoffice-walkthrough.sh` | Drives the backoffice built without the mock (`npm run start:api`) in Chromium through login, complexes, payments, history, support access, support view, audit, plans, plan fields and operator permissions; screenshots at desktop and mobile width; fails on any network, CORS or 5xx error. Uses `backoffice-walkthrough.cjs` |
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

### Subscriptions and backoffice (PLATODO-27)

```bash
# 1. Local environment (infra/local/README.md). Start the api with a short purge schedule and the staff and player
#    booking apis on, and the backoffice without the mock:
(cd api && TENANCY_PURGE_CRON='*/10 * * * * *' PLAYTODOO_BOOKINGS_STAFF_API_ENABLED=true \
  PLAYTODOO_BOOKINGS_PLAYER_API_ENABLED=true ./mvnw spring-boot:run)
(cd backoffice && npm run start:api -- --host 127.0.0.1)
# 2. Api walkthrough (CA1 to CA19):
infra/local/e2e/subscriptions-walkthrough.sh > docs/evidence/<card>/recorrido-api.md
# 3. Backoffice in the browser (needs the playwright package and its Chromium):
CAPTURE_DIR=docs/evidence/<card>/backoffice PLAYWRIGHT_NODE_PATH=<node_modules with playwright> \
  infra/local/e2e/backoffice-walkthrough.sh > docs/evidence/<card>/recorrido-backoffice.md
```

With other ports or another compose project, set `PROXY_PORT` and `COMPOSE_PROJECT` (and start the api with
`PLAYTODOO_CORS_BACKOFFICE_ORIGIN=http://admin.localhost:<PROXY_PORT>`). No court module exists yet, so every court
answers as not found: the walkthroughs write their bookings straight to the tenant schema, and creating bookings
under support (and its race) is covered by `SupportConcurrencyIT` in `./mvnw -B verify`.

`tenancy-walkthrough.sh` (PLATODO-58) no longer runs: it seeds the retired `PLATFORM_OWNER` role and calls the
lifecycle endpoints that ADR-014 replaced with the subscription status changes, which `subscriptions-walkthrough.sh`
now covers.

Requirements: `curl`, `jq`, `openssl`, `htpasswd` (Apache utils), `docker`, Google Chrome (or
`CHROME_BIN`) and, for `movil`, the Flutter version pinned in `.github/workflows/movil.yml`.

## Data

Local development only. Every run creates its own complexes (`e2ea<run>`, `e2eb<run>`) and accounts
under `example.test`. The password is random, kept in memory and stored only as a BCrypt hash in the
local database; it is never printed. The Kuador owner and the staff passwords are written straight
to the local PostgreSQL because no endpoint creates them yet. The subscriptions walkthroughs also seed a
SUPER_ADMIN, an OPERATOR and a player the same way, test values for the plan currencies and periodicities while they
are still empty (`[POR DEFINIR]` for real environments), and move the purge date of the cancelled complex to the
past to stand in for the 45 days of retention. Remove the data with
`docker compose -f infra/local/compose.yaml down -v`.
