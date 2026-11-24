#!/usr/bin/env python3
"""Fails when a waiver (accepted risk) has no reason/owner/expiry or is expired. Usage: check-waivers.py waivers.yml [today=YYYY-MM-DD]"""
import sys, datetime, yaml

def check(path, today):
    problems = []
    data = yaml.safe_load(open(path)) or {}
    for w in data.get("waivers", []):
        wid = w.get("id", "?")
        for f in ("id", "reason", "owner", "until"):
            if not w.get(f):
                problems.append(f"{wid}: missing '{f}'")
        until = w.get("until")
        if until:
            if isinstance(until, str):
                until = datetime.date.fromisoformat(until)
            if until < today:
                problems.append(f"{wid}: EXPIRED on {until} (owner {w.get('owner')}): fix it or renew with a new decision")
            elif (until - today).days > 90:
                problems.append(f"{wid}: expiry {until} is more than 90 days away: a waiver is a short loan, not a decision")
    return problems

if __name__ == "__main__":
    today = datetime.date.fromisoformat(sys.argv[2]) if len(sys.argv) > 2 else datetime.date.today()
    p = check(sys.argv[1], today)
    print(f"{sys.argv[1]} @ {today}: " + ("OK" if not p else f"{len(p)} problem(s)"))
    for x in p:
        print("  -", x)
    sys.exit(1 if p else 0)
