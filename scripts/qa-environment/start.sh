#!/usr/bin/env bash
#
# Starts the LinID QA stack, under its own compose project, until stop.sh is called. Fresh data every time: the
# certificates, the keys and the configuration are generated, the database is empty but for what the e2e environment
# seeds (docker/init-db: the accounts admin, user1, user2, noaccount, applications, groups...).
#
#   scripts/qa-environment/build.sh     # once, and whenever the code under test changes
#   scripts/qa-environment/start.sh
#
# The application: https://linid.localtest.me:QA_UI_PORT (19320), signing in through LemonLDAP::NG (/auth of the same
# address) as admin / password (also user1, user2, noaccount, same password). Self-signed CA: ignore HTTPS errors.
# The API: https://127.0.0.1:QA_API_PORT (19321), sync-api: https://127.0.0.1:QA_SYNC_PORT (19322, mutual TLS).
#
set -euo pipefail

source "$(dirname "$0")/common.sh"

for i in "$QA_IMAGE_API" "$QA_IMAGE_SYNC" "$QA_IMAGE_UI"; do
  docker image inspect "$i" >/dev/null 2>&1 || { echo "Image $i missing: run scripts/qa-environment/build.sh first" >&2; exit 1; }
done

echo "==> Cleaning a previous run of '$QA_PROJECT'"
compose down -v --remove-orphans >/dev/null 2>&1 || true

echo "==> Generating certificates and configuration"
generate

echo "==> Starting the stack as compose project '$QA_PROJECT'"
compose up -d --quiet-pull >/dev/null 2>&1 || { compose logs --tail 40 >&2; exit 1; }

echo "==> Waiting for the services and the seed"
for i in $(seq 1 90); do
  bad="$(compose ps -a --format '{{.Service}}|{{.State}}|{{.Health}}|{{.ExitCode}}' | awk -F'|' '
    $1 ~ /^init-/ { if ($2 == "exited" && $4 != 0) print $1 " failed"; else if ($2 != "exited") print $1 " running"; next }
    $3 != "healthy" && $1 != "lin-pulsar" { print $1 " " $2 " " $3 }
    $1 == "lin-pulsar" && $2 != "running" { print $1 " " $2 }')"
  [ -z "$bad" ] && break
  if [ "$i" = 90 ] || grep -q ' failed' <<<"$bad"; then
    echo "The stack is not ready: $bad" >&2
    compose ps -a >&2
    compose logs --tail 40 >&2
    exit 1
  fi
  sleep 4
done

curl -fsk -o /dev/null "https://127.0.0.1:$QA_API_PORT/actuator/health" \
  || { echo "The API does not answer on $QA_API_PORT" >&2; compose logs --tail 40 api >&2; exit 1; }

NET="$(docker network ls --filter "label=com.docker.compose.project=$QA_PROJECT" --format '{{.Name}}' | head -1)"
cat <<MSG
==> Ready
  application   $QA_APP_URL   (admin / password; also user1, user2, noaccount)
  API           https://127.0.0.1:$QA_API_PORT   (health: /actuator/health)
  sync-api      https://127.0.0.1:$QA_SYNC_PORT   (mutual TLS)
  LemonLDAP     $QA_APP_URL/auth  (and http://127.0.0.1:$QA_AUTH_PORT, debugging)
  docker network: $NET
  browser in that network: --host-resolver-rules=MAP $QA_HOST:$QA_UI_PORT ui:$QA_UI_PORT  (+ ignore HTTPS errors)
MSG
