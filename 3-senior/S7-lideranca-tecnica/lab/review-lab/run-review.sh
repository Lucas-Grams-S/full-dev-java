#!/usr/bin/env bash
# Runs the WEAK and the STRONG suite: line/branch coverage (JaCoCo) and mutation score (PIT) for each.
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}
for suite in WeakScheduleRulesTest StrongScheduleRulesTest; do
  mvn -B -q clean test org.pitest:pitest-maven:mutationCoverage -Dsuite="$suite" > /dev/null 2>&1
  python3 - "$suite" <<'PY'
import csv, sys, xml.etree.ElementTree as ET
suite = sys.argv[1]
row = [r for r in csv.DictReader(open('target/site/jacoco/jacoco.csv')) if r['CLASS'] == 'ScheduleRules'][0]
lines = int(row['LINE_COVERED']) / (int(row['LINE_COVERED']) + int(row['LINE_MISSED']))
branches = int(row['BRANCH_COVERED']) / (int(row['BRANCH_COVERED']) + int(row['BRANCH_MISSED']))
muts = ET.parse('target/pit-reports/mutations.xml').getroot()
status = [m.get('status') for m in muts]
killed = status.count('KILLED')
print(f"{suite:24s} JaCoCo lines {lines:.0%} branches {branches:.0%} | PIT {killed}/{len(status)} mutants killed ({killed/len(status):.0%})")
for m in muts:
    if m.get('status') != 'KILLED':
        print(f"    SURVIVED line {m.findtext('lineNumber'):>2} {m.findtext('mutator').split('.')[-1]:38s} {m.findtext('description')}")
PY
done
