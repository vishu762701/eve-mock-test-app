#!/usr/bin/env bash
set -e

echo "=== 1. Discovering Cloudflare Worker URL ==="
SUB_RES=$(curl -s -H "Authorization: Bearer $CLOUDFLARE_API_TOKEN" "https://api.cloudflare.com/client/v4/accounts/a50b9cf54c5e84de0183a5d3a4b38844/workers/subdomain")
SUB_CLEAN=$(echo "$SUB_RES" | tr -d '\r\n')
echo "::notice title=Subdomain Response::$SUB_CLEAN"

SUBDOMAIN=$(echo "$SUB_RES" | grep -o '"subdomain":"[^"]*' | cut -d'"' -f4 || true)
if [ -z "$SUBDOMAIN" ]; then
  echo "Subdomain not found in response: $SUB_RES"
  exit 1
fi

WORKER_URL="https://eve-backend.${SUBDOMAIN}.workers.dev"
echo "::notice title=Discovered Live Worker URL::$WORKER_URL"

# Enable workers.dev subdomain route for eve-backend if not already enabled
curl -s -X POST -H "Authorization: Bearer $CLOUDFLARE_API_TOKEN" -H "Content-Type: application/json" -d '{"enabled":true}' "https://api.cloudflare.com/client/v4/accounts/a50b9cf54c5e84de0183a5d3a4b38844/workers/services/eve-backend/environments/production/subdomain" || true

echo "=== 2. Testing Public Health Endpoint ==="
# Retry up to 5 times for DNS propagation
for i in {1..5}; do
  HEALTH_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL/api/health" || true)
  if echo "$HEALTH_RESP" | grep -q 'HTTP_STATUS:200'; then
    echo "Health response: $HEALTH_RESP"
    echo "::notice title=Health Check::PASS (HTTP 200 OK from $WORKER_URL)"
    break
  fi
  echo "Attempt $i waiting for DNS propagation..."
  sleep 3
done

echo "=== 3. Testing Unauthorized Access Rejection ==="
UNAUTH_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL/api/exams" || true)
echo "Unauth response: $UNAUTH_RESP"
if echo "$UNAUTH_RESP" | grep -q 'HTTP_STATUS:401'; then
  echo "::notice title=Unauthorized Protection::PASS (HTTP 401 Unauthorized for unauthenticated requests)"
else
  echo "::error title=Unauthorized Protection::FAIL ($UNAUTH_RESP)"
fi

echo "=== 4. Testing D1 Database Schema on Production ==="
D1_TABLES=$(echo "y" | npx wrangler d1 execute eve-database --remote --command "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE '_cf_%' ORDER BY name;" 2>&1)
ESCAPED_TABLES="${D1_TABLES//$'\n'/'%0A'}"
echo "::notice title=Production D1 Tables::$ESCAPED_TABLES"

echo "=== 5. Testing Production D1 Tables Count ==="
D1_COUNT=$(echo "y" | npx wrangler d1 execute eve-database --remote --command "SELECT COUNT(*) as total_tables FROM sqlite_master WHERE type='table' AND name NOT LIKE 'd1_%' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE '_cf_%';" 2>&1)
ESCAPED_COUNT="${D1_COUNT//$'\n'/'%0A'}"
echo "::notice title=D1 User Tables::$ESCAPED_COUNT"

echo "=== 6. Testing Worker Supabase Media Storage Access ==="
if [ -n "$SUPABASE_SERVICE_ROLE_KEY" ]; then
  SUPA_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" "https://msfihtyllpodkfgxxxdd.supabase.co/storage/v1/bucket/eve-media")
  echo "::notice title=Supabase eve-media Bucket Check::HTTP $SUPA_STATUS"
else
  echo "::notice title=Supabase Check::SUPABASE_SERVICE_ROLE_KEY secret not configured in CI (tested separately)"
fi

echo "ALL LIVE SMOKE TESTS COMPLETED!"
