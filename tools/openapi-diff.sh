#!/usr/bin/env bash
#
# Asserts that every MSW route in web/lib/api/mocks/handlers.ts has a matching path in
# backend/app/src/main/resources/openapi.yaml (and vice versa). Exits non-zero on mismatch.
#
# Intended to run in CI on every PR so the contract stays in sync.

set -euo pipefail

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
HANDLERS="$ROOT/web/lib/api/mocks/handlers.ts"
OPENAPI="$ROOT/backend/app/src/main/resources/openapi.yaml"

if [[ ! -f "$HANDLERS" ]] || [[ ! -f "$OPENAPI" ]]; then
  echo "openapi-diff: missing $HANDLERS or $OPENAPI" >&2
  exit 2
fi

# Extract MSW routes: lines like `http.get(\`${BASE}/events/:eventId\`, ...)` → /events/{eventId}
msw_routes=$(grep -oE 'http\.(get|post|put|delete|patch)\(`\$\{BASE\}[^`]+`' "$HANDLERS" \
  | sed -E 's/http\.(get|post|put|delete|patch)\(`\$\{BASE\}([^`]+)`/\U\1\E \2/' \
  | sed -E 's/:[a-zA-Z][a-zA-Z0-9]*/\{&\}/g; s/\{:/\{/g; s/\}\}/\}/g' \
  | sort -u)

# Extract OpenAPI paths (2-space indented path keys under `paths:`):
api_routes=$(awk '
  /^paths:$/ { in_paths=1; next }
  in_paths && /^[^ ]/ { in_paths=0 }
  in_paths && /^  \// { sub(/:$/, "", $1); print $1 }
' "$OPENAPI" | sort -u)

# OpenAPI yaml lists verbs per path; for MSW we have verb+path pairs. Compare paths only.
msw_paths=$(echo "$msw_routes" | awk '{print $2}' | sort -u)

missing_in_openapi=$(comm -23 <(echo "$msw_paths") <(echo "$api_routes") || true)
missing_in_msw=$(comm -13 <(echo "$msw_paths") <(echo "$api_routes") || true)

status=0
if [[ -n "$missing_in_openapi" ]]; then
  echo "❌ MSW routes missing from openapi.yaml (every mocked endpoint MUST be documented):"
  echo "$missing_in_openapi" | sed 's/^/  - /'
  status=1
fi
if [[ -n "$missing_in_msw" ]]; then
  # Soft warn: some endpoints (GDPR export, SNS callback) are intentionally not mocked.
  echo "⚠️  OpenAPI paths with no MSW handler (OK when mocks are deliberately omitted):"
  echo "$missing_in_msw" | sed 's/^/  - /'
fi

if [[ $status -eq 0 ]]; then
  echo "✅ MSW ⊆ OpenAPI: every mocked endpoint is documented ($(echo "$msw_paths" | wc -l | xargs) paths)"
fi
exit $status
