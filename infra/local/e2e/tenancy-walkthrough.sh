#!/usr/bin/env bash
# End-to-end walkthrough of tenancy on the local environment (epic PLATODO-26, task PLATODO-58).
#
# Runs against the real api through the subdomain proxy (infra/local), never against a mock:
#   active, suspended, archived, reactivated and unknown complex; subdomain change; staff of A
#   rejected on B. Prints a Markdown table with every step and exits non-zero if any step fails.
#
# Requirements: the local environment running (infra/local/README.md), curl, jq, openssl, htpasswd
# and docker. Local development only: it seeds a Kuador owner and the staff passwords straight into
# the local PostgreSQL, with a random password that lives only in this process.
#
# Usage:  infra/local/e2e/tenancy-walkthrough.sh [> walkthrough.md]
#   PROXY_PORT   port of the local proxy (default 8000)
#   ON_STATE     optional command run at each screen state as:
#                  $ON_STATE <state> <tenant-host> <active|unavailable|not-found> <complex name>
#                for example infra/local/e2e/capture-screens.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR
readonly COMPOSE_FILE="$SCRIPT_DIR/../compose.yaml"
readonly ROOT_DOMAIN="localhost"
readonly PORT="${PROXY_PORT:-8000}"
readonly GLOBAL_API="http://api.$ROOT_DOMAIN:$PORT/api/v1"
RUN_ID="$(date +%s)"
readonly RUN_ID
readonly SUBDOMAIN_A="e2ea$RUN_ID"
readonly SUBDOMAIN_B="e2eb$RUN_ID"
readonly SUBDOMAIN_A_NEW="e2ea$RUN_ID-nuevo"
readonly SUBDOMAIN_UNKNOWN="e2ex$RUN_ID"
readonly SUBDOMAIN_MALFORMED="ab"
readonly DEFAULT_RETENTION_DAYS=45
readonly NAME_A="Complejo E2E A"

PASSWORD="$(openssl rand -base64 24)"
readonly PASSWORD
PASSWORD_HASH="{bcrypt}$(printf '%s' "$PASSWORD" | htpasswd -niBC 10 "" | tr -d ':\n')"
readonly PASSWORD_HASH

failures=0
step=0
# Set by request(): status code and body of the last answer.
status=""
body=""

tenant_api() { echo "http://$1.$ROOT_DOMAIN:$PORT/api/v1"; }

sql() {
  docker compose -f "$COMPOSE_FILE" exec -T postgres \
    psql -U platodo -d platodo -v ON_ERROR_STOP=1 -qtA -c "$1"
}

# request METHOD URL [TOKEN] [JSON]: the JSON body goes through stdin so it never shows up in `ps`.
request() {
  local method="$1" url="$2" token="${3:-}" json="${4:-}"
  local args=(-s -o /tmp/platodo-e2e-body.$$ -w '%{http_code}' -X "$method" "$url"
    -H 'Accept: application/json, application/problem+json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$json" ]] && args+=(-H 'Content-Type: application/json' --data-binary @-)
  status="$(printf '%s' "$json" | curl "${args[@]}")"
  body="$(cat /tmp/platodo-e2e-body.$$)"
  rm -f /tmp/platodo-e2e-body.$$
}

field() { jq -r "$1 // empty" <<<"$body" 2>/dev/null || true; }

# check DESCRIPTION REQUEST EXPECTED OBTAINED
check() {
  local description="$1" sent="$2" expected="$3" obtained="$4" result="OK"
  step=$((step + 1))
  if [[ "$expected" != "$obtained" ]]; then
    result="FALLA"
    failures=$((failures + 1))
  fi
  echo "| $step | $description | \`$sent\` | $expected | $obtained | $result |"
}

on_state() { # on_state STATE SUBDOMAIN EXPECTED
  [[ -z "${ON_STATE:-}" ]] && return 0
  "$ON_STATE" "$1" "$2.$ROOT_DOMAIN:$PORT" "$3" "$NAME_A" >&2
}

login() { # login URL EMAIL → token in $body
  request POST "$1" "" "$(jq -nc --arg email "$2" --arg password "$PASSWORD" '{email: $email, password: $password}')"
}

register_tenant() { # register_tenant TOKEN NAME SUBDOMAIN OWNER_EMAIL
  request POST "$GLOBAL_API/platform/tenants" "$1" \
    "$(jq -nc --arg n "$2" --arg s "$3" --arg o "$4" '{displayName: $n, subdomain: $s, ownerEmail: $o}')"
}

set_staff_password() { # set_staff_password TENANT_ID EMAIL
  sql "UPDATE t_${1//-/}.staff_user SET password_hash = '$PASSWORD_HASH' WHERE email = '$2'" >/dev/null
}

echo "# Recorrido de extremo a extremo · tenancy (corrida $RUN_ID)"
echo
echo "Entorno local por subdominios (\`*.$ROOT_DOMAIN:$PORT\`), api real. $(date -u +%Y-%m-%dT%H:%M:%SZ)."
echo
echo "| # | Paso | Solicitud | Esperado | Obtenido | Resultado |"
echo "|---|---|---|---|---|---|"

# --- Preparación: dueño de Kuador y dos complejos ---------------------------------------------------
owner_email="e2e-owner-$RUN_ID@example.test"
sql "INSERT INTO global.platform_user (id, email, password_hash, role, status, created_at)
     VALUES (gen_random_uuid(), '$owner_email', '$PASSWORD_HASH', 'PLATFORM_OWNER', 'ACTIVE', now())" >/dev/null
login "$GLOBAL_API/auth/platform/login" "$owner_email"
check "Login del dueño de Kuador (host global)" "POST api/auth/platform/login" 200 "$status"
platform_token="$(field .accessToken)"

staff_a="e2e-a-$RUN_ID@example.test"
staff_b="e2e-b-$RUN_ID@example.test"
register_tenant "$platform_token" "$NAME_A" "$SUBDOMAIN_A" "$staff_a"
check "Alta del complejo A" "POST api/platform/tenants" "201 ACTIVE" "$status $(field .status)"
tenant_a="$(field .id)"
register_tenant "$platform_token" "Complejo E2E B" "$SUBDOMAIN_B" "$staff_b"
check "Alta del complejo B" "POST api/platform/tenants" "201 ACTIVE" "$status $(field .status)"
tenant_b="$(field .id)"
set_staff_password "$tenant_a" "$staff_a"
set_staff_password "$tenant_b" "$staff_b"

# --- Complejo activo --------------------------------------------------------------------------------
request GET "$(tenant_api "$SUBDOMAIN_A")/tenant"
check "Complejo activo" "GET $SUBDOMAIN_A/tenant" "200 $NAME_A" "$status $(field .displayName)"
on_state activo "$SUBDOMAIN_A" active

# --- Personal de A en B -----------------------------------------------------------------------------
login "$(tenant_api "$SUBDOMAIN_A")/auth/staff/login" "$staff_a"
check "Login del personal de A en A" "POST $SUBDOMAIN_A/auth/staff/login" 200 "$status"
staff_a_token="$(field .accessToken)"
login "$(tenant_api "$SUBDOMAIN_B")/auth/staff/login" "$staff_a"
check "Credenciales del personal de A en B" "POST $SUBDOMAIN_B/auth/staff/login" "401 INVALID_CREDENTIALS" "$status $(field .code)"
request PUT "$(tenant_api "$SUBDOMAIN_B")/tenant/subdomain" "$staff_a_token" '{"subdomain":"e2e-intruso"}'
check "Token del personal de A en el host de B" "PUT $SUBDOMAIN_B/tenant/subdomain" "401 UNAUTHENTICATED" "$status $(field .code)"
login "$(tenant_api "$SUBDOMAIN_B")/auth/staff/login" "$staff_b"
check "Login del personal de B en B" "POST $SUBDOMAIN_B/auth/staff/login" 200 "$status"

# --- Suspendido -------------------------------------------------------------------------------------
request POST "$GLOBAL_API/platform/tenants/$tenant_a/suspend" "$platform_token"
check "Suspender A" "POST api/platform/tenants/{A}/suspend" "200 SUSPENDED" "$status $(field .status)"
request GET "$(tenant_api "$SUBDOMAIN_A")/tenant"
check "Complejo suspendido" "GET $SUBDOMAIN_A/tenant" "403 TENANT_UNAVAILABLE" "$status $(field .code)"
login "$(tenant_api "$SUBDOMAIN_A")/auth/staff/login" "$staff_a"
check "Login del personal en A suspendido" "POST $SUBDOMAIN_A/auth/staff/login" "403 TENANT_UNAVAILABLE" "$status $(field .code)"
request GET "$(tenant_api "$SUBDOMAIN_B")/tenant"
check "B sigue activo mientras A está suspendido" "GET $SUBDOMAIN_B/tenant" "200 Complejo E2E B" "$status $(field .displayName)"
on_state suspendido "$SUBDOMAIN_A" unavailable
request POST "$GLOBAL_API/platform/tenants/$tenant_a/activate" "$platform_token"
check "Activar A" "POST api/platform/tenants/{A}/activate" "200 ACTIVE" "$status $(field .status)"

# --- Archivado y reactivado -------------------------------------------------------------------------
request POST "$GLOBAL_API/platform/tenants/$tenant_a/archive" "$platform_token"
check "Archivar A" "POST api/platform/tenants/{A}/archive" "200 ARCHIVED $DEFAULT_RETENTION_DAYS" "$status $(field .status) $(field .retentionDays)"
request GET "$(tenant_api "$SUBDOMAIN_A")/tenant"
check "Complejo archivado" "GET $SUBDOMAIN_A/tenant" "403 TENANT_UNAVAILABLE" "$status $(field .code)"
on_state archivado "$SUBDOMAIN_A" unavailable
request POST "$GLOBAL_API/platform/tenants/$tenant_a/reactivate" "$platform_token"
check "Reactivar A" "POST api/platform/tenants/{A}/reactivate" "200 ACTIVE" "$status $(field .status)"
request GET "$(tenant_api "$SUBDOMAIN_A")/tenant"
check "Complejo reactivado" "GET $SUBDOMAIN_A/tenant" "200 $NAME_A" "$status $(field .displayName)"
login "$(tenant_api "$SUBDOMAIN_A")/auth/staff/login" "$staff_a"
check "El personal de A recupera su acceso" "POST $SUBDOMAIN_A/auth/staff/login" 200 "$status"
staff_a_token="$(field .accessToken)"
on_state reactivado "$SUBDOMAIN_A" active

# --- Inexistente y mal formado ----------------------------------------------------------------------
request GET "$(tenant_api "$SUBDOMAIN_UNKNOWN")/tenant"
check "Subdominio inexistente" "GET $SUBDOMAIN_UNKNOWN/tenant" "404 TENANT_NOT_FOUND" "$status $(field .code)"
on_state inexistente "$SUBDOMAIN_UNKNOWN" not-found
request GET "$(tenant_api "$SUBDOMAIN_MALFORMED")/tenant"
check "Subdominio mal formado" "GET $SUBDOMAIN_MALFORMED/tenant" "404 TENANT_NOT_FOUND" "$status $(field .code)"
on_state mal-formado "$SUBDOMAIN_MALFORMED" not-found

# --- Cambio de subdominio ---------------------------------------------------------------------------
request PUT "$(tenant_api "$SUBDOMAIN_A")/tenant/subdomain" "$staff_a_token" "{\"subdomain\":\"$SUBDOMAIN_A_NEW\"}"
check "El propietario cambia el subdominio de A" "PUT $SUBDOMAIN_A/tenant/subdomain" "200 $SUBDOMAIN_A_NEW" "$status $(field .subdomain)"
request GET "$(tenant_api "$SUBDOMAIN_A_NEW")/tenant"
check "El subdominio nuevo resuelve al mismo complejo" "GET $SUBDOMAIN_A_NEW/tenant" "200 $NAME_A" "$status $(field .displayName)"
on_state subdominio-nuevo "$SUBDOMAIN_A_NEW" active
request GET "$(tenant_api "$SUBDOMAIN_A")/tenant"
check "El subdominio anterior deja de resolver" "GET $SUBDOMAIN_A/tenant" "404 TENANT_NOT_FOUND" "$status $(field .code)"
on_state subdominio-anterior "$SUBDOMAIN_A" not-found
request PUT "$(tenant_api "$SUBDOMAIN_A_NEW")/tenant/subdomain" "$staff_a_token" "{\"subdomain\":\"$SUBDOMAIN_A_NEW\"}"
check "El token del propietario sigue sirviendo en el host nuevo" "PUT $SUBDOMAIN_A_NEW/tenant/subdomain" 200 "$status"
register_tenant "$platform_token" "Complejo E2E C" "$SUBDOMAIN_A" "e2e-c-$RUN_ID@example.test"
check "Otro complejo no puede tomar el subdominio retirado" "POST api/platform/tenants" "409 SUBDOMAIN_TAKEN" "$status $(field .code)"

echo
if ((failures > 0)); then
  echo "**Resultado: $failures de $step pasos fallaron.**"
  exit 1
fi
echo "**Resultado: los $step pasos dieron lo esperado.**"
