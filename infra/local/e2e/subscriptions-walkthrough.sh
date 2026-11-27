#!/usr/bin/env bash
# End-to-end walkthrough of subscriptions and the Kuador backoffice (epic PLATODO-27, task PLATODO-93).
#
# Runs against the real api through the subdomain proxy (infra/local), never against a mock, and walks the
# acceptance criteria CA1 to CA19: plans and dynamic plan fields, registration, plan and status changes, payments,
# history, operator capabilities, support access with a reason and its audit, suspension, reactivation,
# cancellation and the automatic purge. Prints a Markdown table (one row per step, tagged with its criteria) and
# exits non-zero if any step fails.
#
# Requirements: the local environment running (infra/local/README.md) with the api started with a short purge
# schedule, for example TENANCY_PURGE_CRON='*/10 * * * * *'; curl, jq, openssl, htpasswd and docker.
#
# Local development only. It writes straight to the local PostgreSQL what no endpoint creates yet: a SUPER_ADMIN,
# an OPERATOR, a player and the staff passwords (random, kept in memory, stored only as BCrypt hashes); test values
# for the plan currencies and periodicities when they are still empty ([POR DEFINIR] in production); and, to stand
# in for the 45 days of retention, the purge date of the cancelled complex moved to the past.
#
# Usage:  infra/local/e2e/subscriptions-walkthrough.sh [> walkthrough.md]
#   PROXY_PORT        port of the local proxy (default 8000)
#   COMPOSE_PROJECT   docker compose project of infra/local (default platodo-local)
#   PURGE_WAIT_SECS   how long to wait for the purge job (default 60)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR
readonly COMPOSE_FILE="$SCRIPT_DIR/../compose.yaml"
readonly COMPOSE_PROJECT="${COMPOSE_PROJECT:-platodo-local}"
readonly ROOT_DOMAIN="localhost"
readonly PORT="${PROXY_PORT:-8000}"
readonly PURGE_WAIT_SECS="${PURGE_WAIT_SECS:-60}"
readonly GLOBAL_API="http://api.$ROOT_DOMAIN:$PORT/api/v1"
readonly PLATFORM="$GLOBAL_API/platform"
readonly SUPPORT_HEADER="X-Support-Session"
readonly TEST_CURRENCY="USD"
readonly TEST_PERIODICITY="MONTHLY"
readonly CONCURRENT_ATTEMPTS=10
RUN_ID="$(date +%s)"
readonly RUN_ID
readonly SUBDOMAIN_A="subsa$RUN_ID"
readonly SUBDOMAIN_B="subsb$RUN_ID"

PASSWORD="$(openssl rand -base64 24)"
readonly PASSWORD
PASSWORD_HASH="{bcrypt}$(printf '%s' "$PASSWORD" | htpasswd -niBC 10 "" | tr -d ':\n')"
readonly PASSWORD_HASH
WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/platodo-subscriptions-e2e.XXXXXX")"
readonly WORK_DIR
trap 'rm -rf "$WORK_DIR"' EXIT

failures=0
step=0
# Set by request(): status code and body of the last answer.
status=""
body=""

tenant_api() { echo "http://$1.$ROOT_DOMAIN:$PORT/api/v1"; }

sql() {
  docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" exec -T postgres \
    psql -U platodo -d platodo -v ON_ERROR_STOP=1 -qtA -c "$1"
}

# request METHOD URL [TOKEN] [JSON] [SUPPORT_SESSION]: the JSON body goes through stdin so it never shows up in `ps`.
request() {
  local method="$1" url="$2" token="${3:-}" json="${4:-}" session="${5:-}"
  local args=(-s -o "$WORK_DIR/body" -w '%{http_code}' -X "$method" "$url"
    -H 'Accept: application/json, application/problem+json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$session" ]] && args+=(-H "$SUPPORT_HEADER: $session")
  [[ -n "$json" ]] && args+=(-H 'Content-Type: application/json' --data-binary @-)
  status="$(printf '%s' "$json" | curl "${args[@]}")"
  body="$(cat "$WORK_DIR/body")"
}

field() { jq -r "$1 // empty" <<<"$body" 2>/dev/null || true; }

# check CRITERIA DESCRIPTION REQUEST EXPECTED OBTAINED
check() {
  local criteria="$1" description="$2" sent="$3" expected="$4" obtained="$5" result="OK"
  step=$((step + 1))
  if [[ "$expected" != "$obtained" ]]; then
    result="FALLA"
    failures=$((failures + 1))
  fi
  echo "| $step | $criteria | $description | \`$sent\` | $expected | $obtained | $result |"
}

login() { # login URL EMAIL → token in $body
  request POST "$1" "" "$(jq -nc --arg email "$2" --arg password "$PASSWORD" '{email: $email, password: $password}')"
}

seed_platform_user() { # seed_platform_user EMAIL ROLE NAME
  sql "INSERT INTO global.platform_user (id, email, password_hash, role, status, created_at, display_name)
       VALUES (gen_random_uuid(), '$1', '$PASSWORD_HASH', '$2', 'ACTIVE', now(), '$3')" >/dev/null
}

seed_empty_plan_option() { # seed_empty_plan_option KEY VALUE: test value only while the parameter is still empty
  sql "UPDATE global.platform_parameter SET value = '[\"$2\"]'::jsonb WHERE key = '$1' AND value = '[]'::jsonb" >/dev/null
}

schema_of() { sql "SELECT schema_name FROM global.tenant WHERE id = '$1'"; }

schema_exists() { sql "SELECT count(*) FROM pg_namespace WHERE nspname = '$1'"; }

set_staff_password() { # set_staff_password SCHEMA EMAIL
  sql "UPDATE $1.staff_user SET password_hash = '$PASSWORD_HASH' WHERE email = '$2'" >/dev/null
}

catalog_visibility() { sql "SELECT coalesce((SELECT visible::text FROM global.complex_catalog WHERE tenant_id = '$1'), 'sin fila')"; }

plan_json() { # plan_json NAME
  jq -nc --arg n "$1" --arg c "$TEST_CURRENCY" --arg p "$TEST_PERIODICITY" \
    '{name: $n, price: "100.00", currency: $c, periodicity: $p, courtLimit: 4, userLimit: 10}'
}

register_complex() { # register_complex TOKEN NAME SUBDOMAIN OWNER_EMAIL PLAN_ID
  request POST "$PLATFORM/complexes" "$1" \
    "$(jq -nc --arg n "$2" --arg s "$3" --arg o "$4" --arg p "$5" '{displayName: $n, subdomain: $s, ownerEmail: $o, planId: $p}')"
}

complex_version() { # complex_version TOKEN COMPLEX_ID
  request GET "$PLATFORM/complexes/$2" "$1"
  field .version
}

change_status() { # change_status TOKEN COMPLEX_ID TARGET
  local version
  version="$(complex_version "$1" "$2")"
  request POST "$PLATFORM/complexes/$2/status-changes" "$1" \
    "$(jq -nc --arg t "$3" --argjson v "$version" '{targetStatus: $t, version: $v}')"
}

set_capability() { # set_capability CAPABILITY true|false
  local version
  request GET "$PLATFORM/operator-capabilities" "$super_token"
  version="$(field .version)"
  request PUT "$PLATFORM/operator-capabilities" "$super_token" \
    "$(jq -nc --argjson v "$version" --arg k "$1" --argjson e "$2" '{version: $v, changes: [{key: $k, enabled: $e}]}')"
}

record_payment() { # record_payment TOKEN COMPLEX_ID
  request POST "$PLATFORM/complexes/$2/payments" "$1" \
    "$(jq -nc --arg d "$(date -u +%Y-%m-%d)" '{amount: "100.00", paidOn: $d, method: "TRANSFER"}')"
}

open_support() { # open_support TOKEN COMPLEX_ID REASON
  request POST "$PLATFORM/complexes/$2/support-sessions" "$1" "$(jq -nc --arg r "$3" '{reason: $r}')"
}

seed_booking() { # seed_booking SCHEMA COURT_ID START HOLDER → booking id
  # No endpoint creates bookings yet: the Courts module, which knows the courts and their slots, does not exist, so
  # every court answers as not found (UnconfiguredCourtSlotRules). The booking goes straight to the tenant schema.
  sql "INSERT INTO $1.booking (id, court_id, kind, status, period, holder_name, created_by, created_at, confirmed_at)
       VALUES (gen_random_uuid(), '$2', 'RESERVATION', 'CONFIRMED',
               tstzrange('$3'::timestamptz, '$3'::timestamptz + interval '1 hour', '[)'),
               '$4', gen_random_uuid(), now(), now())
       RETURNING id"
}

booking_status() { sql "SELECT status FROM $1.booking WHERE id = '$2'"; } # booking_status SCHEMA BOOKING_ID

bookings_window() { # query string of a window that holds every booking of this run
  echo "from=$(date -u +%Y-%m-%dT00:00:00Z)&to=$(date -u -v+30d +%Y-%m-%dT00:00:00Z 2>/dev/null || date -u -d '+30 days' +%Y-%m-%dT00:00:00Z)"
}

future_start() { # future_start DAYS HOUR: a whole hour some days ahead, in UTC
  date -u -v+"$1"d +%Y-%m-%dT"$2":00:00Z 2>/dev/null || date -u -d "+$1 days" +%Y-%m-%dT"$2":00:00Z
}

echo "# Recorrido de extremo a extremo · suscripciones y backoffice (corrida $RUN_ID)"
echo
echo "Entorno local por subdominios (\`*.$ROOT_DOMAIN:$PORT\`), api real, sin mock. $(date -u +%Y-%m-%dT%H:%M:%SZ)."
echo
echo "| # | CA | Paso | Solicitud | Esperado | Obtenido | Resultado |"
echo "|---|---|---|---|---|---|---|"

# --- Preparación: equipo Kuador, jugador y opciones de plan de prueba ---------------------------------------------
super_email="e2e-super-$RUN_ID@example.test"
operator_email="e2e-operator-$RUN_ID@example.test"
player_email="e2e-player-$RUN_ID@example.test"
seed_platform_user "$super_email" SUPER_ADMIN "E2E Super $RUN_ID"
seed_platform_user "$operator_email" OPERATOR "E2E Operador $RUN_ID"
sql "INSERT INTO global.player_account (id, email, password_hash, display_name, status, created_at)
     VALUES (gen_random_uuid(), '$player_email', '$PASSWORD_HASH', 'E2E Jugador', 'ACTIVE', now())" >/dev/null
seed_empty_plan_option plan.currencies "$TEST_CURRENCY"
seed_empty_plan_option plan.periodicities "$TEST_PERIODICITY"

login "$GLOBAL_API/auth/platform/login" "$super_email"
check "—" "Login del SUPER_ADMIN" "POST api/auth/platform/login" 200 "$status"
super_token="$(field .accessToken)"
login "$GLOBAL_API/auth/platform/login" "$operator_email"
check "—" "Login del OPERATOR" "POST api/auth/platform/login" 200 "$status"
operator_token="$(field .accessToken)"
request GET "$PLATFORM/me" "$operator_token"
check "CA15" "El OPERATOR empieza sin funciones" "GET api/platform/me" "200 OPERATOR 0" "$status $(field .role) $(field '.capabilities | length')"

# --- CA4: campos de plan dinámicos ----------------------------------------------------------------------------------
request POST "$PLATFORM/plan-fields" "$super_token" "$(jq -nc --arg l "Soporte prioritario $RUN_ID" '{label: $l, type: "BOOLEAN", required: false}')"
check "CA4" "Crear un campo de plan sin cambio de código" "POST api/platform/plan-fields" "201 CUSTOM" "$status $(field .origin)"
field_id="$(field .id)"
request PUT "$PLATFORM/plan-fields/$field_id" "$super_token" \
  "$(jq -nc --arg l "Soporte 24/7 $RUN_ID" --argjson v "$(field .version)" '{label: $l, required: false, version: $v}')"
check "CA4" "Editar el campo" "PUT api/platform/plan-fields/{id}" "200 Soporte 24/7 $RUN_ID" "$status $(field .label)"
request POST "$PLATFORM/plan-fields/$field_id/retirement" "$super_token"
check "CA4" "Retirar el campo" "POST api/platform/plan-fields/{id}/retirement" "200 retirado" "$status $([[ -n "$(field .retiredAt)" ]] && echo retirado)"
basic_field="$(curl -s -H "Authorization: Bearer $super_token" "$PLATFORM/plan-fields" | jq -r '[.[] | select(.origin == "BASIC")][0].id')"
request POST "$PLATFORM/plan-fields/$basic_field/retirement" "$super_token"
check "CA4" "Un campo básico no se retira" "POST api/platform/plan-fields/{básico}/retirement" "422 BASIC_FIELD_READ_ONLY" "$status $(field .code)"

# --- CA3: solo un plan publicado se asigna --------------------------------------------------------------------------
request POST "$PLATFORM/plans" "$super_token" "$(plan_json "Plan E2E Base $RUN_ID")"
check "CA3" "Crear plan (borrador)" "POST api/platform/plans" "201 DRAFT" "$status $(field .status)"
plan_base="$(field .id)"
register_complex "$super_token" "Complejo Suscripción A" "$SUBDOMAIN_A" "e2e-a-$RUN_ID@example.test" "$plan_base"
check "CA3" "Un plan no publicado no se asigna" "POST api/platform/complexes" "422 PLAN_NOT_PUBLISHED" "$status $(field .code)"
request POST "$PLATFORM/plans/$plan_base/publication" "$super_token"
check "CA3" "Publicar el plan" "POST api/platform/plans/{id}/publication" "200 PUBLISHED" "$status $(field .status)"
request POST "$PLATFORM/plans" "$super_token" "$(plan_json "Plan E2E Pro $RUN_ID")"
plan_pro="$(field .id)"
request POST "$PLATFORM/plans/$plan_pro/publication" "$super_token"
check "CA3" "Publicar un segundo plan" "POST api/platform/plans/{id}/publication" "200 PUBLISHED" "$status $(field .status)"

# --- CA1, CA2: alta de complejos ------------------------------------------------------------------------------------
staff_a="e2e-a-$RUN_ID@example.test"
staff_b="e2e-b-$RUN_ID@example.test"
register_complex "$super_token" "Complejo Suscripción A" "$SUBDOMAIN_A" "$staff_a" "$plan_base"
check "CA1" "Alta del complejo A" "POST api/platform/complexes" "201 ACTIVE ACTIVE" "$status $(field .tenantStatus) $(field .subscription.status)"
complex_a="$(field .id)"
register_complex "$super_token" "Complejo Suscripción B" "$SUBDOMAIN_B" "$staff_b" "$plan_base"
check "CA1" "Alta del complejo B" "POST api/platform/complexes" "201 ACTIVE ACTIVE" "$status $(field .tenantStatus) $(field .subscription.status)"
complex_b="$(field .id)"
schema_a="$(schema_of "$complex_a")"
schema_b="$(schema_of "$complex_b")"
check "CA1" "Cada complejo tiene su esquema" "SQL pg_namespace" "1 1" "$(schema_exists "$schema_a") $(schema_exists "$schema_b")"
request GET "$(tenant_api "$SUBDOMAIN_A")/tenant"
check "CA1" "A responde en su subdominio" "GET $SUBDOMAIN_A/tenant" "200 Complejo Suscripción A" "$status $(field .displayName)"
tenants_before="$(sql "SELECT count(*) FROM global.tenant")"
register_complex "$super_token" "Complejo Repetido" "$SUBDOMAIN_A" "e2e-r-$RUN_ID@example.test" "$plan_base"
check "CA2" "Subdominio repetido" "POST api/platform/complexes" "409 SUBDOMAIN_TAKEN" "$status $(field .code)"
check "CA2" "El rechazo no crea nada" "SQL count(global.tenant)" "$tenants_before" "$(sql "SELECT count(*) FROM global.tenant")"
request GET "$GLOBAL_API/subdomains/$SUBDOMAIN_A/availability" "$super_token"
check "CA2" "La disponibilidad lo marca en uso" "GET api/subdomains/{sub}/availability" "200 false TAKEN" "$status $(field '.available | tostring') $(field .reason)"

# --- CA5, CA6, CA7, CA19: plan, estado, pagos, historial y listado ---------------------------------------------------
version="$(complex_version "$super_token" "$complex_a")"
request PUT "$PLATFORM/complexes/$complex_a/plan" "$super_token" "$(jq -nc --arg p "$plan_pro" --argjson v "$version" '{planId: $p, version: $v}')"
check "CA5" "Cambio de plan de A" "PUT api/platform/complexes/{A}/plan" "200 Plan E2E Pro $RUN_ID" "$status $(field .subscription.plan.name)"
change_status "$super_token" "$complex_a" EXPIRING
check "CA6" "Cambio manual a «por vencer»" "POST api/platform/complexes/{A}/status-changes" "200 EXPIRING ACTIVE" "$status $(field .subscription.status) $(field .tenantStatus)"
check "CA6" "Queda el responsable del cambio" "GET api/platform/complexes/{A}" "E2E Super $RUN_ID" "$(field .subscription.statusChangedBy.displayName)"
record_payment "$super_token" "$complex_a"
check "CA7" "Registrar pago de A" "POST api/platform/complexes/{A}/payments" "201 100.00 TRANSFER" "$status $(field .amount) $(field .method)"
request GET "$PLATFORM/complexes/$complex_a/payments" "$super_token"
check "CA7" "El pago está en el historial de pagos" "GET api/platform/complexes/{A}/payments" "200 1" "$status $(field '.items | length')"
request GET "$PLATFORM/complexes/$complex_a/history" "$super_token"
check "CA5" "Historial con el cambio de plan (anterior y nuevo)" "GET api/platform/complexes/{A}/history" \
  "Plan E2E Base $RUN_ID → Plan E2E Pro $RUN_ID" \
  "$(jq -r '[.items[] | select(.kind == "PLAN")][0] | "\(.before) → \(.after)"' <<<"$body")"
check "CA6" "Historial con el cambio de estado y su responsable" "GET api/platform/complexes/{A}/history" \
  "ACTIVE → EXPIRING E2E Super $RUN_ID" \
  "$(jq -r '[.items[] | select(.kind == "STATUS")][0] | "\(.before) → \(.after) \(.changedBy.displayName)"' <<<"$body")"
request GET "$PLATFORM/complexes?size=100" "$super_token"
check "CA19" "Listado con plan, estado y último pago de A" "GET api/platform/complexes" \
  "Plan E2E Pro $RUN_ID EXPIRING 100.00" \
  "$(jq -r --arg id "$complex_a" '.items[] | select(.id == $id) | "\(.plan.name) \(.subscriptionStatus) \(.lastPayment.amount)"' <<<"$body")"

# --- CA15, CA16: funciones del OPERATOR -----------------------------------------------------------------------------
request GET "$PLATFORM/complexes" "$operator_token"
check "CA15" "El OPERATOR lee el listado" "GET api/platform/complexes" 200 "$status"
record_payment "$operator_token" "$complex_a"
check "CA15" "OPERATOR sin la función no registra pagos" "POST api/platform/complexes/{A}/payments" 403 "$status"
register_complex "$operator_token" "Complejo del Operador" "subso$RUN_ID" "e2e-o-$RUN_ID@example.test" "$plan_base"
check "CA15" "OPERATOR sin la función no da de alta" "POST api/platform/complexes" 403 "$status"
open_support "$operator_token" "$complex_a" "Revisión de reservas pedida por el complejo"
check "CA15" "OPERATOR sin la función no accede a datos" "POST api/platform/complexes/{A}/support-sessions" 403 "$status"
version="$(complex_version "$operator_token" "$complex_a")"
request PUT "$PLATFORM/complexes/$complex_a/plan" "$operator_token" "$(jq -nc --arg p "$plan_base" --argjson v "$version" '{planId: $p, version: $v}')"
check "CA15" "El cambio de plan es solo del SUPER_ADMIN" "PUT api/platform/complexes/{A}/plan" 403 "$status"
request PUT "$PLATFORM/operator-capabilities" "$operator_token" '{"version":0,"changes":[]}'
check "CA16" "El OPERATOR no configura funciones" "PUT api/platform/operator-capabilities" 403 "$status"
set_capability SUBSCRIPTION_PAYMENT_RECORD true
check "CA16" "SUPER_ADMIN habilita «registrar pagos»" "PUT api/platform/operator-capabilities" "200 true" \
  "$status $(jq -r '.items[] | select(.key == "SUBSCRIPTION_PAYMENT_RECORD") | .enabled' <<<"$body")"
record_payment "$operator_token" "$complex_a"
check "CA16" "Aplica: el OPERATOR registra el pago" "POST api/platform/complexes/{A}/payments" 201 "$status"
register_complex "$operator_token" "Complejo del Operador" "subso$RUN_ID" "e2e-o-$RUN_ID@example.test" "$plan_base"
check "CA15" "Lo no habilitado se sigue denegando" "POST api/platform/complexes" 403 "$status"
set_capability SUBSCRIPTION_PAYMENT_RECORD false
record_payment "$operator_token" "$complex_a"
check "CA16" "Retirada la función, se deniega en la siguiente solicitud" "POST api/platform/complexes/{A}/payments" "403 CAPABILITY_REVOKED" "$status $(field .code)"
request GET "$PLATFORM/operator-capabilities/changes" "$super_token"
check "CA16" "Los cambios quedan registrados con responsable" "GET api/platform/operator-capabilities/changes" \
  "2 E2E Super $RUN_ID" \
  "$(jq -r --arg who "E2E Super $RUN_ID" '[.items[] | select(.key == "SUBSCRIPTION_PAYMENT_RECORD" and .changedBy.displayName == $who)] | "\(length) \(.[0].changedBy.displayName)"' <<<"$body")"

# --- CA18: el personal de un complejo no entra al backoffice ni a otro complejo ------------------------------------
set_staff_password "$schema_a" "$staff_a"
set_staff_password "$schema_b" "$staff_b"
login "$(tenant_api "$SUBDOMAIN_A")/auth/staff/login" "$staff_a"
check "CA18" "Login del personal de A en A" "POST $SUBDOMAIN_A/auth/staff/login" 200 "$status"
staff_a_token="$(field .accessToken)"
request GET "$PLATFORM/complexes" "$staff_a_token"
check "CA18" "El token del personal no sirve en el backoffice" "GET api/platform/complexes" 401 "$status"
request GET "$PLATFORM/me" "$staff_a_token"
check "CA18" "Ni para identificarse como Kuador" "GET api/platform/me" 401 "$status"
login "$(tenant_api "$SUBDOMAIN_B")/auth/staff/login" "$staff_a"
check "CA18" "Credenciales del personal de A en B" "POST $SUBDOMAIN_B/auth/staff/login" "401 INVALID_CREDENTIALS" "$status $(field .code)"
request GET "$(tenant_api "$SUBDOMAIN_B")/staff/bookings?$(bookings_window)" "$staff_a_token"
check "CA18" "Token del personal de A en el host de B" "GET $SUBDOMAIN_B/staff/bookings" 401 "$status"

# --- CA12, CA13, CA14: acceso de soporte en un complejo activo ----------------------------------------------------
court_a="$(uuidgen | tr '[:upper:]' '[:lower:]')"
booking_kept="$(seed_booking "$schema_a" "$court_a" "$(future_start 3 18)" "Reserva futura E2E")"
booking_support="$(seed_booking "$schema_a" "$court_a" "$(future_start 3 20)" "Reserva a cancelar por soporte")"
booking_race="$(seed_booking "$schema_a" "$court_a" "$(future_start 4 20)" "Reserva de la carrera")"
booking_suspended="$(seed_booking "$schema_a" "$court_a" "$(future_start 5 20)" "Reserva a cancelar en suspendido")"
request GET "$(tenant_api "$SUBDOMAIN_A")/staff/bookings?$(bookings_window)" "$super_token"
check "CA12" "Sin sesión de soporte no se muestran datos" "GET $SUBDOMAIN_A/staff/bookings" "403 SUPPORT_SESSION_INVALID sin datos" \
  "$status $(field .code) $(jq -r 'if type == "array" then "con datos" else "sin datos" end' <<<"$body")"
open_support "$super_token" "$complex_a" "   "
check "CA12" "Sin motivo se deniega" "POST api/platform/complexes/{A}/support-sessions" "422 REASON_REQUIRED" "$status $(field .code)"
open_support "$super_token" "$complex_a" "El complejo pidió revisar una reserva duplicada"
check "CA13" "Con motivo se abre la sesión" "POST api/platform/complexes/{A}/support-sessions" "201 $SUBDOMAIN_A.$ROOT_DOMAIN ACTIVE" \
  "$status $(field .tenantHost) $(field .tenantStatus)"
session_a="$(field .sessionId)"
request GET "$(tenant_api "$SUBDOMAIN_A")/staff/bookings?$(bookings_window)" "$super_token" "" "$session_a"
check "CA13" "Lectura de soporte en A" "GET $SUBDOMAIN_A/staff/bookings + $SUPPORT_HEADER" "200 4 false" \
  "$status $(jq -r '"\(length) \(any(.[]; .inactive))"' <<<"$body")"
request POST "$(tenant_api "$SUBDOMAIN_A")/staff/bookings/$booking_support/cancel" "$super_token" "" "$session_a"
check "CA13" "Edición de soporte en A (cancelar reserva)" "POST $SUBDOMAIN_A/staff/bookings/{id}/cancel + $SUPPORT_HEADER" "200 CANCELLED" "$status $(field .status)"
request GET "$(tenant_api "$SUBDOMAIN_B")/staff/bookings?$(bookings_window)" "$super_token" "" "$session_a"
check "CA14" "La sesión de A no abre B" "GET $SUBDOMAIN_B/staff/bookings + sesión de A" "403 SUPPORT_SESSION_INVALID" "$status $(field .code)"
request POST "$(tenant_api "$SUBDOMAIN_B")/staff/bookings/$booking_kept/cancel" "$super_token" "" "$session_a"
check "CA14" "Ni permite editar en B" "POST $SUBDOMAIN_B/staff/bookings/{id de A}/cancel + sesión de A" "403 SUPPORT_SESSION_INVALID" "$status $(field .code)"
check "CA14" "La reserva de A sigue intacta y B no tiene reservas" "SQL $schema_a / $schema_b" "CONFIRMED 0" \
  "$(booking_status "$schema_a" "$booking_kept") $(sql "SELECT count(*) FROM $schema_b.booking")"
request GET "$PLATFORM/complexes/$complex_a/audit-entries?size=100" "$super_token"
check "CA13" "Auditoría de A: persona, motivo, operación y resultado" "GET api/platform/complexes/{A}/audit-entries" \
  "E2E Super $RUN_ID · El complejo pidió revisar una reserva duplicada · ALLOWED" \
  "$(jq -r '[.items[] | select(.operationId == "cancelBookingByStaff")][0] | "\(.actor.displayName) · \(.reason) · \(.result)"' <<<"$body")"
check "CA13" "La auditoría registra también lo denegado" "GET api/platform/complexes/{A}/audit-entries" "sí" \
  "$(jq -r 'if ([.items[] | select(.result != "ALLOWED")] | length) > 0 then "sí" else "no" end' <<<"$body")"
request GET "$PLATFORM/complexes/$complex_b/audit-entries?size=100" "$super_token"
check "CA14" "La auditoría de B no muestra la sesión de A" "GET api/platform/complexes/{B}/audit-entries" 0 \
  "$(jq -r --arg s "$session_a" '[.items[] | select(.sessionId == $s)] | length' <<<"$body")"
request GET "$PLATFORM/complexes/$complex_a/audit-entries" "$operator_token"
check "CA15" "La auditoría es solo del SUPER_ADMIN" "GET api/platform/complexes/{A}/audit-entries" 403 "$status"
request DELETE "$PLATFORM/support-sessions/$session_a" "$super_token"
check "CA13" "Cerrar la sesión" "DELETE api/platform/support-sessions/{id}" 204 "$status"
request GET "$(tenant_api "$SUBDOMAIN_A")/staff/bookings?$(bookings_window)" "$super_token" "" "$session_a"
check "CA12" "Una sesión cerrada ya no muestra datos" "GET $SUBDOMAIN_A/staff/bookings + sesión cerrada" "403 SUPPORT_SESSION_INVALID" "$status $(field .code)"

# --- CA17: ediciones concurrentes de una reserva bajo soporte ------------------------------------------------------
open_support "$super_token" "$complex_a" "Prueba de concurrencia de reservas"
session_c1="$(field .sessionId)"
open_support "$super_token" "$complex_a" "Segunda sesión de soporte en paralelo"
session_c2="$(field .sessionId)"
login "$(tenant_api "$SUBDOMAIN_A")/auth/staff/login" "$staff_a"
staff_a_token="$(field .accessToken)"
for i in $(seq 1 "$CONCURRENT_ATTEMPTS"); do
  case $((i % 3)) in
    0) printf '%s|\n' "$staff_a_token" ;;
    1) printf '%s|%s\n' "$super_token" "$session_c1" ;;
    2) printf '%s|%s\n' "$super_token" "$session_c2" ;;
  esac
done > "$WORK_DIR/race-callers"
race_url="$(tenant_api "$SUBDOMAIN_A")/staff/bookings/$booking_race/cancel"
while IFS='|' read -r token session; do
  (
    args=(-s -o /dev/null -w '%{http_code}\n' -X POST "$race_url" -H "Authorization: Bearer $token")
    [[ -n "$session" ]] && args+=(-H "$SUPPORT_HEADER: $session")
    curl "${args[@]}" >>"$WORK_DIR/race"
  ) &
done < "$WORK_DIR/race-callers"
wait
server_errors="$(grep -c '^5' "$WORK_DIR/race" || true)"
answered="$(grep -cE '^(200|409)$' "$WORK_DIR/race" || true)"
check "CA17" "$CONCURRENT_ATTEMPTS cancelaciones simultáneas de una reserva (personal y 2 sesiones de soporte)" \
  "POST $SUBDOMAIN_A/staff/bookings/{id}/cancel ×$CONCURRENT_ATTEMPTS" "$CONCURRENT_ATTEMPTS respondidas, 0 errores" \
  "$answered respondidas, $server_errors errores"
check "CA17" "La reserva queda cancelada una sola vez" "SQL $schema_a.booking" "CANCELLED 1" \
  "$(sql "SELECT status || ' ' || count(*) OVER () FROM $schema_a.booking WHERE id = '$booking_race'")"

# --- CA8, CA9, CA11: suspensión ---------------------------------------------------------------------------------------
login "$GLOBAL_API/auth/players/login" "$player_email"
player_token="$(field .accessToken)"
set_capability TENANT_DATA_ACCESS true
change_status "$super_token" "$complex_a" SUSPENDED
check "CA8" "Suspender A" "POST api/platform/complexes/{A}/status-changes" "200 SUSPENDED SUSPENDED" \
  "$status $(field .subscription.status) $(field .tenantStatus)"
check "CA9" "Corre el plazo de conservación (45 días)" "GET api/platform/complexes/{A}" "45 sí" \
  "$(field .retentionDays) $([[ -n "$(field .purgeDueAt)" ]] && echo sí)"
login "$(tenant_api "$SUBDOMAIN_A")/auth/staff/login" "$staff_a"
check "CA8" "El personal no entra a A suspendido" "POST $SUBDOMAIN_A/auth/staff/login" "403 TENANT_UNAVAILABLE" "$status $(field .code)"
request GET "$(tenant_api "$SUBDOMAIN_A")/staff/bookings?$(bookings_window)" "$staff_a_token"
check "CA8" "Ni con un token emitido antes" "GET $SUBDOMAIN_A/staff/bookings" "403 TENANT_UNAVAILABLE" "$status $(field .code)"
request POST "$(tenant_api "$SUBDOMAIN_A")/bookings" "$player_token" "$(jq -nc --arg c "$court_a" --arg s "$(future_start 6 18)" '{courtId: $c, start: $s}')"
check "CA8" "El jugador no reserva en A suspendido" "POST $SUBDOMAIN_A/bookings" "403 TENANT_UNAVAILABLE" "$status $(field .code)"
check "CA8" "A no se muestra en el catálogo de cercanos" "SQL global.complex_catalog.visible" false "$(catalog_visibility "$complex_a")"
check "CA8" "B sigue visible" "SQL global.complex_catalog.visible" true "$(catalog_visibility "$complex_b")"
open_support "$operator_token" "$complex_a" "Consulta del operador"
check "CA11" "El OPERATOR habilitado no entra a un suspendido" "POST api/platform/complexes/{A}/support-sessions" "403 TENANT_NOT_ACCESSIBLE" "$status $(field .code)"
open_support "$super_token" "$complex_a" "Soporte al complejo suspendido por pedido del dueño"
check "CA11" "El SUPER_ADMIN entra con motivo" "POST api/platform/complexes/{A}/support-sessions" "201 SUSPENDED" "$status $(field .tenantStatus)"
session_s="$(field .sessionId)"
request GET "$(tenant_api "$SUBDOMAIN_A")/staff/bookings?$(bookings_window)" "$super_token" "" "$session_s"
check "CA9" "Las reservas futuras se conservan, inactivas" "GET $SUBDOMAIN_A/staff/bookings + $SUPPORT_HEADER" "200 2 true" \
  "$status $(jq -r '[.[] | select(.status == "CONFIRMED")] | "\(length) \(all(.inactive))"' <<<"$body")"
request POST "$(tenant_api "$SUBDOMAIN_A")/staff/bookings/$booking_suspended/cancel" "$super_token" "" "$session_s"
check "CA11" "El SUPER_ADMIN edita en un suspendido (cancelar)" "POST $SUBDOMAIN_A/staff/bookings/{id}/cancel + $SUPPORT_HEADER" "200 CANCELLED true" \
  "$status $(field .status) $(field .inactive)"

# --- Reactivación ------------------------------------------------------------------------------------------------------
change_status "$super_token" "$complex_a" ACTIVE
check "CA9" "Reactivar A antes del plazo" "POST api/platform/complexes/{A}/status-changes" "200 ACTIVE" "$status $(field .tenantStatus)"
login "$(tenant_api "$SUBDOMAIN_A")/auth/staff/login" "$staff_a"
check "CA9" "El personal recupera el acceso" "POST $SUBDOMAIN_A/auth/staff/login" 200 "$status"
staff_a_token="$(field .accessToken)"
request GET "$(tenant_api "$SUBDOMAIN_A")/staff/bookings?$(bookings_window)" "$staff_a_token"
check "CA9" "La reserva conservada vuelve intacta y activa" "GET $SUBDOMAIN_A/staff/bookings" "200 CONFIRMED false" \
  "$status $(jq -r --arg id "$booking_kept" '.[] | select(.id == $id) | "\(.status) \(.inactive)"' <<<"$body")"
check "CA9" "A vuelve al catálogo" "SQL global.complex_catalog.visible" true "$(catalog_visibility "$complex_a")"

# --- CA10: cancelación y purga al vencer el plazo -----------------------------------------------------------------------
record_payment "$super_token" "$complex_a" >/dev/null
change_status "$super_token" "$complex_a" CANCELLED
check "CA10" "Cancelar A" "POST api/platform/complexes/{A}/status-changes" "200 CANCELLED ARCHIVED" "$status $(field .subscription.status) $(field .tenantStatus)"
change_status "$super_token" "$complex_a" ACTIVE
check "CA10" "Una cancelada no se reactiva" "POST api/platform/complexes/{A}/status-changes" "422 TRANSITION_NOT_ALLOWED" "$status $(field .code)"
request GET "$(tenant_api "$SUBDOMAIN_A")/tenant"
check "CA8" "Cancelada bloquea igual que suspendida" "GET $SUBDOMAIN_A/tenant" "403 TENANT_UNAVAILABLE" "$status $(field .code)"
# Stand-in for the 45 days of retention: the purge date of A moves to the past (local database only).
sql "UPDATE global.tenant SET purge_due_at = now() - interval '1 minute' WHERE id = '$complex_a'" >/dev/null
for _ in $(seq 1 "$PURGE_WAIT_SECS"); do
  [[ "$(sql "SELECT status FROM global.tenant WHERE id = '$complex_a'")" == "PURGED" ]] && break
  sleep 1
done
request GET "$PLATFORM/complexes/$complex_a" "$super_token"
check "CA10" "Vencido el plazo, la tarea programada purga A" "GET api/platform/complexes/{A}" "200 PURGED sí" \
  "$status $(field .tenantStatus) $([[ -n "$(field .purgedAt)" ]] && echo sí)"
check "CA10" "El esquema de A ya no existe" "SQL pg_namespace" 0 "$(schema_exists "$schema_a")"
check "CA10" "A sale del catálogo" "SQL global.complex_catalog" "sin fila" "$(catalog_visibility "$complex_a")"
check "CA10" "Se redacta el correo del propietario" "SQL global.tenant.owner_email" "redactado" \
  "$(sql "SELECT CASE WHEN owner_email IS NULL OR owner_email <> '$staff_a' THEN 'redactado' ELSE owner_email END FROM global.tenant WHERE id = '$complex_a'")"
request GET "$(tenant_api "$SUBDOMAIN_A")/tenant"
check "CA10" "El subdominio de A ya no da acceso" "GET $SUBDOMAIN_A/tenant" "404 TENANT_NOT_FOUND" "$status $(field .code)"
check "CA14" "La purga deja intacto el esquema de B" "SQL pg_namespace" 1 "$(schema_exists "$schema_b")"
request GET "$(tenant_api "$SUBDOMAIN_B")/tenant"
check "CA14" "B sigue activo tras la purga de A" "GET $SUBDOMAIN_B/tenant" "200 Complejo Suscripción B" "$status $(field .displayName)"
request GET "$PLATFORM/complexes/$complex_a/payments" "$super_token"
check "CA10" "Se conservan los pagos de Kuador" "GET api/platform/complexes/{A}/payments" "200 3" "$status $(field '.items | length')"
request GET "$PLATFORM/complexes/$complex_a/history" "$super_token"
check "CA5" "Se conserva el historial de plan y estado" "GET api/platform/complexes/{A}/history" "200 sí" \
  "$status $([[ "$(field '.items | length')" -ge 5 ]] && echo sí)"
request GET "$PLATFORM/complexes/$complex_a/audit-entries?size=100" "$super_token"
check "CA13" "Se conserva la auditoría sin ningún motivo legible" "GET api/platform/complexes/{A}/audit-entries" "200 sí 0" \
  "$status $([[ "$(field '.items | length')" -gt 0 ]] && echo sí) $(jq -r '[.items[] | select(.reason != null)] | length' <<<"$body")"

set_capability TENANT_DATA_ACCESS false >/dev/null

echo
if ((failures > 0)); then
  echo "**Resultado: $failures de $step pasos fallaron.**"
  exit 1
fi
echo "**Resultado: los $step pasos dieron lo esperado.**"
