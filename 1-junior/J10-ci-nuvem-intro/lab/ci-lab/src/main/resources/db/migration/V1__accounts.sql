create table accounts (
    id         uuid           not null default uuidv7(),
    owner      varchar(100)   not null,
    balance    numeric(19, 2) not null,
    created_at timestamptz    not null default now(),
    constraint pk_accounts primary key (id),
    constraint ck_accounts_balance check (balance >= 0)
);
