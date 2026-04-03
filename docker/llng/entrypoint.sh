#!/bin/sh
set -e

export OIDC_PRIVATE_KEY=$(awk '{printf "%s\\n", $0}' /etc/keys/oidc.key)
export OIDC_PUBLIC_KEY=$(awk '{printf "%s\\n", $0}' /etc/keys/oidc.pub)

envsubst '${LEMONLDAP_DATABASE_ADMIN_USER} ${LEMONLDAP_DATABASE_ADMIN_PASSWORD} ${LEMONLDAP_DATABASE_USER} ${LEMONLDAP_DATABASE_PASSWORD} ${LEMONLDAP_DATABASE_NAME} ${OIDC_PRIVATE_KEY} ${OIDC_PUBLIC_KEY}' < /var/lib/lemonldap-ng/conf/lmConf-1.json.template > /var/lib/lemonldap-ng/conf/lmConf-1.json

exec dumb-init -- /bin/sh /docker-entrypoint.sh