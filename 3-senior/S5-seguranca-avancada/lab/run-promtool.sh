#!/usr/bin/env bash
# promtool (image prom/prometheus, run with --entrypoint promtool) on the audit rules: check + test + one MUTANT. Needs Docker.
set -u
cd "$(dirname "$0")"
IMG=prom/prometheus
W="$(mktemp -d)"; chmod 755 "$W"
cp prometheus/audit-rules.yml prometheus/audit-rules.test.yml "$W/"; chmod 644 "$W"/*
pt() { docker run --rm --entrypoint promtool -v "$1:/w:ro" -w /w "$IMG" "${@:2}" 2>&1; }
echo "\$ promtool --version"; docker run --rm --entrypoint promtool "$IMG" --version | head -1
echo; echo "\$ promtool check rules audit-rules.yml";  pt "$W" check rules audit-rules.yml
echo; echo "\$ promtool test rules audit-rules.test.yml"; pt "$W" test rules audit-rules.test.yml
M="$(mktemp -d)"; chmod 755 "$M"; cp "$W"/* "$M/"
sed -i 's/ and payflow:audit_events:rate5m > 0.5//' "$M/audit-rules.yml"
echo; echo "\$ MUTANT: remove the traffic floor (' and payflow:audit_events:rate5m > 0.5')"; pt "$M" test rules audit-rules.test.yml | grep -E "SUCCESS|FAILED|alertname|got:|exp:" | head -12
echo "(exit code of the mutant run is non-zero when the test notices)"
M2="$(mktemp -d)"; chmod 755 "$M2"; cp "$W"/* "$M2/"
sed -i 's/for: 5m/for: 1m/' "$M2/audit-rules.yml"
echo; echo "\$ MUTANT 2: shorten 'for: 5m' to 'for: 1m'"; pt "$M2" test rules audit-rules.test.yml | grep -E "SUCCESS|FAILED|alertname|got:|exp:" | head -8
rm -f "$W"/* "$M"/* "$M2"/*; rmdir "$W" "$M" "$M2"
