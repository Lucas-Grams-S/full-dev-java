import json, sys
# Condenses `trivy --format json` into one line per finding (stable, readable in a lesson).
d = json.load(open(sys.argv[1]))
rows = []
for r in d.get("Results", []):
    t = r.get("Target")
    for v in r.get("Vulnerabilities", []) or []:
        rows.append((v["Severity"], t, f'{v["VulnerabilityID"]} {v["PkgName"]} {v["InstalledVersion"]} -> fixed in {v.get("FixedVersion","?")}'))
    for m in r.get("Misconfigurations", []) or []:
        rows.append((m["Severity"], t, f'{m["ID"]} {m["Title"]}'))
    for s in r.get("Secrets", []) or []:
        rows.append((s["Severity"], t, f'{s["RuleID"]} line {s["StartLine"]}: {s["Title"]}'))
order = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3}
rows.sort(key=lambda x: (order.get(x[0], 9), x[1], x[2]))
limit = int(sys.argv[2]) if len(sys.argv) > 2 else 12
print(f"{len(rows)} finding(s); first {min(limit, len(rows))} by severity:")
for sev, t, msg in rows[:limit]:
    print(f"  {sev:<8} {t:<18} {msg[:150]}")
from collections import Counter
print("by severity:", dict(Counter(r[0] for r in rows)))
