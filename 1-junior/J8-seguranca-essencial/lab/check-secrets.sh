#!/usr/bin/env bash
# J8.07: guarda barata contra segredo no Git (NÃO substitui o gitleaks: pega só os casos óbvios).
# Uso (na raiz do repositório): bash scripts/check-secrets.sh
# Procura, nos arquivos RASTREADOS pelo Git, por: chave privada, chave de acesso da AWS e senha/segredo literal em YAML/properties.
# Os perfis dev e test (application-dev.yml, application-test.yml) e o código de teste podem ter valores de laboratório.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"

# o git grep imprime "caminho:linha:texto": o filtro olha só o caminho (antes do primeiro ':')
ALLOW='^(([^:]*/)?application-(dev|test)\.(yml|yaml|properties)|src/test/[^:]*|([^:]*/)?docs/[^:]*|[^:]*\.md):'
status=0

check() {   # check "descrição" "regex" [pathspec...]  (sem pathspec: todos os arquivos rastreados)
  local hits; local -a paths=("${@:3}"); [ ${#paths[@]} -eq 0 ] && paths=(.)
  hits=$(git grep -nIE -e "$2" -- "${paths[@]}" 2>/dev/null | grep -Ev "$ALLOW" || true)
  if [ -n "$hits" ]; then
    echo "✗ $1:"; echo "$hits" | sed 's/^/    /'; status=1
  else
    echo "✓ $1: nada encontrado"
  fi
}

check "chave privada"                  '-----BEGIN (RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY'
check "chave de acesso da AWS"         'AKIA[0-9A-Z]{16}'
check "senha/segredo literal em config" '^[[:space:]]*[A-Za-z_.-]*(password|secret|token|api-?key)[A-Za-z_.-]*[[:space:]]*[:=][[:space:]]*["'"'"']?[A-Za-z0-9+/_.=-]{8,}' '*.yml' '*.yaml' '*.properties'

exit $status
