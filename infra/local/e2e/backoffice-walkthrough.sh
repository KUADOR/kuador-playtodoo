#!/usr/bin/env bash
# Browser walkthrough of the backoffice against the real api (epic PLATODO-27, task PLATODO-93).
#
# Prepares its own data through the real api (a plan, a complex with a payment) and the local PostgreSQL (a
# SUPER_ADMIN and one booking, which no endpoint creates yet), then runs backoffice-walkthrough.cjs with Playwright.
# Prints a Markdown table and exits non-zero if any step fails.
#
# Requirements: the local environment running (infra/local/README.md) with the backoffice started WITHOUT the mock
# (`npm run start:api`), the api with the staff booking api enabled (PLAYTODOO_BOOKINGS_STAFF_API_ENABLED=true),
# curl, jq, openssl, htpasswd, docker, node and the `playwright` package with its Chromium.
#
# Usage:  CAPTURE_DIR=docs/evidence/<card>/backoffice PLAYWRIGHT_NODE_PATH=<node_modules with playwright> \
#           infra/local/e2e/backoffice-walkthrough.sh
#   PROXY_PORT, COMPOSE_PROJECT as in subscriptions-walkthrough.sh.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR
readonly COMPOSE_FILE="$SCRIPT_DIR/../compose.yaml"
readonly COMPOSE_PROJECT="${COMPOSE_PROJECT:-platodo-local}"
readonly PORT="${PROXY_PORT:-8000}"
readonly PLATFORM="http://api.localhost:$PORT/api/v1/platform"
readonly LOGIN_URL="http://api.localhost:$PORT/api/v1/auth/platform/login"
readonly TEST_CURRENCY="USD"
readonly TEST_PERIODICITY="MONTHLY"
: "${CAPTURE_DIR:?Set CAPTURE_DIR to the folder for the screenshots}"
: "${PLAYWRIGHT_NODE_PATH:?Set PLAYWRIGHT_NODE_PATH to a node_modules folder that has playwright}"
RUN_ID="$(date +%s)"
readonly RUN_ID

PASSWORD="$(openssl rand -base64 24)"
readonly PASSWORD
PASSWORD_HASH="{bcrypt}$(printf '%s' "$PASSWORD" | htpasswd -niBC 10 "" | tr -d ':\n')"
readonly PASSWORD_HASH

sql() {
  docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" exec -T postgres \
    psql -U platodo -d platodo -v ON_ERROR_STOP=1 -qtA -c "$1"
}

api() { # api METHOD PATH [JSON] → body; fails on any non-2xx answer
  local args=(-sf -X "$1" "$PLATFORM$2" -H "Authorization: Bearer $token")
  [[ -n "${3:-}" ]] && args+=(-H 'Content-Type: application/json' --data-binary @-)
  printf '%s' "${3:-}" | curl "${args[@]}"
}

email="e2e-backoffice-$RUN_ID@example.test"
sql "INSERT INTO global.platform_user (id, email, password_hash, role, status, created_at, display_name)
     VALUES (gen_random_uuid(), '$email', '$PASSWORD_HASH', 'SUPER_ADMIN', 'ACTIVE', now(), 'E2E Backoffice')" >/dev/null
sql "UPDATE global.platform_parameter SET value = '[\"$TEST_CURRENCY\"]'::jsonb WHERE key = 'plan.currencies' AND value = '[]'::jsonb" >/dev/null
sql "UPDATE global.platform_parameter SET value = '[\"$TEST_PERIODICITY\"]'::jsonb WHERE key = 'plan.periodicities' AND value = '[]'::jsonb" >/dev/null
token="$(jq -nc --arg e "$email" --arg p "$PASSWORD" '{email: $e, password: $p}' \
  | curl -sf -X POST "$LOGIN_URL" -H 'Content-Type: application/json' --data-binary @- | jq -r .accessToken)"

plan_id="$(api POST /plans "$(jq -nc --arg n "Plan Backoffice E2E $RUN_ID" --arg c "$TEST_CURRENCY" --arg p "$TEST_PERIODICITY" \
  '{name: $n, price: "100.00", currency: $c, periodicity: $p, courtLimit: 4, userLimit: 10}')" | jq -r .id)"
api POST "/plans/$plan_id/publication" >/dev/null
complex_id="$(api POST /complexes "$(jq -nc --arg s "bo$RUN_ID" --arg o "e2e-bo-$RUN_ID@example.test" --arg p "$plan_id" \
  '{displayName: "Complejo Backoffice E2E", subdomain: $s, ownerEmail: $o, planId: $p}')" | jq -r .id)"
api POST "/complexes/$complex_id/payments" "$(jq -nc --arg d "$(date -u +%Y-%m-%d)" '{amount: "100.00", paidOn: $d, method: "TRANSFER"}')" >/dev/null
schema="$(sql "SELECT schema_name FROM global.tenant WHERE id = '$complex_id'")"
# Today at noon in local time, so the support view (P9), which opens on today, lists it.
sql "INSERT INTO $schema.booking (id, court_id, kind, status, period, holder_name, created_by, created_at, confirmed_at)
     VALUES (gen_random_uuid(), gen_random_uuid(), 'RESERVATION', 'CONFIRMED',
             tstzrange('$(date +%Y-%m-%dT12:00:00%z)'::timestamptz, '$(date +%Y-%m-%dT13:00:00%z)'::timestamptz, '[)'),
             'Reserva del recorrido backoffice', gen_random_uuid(), now(), now())" >/dev/null

echo "# Recorrido del backoffice contra la api real (corrida $RUN_ID)"
echo
echo "Backoffice servido sin mock (\`npm run start:api\`) en \`http://admin.localhost:$PORT\`. $(date -u +%Y-%m-%dT%H:%M:%SZ)."
echo
BACKOFFICE_URL="http://admin.localhost:$PORT" LOGIN_EMAIL="$email" LOGIN_PASSWORD="$PASSWORD" COMPLEX_ID="$complex_id" \
  CAPTURE_DIR="$CAPTURE_DIR" NODE_PATH="$PLAYWRIGHT_NODE_PATH" node "$SCRIPT_DIR/backoffice-walkthrough.cjs"
