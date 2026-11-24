#!/usr/bin/env bash
# Rolling update of the PayFlow chart by IMAGE DIGEST. Helm 4: --rollback-on-failure (was --atomic).
# Usage: helm-deploy.sh ENVIRONMENT NAMESPACE DIGEST
set -Eeuo pipefail
env_name="${1:?environment (staging|production)}"; ns="${2:?namespace}"; digest="${3:?sha256:...}"
[[ "$digest" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "not a digest: $digest" >&2; exit 1; }
chart="$(dirname "$0")/../../deploy/chart/payflow"

previous="$(helm history payflow -n "$ns" --max 1 -o json 2>/dev/null | jq -r '.[0].revision // "none"')"
echo "previous revision: $previous"

helm upgrade --install payflow "$chart" \
  --namespace "$ns" \
  -f "$chart/values-${env_name}.yaml" \
  --set-string image.digest="$digest" \
  --rollback-on-failure \
  --wait --timeout 5m \
  --history-max 10

helm status payflow -n "$ns" -o json | jq -r '"release \(.name): revision \(.version), status \(.info.status)"'
