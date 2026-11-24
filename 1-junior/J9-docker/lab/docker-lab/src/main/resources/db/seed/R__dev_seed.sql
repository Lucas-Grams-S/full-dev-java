insert into accounts (id, owner, balance)
values ('019f0000-0000-7000-8000-000000000001', 'ana@payflow.dev', 100.00),
       ('019f0000-0000-7000-8000-000000000002', 'bruno@payflow.dev', 50.00)
on conflict (id) do nothing;
