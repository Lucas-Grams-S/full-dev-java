#!/usr/bin/env bash
# Smoke test through the PUBLIC entry point. It must prove four things in seconds:
#  1) the app answers; 2) the security chain is on (no token = 401, not 200);
#  3) errors carry the support id (X-Request-Id, decision 38); 4) the management port is NOT public (decision 138).
# Usage: smoke-test.sh BASE_URL
set -Eeuo pipefail
base="${1:?base url}"
fail=0

status() { curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$@"; }
expect() {   # expect DESCRIPTION EXPECTED_CODE URL
  local got; got="$(status "$3" || true)"
  if [[ "$got" == "$2" ]]; then echo "  ok   $1 -> $got"; else echo "  FAIL $1 -> $got (expected $2)"; fail=1; fi
}

echo "smoke test: $base"
expect "protected API without token" 401 "$base/api/v1/accounts"
expect "management port is not published" 404 "$base/actuator/health"
expect "no internal route" 404 "$base/internal/anything"

rid="$(curl -s -D - -o /dev/null --max-time 5 -H 'X-Request-Id: smoke-test-0001' "$base/api/v1/accounts" | tr -d '\r' | awk -F': ' 'tolower($1)=="x-request-id"{print $2}')"
if [[ "$rid" == "smoke-test-0001" ]]; then echo "  ok   X-Request-Id echoed"; else echo "  FAIL X-Request-Id not echoed (got '${rid}')"; fail=1; fi

exit "$fail"
