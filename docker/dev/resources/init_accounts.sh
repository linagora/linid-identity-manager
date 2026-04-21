#!/bin/sh
set -e

until psql -h db -p 5432 -U "$DATABASE_ADMIN_USER" -d "$DATABASE_NAME" \
-c "SELECT to_regclass('public.accounts');" | grep -q accounts
do
  sleep 2
done

psql -h db -p 5432 -U "$DATABASE_ADMIN_USER" -d "$DATABASE_NAME" -f /init_accounts.sql
