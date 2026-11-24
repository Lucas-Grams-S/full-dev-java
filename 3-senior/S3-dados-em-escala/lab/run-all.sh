#!/usr/bin/env bash
# Runs the whole S3 laboratory and rewrites every out-*.txt. Needs Docker (postgres:18), JDK 25, spring-jdbc 7.0.10 + PostgreSQL JDBC in ~/.m2.
# One container (lab-s3-pg) is created, seeded once (200k users, 200k accounts, 3M ledger entries, 1M transfers) and REMOVED at the end;
# run-s303.sh creates and removes its own primary/replica pair. Use ONLY="s305 s307" to run a subset. Takes ~15 min.
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
ALL="s301 s302 s302-swap s303 s304 s305 s306 s307 s308 s309 s309-generated"; RUN="${ONLY:-$ALL}"
trap 'pg_down; docker rm -f lab-s3-primary lab-s3-replica >/dev/null 2>&1 || true' EXIT
if docker ps --format '{{.Names}}' | grep -qx "$LAB_PG" && [ -n "${REUSE:-}" ]; then echo "reusing running $LAB_PG"; else
  pg_up; psqlx < schema.sql; psqlx < seed.sql >/dev/null; make_seed; fi
for s in $RUN; do echo "=== run-$s.sh"; ONLY= ./run-$s.sh >/dev/null 2>&1 || echo "run-$s.sh exited with $?"; done
echo "done; outputs:"; ls -1 out-s30*.txt
