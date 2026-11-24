#!/usr/bin/env bash
# Scans a LOCAL image (docker save | trivy --input): no Docker socket is handed to the scanner.
# Usage: scan-image.sh IMAGE:TAG   -> exit 1 if there is a HIGH/CRITICAL with a fix; writes sbom.cdx.json
set -Eeuo pipefail
cd "$(dirname "$0")/../.."
# shellcheck source=scripts/ci/_trivy.sh
source scripts/ci/_trivy.sh
image="${1:?image:tag}"
tar="$(mktemp --suffix=.tar)"; trap 'rm -f "$tar"' EXIT
docker save "$image" -o "$tar"
cp "$tar" ./image.tar
trap 'rm -f "$tar" ./image.tar' EXIT
trivy image --input /work/image.tar --format cyclonedx --output /work/sbom.cdx.json --quiet
trivy image --input /work/image.tar --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 \
  --ignorefile .trivyignore.yaml --no-progress
