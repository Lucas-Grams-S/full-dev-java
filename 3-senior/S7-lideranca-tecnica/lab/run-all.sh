#!/usr/bin/env bash
# S7 lab: rewrites every out-s70*.txt with REAL output. Sections: s701..s707; ONLY=s704 ./run-all.sh runs one.
# Needs JDK 25, Maven 3.9 (review-lab, ai-lab, legacy-upgrade), git and network for the first clone of spring-petclinic.
# Everything is deterministic (fixed seeds, fixed "today", pinned commit) except Maven's download noise.
set -uo pipefail
cd "$(dirname "$0")"
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}
J="$JAVA_HOME/bin/java"
ONLY=${ONLY:-}
TODAY=2026-11-23
PETCLINIC_COMMIT=500158f732419217507c7656904b8e6aa1bcc0d6
run() { [[ -z "$ONLY" || "$ONLY" == "$1" ]]; }

if run s701; then
  "$J" -Dtoday=$TODAY DocLab.java adr ../../../projeto/adrs > out-s701-adr.txt
  { echo "### good RFC"; "$J" -Dtoday=$TODAY DocLab.java rfc samples/rfc-0001-transferencias-recorrentes.md
    echo; echo "### bad RFC"; "$J" -Dtoday=$TODAY DocLab.java rfc samples/rfc-ruim.md
    tmp=$(mktemp -d)
    grep -v '^- \*\*Decisor:\*\*' samples/rfc-0001-transferencias-recorrentes.md > "$tmp/rfc-mutant.md"
    echo; echo "### mutant: good RFC without the Decisor line"; "$J" -Dtoday=$TODAY DocLab.java rfc "$tmp/rfc-mutant.md"
    echo; echo "### same good RFC read on 2026-12-10 (comment deadline passed, still 'em revisão')"; "$J" -Dtoday=2026-12-10 DocLab.java rfc samples/rfc-0001-transferencias-recorrentes.md
    rm -r "${tmp:?}"
  } > out-s701-rfc.txt
fi

if run s702; then
  { echo "### good design doc"; "$J" -Dtoday=$TODAY DocLab.java design samples/design-transferencias-recorrentes.md
    echo; echo "### bad design doc"; "$J" -Dtoday=$TODAY DocLab.java design samples/design-ruim.md
    tmp=$(mktemp -d)
    sed 's/[Rr]eversão/volta/g' samples/design-transferencias-recorrentes.md > "$tmp/design-mutant.md"
    echo; echo "### mutant: good design doc without the word 'reversão'"; "$J" -Dtoday=$TODAY DocLab.java design "$tmp/design-mutant.md"
    rm -r "${tmp:?}"
  } > out-s702-design.txt
  for m in dates zones burst keys; do echo "### $m"; "$J" RecurrenceSpike.java $m; echo; done > out-s702-spike.txt
fi

if run s703; then
  for m in tasks risks forecast; do echo "### $m"; "$J" EstimateLab.java $m; echo; done > out-s703-estimate.txt
fi

if run s704; then
  dir=${PETCLINIC_DIR:-}
  if [[ -z "$dir" ]]; then dir=$(mktemp -d); git clone -q https://github.com/spring-projects/spring-petclinic.git "$dir/petclinic"; dir="$dir/petclinic"; fi
  "$J" HotspotLab.java "$dir" $PETCLINIC_COMMIT src/main/java > out-s704-hotspots.txt
  "$J" StranglerLab.java > out-s704-strangler.txt
fi

if run s704-rewrite; then   # slow: downloads OpenRewrite and its recipes (~100 MB) the first time
  ( cd legacy-upgrade
    for recipe in org.openrewrite.java.migrate.UpgradeToJava25 org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta; do
      mvn -B -q org.openrewrite.maven:rewrite-maven-plugin:6.46.1:dryRun \
        -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-migrate-java:3.42.1 \
        -Drewrite.activeRecipes=$recipe > /dev/null 2>&1
      sed 's#3-senior/S7-lideranca-tecnica/lab/legacy-upgrade/##g' target/rewrite/rewrite.patch > ../out-s704-rewrite-${recipe##*.}.patch
    done
    # the same recipe WITHOUT the recipe artifact: what the error looks like
    mvn -B org.openrewrite.maven:rewrite-maven-plugin:6.46.1:dryRun \
      -Drewrite.activeRecipes=org.openrewrite.java.migrate.UpgradeToJava25 2>&1 | grep -E '^\[ERROR\] (Failed|Did you)' > ../out-s704-rewrite-notfound.txt
    rm -r target )
fi

if run s705; then
  ( cd review-lab && ./run-review.sh && rm -r target ) > out-s705-review.txt 2>&1
  "$J" FlakyLab.java > out-s705-flaky.txt
fi

if run s706; then
  { echo "### debt cost of delay quantified"; "$J" PriorityLab.java; echo; echo "### nobody quantified the debt"; "$J" PriorityLab.java nodebtcod; echo; echo "### the same debt in two slices"; "$J" PriorityLab.java sliced; } > out-s706-priority.txt
fi

if run s707; then
  ( cd ai-lab && mvn -B -q test 2>&1 | grep -E '^AI[0-9]' | sort; mvn -B -q test > /dev/null 2>&1 && echo "Tests: 6 green (mvn test exit 0)"; rm -r target ) > out-s707-ai.txt
fi
echo "done: $(ls out-s70* | wc -l) output files"
