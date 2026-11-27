#!/usr/bin/env bash
# Usage: railpack-build.sh <piece-dir> <image> [KEY=VALUE ...]
# Mirrors Dokploy's Railpack build: `railpack prepare` + `docker buildx build` with the Railpack frontend.
set -euo pipefail
RAILPACK="${RAILPACK:-railpack}"
dir=$1; image=$2; shift 2
out="${TMPDIR:-/tmp}/platodo96-build/$(basename "$dir")"
mkdir -p "$out"
env_flags=(); secret_flags=()
for pair in "$@"; do
  env_flags+=(--env "$pair")
  key=${pair%%=*}
  export "$pair"
  secret_flags+=(--secret "id=$key,env=$key")
done
"$RAILPACK" prepare "$dir" --plan-out "$out/plan.json" --info-out "$out/info.json" "${env_flags[@]}"
docker buildx build --progress=plain \
  --build-arg BUILDKIT_SYNTAX=ghcr.io/railwayapp/railpack-frontend:v0.44.0 \
  "${secret_flags[@]}" -f "$out/plan.json" --output "type=docker,name=$image" "$dir"
