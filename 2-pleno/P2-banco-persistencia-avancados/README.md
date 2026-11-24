# P2 — Banco de dados e persistência avançados

> 🔵 Pleno · ⏱️ 18h (12 sessões de 1h30) · Status: ✅ concluído (2026-11-13) — **validado na execução final (2026-11-24); veja o bloco abaixo**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído em `~/docs/payflow`, tag `payflow-P2`).** **Executado:** laboratório: **33/33 `*IT`** (PostgreSQL 18 + Redis 8, `persistence-lab`) e os experimentos `exp1`–`exp3` (isolamento, *lost update*, *deadlock*) com os mesmos fatos-chave das aulas (*deadlock detected*, `could not serialize`). No PayFlow: `lockAllOrdered` + `DeadlockPreventionTest` (**175/200 falhas sem a trava, 0/200 com ela**), `ConcurrentTransfersInvariantsTest` com **0 conflitos repetidos**, V5 (`CREATE INDEX CONCURRENTLY`) e V6, e a prova de que a V5 **trava** o Flyway sem `transactional-lock: false`; `mvn verify` com **259 testes**, Checkstyle 0. **Corrigido nas aulas:** P2.02: o `submit` da J6.03 carregava a conta de origem antes do `lockAllOrdered` (instância velha → `UPDATE ... WHERE version` falha → 409 na corrida da mesma chave): troca por `existsByIdAndOwnerId`; P2.05: contagem de migrações do teste da J5.11 (7 aplicadas, versão 6). **Ainda não executado:** retry com `@Retryable` (`TransferRetryFacade`), cursor/`StatementService`, `PoolExhaustionTest`, cache Redis no PayFlow (`CacheConfig`, `BalanceCacheTest`), `LedgerEntryImporter`, visão materializada V7/V8 e o caso do extrato lento com 5 milhões de linhas (o `sql-lab` com `10-big-data.sql` não foi reexecutado: ~1 GB de disco). Os avisos "não executado" / "revisado por leitura" nas aulas descrevem a escrita original.

## 🎯 Objetivo do módulo
Dominar **consistência, locking e desempenho de consultas e de JPA**: saber **o que cada nível de isolamento deixa passar**, **escolher e provar** a proteção de uma operação (`UPDATE` atômico, `@Version`, `FOR UPDATE`), **ler um plano de execução**, **projetar índices** medindo o seu custo, **dimensionar o pool**, **contar os comandos SQL do JPA**, **cachear só o que vale** (e tratar o que dá errado) e **conduzir uma investigação de lentidão** do chamado à ADR.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P2.01](P2.01-niveis-de-isolamento-e-anomalias.md) | Níveis de isolamento e anomalias (experimentos em duas sessões) | 1h30 |
| [P2.02](P2.02-lost-update-locking-otimista-e-pessimista.md) | *Lost update*, locking otimista (`@Version`) e pessimista | 1h30 |
| [P2.03](P2.03-deadlocks-no-banco.md) | *Deadlocks* no banco | 1h30 |
| [P2.04](P2.04-planos-de-execucao-e-explain-analyze.md) | Planos de execução e `EXPLAIN ANALYZE` | 1h30 |
| [P2.05](P2.05-estrategia-de-indices.md) | Estratégia de índices (compostos, parciais, cobertura) | 1h30 |
| [P2.06](P2.06-pool-de-conexoes-hikaricp-e-dimensionamento.md) | Pool de conexões (HikariCP) e dimensionamento | 1h30 |
| [P2.07](P2.07-performance-de-jpa-fetch-joins-entity-graphs-e-projecoes.md) | Performance de JPA: *fetch joins*, *entity graphs* e projeções | 1h30 |
| [P2.08](P2.08-batch-cache-de-segundo-nivel-sql-nativo-jooq-e-jdbcclient.md) | *Batch*, cache de segundo nível, SQL nativo, jOOQ e `JdbcClient` | 1h30 |
| [P2.09](P2.09-redis-e-cacheable-ttl-invalidacao-e-cache-stampede.md) | Redis e `@Cacheable`: TTL, invalidação e *cache stampede* | 1h30 |
| [P2.10](P2.10-visao-de-nosql-mongodb-e-dynamodb.md) | Visão de NoSQL (MongoDB, DynamoDB) | 1h30 |
| [P2.11](P2.11-estudo-de-caso-o-extrato-lento.md) | 🏗️ Estudo de caso: o extrato lento com 50 milhões de linhas (**5 milhões medidos**; o resto, extrapolação marcada) | 1h30 |
| [P2.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratórios prontos em [`lab/`](lab/README.md): `lab/sql-lab/` (SQL + `psql`/`pgbench` contra um PostgreSQL 18 em Docker) e `lab/persistence-lab/` (Maven, Spring Boot 4.1.2, **33 testes de integração** contra PostgreSQL e Redis reais).

> ⚠️ **Aviso de verificação (leia antes de estudar):** os **experimentos** (isolamento e *write skew*, *lost update* e as formas de proteção, *deadlock*, 8 consultas do extrato sobre **5 milhões de linhas** com `EXPLAIN`, índices e seu custo de escrita, `CREATE INDEX CONCURRENTLY`, curva de vazão do pool, contagem de comandos SQL do Hibernate, *batch*, cache de 2º nível, `JdbcClient`, cache Redis com seus modos de falha, o autobloqueio do Flyway com `CONCURRENTLY`) foram **executados de verdade**. **Não executado:** **tudo** o que toca no **PayFlow do aluno** (classes, migrações, testes e `compose.yaml` novos: revisados **por leitura**), o **jOOQ**, **MongoDB** e **DynamoDB** (aula de visão, só documentação), o **Redis com senha/TLS/ElastiCache**, o RDS e o **volume de 50 milhões** do título do estudo de caso. **Os tempos variam** (máquina de 12 núcleos compartilhada); valem a **ordem de grandeza** e a **contagem de comandos**.

> **Avisos de ordem:** a P2.01 **reabre** o isolamento que a J3.10 só introduziu; a P2.02 **fecha** a ADR 0013 (otimista × `FOR UPDATE` × `UPDATE` condicional); a P2.07 **estende** a J5.12 (N+1) com coleções e projeções; a P2.09 (Redis) usa o *wiring* de segredos/Compose da J9 e a LGPD da J8.07. **Nenhum conceito de P3+ é pré-requisito** (mensageria, *outbox*, Kafka aparecem só como destino; o `FOR UPDATE SKIP LOCKED` da P2.02 é a ponte para a fila em tabela da P3).

## 🏗️ Entrega no PayFlow
Travas **ordenadas** nas transferências (`lockAllOrdered`) com `@Version` mantido e *retry* fora da transação; **índice de cobertura** (`V5`/`V6`, `CREATE INDEX CONCURRENTLY`), predicados sem função na coluna e **paginação por cursor**; resumo mensal como **modelo de leitura** (`V7`); pool dimensionado; leitura JPA **contada e testada**; *batching*; **cache Redis só para exibição** (`immediateWrites`, *evict* após o *commit*, tolerante à queda); `docs/` de decisão e **ADR 0014**. Tag `payflow-P2`. **Tudo por leitura** (ver o checkpoint).

## ➡️ Próximo
[P3 — Comunicação síncrona e assíncrona](../P3-comunicacao-sincrona-assincrona/README.md)
