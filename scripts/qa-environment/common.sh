# Shared by build.sh, start.sh and stop.sh. Not meant to be run on its own.
#
# The stack is the docker/e2e one (PostgreSQL, OPA, LemonLDAP::NG as the OIDC provider, lin-pulsar, api, sync-api, ui),
# with the images built from this working tree and the configuration of the e2e environment, rendered here by
# start.sh (what `task setup:e2e` does, without the task binary, Java or envsubst on the host) into
# scripts/qa-environment/generated/, which stop.sh deletes. Superset is left out. Nothing is published but on 127.0.0.1,
# on ports of their own.
#
# Host names. The OIDC login embeds the public address of the application (the issuer, the redirect URIs, the
# cookies): it must be the same for the browser, the UI server and the API, which all call the portal through it.
#   QA_HOST=linid.localtest.me, the name the e2e environment already uses:
#   - on a developer's machine it resolves to 127.0.0.1 on the public DNS, and the UI is published there;
#   - in the container browser of Twake QA, the stack's network gives it to the ui container (a network alias), and
#     --host-resolver-rules=MAP linid.localtest.me:QA_UI_PORT ui:QA_UI_PORT says so to Chromium as well;
#   - inside the stack, the same alias makes the API and the UI server reach the issuer without leaving it.
#   The UI server listens on QA_UI_PORT itself (not 443), so that the address is the same everywhere.
#   *.localhost would also do for a browser, but certificates are https here (cookies are Secure), the browser must
#   ignore the self-signed CA anyway, and localtest.me is what the checked-in templates are written for.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
E2E_DIR="$REPO_DIR/docker/e2e"
GEN_DIR="$SCRIPT_DIR/generated"

export QA_PROJECT="${QA_PROJECT:-linid-qa}"
export QA_UI_PORT="${QA_UI_PORT:-19320}"     # https://QA_HOST:QA_UI_PORT, the application and the portal (/auth)
export QA_API_PORT="${QA_API_PORT:-19321}"   # https://127.0.0.1:QA_API_PORT, the API
export QA_SYNC_PORT="${QA_SYNC_PORT:-19322}" # https://127.0.0.1:QA_SYNC_PORT, sync-api (mutual TLS)
# The portal is served by the UI under /auth: QA_AUTH_PORT only publishes LemonLDAP::NG's own http port, for debugging
export QA_AUTH_PORT="${QA_AUTH_PORT:-19323}"
export QA_HOST="${QA_HOST:-linid.localtest.me}"
export QA_APP_URL="https://$QA_HOST:$QA_UI_PORT"
export QA_GEN_DIR="$GEN_DIR"
export QA_IMAGE_API="linid-qa-api" QA_IMAGE_SYNC="linid-qa-sync-api" QA_IMAGE_UI="linid-qa-ui"
export DOCKER_BUILDKIT=1
# Network of the image builds (Maven, pnpm downloads): the default bridge of BuildKit stalls on some hosts (MTU, IPv6)
QA_BUILD_NETWORK="${QA_BUILD_NETWORK:-host}"

for arg in "$@"; do
  echo "Unknown argument: $arg" >&2
  exit 1
done

compose() {
  docker compose -p "$QA_PROJECT" -f "$SCRIPT_DIR/docker-compose.qa.yml" "$@"
}

# render <template>: the ${VARIABLES} of the template replaced by those of the environment (what envsubst does)
render() {
  perl -pe 's/\$\{(\w+)\}/defined $ENV{$1} ? $ENV{$1} : ""/ge' "$1"
}

# Removes what start.sh generated, whoever owns it (containers write in some of it)
clean_generated() {
  [ -e "$GEN_DIR" ] || return 0
  rm -rf "$GEN_DIR" 2>/dev/null || true
  if [ -e "$GEN_DIR" ]; then
    docker run --rm -v "$GEN_DIR:/g" --entrypoint sh eclipse-temurin:25-jre -c 'rm -rf /g/* /g/.[!.]*' >/dev/null 2>&1 || true
    rm -rf "$GEN_DIR"
  fi
}

# generate: the certificates, the keys and the configuration files, in $GEN_DIR (what the Taskfile's
# setup:certs:e2e, setup:lemonldap-config:e2e and setup:config do, for the QA addresses)
generate() {
  clean_generated
  mkdir -p "$GEN_DIR/certs" "$GEN_DIR/llng/conf" "$GEN_DIR/conf"
  local g="$GEN_DIR/certs" pass=password   # the public test value of docker/e2e/env/cert.env
  ( set -a
    . "$E2E_DIR/env/cert.env"
    pass="$SSL_KEY_PASSWORD"
    cd "$g"
    # The local CA, and the server certificate it signs, shared by all the HTTPS services
    openssl req -x509 -newkey rsa:2048 -keyout ca.key -out ca.crt -days 30 -nodes \
      -subj "/CN=LinID QA CA/O=LinID/C=FR" >/dev/null 2>&1
    openssl req -x509 -newkey rsa:2048 -keyout server.key -out server.crt -days 30 -nodes \
      -subj "/CN=$QA_HOST/OU=Test/O=Test/L=Test/ST=Test/C=FR" \
      -addext "basicConstraints=critical,CA:FALSE" \
      -addext "subjectAltName=DNS:$QA_HOST,DNS:ui,DNS:api,DNS:sync-api,DNS:auth,DNS:lemon,DNS:localhost,IP:127.0.0.1" \
      -CA ca.crt -CAkey ca.key >/dev/null 2>&1
    # OIDC signing keys
    openssl genpkey -algorithm RSA -out oidc.key -pkeyopt rsa_keygen_bits:2048 >/dev/null 2>&1
    openssl pkey -in oidc.key -pubout -out oidc.pub >/dev/null 2>&1
    # API and sync-api keystores, from the server certificate
    openssl pkcs12 -export -in server.crt -inkey server.key -name linid -out api-keystore.p12 -passout pass:"$pass"
    openssl pkcs12 -export -in server.crt -inkey server.key -name linid -out sync-api-keystore.p12 -passout pass:"$pass"
    # Client certificate presented by the API to sync-api (mutual TLS)
    openssl req -x509 -newkey rsa:2048 -keyout api-client.key -out api-client.crt -days 30 -nodes \
      -subj "/CN=linid-api" -addext "basicConstraints=critical,CA:FALSE" -addext "extendedKeyUsage=clientAuth" \
      -CA ca.crt -CAkey ca.key >/dev/null 2>&1
    openssl pkcs12 -export -in api-client.crt -inkey api-client.key -name linid-api -out api-client-keystore.p12 \
      -passout pass:"$pass"
    chmod 644 ./*
  ) || { echo "Certificate generation failed" >&2; return 1; }
  # The truststore trusting the CA: keytool runs in a container
  ( . "$E2E_DIR/env/cert.env"
    docker run --rm --user "$(id -u):$(id -g)" -v "$g:/certs" --entrypoint keytool eclipse-temurin:25-jre \
      -importcert -noprompt -trustcacerts -alias linid-ca -file /certs/ca.crt -keystore /certs/truststore.jks \
      -storepass "$SSL_TRUSTSTORE_PASSWORD" >/dev/null 2>&1 ) || { echo "Truststore generation failed" >&2; return 1; }
  chmod 644 "$g"/*

  # The configuration: the e2e environment's own values, with the addresses of the QA stack
  ( set -a
    . "$E2E_DIR/env/local.env"
    . "$E2E_DIR/env/ui.env"
    LEMONLDAP_PORTAL_HOSTNAME="$QA_HOST" LEMONLDAP_SSO_DOMAIN="$QA_HOST" PORTAL_HOSTNAME="$QA_HOST" SSODOMAIN="$QA_HOST"
    LEMONLDAP_REDIRECT_URL="$QA_APP_URL" LEMONLDAP_PORTAL_URL="$QA_APP_URL/auth"
    OIDC_PRIVATE_KEY="$(awk '{printf "%s\\n", $0}' "$g/oidc.key")"
    OIDC_PUBLIC_KEY="$(awk '{printf "%s\\n", $0}' "$g/oidc.pub")"
    render "$E2E_DIR/resources/lmConf-1.template.json" > "$GEN_DIR/llng/conf/lmConf-1.json"
    render "$E2E_DIR/resources/config.template.json" > "$GEN_DIR/conf/config.json"
    # the UI server listens on the port of the public address, not on 443 (see the top of common.sh)
    render "$E2E_DIR/resources/server-config.template.json" \
      | perl -pe 's/("port":\s*)443\b/${1}'"$QA_UI_PORT"'/' > "$GEN_DIR/conf/server-config.json"
  ) || { echo "Configuration generation failed" >&2; return 1; }
  grep -q "\"port\": $QA_UI_PORT" "$GEN_DIR/conf/server-config.json" || { echo "server-config.json: port not set" >&2; return 1; }
  chmod -R a+rX "$GEN_DIR"
}
