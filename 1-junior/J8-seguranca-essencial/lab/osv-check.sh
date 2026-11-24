#!/usr/bin/env bash
# J8.07: consulta o OSV (banco aberto de vulnerabilidades) para bibliotecas Maven.
# Uso: bash osv-check.sh grupo:artefato:versão [grupo:artefato:versão ...]
# Só as COORDENADAS (grupo, artefato, versão) vão para api.osv.dev; nenhum dado seu ou do projeto.
set -uo pipefail
for coord in "$@"; do
  IFS=: read -r group artifact version <<<"$coord"
  body=$(curl -s -m 40 -X POST https://api.osv.dev/v1/query -H 'Content-Type: application/json' \
         -d "{\"package\":{\"ecosystem\":\"Maven\",\"name\":\"$group:$artifact\"},\"version\":\"$version\"}")
  cves=$(printf '%s' "$body" | grep -o '"CVE-[0-9]*-[0-9]*"' | tr -d '"' | sort -u | tr '\n' ' ')
  ids=$(printf '%s' "$body" | grep -o '"id": *"[^"]*"' | wc -l)
  echo "$coord -> $ids registro(s) no OSV; CVEs: ${cves:-nenhum}"
done
