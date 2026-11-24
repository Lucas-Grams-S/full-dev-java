# Laboratório da P2

Dois laboratórios; **nenhum** toca no repositório `payflow` do aluno. Requisitos: **Docker** e, para o Maven, **JDK 25** (`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`; o `java` padrão do shell pode ser o 17) e Maven 3.9.

## Containers do laboratório (descartáveis)
```bash
docker run -d --name lab-p2-pg -e POSTGRES_PASSWORD=lab -e POSTGRES_DB=payflow -p 127.0.0.1:55432:5432 \
  --memory 900m --shm-size 256m -v lab-p2-pgdata:/var/lib/postgresql postgres:18 \
  -c shared_buffers=256MB -c log_lock_waits=on -c deadlock_timeout=1s -c max_connections=200 \
  -c shared_preload_libraries=pg_stat_statements,auto_explain -c auto_explain.log_min_duration=50ms -c auto_explain.log_analyze=on
docker run -d --name lab-p2-redis -p 127.0.0.1:56379:6379 --memory 200m redis:8
# ao terminar (limpeza):
docker rm -f lab-p2-pg lab-p2-redis; docker volume rm lab-p2-pgdata
```
Tudo escuta só em `127.0.0.1` (J9). O banco `payflow_jpa` (Maven) é criado à parte: `docker exec -i lab-p2-pg psql -U postgres -c "CREATE DATABASE payflow_jpa"` e `psql … -d payflow_jpa -f sql-lab/00-schema.sql`.

## `sql-lab/` (SQL, duas sessões, `pgbench`)
> ⚠️ **`run.sh` → `reset` apaga e recria o banco `payflow` do container do laboratório** (os experimentos `exp1`, `exp1b`, `exp2`, `exp3` e `regen-and-case-study.sh` o chamam). Rode `10-big-data.sql` **depois** deles (ou `regen-and-case-study.sh`, que refaz tudo). Os scripts `20` a `24`, `30`, `exp6`, `exp7`, `exp8`, `exp9` e `refresh-outputs.sh` **não** chamam `reset`.

| Arquivo | Aula | O que faz |
|---|---|---|
| `00-schema.sql`, `01-seed.sql`, `run.sh` | todas | esquema V1–V4 da J3.12 (extraído da aula), 4 contas de R$ 1 000,00, helpers `psql_a`/`psql_b` |
| `exp1-isolation.sh`, `exp1b-reconciliation.sh` | P2.01 | leitura não repetível/fantasma, `READ UNCOMMITTED`, *write skew* (3 níveis), conciliação com 2 comandos |
| `exp2-lost-update.sh` | P2.02 | *lost update* (RC e RR), `@Version` na mão, `UPDATE` atômico/condicional, `FOR UPDATE`, `NOWAIT`/`SKIP LOCKED`/`lock_timeout` |
| `exp3-deadlock.sh` | P2.03 | *deadlock* cruzado, correção por ordem de `id`, `pg_blocking_pids`, *deadlock* por índice único |
| `10-big-data.sql` | P2.04 a P2.11 | 1 000 usuários, 20 000 contas, **5 000 000** lançamentos, 1 000 000 transferências (≈ 1 min; ≈ 1 GB) |
| `20-explain.sql`, `21-indexes.sql`, `22-indexes-2.sql`, `23-write-cost.sql`, `exp6-create-index.sh` | P2.04, P2.05 | planos do extrato; cobertura, parcial, plano genérico, ordem de colunas, *skip scan*, custo de escrita, `CONCURRENTLY` |
| `transfer.pgbench`, `transfer-with-wait.pgbench`, `exp7-pool.sh` | P2.06 | vazão × nº de conexões (com e sem espera dentro da transação) |
| `24-read-model.sql`, `exp8-case-study.sh`, `exp9-write-tps.sh`, `regen-and-case-study.sh` | P2.11 | visão materializada; consultas antes/depois (S0/S1/S2); vazão de escrita |
| `30-stats-and-statements.sql` | P2.04 | estatística velha e `pg_stat_statements` |
| `out-*.txt` | todas | **saídas reais** que as aulas citam (de uma execução; os tempos variam) |

## `persistence-lab/` (Maven, Spring Boot 4.1.2)
`mvn -B -ntp test -Dtest='*IT'` (os testes são `*IT`: o Surefire **não** os pega pelo padrão; o `mvn test` sem `-Dtest` **não roda nada**). Precisam dos dois containers acima e do banco `payflow_jpa`. Saída resumida em `out-tests.txt`.

| Teste | Aula | Prova |
|---|---|---|
| `ConcurrencyIT` | P2.02, P2.03 | otimista sem/com `@Retryable`, pessimista ordenado, *deadlock*, `UPDATE` atômico, `NOWAIT`, retry curando o *deadlock* |
| `LockTimeoutIT` | P2.02 | `NOWAIT`, *hint* `lock.timeout`, `SET LOCAL lock_timeout` |
| `IsolationIT` | P2.01 | *write skew* em `READ COMMITTED` × `SERIALIZABLE`; `40001` vira `CannotAcquireLockException` |
| `FetchIT`, `BatchFetchIT`, `SqlShapeIT` | P2.07 | comandos SQL (N+1, `@EntityGraph`, *batch fetching*, projeções, *keyset*), `MultipleBagFetchException`, SQL gerado |
| `BatchOffIT`, `BatchOnIT`, `SecondLevelCacheIT`, `JdbcClientIT` | P2.08 | *batch*, L2, `JdbcClient` |
| `PoolIT` | P2.06 | esgotamento do pool, `HikariPoolMXBean`, aviso de vazamento |
| `CacheIT`, `CacheOutageStrictIT`, `CacheOutageTolerantIT` | P2.09 | cache-aside, dado velho, *evict* antes do *commit*, *stampede*, `null`, escritas assíncronas, queda do Redis |
| `FlywayConcurrentIT` | P2.05 | `CREATE INDEX CONCURRENTLY` no Flyway: travamento padrão × `transactional.lock=false` |

**Avisos:** (1) tempos e contagens de repetição **mudam** de execução para execução (máquina compartilhada); (2) o `FlywayConcurrentIT` deixa a migração **travada por 15 s** de propósito; (3) o `exp7`/`exp8` competem por CPU com o banco (o `pgbench` roda **dentro** do container); (4) **limpe** os containers e o volume ao terminar (comandos acima); (5) `target/` não é versionado.
