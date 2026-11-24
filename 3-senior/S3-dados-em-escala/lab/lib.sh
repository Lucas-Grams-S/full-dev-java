# Shared helpers for the S3 lab (PostgreSQL 18 in Docker). Source it: . ./lib.sh
LAB_PG=lab-s3-pg
psqlx() { docker exec -i "$LAB_PG" psql -U postgres -d "${DB:-payflow}" -v ON_ERROR_STOP=1 -X -q "$@"; }
pg_up() {
  docker rm -f "$LAB_PG" >/dev/null 2>&1 || true
  docker run -d --name "$LAB_PG" -e POSTGRES_PASSWORD=lab postgres:18 \
    -c wal_level=logical -c max_wal_senders=5 -c max_replication_slots=5 -c shared_buffers=256MB -c max_wal_size=4GB >/dev/null
  until docker exec "$LAB_PG" pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done; sleep 2
  docker exec "$LAB_PG" psql -U postgres -qc "drop database if exists payflow" -c "create database payflow"
}
pg_down() { docker rm -f "$LAB_PG" >/dev/null 2>&1 || true; }
# Fast reset: payflow = copy of the seeded template payflow_seed (create it once with make_seed).
make_seed() { docker exec "$LAB_PG" psql -U postgres -qc "drop database if exists payflow_seed" -c "create database payflow_seed template payflow"; }
reset_db()  { docker exec "$LAB_PG" psql -U postgres -qc "drop database if exists payflow with (force)" -c "create database payflow template payflow_seed"; }

# Monotonic milliseconds (the wall clock of the lab machine jumped once; /proc/uptime does not).
now_ms() { awk '{ printf "%d\n", $1 * 1000 }' /proc/uptime; }
