# Cheatsheet — SQL, PostgreSQL e JDBC (módulo J3)

> Testado em PostgreSQL 18.6. Detalhes e explicações nas aulas J3.01 a J3.12.

## psql (dentro do container `payflow-pg`)
| Objetivo | Comando |
|---|---|
| Entrar | `docker exec -it payflow-pg psql -U payflow -d payflow` |
| Rodar um arquivo | `docker exec -i payflow-pg psql -U payflow -d payflow -v ON_ERROR_STOP=1 < arquivo.sql` |
| Um comando só | `docker exec payflow-pg psql -U payflow -d payflow -c "SELECT 1"` |
| Estou pronto? | `docker exec payflow-pg pg_isready -U payflow -d payflow` |
| Listar bancos / tabelas / descrever | `\l` · `\dt` · `\d tabela` (`\d+` para mais) |
| Índices | `\di` |
| Saída expandida / tempo / sair | `\x` · `\timing` · `\q` |

## Tipos do PayFlow
`UUID` (`DEFAULT uuidv7()`) · `NUMERIC(19,2)` para dinheiro · `VARCHAR(n)` · `TIMESTAMPTZ` · `BIGINT` · nunca `FLOAT` para dinheiro.

## DDL e DML
```sql
CREATE TABLE t (id UUID NOT NULL DEFAULT uuidv7(), ..., CONSTRAINT pk_t PRIMARY KEY (id), CONSTRAINT fk_t_x FOREIGN KEY (x_id) REFERENCES x (id), CONSTRAINT ck_t_y CHECK (y > 0), CONSTRAINT uk_t_z UNIQUE (z));
ALTER TABLE t ADD COLUMN c TIPO;  ALTER TABLE t DROP COLUMN c;  ALTER TABLE t ADD CONSTRAINT ...;
DROP TABLE t;  TRUNCATE t;
INSERT INTO t (a, b) VALUES (1, 2), (3, 4) RETURNING id;
INSERT ... ON CONFLICT (col) DO NOTHING;    INSERT ... ON CONFLICT (col) DO UPDATE SET a = EXCLUDED.a;
UPDATE t SET a = 1 WHERE ...;   DELETE FROM t WHERE ...;       -- sempre com WHERE; ensaie com BEGIN/ROLLBACK
```
Prefixos de nome: `pk_` `uk_` `ck_` `fk_` e `idx_<tabela>_<colunas>`. Tabelas/colunas em `snake_case`, sem aspas.

## SELECT
```sql
SELECT col AS apelido, expr FROM t WHERE cond ORDER BY col DESC, id DESC LIMIT 10 OFFSET 0;
-- ordem lógica: FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT
```
- `NULL`: `IS NULL` / `IS NOT NULL` (nunca `= NULL`); `COALESCE(a, b)`.
- Período: `>= início AND < dia seguinte` (não `BETWEEN` em timestamp).
- Texto: `LIKE 'a%'`, `ILIKE` (sem diferenciar maiúsculas). Valor entre aspas **simples**.
- Keyset: `WHERE (created_at, id) < (:last_created_at, :last_id) ORDER BY created_at DESC, id DESC LIMIT n`.

## JOIN
`JOIN` (só os que casam) · `LEFT JOIN` (todos da esquerda) · anti-join: `LEFT JOIN ... WHERE d.id IS NULL` ou `NOT EXISTS` (cuidado com `NOT IN` e `NULL`) · condição da direita de um `LEFT JOIN` vai no `ON`, não no `WHERE` · `JOIN` 1:N antes de `SUM` faz *fan-out*.

## Agregação, subquery, CTE, janela
```sql
SELECT g, count(*), sum(x) FILTER (WHERE c) FROM t GROUP BY g HAVING count(*) > 1;
WHERE v > (SELECT avg(v) FROM t)          WHERE EXISTS (SELECT 1 FROM u WHERE u.t_id = t.id)
WITH passo AS (SELECT ...) SELECT ... FROM passo;       WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 5) SELECT * FROM n;
sum(x) OVER (PARTITION BY g ORDER BY created_at, id)    row_number() OVER (...)    lag(x) OVER (...)
```
`SUM` de vazio é `NULL` → `COALESCE(SUM(x), 0)`. Função de janela não pode estar no `WHERE`. Desempate único no `OVER (ORDER BY ...)`.

## Conferências do PayFlow (devem retornar **0 linhas**)
```sql
-- regra 3: saldo = soma dos lançamentos
SELECT a.id FROM accounts a LEFT JOIN ledger_entries l ON l.account_id = a.id
GROUP BY a.id, a.balance
HAVING a.balance <> COALESCE(SUM(CASE l.type WHEN 'CREDIT' THEN l.amount ELSE -l.amount END), 0);
-- regra 2: transferência concluída tem débito = crédito = valor
SELECT t.id FROM transfers t LEFT JOIN ledger_entries l ON l.transfer_id = t.id
WHERE t.status = 'COMPLETED' GROUP BY t.id, t.amount
HAVING COALESCE(sum(l.amount) FILTER (WHERE l.type = 'DEBIT'), 0) <> t.amount
    OR COALESCE(sum(l.amount) FILTER (WHERE l.type = 'CREDIT'), 0) <> t.amount;
```

## Índices e EXPLAIN
- `CREATE INDEX idx_t_a_b ON t (a, b DESC);` — igualdade primeiro, ordenação depois. `CREATE INDEX ... ON t (lower(col));` para expressões.
- `PRIMARY KEY` e `UNIQUE` criam índice; **FKs não**.
- `EXPLAIN (ANALYZE, BUFFERS) consulta;` — `ANALYZE` **executa**: em `UPDATE`/`DELETE` use `BEGIN; ... ROLLBACK;`.
- Não usa índice: tabela pequena, pouca seletividade, função na coluna, `LIKE '%x'`, só a 2ª coluna do composto.
- Índice custa escrita (medido: ~11x mais lento com 3 índices em 1 milhão de inserções).

## Transações
```sql
BEGIN;  ...;  COMMIT;        ROLLBACK;        SAVEPOINT s;  ROLLBACK TO SAVEPOINT s;
```
Sem `BEGIN`, cada comando é confirmado na hora (autocommit). Erro dentro da transação → `current transaction is aborted` até o `ROLLBACK`. Padrão: `READ COMMITTED`.

## JDBC (Java)
```java
try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT ... WHERE id = ?")) {
    ps.setObject(1, uuid);
    try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { rs.getBigDecimal("balance"); rs.getObject("created_at", OffsetDateTime.class); } }
}
// transação: c.setAutoCommit(false); ...; c.commit();  catch → c.rollback();
```
`NUMERIC` → `BigDecimal` · `TIMESTAMPTZ` → `OffsetDateTime` (`.toInstant()`) · `UUID` → `UUID`. Nome de coluna/ordem: lista permitida, nunca `?` nem concatenação. `SQLState`: `23505` unicidade · `23503` FK · `23514` CHECK · `40P01` deadlock · `42P01` tabela inexistente · `28P01` senha · `3D000` banco inexistente.

## Flyway
`V<n>__descrição.sql` (dois `_`) · `R__descrição.sql` · não editar migração aplicada (checksum) · nome fora do padrão é **ignorado em silêncio** · `clean` desabilitado por padrão (só em teste) · `Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate()`.
