#!/usr/bin/env bash
# P9.08: PostgreSQL 18 streaming replication in Docker (primary + replica) and what asynchronous replication does to reads.
# Cleans up its containers, network and volumes at the end. Needs Docker, postgres:18 and a JDK + the PostgreSQL JDBC jar.
set -Eeuo pipefail
cd "$(dirname "$0")"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
PGJAR="${PGJAR:-$(ls "$HOME"/.m2/repository/org/postgresql/postgresql/*/postgresql-*.jar | grep -v sources | sort -V | tail -1)}"
cleanup() { docker rm -f lab-p9-pg-primary lab-p9-pg-replica >/dev/null 2>&1 || true
            docker volume rm lab-p9-primary lab-p9-standby >/dev/null 2>&1 || true
            docker network rm lab-p9-net >/dev/null 2>&1 || true; }
trap cleanup EXIT; cleanup
docker network create lab-p9-net >/dev/null

docker run -d --name lab-p9-pg-primary --network lab-p9-net -p 127.0.0.1:55432:5432 -v lab-p9-primary:/var/lib/postgresql \
  -e POSTGRES_PASSWORD=lab postgres:18 -c wal_level=replica -c max_wal_senders=5 -c hot_standby=on >/dev/null
until docker exec lab-p9-pg-primary pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done; sleep 2
docker exec lab-p9-pg-primary psql -U postgres -qc "create role repl with replication login password 'repl'"
docker exec lab-p9-pg-primary bash -c 'echo "host replication repl all scram-sha-256" >> "$PGDATA/pg_hba.conf"'
docker exec lab-p9-pg-primary psql -U postgres -qc "select pg_reload_conf()" >/dev/null

echo '$ pg_basebackup (copies the primary and writes standby.signal + primary_conninfo)'
docker run --rm --network lab-p9-net -v lab-p9-standby:/var/lib/postgresql -e PGPASSWORD=repl --entrypoint bash postgres:18 -c '
  mkdir -p /var/lib/postgresql/18/docker && chown -R postgres:postgres /var/lib/postgresql && chmod 700 /var/lib/postgresql/18/docker
  gosu postgres pg_basebackup -h lab-p9-pg-primary -U repl -D /var/lib/postgresql/18/docker -R -X stream' 2>&1 | tail -2
docker run -d --name lab-p9-pg-replica --network lab-p9-net -p 127.0.0.1:55433:5432 -v lab-p9-standby:/var/lib/postgresql \
  -e POSTGRES_PASSWORD=lab postgres:18 -c hot_standby=on >/dev/null
until docker exec lab-p9-pg-replica pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done; sleep 2
echo -n 'replica in recovery (read-only)? '; docker exec lab-p9-pg-replica psql -U postgres -Atc "select pg_is_in_recovery()"
echo -n 'write on the replica -> '; docker exec lab-p9-pg-replica psql -U postgres -Atc "create table nope(x int)" 2>&1 | head -1 || true

run() { "$JAVA_HOME/bin/java" -cp "$PGJAR" ReplicaLagLab.java "$@" 2>&1 | grep -v '^WARNING'; }
echo; echo '# A) natural lag (replication as it is, same host, idle machine): the lag is tiny, but NOT zero'
run natural 300
echo; echo '# B) simulated slow replica: recovery_min_apply_delay = 2s (a legitimate PostgreSQL setting, used here only to make the lag visible)'
docker exec lab-p9-pg-replica psql -U postgres -qc "alter system set recovery_min_apply_delay = '2s'"
docker exec lab-p9-pg-replica psql -U postgres -qc "select pg_reload_conf()" >/dev/null; sleep 1
run delayed-2s 5
