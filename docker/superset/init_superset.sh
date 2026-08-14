#!/bin/bash
set -e

echo "Waiting 5 seconds before starting..."
sleep 5

# Install authlib and psycopg2
. /app/.venv/bin/activate
pip install --no-cache-dir authlib psycopg2-binary psycopg2

# Create admin user
superset fab create-admin \
    --username $SUPERSET_ADMIN_USER \
    --firstname Superset \
    --lastname $SUPERSET_ADMIN_USER \
    --email $SUPERSET_ADMIN_EMAIL \
    --password $SUPERSET_ADMIN_PASSWORD || true

# Init superset
superset db upgrade
superset init

echo "Configuring guest role for embedded dashboards..."
superset shell < /app/superset/init_guest_role.py

superset fab create-user \
    --username $SUPERSET_BROKER_USERNAME \
    --firstname Broker \
    --lastname Service \
    --email $SUPERSET_BROKER_EMAIL \
    --password $SUPERSET_BROKER_PASSWORD \
    --role GuestTokenIssuer || true

echo "Starting Superset server..."
exec gunicorn \
    --bind 0.0.0.0:8443 \
    --certfile=/app/certs/superset.crt \
    --keyfile=/app/certs/superset.key \
    --workers 5 \
    --worker-class gthread \
    --threads 4 \
    --timeout 200 \
    --access-logfile - \
    --error-logfile - \
    "superset.app:create_app()"
