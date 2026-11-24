#!/usr/bin/env bash
# Runs Trivy (image ghcr.io/aquasecurity/trivy:0.76.2) over samples/trivy-sca and samples/trivy-iac and condenses the JSON.
# Needs Docker. The vulnerability DB is downloaded once into ~/.cache/s5-trivy (~1.7 GB: delete it when you finish the module).
# NOTE: a scan that sees a pom.xml asks Maven Central for POMs (Trivy resolves transitive dependencies); repeated runs got HTTP 429
# ("Retry-After: 1795"), so the SCA sample lives in its own directory and ONLY=sca runs it alone.
set -u
cd "$(dirname "$0")"
IMG=ghcr.io/aquasecurity/trivy:0.76.2
CACHE="$HOME/.cache/s5-trivy"; mkdir -p "$CACHE"
W="$(mktemp -d)"; chmod 755 "$W"
s() { [ -z "${ONLY:-}" ] || [ "$ONLY" = "$1" ]; }
t() { d="$1"; shift; docker run --rm -v "$PWD/samples/$d:/work:ro" -v "$CACHE:/root/.cache" "$IMG" fs "$@" --format json --quiet /work > "$W/o.json" 2>"$W/err.txt" || { head -c 400 "$W/err.txt" >&2; return 1; }; python3 -I trivy-summary.py "$W/o.json" "${LIMIT:-14}"; }
s sca    && LIMIT=9  t trivy-sca --scanners vuln                          > out-s504-trivy-sca.txt
s secret && LIMIT=9  t trivy-iac --scanners secret --skip-check-update    > out-s504-trivy-secret.txt
s iac    && LIMIT=20 t trivy-iac --scanners misconfig --skip-check-update > out-s504-trivy-iac.txt
docker run --rm "$IMG" --version | head -1 > out-s504-trivy-version.txt
rm -f "$W/o.json" "$W/err.txt"; rmdir "$W"
echo done
