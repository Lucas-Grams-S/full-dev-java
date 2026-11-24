#!/usr/bin/env bash
# Portão de implantação: só segue se a imagem (por DIGEST) tiver assinatura válida da chave pública informada.
# Uso: verify-before-deploy.sh <repo@sha256:...> <cosign.pub>
# Em produção o cosign é um binário do runner (fixado por versão/digest); aqui roda pelo contêiner do laboratório.
set -euo pipefail
IMAGE="${1:?image by digest}"; PUBKEY="${2:?public key}"
[[ "$IMAGE" == *@sha256:* ]] || { echo "REFUSED: '$IMAGE' is not a digest reference (tags are mutable)"; exit 2; }
COSIGN_RUN=${COSIGN_RUN:-"docker run --rm --network host -v $(dirname "$PUBKEY"):/keys:ro ghcr.io/sigstore/cosign/cosign:latest"}
if $COSIGN_RUN verify --key "/keys/$(basename "$PUBKEY")" --insecure-ignore-tlog=true "$IMAGE" >/dev/null 2>&1; then
  echo "OK: signature valid for ${IMAGE#*@}"
else
  echo "REFUSED: no valid signature for ${IMAGE#*@}"
  exit 1
fi
