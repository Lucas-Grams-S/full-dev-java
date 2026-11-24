#!/usr/bin/env bash
# S3.09: migrate users.phone (free text) -> users.phone_e164 (normalized) with the system running: expand, backfill under load, verify, contract.
# MODE=trigger (the real procedure) or MODE=mutant (same steps WITHOUT the trigger: proves that the drift check can fail).
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
mkdir -p "$OUT"

run_mode() {
  local MODE="$1" FILE="$2"
  docker exec "$LAB_PG" bash -c 'pkill pgbench >/dev/null 2>&1 || true'
  reset_db
  {
  echo "## MODE=$MODE"
  # 1. dirty data, as in real life: 2000 users with a phone that is not a Brazilian number
  psqlx -c "WITH x AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM users WHERE phone IS NOT NULL)
            UPDATE users u SET phone = CASE x.rn % 4 WHEN 0 THEN 'sem telefone' WHEN 1 THEN '123' WHEN 2 THEN '+1 202 555 0100' ELSE '11 1234-5678' END
            FROM x WHERE u.id = x.id AND x.rn <= 2000"
  # old-code write path: 90% UPDATE users SET phone (free text), 10% INSERT of a new user with a phone
  docker exec "$LAB_PG" bash -c "cat > /tmp/old_upd.sql <<'P'
\set rid random(1,200000)
UPDATE users SET phone = '(11) 9' || lpad((random()*9999)::int::text, 4, '0') || '-' || lpad((random()*9999)::int::text, 4, '0') WHERE email = 'user' || :rid || '@example.test';
P
cat > /tmp/old_ins.sql <<'P'
\set n random(1,2000000000)
INSERT INTO users (email, full_name, phone) VALUES ('late' || :n || '@example.test', 'Late User', '11' || lpad((random()*99999999)::int::text, 9, '9'));
P"
  echo "-- load: old code writing users.phone (4 clients, 90% UPDATE / 10% INSERT) for 30 s; the migration steps run during it"
  docker exec "$LAB_PG" bash -c "rm -f /tmp/m.*; (pgbench -U postgres -n -f /tmp/old_upd.sql@9 -f /tmp/old_ins.sql@1 -c 4 -T 30 -l --log-prefix=/tmp/m payflow > /tmp/m.out 2>&1 &)"
  sleep 3

  if [ "$MODE" = mutant ]; then echo "-- step 1 (V18, expand) as the MUTANT: ADD COLUMN + function, but WITHOUT the trigger"; else echo "-- step 1 (V18, expand): ADD COLUMN + function + trigger, with lock_timeout and retries"; fi
  local T0 T1 attempt=0; T0=$(now_ms)
  local SQLFILE="V18__expand_users_phone_e164.sql"
  if [ "$MODE" = mutant ]; then sed '/^CREATE TRIGGER/,$d' "$SQLFILE" > /tmp/v18.$$; SQLFILE=/tmp/v18.$$; fi
  until { echo "SET lock_timeout = '2s';"; echo "BEGIN;"; cat "$SQLFILE"; echo "COMMIT;"; } | psqlx 2>/tmp/ddl.err; do
    attempt=$((attempt+1)); echo "   attempt $attempt failed: $(head -1 /tmp/ddl.err)"; psqlx -c "DROP FUNCTION IF EXISTS normalize_phone_br(text) CASCADE" -c "DROP FUNCTION IF EXISTS users_sync_phone_e164() CASCADE" -c "ALTER TABLE users DROP COLUMN IF EXISTS phone_e164"; sleep 1
  done
  T1=$(now_ms); echo "   V18 applied after $attempt retries in $((T1-T0)) ms"; rm -f /tmp/v18.$$ /tmp/ddl.err
  sleep 1

  echo "-- step 2: backfill in batches of 10,000 by id (keyset), COMMIT per batch, 100 ms pause"
  T0=$(now_ms)
  psqlx -c "DO \$\$ DECLARE last uuid := '00000000-0000-0000-0000-000000000000'; n int; b int := 0; tot bigint := 0; BEGIN LOOP
      WITH k AS (SELECT id FROM users WHERE id > last ORDER BY id LIMIT 10000),
           u AS (UPDATE users t SET phone_e164 = normalize_phone_br(t.phone) FROM k WHERE t.id = k.id RETURNING t.id)
      SELECT (array_agg(id ORDER BY id DESC))[1], count(*) INTO last, n FROM u;
      EXIT WHEN n = 0; b := b + 1; tot := tot + n; COMMIT; PERFORM pg_sleep(0.1); END LOOP;
      RAISE NOTICE 'backfill: % batches, % rows', b, tot; END \$\$" 2>&1
  echo "   backfill took $(( $(now_ms) - T0 )) ms"

  echo "-- wait for the load to finish, then verify"
  docker exec "$LAB_PG" bash -c 'while pgrep pgbench >/dev/null; do sleep 1; done'
  docker exec "$LAB_PG" bash -c 'grep -E "number of transactions actually processed|number of failed transactions|latency average" /tmp/m.out | head -3 | sed "s/^/   pgbench: /"; cat /tmp/m.[0-9]* | awk "{ if (\$3>m) m=\$3; n++ } END { printf \"   pgbench: max latency %.1f ms over %d transactions\\n\", m/1000, n }"'

  echo "-- step 3 (verify): drift between the stored column and the function applied to the old column"
  psqlx -At -c "select 'users: ' || count(*) || ' | with phone: ' || count(phone) || ' | stored phone_e164 not null: ' || count(phone_e164) || ' | invalid (go to review): ' || count(*) FILTER (WHERE phone IS NOT NULL AND normalize_phone_br(phone) IS NULL) from users"
  psqlx -At -c "select 'DRIFT rows (phone_e164 IS DISTINCT FROM normalize_phone_br(phone)): ' || count(*) from users where phone_e164 IS DISTINCT FROM normalize_phone_br(phone)"
  if [ "$MODE" = trigger ]; then
    psqlx -At -F$'\t' -c "select id, coalesce(phone,''), coalesce(phone_e164,'') from users" > /tmp/xcheck.$$
    "$JAVA_HOME/bin/java" PhoneNormalizer.java crosscheck /tmp/xcheck.$$
    rm -f /tmp/xcheck.$$
    echo "-- step 4 (contract, V23 + V24) once every reader uses phone_e164 and the old code is gone"
    psqlx <<'SQL'
\timing on
SET lock_timeout = '2s';
ALTER TABLE users ADD CONSTRAINT ck_users_phone_e164 CHECK (phone_e164 ~ '^\+55[1-9][0-9]{9,10}$') NOT VALID;
ALTER TABLE users VALIDATE CONSTRAINT ck_users_phone_e164;
\echo 'V23: drop the trigger and the function'
DROP TRIGGER trg_users_sync_phone ON users;
DROP FUNCTION users_sync_phone_e164();
\echo 'V24: drop the old column'
ALTER TABLE users DROP COLUMN phone;
\timing off
SELECT 'columns now: ' || string_agg(column_name, ', ' ORDER BY ordinal_position) FROM information_schema.columns WHERE table_name = 'users';
SQL
  fi
  } > "$OUT/$FILE" 2>&1
  sed -i '/^Terminated$/d' "$OUT/$FILE"
}
run_mode trigger out-s309-migration.txt
run_mode mutant out-s309-mutant.txt

{
echo '## E) the Java normalizer against its documented cases, and the SQL function against the SAME cases'
"$JAVA_HOME/bin/java" PhoneNormalizer.java cases phone-cases.tsv
reset_db
psqlx < V18__expand_users_phone_e164.sql
bad=0
while IFS=$'\t' read -r input expected; do
  [ "$input" = "-" ] && sql_in="NULL" || sql_in="'${input//\'/\'\'}'"
  got=$(psqlx -At -c "select coalesce(normalize_phone_br($sql_in), '-')")
  if [ "$got" != "$expected" ]; then echo "SQL MISMATCH [$input] sql=$got expected=$expected"; bad=$((bad+1)); fi
done < phone-cases.tsv
echo "SQL cases: $(wc -l < phone-cases.tsv) rows checked, $bad mismatches"
} > "$OUT/out-s309-cases.txt" 2>&1
cat "$OUT"/out-s309-*.txt | cut -c1-220
