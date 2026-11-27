#!/usr/bin/env bash
#
# Stops the LinID QA stack, deletes its data and the files start.sh generated.
#
#   scripts/qa-environment/stop.sh
#
set -euo pipefail

source "$(dirname "$0")/common.sh"

compose down -v --remove-orphans
clean_generated
echo "==> Stopped"
