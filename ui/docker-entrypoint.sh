#!/bin/sh
set -e

APP_ENV="${APP_ENV:-DEV}"

sed "s/__APP_ENV__/${APP_ENV}/g" /usr/share/nginx/html/config.json > /tmp/config.json.tmp
# Use cat to write the content back (avoiding 'mv' which fails with mounted volumes)
cat /tmp/config.json.tmp > /usr/share/nginx/html/config.json
rm -f /tmp/config.json.tmp

exec "$@"
