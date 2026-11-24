#!/usr/bin/env bash
# Cria os Secrets do namespace payflow a partir de arquivos em ./secrets (fora do Git: decisão 70/J9.03).
# Os NOMES dos arquivos viram as chaves; com "configtree:" viram propriedades do Spring (J9.03).
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
mkdir -p secrets
[ -f secrets/db_password ] || { head -c 24 /dev/urandom | base64 | tr -d '\n=+/' > secrets/db_password; }
[ -f secrets/jwt_secret ]  || { head -c 48 /dev/urandom | base64 | tr -d '\n' > secrets/jwt_secret; }
kubectl apply -f manifests/00-namespace.yaml
kubectl -n payflow create secret generic payflow-db \
  --from-file=password=secrets/db_password --dry-run=client -o yaml | kubectl apply -f -
kubectl -n payflow create secret generic payflow-app \
  --from-file=spring.datasource.password=secrets/db_password \
  --from-file=payflow.security.jwt.secret=secrets/jwt_secret \
  --dry-run=client -o yaml | kubectl apply -f -
echo "Secrets criados. Lembre: base64 NÃO é criptografia (P7.04)."
