#!/usr/bin/env bash
# S3.03: streaming replica (PostgreSQL 18 in Docker): recovery conflicts and Spring read/write routing with an LSN token.
# Creates and REMOVES its own containers (lab-s3-primary / lab-s3-replica), network and volumes. Needs Docker, postgres:18, JDK 25, spring-jdbc 7.0.10 in ~/.m2.
set -Eeuo pipefail; cd "$(dirname "$0")"
OUT=${OUT:-.}
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"; M="$HOME/.m2/repository"
CP="$(ls $M/org/springframework/spring-{jdbc,tx,core,beans,context,aop,expression}/7.0.10/*.jar | grep -v sources | tr '\n' ':')$M/commons-logging/commons-logging/1.3.5/commons-logging-1.3.5.jar:$M/org/jspecify/jspecify/1.0.1/jspecify-1.0.1.jar:$M/org/postgresql/postgresql/42.7.14/postgresql-42.7.14.jar"
P=lab-s3-primary; R=lab-s3-replica
cleanup() { docker rm -f $P $R >/dev/null 2>&1 || true; docker volume rm lab-s3-pvol lab-s3-rvol >/dev/null 2>&1 || true; docker network rm lab-s3-net >/dev/null 2>&1 || true; rm -rf "${CLS:-/nonexistent-dir}"; }
trap cleanup EXIT; cleanup
pp() { docker exec -i $P psql -U postgres -d payflow -v ON_ERROR_STOP=1 -X -q "$@"; }
rr() { docker exec -i $R psql -U postgres -d payflow -X -q "$@"; }
docker network create lab-s3-net >/dev/null
docker run -d --name $P --network lab-s3-net -p 127.0.0.1:55501:5432 -v lab-s3-pvol:/var/lib/postgresql -e POSTGRES_PASSWORD=lab \
  postgres:18 -c wal_level=replica -c max_wal_senders=5 -c hot_standby=on >/dev/null
until docker exec $P pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done; sleep 2
docker exec $P psql -U postgres -qc "create database payflow" -c "create role repl with replication login password 'repl'"
docker exec $P bash -c 'echo "host replication repl all scram-sha-256" >> "$PGDATA/pg_hba.conf"'; docker exec $P psql -U postgres -qc "select pg_reload_conf()" >/dev/null
docker run --rm --network lab-s3-net -v lab-s3-rvol:/var/lib/postgresql -e PGPASSWORD=repl --entrypoint bash postgres:18 -c '
  mkdir -p /var/lib/postgresql/18/docker && chown -R postgres:postgres /var/lib/postgresql && chmod 700 /var/lib/postgresql/18/docker
  gosu postgres pg_basebackup --checkpoint=fast -h lab-s3-primary -U repl -D /var/lib/postgresql/18/docker -R -X stream' >/dev/null 2>&1
docker run -d --name $R --network lab-s3-net -p 127.0.0.1:55502:5432 -v lab-s3-rvol:/var/lib/postgresql -e POSTGRES_PASSWORD=lab postgres:18 -c hot_standby=on >/dev/null
until docker exec $R pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done; sleep 2
pp -c "create table rw_probe(id bigint primary key, v text)" -c "create table t_conf as select g as id, md5(g::text) as v from generate_series(1,300000) g" -c "alter table t_conf add primary key (id)"
sleep 2

{
echo '## A) what the primary knows about its replica (pg_stat_replication) and what the replica knows about itself'
pp -c "select application_name, state, sync_state, write_lag is not null as has_write_lag, replay_lag is not null as has_replay_lag from pg_stat_replication"
rr -Atc "select 'replica: pg_is_in_recovery=' || pg_is_in_recovery() || ', replayed up to ' || pg_last_wal_replay_lsn()"
echo
echo '## B) recovery conflict: a long query on the replica x VACUUM on the primary'
for FB in off on; do
  echo "--- hot_standby_feedback=$FB, max_standby_streaming_delay=3s"
  rr -c "alter system set max_standby_streaming_delay='3s'" -c "alter system set hot_standby_feedback=$FB" -c "select pg_reload_conf()" >/dev/null; sleep 1
  pp -c "insert into t_conf select g, md5(g::text) from generate_series(300001, 600000) g on conflict do nothing" -c "vacuum t_conf"
  pp -c "update t_conf set v = v where id <= 10"
  ( rr -c "begin isolation level repeatable read" -c "select count(*) as before_sleep from t_conf" -c "select pg_sleep(12)" -c "select count(*) as after_sleep from t_conf" -c "commit" 2>&1 | sed 's/^/  [replica long query] /' ) &
  sleep 2
  pp -c "delete from t_conf where id % 2 = 0 and id > 300000"
  echo "  [primary] DELETE of 150000 rows, then VACUUM VERBOSE says: $(pp -c 'vacuum (verbose) t_conf' 2>&1 | grep -o 'tuples: [0-9]* removed, [0-9]* remain, [0-9]* are dead but not yet removable' | head -1)"
  wait
done
} > "$OUT/out-s303-replica.txt" 2>&1

echo '## C) Spring routing (Java, spring-jdbc 7.0.10) against the REAL primary and replica' > "$OUT/out-s303-routing.txt"
CLS="$(mktemp -d)"; "$JAVA_HOME/bin/javac" -d "$CLS" -cp "$CP" RoutingLab.java
run() { "$JAVA_HOME/bin/java" -cp "$CLS:$CP" RoutingLab "$@" 2>&1 | grep -v '^WARNING'; }
{
echo '--- C1) which node serves a read-only transaction'
run routing
echo '--- C2) read right after a write: natural replication lag (same host)'
run stale 100 notoken; run stale 100 token
echo '--- C3) replica 2 s behind (recovery_min_apply_delay = 2s, only to make the lag visible)'
rr -c "alter system set recovery_min_apply_delay='2s'" -c "select pg_reload_conf()" >/dev/null; sleep 2
run stale 20 notoken; run stale 20 token
rr -c "alter system reset recovery_min_apply_delay" -c "select pg_reload_conf()" >/dev/null
echo '--- C4) replica stopped (docker stop)'
docker stop $R >/dev/null
run down
} >> "$OUT/out-s303-routing.txt" 2>&1
cat "$OUT"/out-s303-*.txt
