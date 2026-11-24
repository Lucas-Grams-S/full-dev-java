-- Cria o PRIMEIRO usuario (ADMIN) e a conta dele. Producao nao tem seed (decisao 31).
-- Uso: psql -v email=... -v full_name=... -v hash=... -f bootstrap-admin.sql   (o hash ja com o prefixo {bcrypt})
\set ON_ERROR_STOP on
BEGIN;
WITH u AS (
  INSERT INTO users (email, full_name, password_hash, role)
  VALUES (lower(trim(:'email')), :'full_name', :'hash', 'ADMIN')
  RETURNING id
)
INSERT INTO accounts (owner_id) SELECT id FROM u;
COMMIT;
SELECT u.email, u.role, length(u.password_hash) AS hash_len, a.status, a.balance
FROM users u JOIN accounts a ON a.owner_id = u.id;
