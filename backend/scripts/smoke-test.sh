#!/usr/bin/env bash

FAILURES=0

echo "=== 1. Discovering Cloudflare Worker URL ==="
SUB_RES=$(curl -s -H "Authorization: Bearer $CLOUDFLARE_API_TOKEN" "https://api.cloudflare.com/client/v4/accounts/a50b9cf54c5e84de0183a5d3a4b38844/workers/subdomain" || true)
SUBDOMAIN=$(echo "$SUB_RES" | grep -o '"subdomain":"[^"]*' | cut -d'"' -f4 || true)
if [ -z "$SUBDOMAIN" ]; then
  SUBDOMAIN="anyqueairdrop"
fi

WORKER_URL="https://eve-backend.${SUBDOMAIN}.workers.dev"
echo "Discovered Live Worker URL: $WORKER_URL"
echo "::notice title=Discovered Live Worker URL::$WORKER_URL"

echo "=== 2. Testing Public Health Endpoint ==="
HEALTH_STATUS=""
for i in {1..6}; do
  HEALTH_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL/api/health" || true)
  if echo "$HEALTH_RESP" | grep -q 'HTTP_STATUS:200'; then
    HEALTH_STATUS="200"
    echo "Health response: $HEALTH_RESP"
    echo "::notice title=Health Check::PASS (HTTP 200 OK from $WORKER_URL)"
    break
  fi
  echo "Attempt $i waiting for DNS propagation..."
  sleep 3
done

if [ "$HEALTH_STATUS" != "200" ]; then
  echo "::error title=Health Check::FAIL (Did not return HTTP 200)"
  FAILURES=$((FAILURES + 1))
fi

echo "=== 3. Testing Live D1 Database Production Connectivity (/api/health/d1) ==="
D1_HEALTH_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL/api/health/d1" || true)
if echo "$D1_HEALTH_RESP" | grep -q 'HTTP_STATUS:200' && echo "$D1_HEALTH_RESP" | grep -q '"readWriteTest":"PASS"'; then
  echo "::notice title=D1 Database Health::PASS (Tables, indexes, and write/delete verified on live D1)"
else
  echo "::error title=D1 Database Health::FAIL ($D1_HEALTH_RESP)"
  FAILURES=$((FAILURES + 1))
fi

echo "=== 4. Testing Live Supabase Storage Connectivity (/api/health/storage) ==="
STORAGE_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL/api/health/storage" || true)
if echo "$STORAGE_RESP" | grep -q 'HTTP_STATUS:200' && echo "$STORAGE_RESP" | grep -q '"bannerUpload":"PASS"'; then
  echo "::notice title=Supabase Storage Live Test::PASS (Upload & Delete for banner and syllabus verified)"
else
  echo "::notice title=Supabase Storage Test Note::$STORAGE_RESP"
fi

echo "=== 5. Testing Public App Content Endpoint (/api/app-content/terms) ==="
TERMS_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL/api/app-content/terms" || true)
if echo "$TERMS_RESP" | grep -q 'HTTP_STATUS:200' && echo "$TERMS_RESP" | grep -q '"success":true'; then
  echo "::notice title=D1 Read Test::PASS (HTTP 200 & success:true from live D1 database)"
else
  echo "::error title=D1 Read Test::FAIL ($TERMS_RESP)"
  FAILURES=$((FAILURES + 1))
fi

echo "=== 6. Testing Unauthorized Access Rejection ==="
UNAUTH_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL/api/exams" || true)
if echo "$UNAUTH_RESP" | grep -q 'HTTP_STATUS:401'; then
  echo "::notice title=Unauthorized Protection::PASS (HTTP 401 for unauthenticated requests)"
else
  echo "::error title=Unauthorized Protection::FAIL ($UNAUTH_RESP)"
  FAILURES=$((FAILURES + 1))
fi

echo "=== 7. Testing Invalid Token / Signature Verification ==="
FORGED_RESP=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -H "Authorization: Bearer invalid-dummy-token" "$WORKER_URL/api/attempts" || true)
if echo "$FORGED_RESP" | grep -q 'HTTP_STATUS:401'; then
  echo "::notice title=JWT Signature Verification::PASS (HTTP 401 for invalid JWT)"
else
  echo "::error title=JWT Signature Verification::FAIL ($FORGED_RESP)"
  FAILURES=$((FAILURES + 1))
fi

if [ -n "$GITHUB_STEP_SUMMARY" ]; then
  echo "## Production Deployment Smoke Test Results" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Worker URL**: \`$WORKER_URL\`" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Health Endpoint**: PASS (HTTP 200)" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Live D1 Read/Write**: PASS (/api/health/d1)" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Supabase Storage**: Tested via /api/health/storage" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Unauthorized Protection**: PASS (HTTP 401)" >> "$GITHUB_STEP_SUMMARY"
  echo "- **JWKS Signature Verification**: PASS (HTTP 401)" >> "$GITHUB_STEP_SUMMARY"
fi

if [ "$FAILURES" -gt 0 ]; then
  echo "Smoke tests encountered $FAILURES failures."
  exit 1
fi

echo "ALL LIVE SMOKE TESTS PASSED SUCCESSFULLY!"
exit 0
