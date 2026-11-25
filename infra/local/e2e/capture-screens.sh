#!/usr/bin/env bash
# Screenshots of one state of the walkthrough, as the clients see it against the real api.
# Called by tenancy-walkthrough.sh through ON_STATE:
#   capture-screens.sh <state> <tenant-host> <expected: active|unavailable|not-found> <complex name>
#
#   CAPTURE_DIR    folder for the PNG files (required):
#                    app/escritorio-<state>.png, app/movil-<state>.png  web of the complex (Chrome)
#                    movil/<state>.png                                  Flutter app (only with MOVIL_DEVICE)
#   CHROME_BIN     Chrome or Chromium binary (default: Google Chrome on macOS)
#   MOVIL_DEVICE   optional Flutter device id (an iOS simulator or Android emulator) to also run
#                  movil/integration_test/tenant_walkthrough_test.dart, which checks the expected screen
set -euo pipefail

readonly STATE="$1"
readonly HOST="$2"
readonly EXPECTED="$3"
readonly DISPLAY_NAME="$4"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_DIR
readonly MOVIL_DIR="$SCRIPT_DIR/../../../movil"
readonly CHROME="${CHROME_BIN:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
# The app checks the complex with GET /tenant after loading; this budget lets that answer arrive.
readonly RENDER_BUDGET_MS=5000
readonly DESKTOP_SIZE="1280,800"
readonly MOBILE_SIZE="390,844"

: "${CAPTURE_DIR:?Set CAPTURE_DIR to the folder for the screenshots}"
OUTPUT_DIR="$(mkdir -p "$CAPTURE_DIR" && cd "$CAPTURE_DIR" && pwd)"
readonly OUTPUT_DIR

capture_app() { # capture_app NAME WIDTH,HEIGHT
  mkdir -p "$OUTPUT_DIR/app"
  "$CHROME" --headless --disable-gpu --hide-scrollbars --no-first-run --no-default-browser-check \
    --user-data-dir="$(mktemp -d)" --virtual-time-budget="$RENDER_BUDGET_MS" \
    --window-size="$2" --screenshot="$OUTPUT_DIR/app/$1-$STATE.png" "http://$HOST/" 2>/dev/null
}

capture_movil() {
  local subdomain="${HOST%%.*}" root="${HOST#*.}"
  (cd "$MOVIL_DIR" && CAPTURE_DIR="$OUTPUT_DIR/movil" flutter drive -d "$MOVIL_DEVICE" \
    --driver=test_driver/integration_test.dart \
    --target=integration_test/tenant_walkthrough_test.dart \
    "--dart-define=PLAYTODOO_TENANT_URL_PATTERN=http://{subdomain}.$root" \
    "--dart-define=E2E_SUBDOMAIN=$subdomain" "--dart-define=E2E_EXPECTED=$EXPECTED" \
    "--dart-define=E2E_DISPLAY_NAME=$DISPLAY_NAME" "--dart-define=E2E_STATE=$STATE" >/dev/null)
}

capture_app escritorio "$DESKTOP_SIZE"
capture_app movil "$MOBILE_SIZE"
echo "app: $OUTPUT_DIR/app/{escritorio,movil}-$STATE.png ($HOST)"
if [[ -n "${MOVIL_DEVICE:-}" ]]; then
  capture_movil
  echo "movil: $OUTPUT_DIR/movil/$STATE.png ($HOST, $EXPECTED)"
fi
