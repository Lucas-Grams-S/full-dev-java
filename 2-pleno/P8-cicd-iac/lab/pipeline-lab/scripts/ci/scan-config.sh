#!/usr/bin/env bash
# Misconfiguration scan (Terraform, Kubernetes YAML, Dockerfile). Usage: scan-config.sh PATH
set -Eeuo pipefail
cd "$(dirname "$0")/../.."
# shellcheck source=scripts/ci/_trivy.sh
source scripts/ci/_trivy.sh
trivy config --severity MEDIUM,HIGH,CRITICAL --exit-code 1 --ignorefile .trivyignore.yaml "${1:?path to scan}"
