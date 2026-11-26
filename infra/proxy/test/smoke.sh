#!/usr/bin/env bash
# Smoke test of the subdomain proxy (infra/proxy/Caddyfile.local + routes.caddy).
# Starts the proxy in front of echo upstreams and checks routing, Host forwarding and 404s.
# Usage: infra/proxy/test/smoke.sh   (needs Docker with Compose and curl)
set -euo pipefail

readonly COMPOSE_FILE="$(cd "$(dirname "$0")" && pwd)/compose.yaml"
readonly PORT="${SMOKE_PROXY_PORT:-18000}"
readonly SPOOFED_FORWARDED_HOST="spoofed.localhost"
failures=0

compose() { SMOKE_PROXY_PORT="$PORT" docker compose -f "$COMPOSE_FILE" "$@"; }

cleanup() { compose down --remove-orphans >/dev/null 2>&1 || true; }
trap cleanup EXIT

# request <host> <path>: prints the echoed response body (CRLF stripped), then the HTTP status.
# Sends a spoofed X-Forwarded-Host that the proxy must replace (the client is not a trusted proxy).
request() {
  local host="$1" path="$2"
  curl -sS --max-time 5 --resolve "${host}:${PORT}:127.0.0.1" \
    -H "X-Forwarded-Host: ${SPOOFED_FORWARDED_HOST}" \
    -w '\n%{http_code}' "http://${host}:${PORT}${path}" | tr -d '\r'
}

pass() { echo "ok   $1"; }
fail() { echo "FAIL $1"; failures=$((failures + 1)); }

# expect_routed <host> <path> <upstream>: 200 from <upstream>, with the original Host and X-Forwarded-Host.
expect_routed() {
  local host="$1" path="$2" upstream="$3" response status body
  response="$(request "$host" "$path")"
  status="${response##*$'\n'}"
  body="${response%$'\n'*}"
  local name="${host}${path} -> ${upstream}"
  if [[ "$status" != "200" ]]; then fail "$name (status $status)"; return; fi
  grep -qx "Name: ${upstream}" <<<"$body" || { fail "$name (answered by another upstream)"; return; }
  grep -qx "Host: ${host}:${PORT}" <<<"$body" || { fail "$name (Host not preserved)"; return; }
  grep -qx "X-Forwarded-Host: ${host}:${PORT}" <<<"$body" || { fail "$name (X-Forwarded-Host not set by the proxy)"; return; }
  pass "$name"
}

# expect_not_found <host> <path>: the proxy answers 404 without reaching any upstream.
expect_not_found() {
  local host="$1" path="$2" response status
  response="$(request "$host" "$path")"
  status="${response##*$'\n'}"
  if [[ "$status" == "404" ]]; then pass "${host}${path} -> 404"; else fail "${host}${path} -> 404 (status $status)"; fi
}

compose up -d --wait --quiet-pull

echo "Tenant hosts reach the api (/api) and the app (rest) with their own Host:"
expect_routed canchaselsol.localhost /api/v1/tenant api
expect_routed club-norte-2.localhost /api/v1/tenant api
expect_routed canchaselsol.localhost / app
expect_routed canchaselsol.localhost /apix app

echo "Global hosts:"
expect_routed api.localhost /api/v1/auth/players/login api
expect_routed admin.localhost / backoffice

echo "Malformed single-label hosts reach the api and the app, which answer TENANT_NOT_FOUND:"
expect_routed ab.localhost /api/v1/tenant api
expect_routed ab.localhost / app
expect_routed -club.localhost /api/v1/tenant api
expect_routed club-.localhost /api/v1/tenant api
expect_routed club--norte.localhost /api/v1/tenant api
expect_routed xn--club.localhost /api/v1/tenant api
expect_routed "$(printf 'a%.0s' {1..64}).localhost" /api/v1/tenant api

echo "Hosts that must not reach any upstream:"
expect_not_found localhost /
expect_not_found www.localhost /
expect_not_found nested.club.localhost /api/v1/tenant
expect_not_found canchaselsol.example.com /api/v1/tenant

if (( failures > 0 )); then
  echo "${failures} check(s) failed"
  compose logs proxy | tail -n 50
  exit 1
fi
echo "All proxy checks passed"
