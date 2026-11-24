#!/usr/bin/env bash
# Least-privilege roles and an append-only audit table on a throwaway PostgreSQL 18 container (lab-s5-pg). Needs Docker.
set -u
cd "$(dirname "$0")"
C=lab-s5-pg
docker rm -f $C >/dev/null 2>&1
docker run -d --rm --name $C -e POSTGRES_PASSWORD=super -p 127.0.0.1:55435:5432 postgres:18 >/dev/null
until docker exec $C pg_isready -U postgres >/dev/null 2>&1; do sleep 1; done; sleep 2
docker exec -i $C psql -U postgres -q < roles.sql 2>&1 | grep -v '^$'
# NOTE: the official image trusts 127.0.0.1 (pg_hba "trust"): connect through the container IP so the PASSWORD is really checked.
H="$(docker exec $C hostname -i | awk '{print $1}')"
as() { u="$1"; p="$2"; f="$3"; docker exec -i -e PGPASSWORD="$p" $C sh -c "stdbuf -oL psql -h $H -U $u -d payflow -q -e -P pager=off 2>&1" < "$f" | sed -E 's/^psql:[^ ]+:[0-9]+: //' | grep -v '^CONTEXT:'; }
{
  echo "--- seed, as payflow_migrator"; as payflow_migrator mig roles-seed.sql
  echo "--- connected as payflow_app"; as payflow_app app roles-app.sql
  echo "--- connected as payflow_readonly"; as payflow_readonly ro roles-readonly.sql
  echo "--- audit_events is append-only even for the OWNER of the table (payflow_migrator, then SET ROLE payflow_owner)"; as payflow_migrator mig roles-owner.sql
  echo "--- break-glass: a human role whose login is time-boxed (VALID UNTIL applies to password logins)"
  until_ok="$(date -u -d '+1 hour' '+%Y-%m-%d %H:%M:%S+00')"; until_past="$(date -u -d '-1 minute' '+%Y-%m-%d %H:%M:%S+00')"
  docker exec -i $C psql -U postgres -q -c "ALTER ROLE breakglass_ana VALID UNTIL '$until_ok'"
  docker exec -e PGPASSWORD=bg $C psql -h "$H" -U breakglass_ana -d payflow -Atc "SELECT 'inside the window: login ok as ' || current_user" 2>&1
  docker exec -i $C psql -U postgres -q -c "ALTER ROLE breakglass_ana VALID UNTIL '$until_past'"
  docker exec -e PGPASSWORD=bg $C psql -h "$H" -U breakglass_ana -d payflow -Atc "SELECT 'after the window: login ok?!'" 2>&1
} 
docker rm -f $C >/dev/null 2>&1
