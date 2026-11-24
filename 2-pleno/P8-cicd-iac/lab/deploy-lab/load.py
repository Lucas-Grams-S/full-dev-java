#!/usr/bin/env python3
"""Tiny load generator: N requests, C threads, optional action in the middle.

usage: load.py URL N C [--at SECONDS CMD]   (CMD runs in a shell while the load runs)
prints one summary line per version and the number of failures (connection errors + 5xx).
"""
import collections, subprocess, sys, threading, time, urllib.error, urllib.request

url, n, c = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
action = None
if "--at" in sys.argv:
    i = sys.argv.index("--at")
    action = (float(sys.argv[i + 1]), sys.argv[i + 2])

lock = threading.Lock()
counts = collections.Counter()
timeline = []          # (seconds since start, label)
t0 = time.time()
todo = list(range(n))

def one():
    try:
        with urllib.request.urlopen(url, timeout=3) as r:
            return r.headers.get("X-Version", "?"), r.status
    except urllib.error.HTTPError as e:
        return e.headers.get("X-Version", "?"), e.code
    except Exception as e:  # connection refused, reset, timeout
        return "none", type(e).__name__

def worker():
    while True:
        with lock:
            if not todo:
                return
            todo.pop()
        v, s = one()
        with lock:
            counts[(v, s)] += 1
            timeline.append((time.time() - t0, v, s))
        time.sleep(0.01)  # ~100 req/s per thread: the experiment must outlast the deploy

def act():
    time.sleep(action[0])
    t = time.time() - t0
    subprocess.run(action[1], shell=True, check=False)
    print(f"[action ran at t={t:.2f}s, took {time.time()-t0-t:.2f}s]")

threads = [threading.Thread(target=worker) for _ in range(c)]
if action:
    threads.append(threading.Thread(target=act))
for t in threads: t.start()
for t in threads: t.join()

total = sum(counts.values())
bad = sum(v for (ver, st), v in counts.items() if not (isinstance(st, int) and st < 500))
print(f"total={total} failures={bad} duration={time.time()-t0:.1f}s")
for (ver, st), v in sorted(counts.items(), key=lambda kv: str(kv[0])):
    print(f"  version={ver:<5} status={st!s:<22} {v:>5} ({100*v/total:.1f}%)")
if bad:
    ts = [t for t, ver, st in timeline if not (isinstance(st, int) and st < 500)]
    print(f"  first failure at t={min(ts):.2f}s, last at t={max(ts):.2f}s (window {max(ts)-min(ts):.2f}s)")
