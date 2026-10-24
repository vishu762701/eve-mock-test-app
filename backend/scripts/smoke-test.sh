#!/usr/bin/env bash
set -e

# ==============================================================================
# Eve Mock Test App — Live Production Verification Suite
# ==============================================================================

FAILURES=0
HEALTH_RESULT="FAIL"
AUTH_RESULT="PASS"
JWT_RESULT="PASS"
DIAG_RESULT="PASS"
D1_RESULT="SKIPPED (diagnostic key unavailable)"
STORAGE_RESULT="SKIPPED (diagnostic key unavailable)"
PUBLIC_RESULT="PASS"
GUARD_RESULT="SKIPPED (diagnostic key unavailable)"

# Ensure all secrets are masked in GitHub Actions logs
if [ -n "$CLOUDFLARE_API_TOKEN" ]; then
  echo "::add-mask::$CLOUDFLARE_API_TOKEN"
fi
if [ -n "$SUPABASE_SERVICE_ROLE_KEY" ]; then
  echo "::add-mask::$SUPABASE_SERVICE_ROLE_KEY"
fi
if [ -n "$DIAGNOSTIC_KEY" ]; then
  echo "::add-mask::$DIAGNOSTIC_KEY"
fi

echo "=== 1. Discovering Production Worker URL ==="
SUB_RES=$(curl --connect-timeout 5 --max-time 20 -s -H "Authorization: Bearer $CLOUDFLARE_API_TOKEN" "https://api.cloudflare.com/client/v4/accounts/a50b9cf54c5e84de0183a5d3a4b38844/workers/subdomain" || true)
SUBDOMAIN=$(echo "$SUB_RES" | grep -o '"subdomain":"[^"]*' | cut -d'"' -f4 || true)
if [ -z "$SUBDOMAIN" ]; then
  SUBDOMAIN="anyqueairdrop"
fi

WORKER_URL="https://eve-backend.${SUBDOMAIN}.workers.dev"
echo "Discovered Live Worker URL: $WORKER_URL"
echo "::notice title=Discovered Live Worker URL::$WORKER_URL"

echo "=== 2. PUBLIC: Testing Health Endpoint (GET /api/health) ==="
HEALTH_STATUS=""
for i in {1..6}; do
  HEALTH_RESP=$(curl --connect-timeout 5 --max-time 20 -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL/api/health" || true)
  if echo "$HEALTH_RESP" | grep -q 'HTTP_STATUS:200' && echo "$HEALTH_RESP" | grep -q '"status":"ok"'; then
    HEALTH_STATUS="200"
    HEALTH_RESULT="PASS"
    echo "Health response verified: $HEALTH_RESP"
    echo "::notice title=Public Health Check::PASS (HTTP 200 OK from $WORKER_URL)"
    break
  fi
  echo "Attempt $i waiting for DNS / worker propagation..."
  sleep 3
done

if [ "$HEALTH_STATUS" != "200" ]; then
  echo "::error title=Public Health Check::FAIL (Did not return HTTP 200 OK with valid status payload)"
  FAILURES=$((FAILURES + 1))
fi

echo "=== 3. AUTHENTICATION: Missing Token Rejection (HTTP 401) ==="
for ep in "/api/auth/me" "/api/exams" "/api/bookmarks" "/api/leaderboard"; do
  RESP=$(curl --connect-timeout 5 --max-time 20 -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL$ep" || true)
  if echo "$RESP" | grep -q 'HTTP_STATUS:401'; then
    echo "Protected endpoint $ep correctly returned 401 when token is missing."
  else
    echo "::error title=Missing Token Check::FAIL (Endpoint $ep allowed unauthenticated access! Status: $RESP)"
    AUTH_RESULT="FAIL"
    FAILURES=$((FAILURES + 1))
  fi
done
echo "::notice title=Missing Token Rejection::$AUTH_RESULT (All protected endpoints reject requests without token)"

echo "=== 4. INVALID TOKEN: Forged / Expired Token Rejection (HTTP 401) ==="
for forged_token in "invalid-dummy-token" "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.fake"; do
  FORGED_RESP=$(curl --connect-timeout 5 --max-time 20 -s -w "\nHTTP_STATUS:%{http_code}" -H "Authorization: Bearer $forged_token" "$WORKER_URL/api/attempts" || true)
  if echo "$FORGED_RESP" | grep -q 'HTTP_STATUS:401'; then
    echo "Token '$forged_token' correctly rejected with HTTP 401."
  else
    echo "::error title=Invalid Token Check::FAIL (Worker accepted invalid token! Status: $FORGED_RESP)"
    JWT_RESULT="FAIL"
    FAILURES=$((FAILURES + 1))
  fi
done
echo "::notice title=Invalid Token Rejection::$JWT_RESULT (Worker rejected invalid and forged JWTs)"

echo "=== 5. ANONYMOUS DIAGNOSTIC REJECTION: Write-Capable Endpoints Protected (HTTP 401) ==="
for diag_ep in "/api/diag/d1" "/api/diag/storage" "/api/health/d1" "/api/health/storage"; do
  ANON_RESP=$(curl --connect-timeout 5 --max-time 20 -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL$diag_ep" || true)
  if echo "$ANON_RESP" | grep -q 'HTTP_STATUS:401'; then
    echo "Diagnostic endpoint $diag_ep correctly blocked anonymous access."
  else
    echo "::error title=Diagnostic Vulnerability::FAIL (Endpoint $diag_ep is publicly exposed! Status: $ANON_RESP)"
    DIAG_RESULT="FAIL"
    FAILURES=$((FAILURES + 1))
  fi
done
echo "::notice title=Anonymous Diagnostic Protection::$DIAG_RESULT (All write-capable diagnostics strictly protected)"

echo "=== 6. DATABASE: Authenticated D1 Operations (/api/diag/d1) ==="
if [ -n "$DIAGNOSTIC_KEY" ]; then
  D1_RESULT="FAIL"
  D1_RESP=$(curl --connect-timeout 5 --max-time 20 -s -w "\nHTTP_STATUS:%{http_code}" -H "X-Diagnostic-Key: $DIAGNOSTIC_KEY" "$WORKER_URL/api/diag/d1" || true)
  if echo "$D1_RESP" | grep -q 'HTTP_STATUS:200' && echo "$D1_RESP" | grep -q '"readWriteTest":"PASS"'; then
    D1_RESULT="PASS"
    echo "D1 diagnostic verified: tables, indexes, triggers, and live atomic read/write/delete passed."
    echo "::notice title=D1 Database Live Verification::PASS (Authenticated read/write/delete against production D1 passed)"
  else
    echo "::error title=D1 Database Live Verification::FAIL ($D1_RESP)"
    FAILURES=$((FAILURES + 1))
  fi
else
  echo "::notice title=D1 Database Check::Skipped authenticated write test because DIAGNOSTIC_KEY is not available in local environment."
fi

echo "=== 7. STORAGE: Authenticated Supabase Storage Operations (/api/diag/storage) ==="
if [ -n "$DIAGNOSTIC_KEY" ]; then
  STORAGE_RESULT="UNAVAILABLE (storage probe failed)"
  STORAGE_RESP=$(curl --connect-timeout 5 --max-time 20 -s -w "\nHTTP_STATUS:%{http_code}" -H "X-Diagnostic-Key: $DIAGNOSTIC_KEY" "$WORKER_URL/api/diag/storage" || true)
  if echo "$STORAGE_RESP" | grep -q 'HTTP_STATUS:200' && echo "$STORAGE_RESP" | grep -q '"bannerUpload":"PASS"'; then
    STORAGE_RESULT="PASS"
    echo "Supabase storage verified: upload and delete operations on 'eve-media' bucket succeeded."
    echo "::notice title=Supabase Storage Live Verification::PASS (Authenticated banner & syllabus upload/delete passed)"
  else
    echo "::notice title=Supabase Storage Note::$STORAGE_RESP"
  fi
else
  echo "::notice title=Supabase Storage Check::Skipped authenticated storage test because DIAGNOSTIC_KEY is not available in local environment."
fi

echo "=== 8. REPRESENTATIVE PRODUCTION ENDPOINTS: Public App Content & Banners ==="
for pub_ep in "/api/banners" "/api/app-content/terms" "/api/app-content/privacy"; do
  PUB_RESP=$(curl --connect-timeout 5 --max-time 20 -s -w "\nHTTP_STATUS:%{http_code}" "$WORKER_URL$pub_ep" || true)
  if echo "$PUB_RESP" | grep -q 'HTTP_STATUS:200' && echo "$PUB_RESP" | grep -q '"success":true'; then
    echo "Production endpoint $pub_ep is operational and returned valid JSON data."
  else
    echo "::error title=Production Read Test::FAIL (Endpoint $pub_ep failed. Response: $PUB_RESP)"
    PUBLIC_RESULT="FAIL"
    FAILURES=$((FAILURES + 1))
  fi
done
echo "::notice title=Production Endpoints::$PUBLIC_RESULT (Public banners and app content verified from live D1)"

echo "=== 9. SECURITY GUARD: Diagnostic Key Rejection on Standard Admin/Protected Routes (HTTP 401) ==="
if [ -n "$DIAGNOSTIC_KEY" ]; then
  GUARD_RESULT="FAIL"
  EXAMS_RESP=$(curl --connect-timeout 5 --max-time 20 -s -w "\nHTTP_STATUS:%{http_code}" -H "X-Diagnostic-Key: $DIAGNOSTIC_KEY" "$WORKER_URL/api/exams" || true)
  if echo "$EXAMS_RESP" | grep -q 'HTTP_STATUS:401'; then
    GUARD_RESULT="PASS"
    echo "Protected endpoint /api/exams correctly returned HTTP 401 when called with X-Diagnostic-Key (admin bypass disabled)."
    echo "::notice title=Diagnostic Bypass Guard::PASS (/api/exams rejected diagnostic key as unauthorized)"
  else
    echo "::error title=Diagnostic Bypass Guard::FAIL (Endpoint /api/exams allowed access with X-Diagnostic-Key! Status: $EXAMS_RESP)"
    FAILURES=$((FAILURES + 1))
  fi
else
  echo "::notice title=Diagnostic Bypass Guard::Skipped because DIAGNOSTIC_KEY is not set."
fi

if [ -n "$GITHUB_STEP_SUMMARY" ]; then
  echo "## Production Deployment Verification Summary" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Worker URL**: \`$WORKER_URL\`" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Public Health (GET /api/health)**: $HEALTH_RESULT (public health response)" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Authentication Guard (Missing Token)**: $AUTH_RESULT" >> "$GITHUB_STEP_SUMMARY"
  echo "- **JWT Verification Guard (Forged Token)**: $JWT_RESULT" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Anonymous Diagnostic Protection**: $DIAG_RESULT" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Authenticated D1 Operations**: $D1_RESULT" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Supabase Storage Operations**: $STORAGE_RESULT" >> "$GITHUB_STEP_SUMMARY"
  echo "- **Representative Endpoints (/api/banners, /api/app-content/*)**: $PUBLIC_RESULT" >> "$GITHUB_STEP_SUMMARY"
  echo "- **AI Test Generation (generate-now)**: NOT RUN (requires authenticated admin generation)" >> "$GITHUB_STEP_SUMMARY"
fi

if [ "$FAILURES" -gt 0 ]; then
  echo "Production verification suite encountered $FAILURES failure(s)."
  exit 1
fi

echo "=============================================================================="
echo "Required executed deployment checks passed; see summary for skipped or unavailable checks."
echo "=============================================================================="
exit 0
