#!/bin/sh
set -e

export PGPASSWORD="$POSTGRES_PASSWORD"

echo "========================================"
echo "Init DB starting"
echo "========================================"

until psql -h "$DATABASE_HOST" -p "$DATABASE_PORT" -U "$POSTGRES_USER" -d "$LINID_DATABASE_NAME" \
-c "SELECT to_regclass('public.accounts');" | grep -q accounts
do
  echo "Waiting for LinID schema..."
  sleep 2
done

echo "========================================"
echo "Init LinID starting"
echo "========================================"
psql -h "$DATABASE_HOST" -p "$DATABASE_PORT" -U "$POSTGRES_USER" -d "$LINID_DATABASE_NAME" -f /app/scripts/init_linid.sql

echo "========================================"
echo "Init LemonLDAP::NG starting"
echo "========================================"
psql -h "$DATABASE_HOST" -p "$DATABASE_PORT" -U "$POSTGRES_USER" -d "$LEMONLDAP_DATABASE_NAME" -f /app/scripts/init_lemon.sql

echo "========================================"
echo "Init DataLake starting"
echo "========================================"
psql -h "$DATABASE_HOST" -p "$DATABASE_PORT" -U "$POSTGRES_USER" -d "$DATALAKE_DATABASE_NAME" -f /app/scripts/init_datalake.sql
