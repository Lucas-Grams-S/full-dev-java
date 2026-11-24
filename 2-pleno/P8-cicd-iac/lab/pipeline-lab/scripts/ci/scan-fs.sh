#!/usr/bin/env bash
# Dependencies of the repository (pom.xml, lockfiles). Fails on HIGH/CRITICAL that already have a fix.
set -Eeuo pipefail
cd "$(dirname "$0")/../.."
# shellcheck source=scripts/ci/_trivy.sh
source scripts/ci/_trivy.sh
trivy fs --scanners vuln --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 \
  --ignorefile .trivyignore.yaml --no-progress .
