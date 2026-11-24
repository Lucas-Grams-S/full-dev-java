#!/usr/bin/env bash
# Cadeia de suprimentos do identity-lab: SBOM (Maven e imagem), Trivy, assinatura com cosign num registro local.
# Regrava out-*.txt. Uso: ./run-supply-chain.sh   (ONLY=s2 para uma seção só). Limpa contêineres, imagens e chaves ao fim.
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
LAB="$HERE/../identity-lab"
OUT="$HERE"
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
TRIVY="ghcr.io/aquasecurity/trivy:0.76.2@sha256:8f6bb947dbc528902e11f2f5be52f7b519f6a39d5d148f41166f2040499768fb"
COSIGN_IMG="ghcr.io/sigstore/cosign/cosign:latest"
CACHE="$HOME/.cache/p8-trivy"
W="$(mktemp -d)"; chmod 755 "$W"; mkdir -p "$W/keys" && chmod 777 "$W/keys"
REG=127.0.0.1:5001; REPO="$REG/payflow/identity-lab"
trivy()  { docker run --rm -v "$CACHE":/root/.cache/ -v "$W":/work "$TRIVY" "$@"; }
cosign() { docker run --rm --network host --user "$(id -u):$(id -g)" -e HOME=/tmp -e COSIGN_PASSWORD=lab-only \
             -v "$W/keys":/keys -v "$W":/work -w /work "$COSIGN_IMG" "$@"; }
want() { [[ -z "${ONLY:-}" || "${ONLY:-}" == "$1" ]]; }

cleanup() {
  docker rm -f lab-p10-registry >/dev/null 2>&1
  docker rmi -f lab-p10-identity:1 "$REPO:1" "$REPO:2" >/dev/null 2>&1
  rm -rf "${W:?}"
}
trap cleanup EXIT

s0() {   # build e SBOM do Maven
  ( cd "$LAB" && mvn -B -ntp -q package -DskipTests >/dev/null 2>&1 && mvn -B -ntp -q org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom >/dev/null 2>&1 )
  cp "$LAB/target/identity-lab-0.1.0-SNAPSHOT.jar" "$W/app.jar"; cp "$LAB/target/sbom.json" "$W/sbom.json"
}

s1() {   # SBOM do Maven: o que tem, uma consulta, o scan e a PROVA de que o scan enxerga uma falha
  {
    echo '$ jq ... sbom.json   # formato, versão e quantidade de componentes'
    jq -r '"\(.bomFormat) \(.specVersion): \(.components|length) components, \(.dependencies|length) dependency entries"' "$W/sbom.json"
    echo; echo '$ "estamos usando a nimbus-jose-jwt? em qual versão?" (sem reescanear nada)'
    jq -r '.components[] | select(.name=="nimbus-jose-jwt") | "\(.group):\(.name):\(.version)  \(.purl)"' "$W/sbom.json"
    echo; echo '$ trivy sbom sbom.json --severity HIGH,CRITICAL'
    trivy sbom /work/sbom.json --severity HIGH,CRITICAL --no-progress 2>&1 | grep -v "WARN\s*Unsupported hash" | sed -n '/Report Summary/,$p'
    echo "(avisos 'Unsupported hash algorithm' omitidos: $(trivy sbom /work/sbom.json --no-progress 2>&1 | grep -c 'Unsupported hash') linhas; o plugin grava SHA3/SHA-384 que o Trivy ignora)"
  } > "$OUT/out-sbom-maven.txt" 2>&1
  # PROVA: planta um componente sabidamente vulnerável (log4j-core 2.14.1) e confere que o scan o acusa
  python3 - "$W/sbom.json" "$W/sbom-planted.json" <<'PY'
import json, sys
b = json.load(open(sys.argv[1]))
b["components"].append({"type": "library", "group": "org.apache.logging.log4j", "name": "log4j-core", "version": "2.14.1",
                        "purl": "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1?type=jar", "bom-ref": "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1?type=jar"})
json.dump(b, open(sys.argv[2], "w"))
PY
  {
    echo '$ trivy sbom sbom-planted.json --severity CRITICAL   # SBOM com log4j-core 2.14.1 plantado'
    trivy sbom /work/sbom-planted.json --severity CRITICAL --no-progress --exit-code 1 2>&1 | grep -v "WARN\s*Unsupported hash" | sed -n '/Report Summary/,$p' | cut -c1-200 | head -30
    echo "exit=${PIPESTATUS[0]}"
  } > "$OUT/out-sbom-planted.txt" 2>&1
}

s2() {   # imagem: build, scan, SBOM da imagem
  cat > "$W/Dockerfile" <<'DOCKER'
FROM eclipse-temurin:25-jre
RUN useradd --uid 10001 --no-create-home payflow
COPY app.jar /app/app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
DOCKER
  docker build -q -t lab-p10-identity:1 "$W" >/dev/null
  docker save lab-p10-identity:1 -o "$W/image.tar"
  {
    echo '$ trivy image --input image.tar --severity HIGH,CRITICAL --ignore-unfixed'
    trivy image --input /work/image.tar --severity HIGH,CRITICAL --ignore-unfixed --no-progress 2>&1 | sed -n '/Report Summary/,/^Legend/p' | head -20
    echo; echo '$ trivy image --input image.tar --format cyclonedx --output image-sbom.json'
    trivy image --input /work/image.tar --format cyclonedx --output /work/image-sbom.json --no-progress >/dev/null 2>&1
    jq -r '"\(.bomFormat) \(.specVersion): \(.components|length) components in total"' "$W/image-sbom.json"
    echo "by type:"; jq -r '[.components[].type] | group_by(.) | map("\(.[0]): \(length)") | .[]' "$W/image-sbom.json"
    echo; echo '$ "o libssl da imagem?" (sem reescanear)'
    jq -r '.components[] | select(.name|test("^libssl")) | "\(.name) \(.version)"' "$W/image-sbom.json" | head -3
    echo; echo '$ "a nimbus-jose-jwt dentro da imagem?" (o Trivy achou a biblioteca DENTRO do jar)'
    jq -r '.components[] | select(.name|test("nimbus-jose-jwt")) | "\(.name) \(.version)"' "$W/image-sbom.json" | head -3
  } > "$OUT/out-image.txt" 2>&1
}

s3() {   # assinatura e verificação (chave local, registro local, sem transparency log)
  docker rm -f lab-p10-registry >/dev/null 2>&1
  docker run -d --name lab-p10-registry -p 127.0.0.1:5001:5000 registry:2 >/dev/null; sleep 2
  docker tag lab-p10-identity:1 "$REPO:1"; docker push -q "$REPO:1" >/dev/null
  local DIGEST NEW; DIGEST="$(docker inspect --format '{{index .RepoDigests 0}}' "$REPO:1")"
  cosign generate-key-pair --output-key-prefix /keys/cosign >/dev/null 2>&1
  local SIGN=(sign --key /keys/cosign.key --use-signing-config=false --tlog-upload=false --yes)
  local VER=(verify --key /keys/cosign.pub --insecure-ignore-tlog=true)
  {
    echo "digest: ${DIGEST#*@}"
    echo; echo '$ cosign sign --key cosign.key <repo>@<digest>'
    cosign "${SIGN[@]}" "$DIGEST" 2>&1 | grep -v "deprecated" 
    echo; echo '$ cosign verify --key cosign.pub <repo>@<digest>'
    cosign "${VER[@]}" "$DIGEST" 2>&1 | grep -E "^Verification|checks|claims|signatures were" | cut -c1-110
    echo "exit=${PIPESTATUS[0]}"
    echo; echo '$ cosign verify --key OUTRA-chave.pub <repo>@<digest>'
    cosign generate-key-pair --output-key-prefix /keys/other >/dev/null 2>&1
    cosign verify --key /keys/other.pub --insecure-ignore-tlog=true "$DIGEST" 2>&1 | grep -E "^Error|no matching|invalid" | head -2 | cut -c1-200
    echo; echo '$ alguém reenvia OUTRA imagem para a MESMA tag :1 (a tag é mutável)'
    echo 'FROM alpine:3.17' > "$W/Dockerfile.evil"; echo 'RUN echo malicious > /evil' >> "$W/Dockerfile.evil"
    docker build -q -f "$W/Dockerfile.evil" -t "$REPO:2" "$W" >/dev/null
    docker tag "$REPO:2" "$REPO:1"; docker push -q "$REPO:1" >/dev/null
    NEW="$(docker inspect --format '{{index .RepoDigests 0}}' "$REPO:1")"
    echo "tag :1 agora aponta para ${NEW#*@}"
    echo '$ cosign verify --key cosign.pub <repo>:1   # verificando PELA TAG'
    cosign "${VER[@]}" "$REPO:1" 2>&1 | grep -E "^Error|no signatures|Verification" | head -2 | cut -c1-200
    echo '$ cosign verify --key cosign.pub <repo>@<digest ORIGINAL>   # a assinatura continua valendo para o conteúdo original'
    cosign "${VER[@]}" "$DIGEST" 2>&1 | grep -E "^Error|Verification for" | head -1 | cut -c1-120
  } > "$OUT/out-cosign.txt" 2>&1
  {
    echo '$ verify-before-deploy.sh <repo>:1                          # por tag'
    "$HERE/verify-before-deploy.sh" "$REPO:1" "$W/keys/cosign.pub"; echo "exit=$?"
    echo '$ verify-before-deploy.sh <repo>@<digest ORIGINAL, assinado>'
    "$HERE/verify-before-deploy.sh" "$DIGEST" "$W/keys/cosign.pub"; echo "exit=$?"
    echo '$ verify-before-deploy.sh <repo>@<digest da imagem trocada, sem assinatura>'
    "$HERE/verify-before-deploy.sh" "$NEW" "$W/keys/cosign.pub"; echo "exit=$?"
  } > "$OUT/out-gate.txt" 2>&1
  {
    echo '$ cosign attest --predicate sbom.json --type cyclonedx   # o SBOM como atestação assinada, presa ao digest'
    cosign attest --key /keys/cosign.key --use-signing-config=false --tlog-upload=false --yes --type cyclonedx --predicate /work/sbom.json "$DIGEST" 2>&1 | grep -v "deprecated"
    echo; echo '$ cosign verify-attestation --type cyclonedx --key cosign.pub <digest>'
    cosign verify-attestation --key /keys/cosign.pub --insecure-ignore-tlog=true --type cyclonedx "$DIGEST" 2>&1 | grep -E "^Verification|checks|claims|signatures were|^Error" | cut -c1-110
    echo; echo '$ ... | jq: o que o predicado afirma'
    cosign verify-attestation --key /keys/cosign.pub --insecure-ignore-tlog=true --type cyclonedx "$DIGEST" 2>/dev/null \
      | jq -r '.payload | @base64d | fromjson | "predicateType=\(.predicateType) subject.digest=\(.subject[0].digest.sha256[0:16])… components=\(.predicate.components|length)"' | head -1
  } > "$OUT/out-attest.txt" 2>&1
}

want s0 || true
s0
want s1 && s1
want s2 && { [[ -f "$W/app.jar" ]] || s0; s2; }
want s3 && { [[ -f "$W/image.tar" ]] || s2; s3; }
echo "done: $(ls "$OUT"/out-*.txt | wc -l) outputs"
