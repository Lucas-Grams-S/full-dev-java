-- P8.03: why a rename in ONE deploy breaks the old version during a rolling/blue-green update.
-- The old version (N-1) reads owner_name; the new version (N) reads holder_name.
\set ON_ERROR_STOP off
create table accounts (id int primary key, owner_name text not null);
insert into accounts values (1, 'Ana'), (2, 'Bruno');

\echo '--- A) ONE-STEP rename (what a naive migration does)'
alter table accounts rename column owner_name to holder_name;
\echo 'new version (N) reads holder_name:'
select holder_name from accounts where id = 1;
\echo 'old version (N-1), still running during the rollout, reads owner_name:'
select owner_name from accounts where id = 1;

\echo '--- B) EXPAND / CONTRACT (three releases)'
alter table accounts rename column holder_name to owner_name;     -- back to the start
\echo 'release 1 (EXPAND): add the new column, keep the old one, keep both in sync'
alter table accounts add column holder_name text;
update accounts set holder_name = owner_name;                      -- backfill
create function sync_holder() returns trigger language plpgsql as $$
begin
  new.holder_name := coalesce(new.holder_name, new.owner_name);
  new.owner_name  := coalesce(new.owner_name,  new.holder_name);
  return new;
end $$;
create trigger accounts_sync before insert or update on accounts for each row execute function sync_holder();
alter table accounts alter column owner_name drop not null;       -- N inserts only holder_name
\echo 'old version (N-1) still works:'
select owner_name from accounts where id = 1;
\echo 'new version (N) works, and its insert is visible to the old one:'
insert into accounts (id, holder_name) values (3, 'Carla');
select id, holder_name, owner_name from accounts order by id;
\echo 'release 3 (CONTRACT): only after NO old version runs anymore'
drop trigger accounts_sync on accounts;
drop function sync_holder();
alter table accounts drop column owner_name;
alter table accounts alter column holder_name set not null;
select * from accounts order by id;
