#!/bin/bash
# Roda UMA vez, no primeiro boot (EC2 user data, como root). Log: /var/log/cloud-init-output.log
set -Eeuo pipefail

dnf install -y docker jq
systemctl enable --now docker

# Docker Compose nao vem no pacote do Amazon Linux 2023: baixe a versao FIXA e confira o sha256
COMPOSE_VERSION=v5.7.1
PLUGIN_DIR=/usr/local/lib/docker/cli-plugins
mkdir -p "$PLUGIN_DIR"
cd "$PLUGIN_DIR"
base="https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}"
curl -fsSL -o docker-compose "${base}/docker-compose-linux-x86_64"
curl -fsSL -o docker-compose.sha256 "${base}/docker-compose-linux-x86_64.sha256"
echo "$(cut -d' ' -f1 docker-compose.sha256)  docker-compose" | sha256sum -c -
chmod +x docker-compose
rm docker-compose.sha256

# Pasta da aplicacao e o certificado raiz do RDS (para sslmode=verify-full)
install -d -m 755 /opt/payflow/certs
curl -fsSL -o /opt/payflow/certs/global-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
