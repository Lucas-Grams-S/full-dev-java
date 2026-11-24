\set QUIET on
\echo '### A. Estatística velha: tabela nova carregada SEM ANALYZE'
DROP TABLE IF EXISTS lab_stats;
CREATE TABLE lab_stats (id bigint, status text);
ALTER TABLE lab_stats SET (autovacuum_enabled = false);
INSERT INTO lab_stats SELECT g, CASE WHEN g % 100 = 0 THEN 'PENDING' ELSE 'DONE' END FROM generate_series(1, 1000000) g;
CREATE INDEX ON lab_stats (status);
EXPLAIN (ANALYZE, COSTS ON, TIMING OFF, BUFFERS OFF) SELECT * FROM lab_stats WHERE status = 'PENDING';
ANALYZE lab_stats;
EXPLAIN (ANALYZE, COSTS ON, TIMING OFF, BUFFERS OFF) SELECT * FROM lab_stats WHERE status = 'PENDING';
DROP TABLE lab_stats;
\echo '### B. pg_stat_statements: as consultas mais caras depois de uma carga'
SELECT pg_stat_statements_reset();
