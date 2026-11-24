#!/usr/bin/env bash
# S2.07: mede a PARTIDA (do exec ate o fim da primeira requisicao) de MiniApp em varias formas.
# 15 execucoes por variante; imprime mediana, minimo e maximo (ms) e o pico de memoria residente (MB).
# Variantes do JDK 25 (CDS, AppCDS, cache AOT) e, se o Docker tiver a imagem, GraalVM native image
# (ghcr.io/graalvm/native-image-community:25, so para COMPILAR; o binario roda no host).
set -u
cd "$(dirname "$0")"
LAB="$PWD"
J="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}/bin"
W="$(mktemp -d)"; chmod 755 "$W"
"$J/javac" -d "$W" MiniApp.java || exit 1
cd "$W"
"$J/jar" cf app.jar MiniApp*.class
N=15
medir() {   # $1 = nome; resto = o comando completo
  local nome="$1"; shift
  local ts=() rss=0
  for i in $(seq "$N"); do
    local t0 t1
    t0=$(date +%s%N)
    /usr/bin/time -f '%M' -o "$W/rss.txt" "$@" > /dev/null 2>&1
    t1=$(date +%s%N)
    ts+=($(( (t1 - t0) / 1000000 )))
    local r; r=$(tail -1 "$W/rss.txt"); [ "$r" -gt "$rss" ] && rss=$r
  done
  local sorted; sorted=($(printf '%s\n' "${ts[@]}" | sort -n))
  printf '%-44s mediana=%4d ms  min=%4d  max=%4d  RSS pico=%d MB\n' "$nome" "${sorted[$((N/2))]}" "${sorted[0]}" "${sorted[$((N-1))]}" $(( rss / 1024 ))
}
{
  echo "JVM: $("$J/java" -version 2>&1 | head -1)  | $N execucoes por variante | $(nproc) nucleos"
  medir "padrao (CDS do JDK ligado)"            "$J/java" -cp app.jar MiniApp
  medir "-Xshare:off (sem CDS)"                 "$J/java" -Xshare:off -cp app.jar MiniApp
  medir "-XX:TieredStopAtLevel=1 (so C1)"       "$J/java" -XX:TieredStopAtLevel=1 -cp app.jar MiniApp
  # AppCDS dinamico (JDK 13+): grava no fim de uma execucao e reutiliza
  "$J/java" -XX:ArchiveClassesAtExit=app.jsa -cp app.jar MiniApp > /dev/null 2>&1
  medir "AppCDS (-XX:SharedArchiveFile)"        "$J/java" -XX:SharedArchiveFile=app.jsa -cp app.jar MiniApp
  # Cache AOT do JDK 25 (JEP 514): uma etapa para criar, outra para usar
  "$J/java" -XX:AOTCacheOutput=app.aot -cp app.jar MiniApp > /dev/null 2>&1
  medir "cache AOT (-XX:AOTCache, JDK 25)"      "$J/java" -XX:AOTCache=app.aot -cp app.jar MiniApp
  medir "cache AOT + TieredStopAtLevel=1"       "$J/java" -XX:AOTCache=app.aot -XX:TieredStopAtLevel=1 -cp app.jar MiniApp
  ls -l app.jsa app.aot 2>/dev/null | awk '{printf "arquivo %s: %.1f MB\n", $9, $5/1048576}'
  # GraalVM native image (compila num contêiner; mede o binario no host)
  if docker image inspect ghcr.io/graalvm/native-image-community:25 > /dev/null 2>&1; then
    s0=$(date +%s)
    docker run --rm --user "$(id -u):$(id -g)" -v "$W:/work" -w /work ghcr.io/graalvm/native-image-community:25 \
       -cp app.jar MiniApp -o miniapp --no-fallback > native-build.log 2>&1
    echo "native-image: compilacao em $(( $(date +%s) - s0 )) s; binario $(du -m miniapp | cut -f1) MB; $(grep -E 'in total file size|Peak RSS' native-build.log | sed -E 's/^ +//' | tr '\n' ';' | cut -c1-120)"
    medir "native image (binario)"              ./miniapp
  fi
} > "$LAB/out-s207.txt" 2>&1
cd "$LAB" && rm -rf "${W:?}"
echo ok
