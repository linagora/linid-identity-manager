#!/bin/sh
set -e

# Import self-signed certificate into Java truststore
if [ -f /tmp/selfsigned.crt ]; then
  keytool -import -trustcacerts -alias selfsigned \
    -file /tmp/selfsigned.crt \
    -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit -noprompt 2>/dev/null || true
fi

exec java -jar linid-identity-manager-api.jar
