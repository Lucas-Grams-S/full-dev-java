#!/usr/bin/env bash
# S2.07: CRaC (Azul Zulu 25 com CRaC, motor "warp", sem privilegios) em Docker.
# Mede, em 10 execucoes cada, o tempo de "docker run" ate a primeira resposta 200:
#   (a) JVM normal (java -cp ... CracApp)   (b) restore do checkpoint (java -XX:CRaCRestoreFrom=...)
# Os dois incluem o custo de iniciar o contêiner. Regrava out-s207-crac.txt.
set -u
cd "$(dirname "$0")"
LAB="$PWD"
IMG=azul/zulu-openjdk:25-jdk-crac
OUT="$LAB/out-s207-crac.txt"
W="$(mktemp -d)"; chmod 755 "$W"; mkdir "$W/cr"; cp CracApp.java "$W/"
UIDGID="$(id -u):$(id -g)"
docker run --rm --user "$UIDGID" -v "$W:/work" -w /work $IMG javac -d /work/cls CracApp.java > /dev/null 2>&1
docker rm -f crac-cp > /dev/null 2>&1
docker run -d --name crac-cp -p 18081:8080 -v "$W:/work" -w /work $IMG \
   sh -c 'java -XX:CRaCCheckpointTo=/work/cr -cp /work/cls CracApp & wait' > /dev/null
n=0; until curl -s -o /dev/null http://127.0.0.1:18081/accounts; do sleep 0.2; n=$((n+1)); [ "$n" -gt 100 ] && { echo "checkpoint: app nao subiu" > "$OUT"; docker logs crac-cp 2>&1 | tail -5 >> "$OUT"; exit 1; }; done
for i in $(seq 200); do curl -s -o /dev/null http://127.0.0.1:18081/accounts; done      # aquece antes do checkpoint
docker exec crac-cp sh -c 'P=$(jps | grep CracApp | cut -d" " -f1); jcmd $P JDK.checkpoint' > /dev/null 2>&1
sleep 3
{
echo "imagem: $IMG | $(docker run --rm $IMG java -version 2>&1 | sed -n 3p)"
echo "checkpoint: $(du -sk "$W/cr" | cut -f1) KB em disco ($(ls "$W/cr" | tr '\n' ' '))"
} > "$OUT"
docker rm -f crac-cp > /dev/null 2>&1

medir() {   # $1 = nome; resto = argumentos do java
  local nome="$1"; shift
  local ts=()
  for i in $(seq 10); do
    docker rm -f crac-m > /dev/null 2>&1
    local t0 t1
    t0=$(date +%s%N)
    docker run -d --name crac-m -p 18082:8080 -v "$W:/work" -w /work $IMG java "$@" > /dev/null
    local n=0
    until curl -s -o /dev/null -f http://127.0.0.1:18082/accounts; do
      sleep 0.01; n=$((n+1))
      if [ "$n" -gt 1500 ] || [ "$(docker inspect -f '{{.State.Running}}' crac-m)" != "true" ]; then
        printf '%-46s FALHOU (contêiner saiu ou nao respondeu em 15 s)\n' "$nome" >> "$OUT"; docker logs crac-m 2>&1 | tail -3 >> "$OUT"; return
      fi
    done
    t1=$(date +%s%N)
    ts+=($(( (t1 - t0) / 1000000 )))
    docker rm -f crac-m > /dev/null 2>&1
  done
  local sorted; sorted=($(printf '%s\n' "${ts[@]}" | sort -n))
  printf '%-46s mediana=%5d ms  min=%5d  max=%5d\n' "$nome" "${sorted[5]}" "${sorted[0]}" "${sorted[9]}" >> "$OUT"
}
medir "JVM normal (docker run ate o 1o 200)"       -cp /work/cls CracApp
medir "restore do checkpoint CRaC (idem)"          -XX:CRaCRestoreFrom=/work/cr
docker run --rm -v "$W:/work" alpine sh -c 'rm -rf /work/cls /work/cr /work/CracApp.java' > /dev/null 2>&1
cd "$LAB" && rmdir "$W" 2>/dev/null
echo ok
