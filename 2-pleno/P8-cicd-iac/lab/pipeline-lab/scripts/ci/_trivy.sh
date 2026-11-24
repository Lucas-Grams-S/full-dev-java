# shellcheck shell=bash
# Sourced by the scan scripts: runs Trivy from a container pinned by DIGEST (nothing is installed on the runner).
: "${TRIVY_IMAGE:=ghcr.io/aquasecurity/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb}"
TRIVY_CACHE="${TRIVY_CACHE:-$HOME/.cache/trivy}"
mkdir -p "$TRIVY_CACHE"
trivy() {
  docker run --rm -v "$TRIVY_CACHE":/root/.cache/ -v "$PWD":/work -w /work "$TRIVY_IMAGE" "$@"
}
