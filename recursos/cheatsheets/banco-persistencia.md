# Cheatsheet: banco avançado e persistência (P2)

> Tudo abaixo foi **executado** no laboratório da P2 (PostgreSQL 18.6, Spring Boot 4.1.2, Hibernate 7.4.13, Redis 8.10.3) salvo onde marcado ⚠️.

## Qual defesa contra *lost update* / corrida?
| Situação | Use | Observação |
|---|---|---|
| Regra cabe em **um** `UPDATE` (depósito, saque simples) | `UPDATE … SET x = x ± ? WHERE id = ? AND x >= ?` | o 2º espera e **reavalia** o `WHERE`; sem *retry* |
| Transferência (2 linhas), contenção alta | `@Lock(PESSIMISTIC_WRITE)` + `order by id` (**mesma consulta**) | vira `for no key update`; ordem global evita *deadlock* |
| Edição longa, colisão rara | `@Version` + *retry* **fora** da transação | `409`/repetir; sob contenção: ≈ 30 tentativas para 8 sucessos |
| Regra de conjunto (limite de contas) | índice único **parcial** (ou `SERIALIZABLE` + *retry*) | `REPEATABLE READ` **não** basta (*write skew*) |
| Fila em tabela | `FOR UPDATE SKIP LOCKED` | visão inconsistente de propósito |
| Limitar a espera | `NOWAIT`, `SET LOCAL lock_timeout = '3s'` | → `CannotAcquireLockException` (2–4 ms / ≈ 500 ms medidos) |

## Isolamento (PostgreSQL)
`READ COMMITTED` (padrão): snapshot **por comando** · `REPEATABLE READ`: snapshot **por transação** (sem fantasma; ainda *write skew*) · `SERIALIZABLE`: detecta ciclos, **cobra *retry*** (`40001`). `READ UNCOMMITTED` = `READ COMMITTED`. Relatório com 2 comandos → **1 comando** ou `REPEATABLE READ READ ONLY`.

## Exceções do Spring
`ConcurrencyFailureException` ⊃ `OptimisticLockingFailureException` (`ObjectOptimisticLockingFailureException`) e `PessimisticLockingFailureException` ⊃ `CannotAcquireLockException` (*deadlock* `40P01`, `lock_timeout`/`NOWAIT` `55P03`, serialização `40001`). **Repita a superclasse.** *Retry*: `@Retryable(includes = ConcurrencyFailureException.class, maxRetries = 3, delay = 20, jitter = 20, multiplier = 2)` (Framework 7, `@EnableResilientMethods(order = Ordered.HIGHEST_PRECEDENCE)`), **outro bean**, **mesma** `idempotency_key`.

## Diagnóstico de espera/*deadlock*
```sql
SELECT pid, state, wait_event_type, wait_event, pg_blocking_pids(pid) AS bloqueado_por, now() - xact_start AS idade, left(query, 60)
FROM pg_stat_activity WHERE datname = 'payflow' AND pid <> pg_backend_pid() AND state <> 'idle';
-- log_lock_waits = on  (o log do servidor traz as duas consultas do deadlock)
```

## Planos e índices
| Quero | Faça |
|---|---|
| Plano real | `EXPLAIN (ANALYZE, BUFFERS)` (no PG 18 `BUFFERS` é automático); em escrita: `BEGIN; …; ROLLBACK;` |
| Achar as consultas caras | `pg_stat_statements` (ordene por `total_exec_time`), `auto_explain`, `log_min_duration_statement` |
| Lê e descarta (`OFFSET`) | cursor: `WHERE (created_at, id) < (:ts, :id) ORDER BY created_at DESC, id DESC LIMIT n` |
| Função na coluna (`::date`, `lower`) | intervalo semiaberto `>= início AND < fim` ou índice de expressão |
| Índice composto | **igualdade primeiro**, depois intervalo/ordenação; ordem errada = ≈ 260× mais lento |
| Poucas linhas "interessantes" | parcial `WHERE status = 'PENDING'` (**literal**, não parâmetro: plano genérico o ignora) |
| Só poucas colunas | `INCLUDE (type, amount)` → `Index Only Scan` (`Heap Fetches: 0`); `SELECT *` não é coberto |
| Criar sem parar escritas | `CREATE INDEX CONCURRENTLY` (fora de transação; checar `indisvalid`); **Flyway:** `spring.flyway.postgresql.transactional-lock: false` |
| Índice não usado? | `pg_stat_user_indexes.idx_scan` **por semanas**; **nunca** apague `UNIQUE`/PK/FK-index |

## Pool (HikariCP 7.0.2; padrões conferidos)
`maximum-pool-size` 10 · `minimum-idle` = max · `connection-timeout` 30 000 ms (use ≈ 3 000) · `max-lifetime` 30 min · `keepalive-time` 2 min · `leak-detection-threshold` 0. Conta: **réplicas × pool (+ rolling deploy + manutenção) ≤ `max_connections`**. Pico de vazão ≈ `2 × núcleos` do banco. `open-in-view: false`. Nada de rede dentro de `@Transactional`.

## JPA: contar e escolher
`hibernate.generate_statistics=true` + `Statistics#getPrepareStatementCount()` (teste). N+1 → `@EntityGraph` (ToOne) / projeção `record` via `select new` (leitura) / `default_batch_fetch_size: 16` (rede de segurança). Duas coleções → `MultipleBagFetchException`. Paginação com coleção: **sempre** `ORDER BY`. *Batch*: `hibernate.jdbc.batch_size: 50`, `order_inserts`, `order_updates`, `reWriteBatchedInserts=true`; **provar** por contagem. `JdbcClient` não tem `batchUpdate`.

## Cache Redis (Spring Data Redis 4.1.2)
`RedisCacheWriter.create(f, w -> w.immediateWrites())` (**as escritas são assíncronas por padrão**) · `.transactionAware()` (evict **após** o commit do chamador) · serializador **tipado** `JacksonJsonRedisSerializer<T>` (o genérico sem tipagem devolve `LinkedHashMap`) · TTL **sempre** (padrão do Boot: nunca expira) · `sync = true` só com `cacheLocking` (a trava é **por cache**) · `CacheErrorHandler` + `connect-timeout`/`timeout` curtos · Redis: `maxmemory-policy allkeys-lru` (o padrão é `noeviction`), **sem porta publicada**, senha por segredo. **Meça antes**: `SELECT` por PK ≈ 0,017 ms × `GET` ≈ 0,023 ms.

## `pgbench` (dentro do container, ver `lab/sql-lab`)
`pgbench -U postgres -d payflow -n -f script.sql -c 8 -j 4 -T 4` · `\set x random(1, 20000)` · `\sleep 4 ms` · `-M prepared`.
