create table if not exists wallets (id text primary key, balance numeric(19,2) not null check (balance >= 0));
create table if not exists outbox_events (
  id uuid primary key default uuidv7(), aggregate_id text not null, type text not null, payload text not null,
  created_at timestamptz not null default now(), published_at timestamptz, attempts int not null default 0);
create index if not exists idx_outbox_pending on outbox_events (created_at) where published_at is null;
create table if not exists processed_messages (consumer text not null, message_id text not null,
  processed_at timestamptz not null default now(), primary key (consumer, message_id));
create table if not exists payment_requests (id text primary key, payee_wallet text not null, amount numeric(19,2) not null,
  status text not null default 'PENDING', expires_at timestamptz not null, paid_at timestamptz);
create table if not exists payments (id uuid primary key default uuidv7(), request_id text, wallet_id text not null, amount numeric(19,2) not null);
create table if not exists shedlock (name varchar(64) primary key, lock_until timestamp(3) not null, locked_at timestamp(3) not null, locked_by varchar(255) not null);
create table if not exists job_runs (job text not null, instance text not null, ran_at timestamptz not null default clock_timestamp());
