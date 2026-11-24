#!/usr/bin/env bash
# Sobe o Keycloak 26.8.2 do laboratório (modo dev, HTTP, porta 8180) e espera o realm "payflow" responder.
set -euo pipefail
cd "$(dirname "$0")"
NAME=lab-p10-keycloak
docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" -p 127.0.0.1:8180:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=lab-admin-only \
  -v "$PWD/payflow-realm.json:/opt/keycloak/data/import/payflow-realm.json:ro" \
  quay.io/keycloak/keycloak:26.8.2 start-dev --import-realm >/dev/null
for i in $(seq 1 90); do
  if curl -fs http://127.0.0.1:8180/realms/payflow/.well-known/openid-configuration >/dev/null 2>&1; then
    echo "keycloak ready after ~$((i*2))s"; python3 ./kc-setup.py; exit 0
  fi
  sleep 2
done
echo "keycloak did not become ready" >&2; docker logs "$NAME" | tail -30 >&2; exit 1
