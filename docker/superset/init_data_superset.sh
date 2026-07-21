#!/bin/sh
set -e

echo "Waiting for Superset to be ready..."

until curl -sk "$SUPERSET_URL/health" | grep -q "OK"; do
    echo "Superset not ready yet..."
    sleep 5
done

echo "Superset ready"

echo "Authenticating..."

LOGIN_RESPONSE=$(
  curl -sk \
    -X POST "$SUPERSET_URL/api/v1/security/login" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\":\"$SUPERSET_ADMIN_USER\",
      \"password\":\"$SUPERSET_ADMIN_PASSWORD\",
      \"provider\":\"db\",
      \"refresh\":true
    }"
)

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.access_token')

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
    echo "Authentication failed"
    exit 1
fi

echo "Authenticated"

COOKIE_JAR=/tmp/superset.cookies
rm -f "$COOKIE_JAR"

CSRF_RESPONSE=$(
  curl -sk -i \
    -b "$COOKIE_JAR" \
    -c "$COOKIE_JAR" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    "$SUPERSET_URL/api/v1/security/csrf_token/"
)

CSRF_TOKEN=$(echo "$CSRF_RESPONSE" | tail -n1 | jq -r '.result' 2>/dev/null)

if [ -z "$CSRF_TOKEN" ] || [ "$CSRF_TOKEN" = "null" ]; then
    echo "Unable to retrieve CSRF token"
    exit 1
fi

cd /app/superset
ZIP_FILE="dashboard_export.zip"


echo "Importing assets..."

HTTP_CODE=$(
  curl -sk -o /tmp/import_response.txt -w "%{http_code}" \
    -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
    -X POST "$SUPERSET_URL/api/v1/dashboard/import/" \
    -H "Referer: $SUPERSET_URL" \
    -H "Accept: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "X-CSRFToken: $CSRF_TOKEN" \
    -F "formData=@$ZIP_FILE" \
    -F "passwords={\"databases/PostgreSQL.yaml\": \"$DATALAKE_DATABASE_PASSWORD\"}"
)

if [ "$HTTP_CODE" != "200" ]; then
    echo "Import FAILED (HTTP $HTTP_CODE)"
    exit 1
fi

echo "Import completed successfully"

echo "Fetching dashboard id for slug '$DASHBOARD_SLUG'..."

DASHBOARD_RESPONSE=$(
  curl -sk \
    -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    "$SUPERSET_URL/api/v1/dashboard/?q=(filters:!((col:slug,opr:eq,value:'$DASHBOARD_SLUG')))"
)

DASHBOARD_ID=$(echo "$DASHBOARD_RESPONSE" | jq -r '.result[0].id')

if [ -z "$DASHBOARD_ID" ] || [ "$DASHBOARD_ID" = "null" ]; then
    echo "Unable to find dashboard with slug '$DASHBOARD_SLUG'"
    exit 1
fi

echo "Enabling embedding for dashboard $DASHBOARD_ID..."

EMBED_RESPONSE=$(
  curl -sk \
    -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
    -X POST "$SUPERSET_URL/api/v1/dashboard/$DASHBOARD_ID/embedded" \
    -H "Referer: $SUPERSET_URL" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "X-CSRFToken: $CSRF_TOKEN" \
    -d "{\"allowed_domains\": [\"$APPLICATION_URL\"]}"
)

EMBED_UUID=$(echo "$EMBED_RESPONSE" | jq -r '.result.uuid')

if [ -z "$EMBED_UUID" ] || [ "$EMBED_UUID" = "null" ]; then
    echo "Unable to enable embedding for dashboard '$DASHBOARD_SLUG'"
    exit 1
fi

echo "Embedding enabled. UUID: $EMBED_UUID"
