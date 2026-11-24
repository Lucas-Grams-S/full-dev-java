"""Imprime em Markdown a cobertura de linhas lida do jacoco.csv (usado no resumo do job)."""
import csv
import sys

missed = covered = 0
with open(sys.argv[1], newline="", encoding="utf-8") as f:
    for row in csv.DictReader(f):
        missed += int(row["LINE_MISSED"])
        covered += int(row["LINE_COVERED"])
total = missed + covered
ratio = covered / total if total else 0.0
print("### Coverage")
print(f"Lines: **{covered}/{total}** ({ratio:.1%})")
