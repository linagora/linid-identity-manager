#!/usr/bin/env bash
#
# Builds the images of the working tree: api, sync-api and ui, in containers (Maven and pnpm run there, with a cache
# that survives between builds). Run it once, and again whenever the code under test changes.
#
#   scripts/qa-environment/build.sh
#
set -euo pipefail

source "$(dirname "$0")/common.sh"
cd "$REPO_DIR"

for m in api sync-api; do
  case "$m" in api) img="$QA_IMAGE_API" ;; *) img="$QA_IMAGE_SYNC" ;; esac
  echo "==> Building $img"
  docker build -q --network "$QA_BUILD_NETWORK" -f "$SCRIPT_DIR/Dockerfile.java" -t "$img" "$m/" >/dev/null
done
echo "==> Building $QA_IMAGE_UI"
docker build -q --network "$QA_BUILD_NETWORK" -f "$SCRIPT_DIR/Dockerfile.ui" -t "$QA_IMAGE_UI" ui/ >/dev/null
echo "==> Built: $QA_IMAGE_API, $QA_IMAGE_SYNC, $QA_IMAGE_UI"
