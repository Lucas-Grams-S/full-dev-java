#!/usr/bin/env bash
# Regrava as saídas out-s60*.txt do laboratório do S6. Uso: ./run-all.sh   ou   ONLY=s4 ./run-all.sh
# Requer JDK 25 (lançador de código-fonte com vários arquivos). Terraform só na seção s4 (validate, sem conta AWS).
set -uo pipefail
cd "$(dirname "$0")"
JAVA="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}/bin/java"
ONLY="${ONLY:-}"
run() { [[ -z "$ONLY" || "$ONLY" == "$1" ]]; }

s1() {
  "$JAVA" WellArchitectedLab.java tradeoff > out-s601-tradeoff.txt
  { "$JAVA" WellArchitectedLab.java review samples/wa-review-payflow.csv 2026-11-23; echo "exit=$?"; echo
    "$JAVA" WellArchitectedLab.java review samples/wa-review-bad.csv 2026-11-23; echo "exit=$?"; } > out-s601-review.txt
}

s2() {
  "$JAVA" ComputeLab.java curve > out-s602-curve.txt
  "$JAVA" -Dregion=sa-east-1 ComputeLab.java curve > out-s602-curve-sa.txt
  "$JAVA" ComputeLab.java memory > out-s602-memory.txt
}

s3() {
  { "$JAVA" LockInLab.java scan samples/lockin/good; echo "exit=$?"; echo
    "$JAVA" LockInLab.java scan samples/lockin/leaky; echo "exit=$?"; } > out-s603-scan.txt
  "$JAVA" LockInLab.java exit > out-s603-exit.txt
  # os adaptadores de exemplo compilam contra o AWS SDK v2? (só se os JARs estiverem no ~/.m2; o S3 pode faltar)
  local cp tmp
  cp=$(find ~/.m2/repository/software/amazon -name '*.jar' -path '*2.59.1*' ! -name '*sources*' 2>/dev/null | tr '\n' ':')
  tmp=$(mktemp -d)
  { echo "AWS SDK v2 2.59.1 JARs found: $(echo "$cp" | tr ':' '\n' | grep -c jar)"
    if "${JAVA%/java}/javac" -d "$tmp" -cp "$cp" $(find samples/lockin/good -name '*.java' ! -name 'S3ArchiveStore.java'); then
      echo "javac: $(find samples/lockin/good -name '*.java' ! -name 'S3ArchiveStore.java' | wc -l) sample classes compiled (S3ArchiveStore skipped: s3 JAR not in ~/.m2)"
    else echo "javac: FAILED"; fi; } > out-s603-compile.txt 2>&1
  rm -rf "${tmp:?}"
}

s4() {
  "$JAVA" FinOpsLab.java estimate > out-s604-estimate.txt
  "$JAVA" FinOpsLab.java nat > out-s604-nat.txt
  { "$JAVA" FinOpsLab.java tags samples/resources.csv; echo "exit=$?"; } > out-s604-tags.txt
  "$JAVA" FinOpsLab.java commit > out-s604-commit.txt
  "$JAVA" FinOpsLab.java spot > out-s604-spot.txt
  if command -v terraform >/dev/null; then
    export TF_PLUGIN_CACHE_DIR="${TF_PLUGIN_CACHE_DIR:-$HOME/.cache/p8-tf-plugins}"
    { terraform version | head -1
      for d in terraform/finops terraform/organization; do
        echo "== $d"
        (cd "$d" && terraform fmt -check && terraform init -backend=false -input=false -no-color >/dev/null && terraform validate -no-color)
        grep -h 'version' "$d/.terraform.lock.hcl" | head -1
      done
      echo "== mutant: threshold_type -> threshold_kind (must fail)"
      local t; t=$(mktemp -d)
      cp -r terraform/finops/. "$t"/
      sed -i 's/threshold_type             = "PERCENTAGE"/threshold_kind             = "PERCENTAGE"/' "$t/main.tf"
      (cd "$t" && terraform validate -no-color 2>&1 | grep -E '^(Error|Success)' | sort | uniq -c)
      rm -rf "${t:?}" terraform/finops/.terraform terraform/organization/.terraform; } > out-s604-terraform.txt 2>&1
  fi
}

s5() { "$JAVA" LandingZoneLab.java > out-s605-scp.txt; }

s6() {
  "$JAVA" BillLab.java generate > out-s606-generate.txt
  "$JAVA" BillLab.java analyze > out-s606-analyze.txt
  "$JAVA" BillLab.java detect > out-s606-detect.txt
}

for s in s1 s2 s3 s4 s5 s6; do run "$s" && { echo "== $s"; "$s"; }; done
echo "ok: $(ls out-s60*.txt | wc -l) saídas"
