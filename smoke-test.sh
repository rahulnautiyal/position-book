#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

curl --fail --silent --show-error \
  -X POST "$BASE_URL/api/v1/trade-events" \
  -H 'Content-Type: application/json' \
  -d '{"eventId":1001,"type":"BUY","tradingAccount":"SMOKE","securityId":"SEC1","quantity":100}' >/dev/null

curl --fail --silent --show-error \
  -X POST "$BASE_URL/api/v1/trade-events" \
  -H 'Content-Type: application/json' \
  -d '{"eventId":1002,"type":"SELL","tradingAccount":"SMOKE","securityId":"SEC1","quantity":25}' >/dev/null

POSITION=$(curl --fail --silent --show-error "$BASE_URL/api/v1/positions/SMOKE/SEC1")
echo "$POSITION"
grep -q '"quantity":75' <<< "$POSITION"

echo "Smoke test passed."
