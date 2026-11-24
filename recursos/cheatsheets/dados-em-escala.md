# Cheatsheet — Dados em escala (S3)

> Valores de **laboratório** (PostgreSQL 18, 3 milhões de linhas, máquina compartilhada): **modelos**, não o PayFlow. Milissegundos variam.

## Migrações sem downtime (S3.01)
| Tema | Regra / comando | Onde |
|---|---|---|
| Padrão | expand → migrate → verificar → switch → contract; cada fase compatível com a versão N e N+1 | S3.01 |
| Todo DDL em tabela quente | `SET LOCAL lock_timeout = '2s'; SET LOCAL statement_timeout = '30s';` e **repetir** se falhar | S3.01 |
| Fila de *locks* | um `ALTER` esperando bloqueia **as leituras novas** (medido: ~4,6–5,5 s) | S3.01 |
| `ADD COLUMN` | *default* **constante** = catálogo (ms); volátil **reescreve** a tabela | S3.01 |
| `NOT NULL` | `ADD CONSTRAINT c CHECK (col IS NOT NULL) NOT VALID` → `VALIDATE CONSTRAINT c` → `SET NOT NULL` → `DROP CONSTRAINT c` (PG 18: `ADD CONSTRAINT c NOT NULL col NOT VALID`) | S3.01 |
| FK | `ADD ... FOREIGN KEY ... NOT VALID` → `VALIDATE CONSTRAINT` | S3.01 |
| Índice | `CREATE INDEX CONCURRENTLY` em migração própria; confira `indisvalid` | S3.01 |
| *Backfill* | fora do Flyway; lotes por faixa de `id`; `COMMIT` por lote; pausa adaptativa; idempotente | S3.01 |
| Ledger | *append-only*: **sem *backfill*** (a *trigger* recusa `UPDATE`) | S3.01 |
| Procure bloqueios | `select pid, wait_event_type, pg_blocking_pids(pid), query from pg_stat_activity where state <> 'idle'` | S3.01 |

## Particionamento (S3.02)
| Tema | Regra / comando | Onde |
|---|---|---|
| Criar | `CREATE TABLE t (...) PARTITION BY RANGE (created_at)`; `CREATE TABLE t_2026_11 PARTITION OF t FOR VALUES FROM ('2026-11-01') TO ('2026-12-01')` | S3.02 |
| PK / `UNIQUE` | **devem conter a chave de partição** (`PRIMARY KEY (id, created_at)`) | S3.02 |
| Poda | filtro **fechado** (`>= a AND < b`); conte as partições no `EXPLAIN` | S3.02 |
| `DEFAULT` | **evite**: impede `DETACH ... CONCURRENTLY`; use alerta de partições à frente | S3.02 |
| Retenção | `ALTER TABLE t DETACH PARTITION p CONCURRENTLY; DROP TABLE p;` (ms; ~400 kB de WAL × `DELETE` de 76 MB) | S3.02 |
| Converter a existente | índice único `(id, created_at)` `CONCURRENTLY` + `CHECK` da faixa `NOT VALID`/`VALIDATE` → transação curta: renomear, criar particionada, `ATTACH` (≈150–190 ms, 0 falhas) | S3.02 |
| Falta de partição | `no partition of relation "t" found for row` | S3.02 |

## Réplicas e roteamento (S3.03)
| Tema | Regra / comando | Onde |
|---|---|---|
| Atrasos | `select write_lag, flush_lag, replay_lag from pg_stat_replication` (o usuário sente o `replay_lag`) | S3.03 |
| Conflito | `canceling statement due to conflict with recovery`; `max_standby_streaming_delay` × `hot_standby_feedback` (feedback = *bloat* no primário) | S3.03 |
| Spring | `AbstractRoutingDataSource` **+ `LazyConnectionDataSourceProxy`** (sem ele: 0/10 na réplica) | S3.03 |
| *Read-your-writes* | token = `pg_current_wal_lsn()` pós-*commit*; réplica só se `pg_last_wal_replay_lsn() >= token` | S3.03 |
| PayFlow | roteamento **explícito** do `reporting`; saldo/transferência/idempotência no **primário** | S3.03 |

## Arquivamento (S3.04)
| Tema | Regra / comando | Onde |
|---|---|---|
| Ciclo | impressão digital (`count`, `sum`, `md5(string_agg(... ORDER BY id))`) → `DETACH` → `COPY ... TO PROGRAM 'gzip -c > f.csv.gz'` + `sha256sum` → **restaurar e comparar** → manifesto → `DROP` | S3.04 |
| Saldo | gravar `period_balances` **antes**; invariante = abertura + Σ vivo | S3.04 |
| Quem remove | papel **dono** (arquivamento), **não** a aplicação | S3.04 |

## Busca (S3.05)
| Tema | Regra / comando | Onde |
|---|---|---|
| `LIKE '%x%'` | varredura (~220 ms em 1 milhão) | S3.05 |
| Trigrama | `CREATE EXTENSION pg_trgm; CREATE INDEX ... USING gin (col gin_trgm_ops)` (índice grande; erro de digitação caro) | S3.05 |
| *Full-text* | `CREATE TEXT SEARCH CONFIGURATION pt_unaccent (COPY = portuguese)` + `unaccent`; `tsv tsvector GENERATED ALWAYS AS (to_tsvector('pt_unaccent', col)) STORED` + GIN | S3.05 |
| Escopo | dentro de **uma conta** (índice por `account_id`): ~5 ms; sem motor | S3.05 |
| OpenSearch | só pelos gatilhos G1–G4; `dynamic: strict`, `_bulk` com `_id`, *alias* + `_reindex`, filtro por dono no servidor (**não executado**) | S3.05 |

## CDC (S3.06)
| Tema | Regra / comando | Onde |
|---|---|---|
| Ligar | `wal_level = logical`; `select pg_create_logical_replication_slot('s','pgoutput')` | S3.06 |
| Ler (estudo) | `pg_logical_slot_peek_changes('s',NULL,NULL)` (não avança) × `..._get_changes` (avança) | S3.06 |
| Retenção do *slot* | `select slot_name, wal_status, pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) from pg_replication_slots` | S3.06 |
| Proteção | `max_slot_wal_keep_size` **abaixo do espaço livre** + alerta de retenção + `heartbeat` | S3.06 |
| Evento | `REPLICA IDENTITY FULL` traz o antigo; `unchanged-toast-datum` = coluna grande não alterada | S3.06 |
| Debezium (**não executado**) | `PostgresConnector`, `plugin.name=pgoutput`, `publication.autocreate.mode=filtered`, `EventRouter`; outbox **só-insere** | S3.06 |

## Armazenamento, analytics e migração de coluna (S3.07–S3.09)
| Tema | Regra / comando | Onde |
|---|---|---|
| Seis perguntas | acesso · consistência · volume · invariantes · quem opera · custo de errar | S3.07 |
| BRIN | `CREATE INDEX ... USING brin (created_at) WITH (pages_per_range = 32)` (48 kB × 64 MB); só com correlação física alta | S3.07 |
| Escada de analytics | primário → réplica → visão materializada → resumo incremental → armazém → lago | S3.08 |
| Carga incremental | **janela recalculada** com `ON CONFLICT ... DO UPDATE SET x = EXCLUDED.x` (`=`, nunca `+=`) + reconciliação semanal | S3.08 |
| Visão materializada | `REFRESH MATERIALIZED VIEW CONCURRENTLY` exige índice único; não é mais rápido, só não bloqueia leitura | S3.08 |
| Migração de coluna | 5 fases, 4 portões; *trigger* mantém a nova; verificação de deriva (= 0); **mutante** prova o portão; leitura-sombra sem dado pessoal nos *logs* | S3.09 |
| Concordância SQL × Java | uma tabela de casos (`phone-cases.tsv`) roda contra as duas + todos os registros | S3.09 |
| Coluna gerada (PG 18) | `VIRTUAL` não aceita função do usuário; `STORED` reescreve a tabela | S3.09 |
