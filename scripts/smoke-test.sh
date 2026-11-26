#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8081}"
MAX_RETRIES="${MAX_RETRIES:-24}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

echo "[smoke-test] Checking health probe at ${BASE_URL}/actuator/health ..."
for i in $(seq 1 "${MAX_RETRIES}"); do
  if curl -fsS "${BASE_URL}/actuator/health" | grep -q '"status":"UP"'; then
    echo "[smoke-test] Health check PASSED (attempt ${i}/${MAX_RETRIES})."
    break
  fi
  if [ "${i}" -eq "${MAX_RETRIES}" ]; then
    echo "[smoke-test] ERROR: Health check failed after ${MAX_RETRIES} attempts."
    exit 1
  fi
  sleep "${SLEEP_SECONDS}"
done

echo "[smoke-test] Checking deployed version info..."
curl -fsS "${BASE_URL}/actuator/info" | head -c 400
echo ""

echo "[smoke-test] Checking public visitor catalog (/)..."
HTTP_CATALOG=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/")
if [ "${HTTP_CATALOG}" != "200" ]; then
  echo "[smoke-test] ERROR: / returned HTTP ${HTTP_CATALOG}"
  exit 1
fi

echo "[smoke-test] Checking login page (/login)..."
HTTP_LOGIN=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/login")
if [ "${HTTP_LOGIN}" != "200" ]; then
  echo "[smoke-test] ERROR: /login returned HTTP ${HTTP_LOGIN}"
  exit 1
fi

echo "[smoke-test] Checking public REST API (/api/apartments)..."
HTTP_API=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/apartments")
if [ "${HTTP_API}" != "200" ]; then
  echo "[smoke-test] ERROR: /api/apartments returned HTTP ${HTTP_API}"
  exit 1
fi

echo "[smoke-test] ALL SMOKE TESTS PASSED on ${BASE_URL}"
