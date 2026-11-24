#!/usr/bin/env bash
# S8 lab: rewrites every out-s80*.txt with REAL output. Sections s801..s805; ONLY=s803 ./run-all.sh runs one.
# Needs JDK 25 and git. Deterministic (fixed seeds, fixed git dates) EXCEPT s801 hygiene, which reads the HEAD of the
# training repository itself (it changes when you commit).
set -uo pipefail
cd "$(dirname "$0")"
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}
J="$JAVA_HOME/bin/java"
ONLY=${ONLY:-}
run() { [[ -z "$ONLY" || "$ONLY" == "$1" ]]; }

if run s801; then
  { echo "### good portfolio README (PT + EN)"
    "$J" PortfolioLint.java readme samples/portfolio/README.md samples/portfolio/README.en.md
    echo; echo "### bad README"
    "$J" PortfolioLint.java readme samples/readme-ruim.md samples/readme-ruim.en.md
    tmp=$(mktemp -d)
    cp -r samples/portfolio "$tmp/m1"
    python3 - "$tmp/m1/README.en.md" <<'EOF'
import sys
p = sys.argv[1]; s = open(p).read()
i = s.index('## Limitations'); j = s.index('## License')
open(p, 'w').write(s[:i] + s[j:])
EOF
    echo; echo "### mutant 1: the EN version lost its 'Limitations' section"
    "$J" PortfolioLint.java readme "$tmp/m1/README.md" "$tmp/m1/README.en.md"
    cp -r samples/portfolio "$tmp/m2"
    sed -i 's#(docs/design/transferencias-recorrentes.md)#(docs/design/recorrentes.md)#' "$tmp/m2/README.md"
    echo; echo "### mutant 2: a renamed file, the PT link still points to the old name"
    "$J" PortfolioLint.java readme "$tmp/m2/README.md" "$tmp/m2/README.en.md"
    cp -r samples/portfolio "$tmp/m3"
    sed -i 's#^Carteira digital \*\*de estudo\*\*#Carteira digital pronta para produção#' "$tmp/m3/README.md"
    echo; echo "### mutant 3: 'pronta para produção' in the first paragraph"
    "$J" PortfolioLint.java readme "$tmp/m3/README.md" "$tmp/m3/README.en.md"
    rm -r "${tmp:?}"
  } > out-s801-readme.txt
  "$J" PortfolioLint.java hygiene ../../.. > out-s801-hygiene.txt
  ./history-secrets.sh > out-s801-history.txt
fi

if run s802; then
  { for f in cv-junior-ruim cv-junior cv-senior; do "$J" ResumeLint.java cv samples/cv/$f.md; echo; done
    "$J" ResumeLint.java match samples/cv/cv-junior-ruim.md samples/vaga-pleno.md; echo
    "$J" ResumeLint.java match samples/cv/cv-junior.md samples/vaga-pleno.md
  } > out-s802-resume.txt
fi

if run s803; then
  for m in examples fuzz scale; do echo "### $m"; "$J" LiveCodingLab.java $m; echo; done > out-s803-livecoding.txt
fi

if run s804; then
  { "$J" StarLint.java bank samples/star
    tmp=$(mktemp -d)
    sed 's/Rodei/Rodamos/; s/ li / lemos /; s/Percebi/Percebemos/; s/Escrevi/Escrevemos/; s/confirmei/confirmamos/; s/registrei/registramos/' \
      samples/star/01-cobertura-que-mentia.md > "$tmp/01-mutante-nos.md"
    echo; echo "### mutant: story 1 told with 'nós' (same facts)"
    "$J" StarLint.java story "$tmp/01-mutante-nos.md"
    rm -r "${tmp:?}"
  } > out-s804-star.txt
fi

if run s805; then
  { "$J" OfferLab.java selftest
    for m in reductionOnBase alwaysSimplified; do echo; "$J" -Dmutant=$m OfferLab.java selftest; done
    echo; "$J" OfferLab.java clt 4500
    echo; "$J" OfferLab.java clt 12000
    echo; "$J" OfferLab.java compare
  } > out-s805-offer.txt
fi
echo "done: $(ls out-s80* | wc -l) output files"
