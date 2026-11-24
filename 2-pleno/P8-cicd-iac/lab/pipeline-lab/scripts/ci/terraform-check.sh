#!/usr/bin/env bash
# validate every root module and run `terraform test` in every module that has tests/.
# Usage: terraform-check.sh DIR
set -Eeuo pipefail
root="${1:?terraform directory}"
fail=0
while IFS= read -r -d '' dir; do
  echo "== $dir"
  ( cd "$dir" && terraform init -backend=false -input=false >/dev/null && terraform validate -no-color ) || fail=1
  if [[ -d "$dir/tests" ]]; then ( cd "$dir" && terraform test -no-color ) || fail=1; fi
done < <(find "$root" -name '*.tf' -not -path '*/.terraform/*' -printf '%h\0' | sort -zu)
exit "$fail"
