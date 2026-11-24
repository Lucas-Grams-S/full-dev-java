#!/usr/bin/env bash
# S3.05: what PostgreSQL itself can do for "search the statement" before adding OpenSearch (1M synthetic memos). OpenSearch/Elasticsearch NOT executed.
set -Eeuo pipefail; cd "$(dirname "$0")"; . ./lib.sh
OUT=${OUT:-.}
reset_db
{
psqlx <<'SQL'
\pset pager off
CREATE EXTENSION IF NOT EXISTS pg_trgm; CREATE EXTENSION IF NOT EXISTS unaccent;
SELECT setseed(0.42);
\echo '## setup: 1M memos like "Pix para João Silva - Mercado Central"'
CREATE TABLE memo_search AS
WITH w AS (SELECT
  ARRAY['Pix para','Pagamento','Transferência para','Compra','Boleto','Recarga','Mensalidade','Reembolso'] AS verb,
  ARRAY['João','Maria','José','Ana','Pedro','Luiza','Carlos','Fernanda','Joaquim','Mariana','Bruno','Camila'] AS nome,
  ARRAY['Silva','Souza','Oliveira','Santos','Pereira','Costa','Rodrigues','Almeida','Nascimento','Lima'] AS sobrenome,
  ARRAY['Mercado Central','Farmácia Popular','Padaria São João','Aluguel','Conta de luz','Academia','Escola','Posto de gasolina','Restaurante Sabor','Livraria Cultura','Condomínio','Internet fibra'] AS motivo)
SELECT g AS id, verb[1 + (random()*7)::int] || ' ' || nome[1 + (random()*11)::int] || ' ' || sobrenome[1 + (random()*9)::int] || ' - ' || motivo[1 + (random()*11)::int] AS memo
FROM w, generate_series(1, 1000000) g;
ALTER TABLE memo_search ADD PRIMARY KEY (id);
ANALYZE memo_search;
SELECT 'table: ' || pg_size_pretty(pg_total_relation_size('memo_search')) || ' | sample: ' || (SELECT memo FROM memo_search WHERE id = 1);

\echo
\echo '## A) ILIKE with a leading wildcard, no index'
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT count(*) FROM memo_search WHERE memo ILIKE '%mercado%';

\echo
\echo '## B) pg_trgm GIN index (same query, same answer)'
\timing on
CREATE INDEX idx_memo_trgm ON memo_search USING gin (memo gin_trgm_ops);
\timing off
SELECT 'trigram index size: ' || pg_size_pretty(pg_relation_size('idx_memo_trgm'));
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT count(*) FROM memo_search WHERE memo ILIKE '%mercado%';
\echo '-- a typo: word_similarity (<%) finds "farmacia" for "farmcia"'
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT count(*) FROM memo_search WHERE 'farmcia' <% memo;

\echo
\echo '## C) full-text search: portuguese + unaccent, stored generated tsvector + GIN'
CREATE TEXT SEARCH CONFIGURATION pt_unaccent (COPY = portuguese);
ALTER TEXT SEARCH CONFIGURATION pt_unaccent ALTER MAPPING FOR hword, hword_part, word WITH unaccent, portuguese_stem;
\timing on
ALTER TABLE memo_search ADD COLUMN tsv tsvector GENERATED ALWAYS AS (to_tsvector('pt_unaccent', memo)) STORED;
CREATE INDEX idx_memo_tsv ON memo_search USING gin (tsv);
\timing off
SELECT 'tsvector index size: ' || pg_size_pretty(pg_relation_size('idx_memo_tsv')) || ' | heap now (with the tsv column): ' || pg_size_pretty(pg_relation_size('memo_search')) AS sizes;
ANALYZE memo_search;
\echo '-- "joao padaria" typed without accents matches "João" and "Padaria São João":'
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT count(*) FROM memo_search WHERE tsv @@ websearch_to_tsquery('pt_unaccent', 'joao padaria');
\echo '-- top 3 by ts_rank:'
SELECT id, memo, round(ts_rank(tsv, websearch_to_tsquery('pt_unaccent', 'joao padaria'))::numeric, 4) AS rank
FROM memo_search WHERE tsv @@ websearch_to_tsquery('pt_unaccent', 'joao padaria') ORDER BY rank DESC, id LIMIT 3;
\echo '-- stemming: "pagamentos" (plural) finds "Pagamento":'
SELECT count(*) AS matches_plural FROM memo_search WHERE tsv @@ websearch_to_tsquery('pt_unaccent', 'pagamentos');
SELECT count(*) AS matches_exact_word FROM memo_search WHERE memo ILIKE '%pagamentos%';
SQL
} > "$OUT/out-s305-search.txt" 2>&1

{
echo '## D) the cost of keeping the indexes: insert 100k rows with 0, 1 and 2 GIN indexes'
psqlx <<'SQL'
\pset pager off
CREATE TABLE w0 (id bigint, memo text); CREATE TABLE w1 (id bigint, memo text); CREATE TABLE w2 (id bigint, memo text, tsv tsvector GENERATED ALWAYS AS (to_tsvector('pt_unaccent', memo)) STORED);
CREATE INDEX ON w1 USING gin (memo gin_trgm_ops);
CREATE INDEX ON w2 USING gin (memo gin_trgm_ops); CREATE INDEX ON w2 USING gin (tsv);
\timing on
\echo 'no index:'
INSERT INTO w0 SELECT id, memo FROM memo_search WHERE id <= 100000;
\echo 'trigram GIN only:'
INSERT INTO w1 SELECT id, memo FROM memo_search WHERE id <= 100000;
\echo 'trigram GIN + tsvector column + GIN:'
INSERT INTO w2 (id, memo) SELECT id, memo FROM memo_search WHERE id <= 100000;
SQL
} > "$OUT/out-s305-write.txt" 2>&1

{
echo '## E) the SCOPE of the search: one account (1,000 rows per account here) x the whole table'
psqlx <<'SQL'
\pset pager off
ALTER TABLE memo_search ADD COLUMN account_id int GENERATED ALWAYS AS (id % 1000) STORED;
CREATE INDEX idx_memo_account ON memo_search (account_id);
ANALYZE memo_search;
\echo '-- inside ONE account: b-tree on account_id, then ILIKE on its ~1,000 rows (no search engine, no trigram index used)'
DROP INDEX idx_memo_trgm;
EXPLAIN (ANALYZE, COSTS off, TIMING off, BUFFERS off, SUMMARY on) SELECT id, memo FROM memo_search WHERE account_id = 424 AND memo ILIKE '%mercado%' ORDER BY id DESC LIMIT 20;
SQL
} > "$OUT/out-s305-scope.txt" 2>&1
cat "$OUT"/out-s305-*.txt
