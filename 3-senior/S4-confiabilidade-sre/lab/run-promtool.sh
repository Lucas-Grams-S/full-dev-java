#!/usr/bin/env bash
# S4.01: checks the SLO rules and runs their unit tests with promtool (image prom/prometheus), then proves the tests can FAIL (2 mutants).
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
# with --write the output is split into out-s401-promtool.txt (S4.01) and out-s407-promtool.txt (S4.07)
if [ "${1:-}" = "--write" ]; then
  "$0" > "$HERE/out-all-promtool.tmp" 2>&1
  python3 - "$HERE" <<'PY'
import sys
d=sys.argv[1]; t=open(d+"/out-all-promtool.tmp").read(); i=t.index("== S4.07")
open(d+"/out-s401-promtool.txt","w").write(t[:i].rstrip()+"\n"); open(d+"/out-s407-promtool.txt","w").write(t[i:].rstrip()+"\n")
PY
  rm -f "$HERE/out-all-promtool.tmp"; exit 0
fi
cd "$HERE/prometheus"
IMG=${PROM_IMAGE:-prom/prometheus:latest}
pt() { docker run --rm -v "$1":/w -w /w --entrypoint promtool "$IMG" "${@:2}" 2>&1; }
echo "== promtool check rules"; pt "$PWD" check rules slo-rules.yml
echo; echo "== promtool test rules (original)"; pt "$PWD" test rules slo-rules.test.yml
W=$(mktemp -d); chmod 755 "$W"; mkdir "$W/m1" "$W/m2"; chmod 755 "$W/m1" "$W/m2"
cp slo-rules.yml slo-rules.test.yml "$W/m1/"; cp slo-rules.yml slo-rules.test.yml "$W/m2/"
sed -i 's/14.4 \* 0.001/144 * 0.001/' "$W/m1/slo-rules.yml"
python3 - "$W/m2/slo-rules.yml" <<'PY'
import sys
p=sys.argv[1]; s=open(p).read()
s=s.replace(' and payflow:slo_transfers_errors:ratio_rate5m > (14.4 * 0.001)','',1); open(p,'w').write(s)
PY
echo; echo "== mutant M1: threshold 14.4 -> 144 (fast burn can no longer fire) -> the tests must FAIL"; pt "$W/m1" test rules slo-rules.test.yml | grep -E "SUCCESS|FAILED|name:"
echo; echo "== mutant M2: short window removed (alert would stay on after recovery) -> the tests must FAIL"; pt "$W/m2" test rules slo-rules.test.yml | grep -E "SUCCESS|FAILED|name:"
echo; echo "== S4.07: capacity rules (predict_linear)"; pt "$PWD" check rules capacity-rules.yml | grep -E "SUCCESS|FAILED"; pt "$PWD" test rules capacity-rules.test.yml
mkdir "$W/m3"; chmod 755 "$W/m3"; cp capacity-rules.yml capacity-rules.test.yml "$W/m3/"
sed -i 's/payflow_db_free_bytes\[3d\]/payflow_db_free_bytes[6h]/' "$W/m3/capacity-rules.yml"
echo; echo "== mutant M3: the 14-day forecast reads only 6 h (the step is no longer mistaken for a trend) -> the test that DEPENDS on the 3 d window must FAIL"; pt "$W/m3" test rules capacity-rules.test.yml | grep -E "SUCCESS|FAILED|alertname|expr:" | head -5
rm -rf "${W:?}"
