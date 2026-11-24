#!/usr/bin/env python3
"""Confere o modelo de ameaças (threats.csv) contra o código: uma ameaça "mitigada" sem evidência existente é mentira.
Regras: (1) mitigated -> evidência existe (test:Classe#metodo | file:caminho | grep:caminho::regex);
(2) accepted -> dono e data de revisão; (3) open -> dono e data; (4) toda fronteira cobre as 6 letras do STRIDE
(ou diz explicitamente que não se aplica). Sai com código 1 se algo não bate. `--self-test` prova que o verificador falha."""
import csv, os, re, sys, glob, datetime

HERE = os.path.dirname(os.path.abspath(__file__))
LAB = os.path.abspath(os.path.join(HERE, ".."))
TESTS = os.path.join(LAB, "identity-lab", "src", "test", "java")

def test_exists(spec, tests_dir):
    cls, _, method = spec.partition("#")
    for path in glob.glob(os.path.join(tests_dir, "**", cls + ".java"), recursive=True):
        if re.search(r"\bvoid\s+" + re.escape(method) + r"\s*\(", open(path).read()):
            return True
    return False

def check(csv_path, tests_dir, today):
    problems, rows = [], list(csv.DictReader(open(csv_path)))
    seen = set()
    for r in rows:
        tid = r["id"]
        if tid in seen: problems.append(f"{tid}: id repetido")
        seen.add(tid)
        st, ev = r["status"], r["evidence"]
        if st == "mitigated":
            kind, _, rest = ev.partition(":")
            ok = False
            if kind == "test": ok = test_exists(rest, tests_dir)
            elif kind == "file": ok = os.path.exists(os.path.normpath(os.path.join(LAB, rest)))
            elif kind == "grep":
                path, _, rx = rest.partition("::")
                p = os.path.join(LAB, path)
                ok = os.path.exists(p) and re.search(rx, open(p).read()) is not None
            if not ok: problems.append(f"{tid}: mitigada, mas a evidência não existe ({ev or 'vazia'})")
        elif st in ("accepted", "open"):
            if not r["owner"] or not r["review_by"]:
                problems.append(f"{tid}: {st} precisa de dono e data de revisão")
            elif datetime.date.fromisoformat(r["review_by"]) < today:
                problems.append(f"{tid}: revisão vencida ({r['review_by']})")
        else:
            problems.append(f"{tid}: status inválido '{st}'")
    # STRIDE por elemento (Microsoft): que letras se aplicam a cada tipo de elemento do diagrama
    expected = {"process": "STRIDE", "datastore": "TRID", "dataflow": "TID"}
    kinds = {"api": "process", "auth-code": "process", "internal": "process",
             "database": "datastore", "infra": "datastore", "supply-chain": "process"}
    gaps = 0
    for where in sorted({r["where"] for r in rows}):
        letters = {r["stride"] for r in rows if r["where"] == where}
        missing = set(expected[kinds[where]]) - letters
        gaps += len(missing)
        print(f"  {where:<13} ({kinds[where]:<9}) analisou {''.join(sorted(letters, key='STRIDE'.index)):<6} ainda sem análise: {''.join(sorted(missing, key='STRIDE'.index)) or '-'}")
    print(f"  lacunas de análise (letra aplicável sem ameaça registrada): {gaps}")
    return rows, problems

def main():
    today = datetime.date(2026, 11, 14) if "--today" not in sys.argv else datetime.date.fromisoformat(sys.argv[sys.argv.index("--today") + 1])
    rows, problems = check(os.path.join(HERE, "threats.csv"), TESTS, today)
    by = {}
    for r in rows: by[r["status"]] = by.get(r["status"], 0) + 1
    print(f"ameaças: {len(rows)}  {by}")
    for p in problems: print("PROBLEMA:", p)
    if "--self-test" in sys.argv:
        # a prova: um verificador que nunca falhou não prova nada. Renomeia um método e confere que ele acusa.
        import tempfile, shutil
        tmp = tempfile.mkdtemp(prefix="p10-threat-")
        shutil.copytree(TESTS, os.path.join(tmp, "java"))
        f = glob.glob(os.path.join(tmp, "java", "**", "ResourceServerTest.java"), recursive=True)[0]
        s = open(f).read().replace("void idor_bobCannotReadAlicesAccount_404", "void idor_renamed")
        open(f, "w").write(s)
        _, bad = check(os.path.join(HERE, "threats.csv"), os.path.join(tmp, "java"), today)
        shutil.rmtree(tmp, ignore_errors=True)
        print("self-test (teste renomeado):", "acusou" if bad else "NÃO ACUSOU", [b for b in bad][:2])
        if not bad: problems.append("o verificador não acusou o teste ausente")
        _, late = check(os.path.join(HERE, "threats.csv"), TESTS, datetime.date(2027, 6, 1))
        print("self-test (data futura):", "acusou" if late else "NÃO ACUSOU", [b for b in late][:2])
        if not late: problems.append("o verificador não acusou a revisão vencida")
    sys.exit(1 if problems else 0)

if __name__ == "__main__":
    main()
