#!/bin/sh
set -e

APP_ENV="${APP_ENV:-DEV}"

sed "s/__APP_ENV__/${APP_ENV}/g" /usr/share/nginx/html/config.json \
  > /tmp/config.json && mv /tmp/config.json /usr/share/nginx/html/config.json

exec "$@"
