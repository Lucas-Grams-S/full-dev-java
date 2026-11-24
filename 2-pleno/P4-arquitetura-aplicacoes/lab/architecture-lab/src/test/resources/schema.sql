create table if not exists accounts (
  id      uuid primary key,
  owner   varchar(100) not null,
  balance numeric(19,2) not null check (balance >= 0)
);
