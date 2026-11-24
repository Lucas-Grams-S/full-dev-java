# Laboratório do S3 — Dados em escala

Scripts, programas Java e **saídas reais** usadas nas aulas. **Tudo é um modelo** (tabelas simplificadas, dados sintéticos, uma máquina), **não** o PayFlow.

## Requisitos
- Docker (imagem `postgres:18`), bash, **JDK 25** (`/usr/lib/jvm/java-25-openjdk-amd64`) e, para o `RoutingLab`, **spring-jdbc 7.0.10** (+ `spring-tx`, `spring-core`, `spring-beans`, `spring-context`, `spring-aop`, `spring-expression`, `commons-logging 1.3.5`, `jspecify 1.0.1`) e o **driver PostgreSQL 42.7.14** no `~/.m2`.
- ~6 GB livres de memória/disco; **~15 min** para tudo.

## Como rodar
```bash
./run-all.sh                    # sobe lab-s3-pg, semeia (~40 s), roda tudo e regrava os out-*.txt
ONLY="s301 s305" ./run-all.sh   # só alguns (semeia de novo)
```
`run-all.sh` cria o contêiner `lab-s3-pg` (um volume anônimo) e **o remove ao final**; `run-s303.sh` cria e remove **seu próprio** par primário/réplica (`lab-s3-primary`, `lab-s3-replica`, rede e volumes `lab-s3-*`). Nada fica para trás.

## Base de dados
`schema.sql` (users, accounts, transfers, ledger_entries com a *trigger* append-only) + `seed.sql` (**200 mil** usuários e contas, **3 milhões** de lançamentos em 24 meses de `2024-11` a `2026-10`, **1 milhão** de transferências). `lib.sh` tem `pg_up`, `reset_db` (copia o *template* semeado em ~6 s) e `pg_down`.

## Scripts e saídas
| Script | Aula | Saídas | O que mede |
|---|---|---|---|
| `run-s301.sh` | S3.01 | `out-s301-add-column.txt`, `-lock-queue.txt`, `-not-null-fk.txt`, `-backfill.txt` | *default* constante × volátil; **fila de *locks***; `NOT NULL`/FK em dois passos (e PG 18); *backfill* único × lotes com `pgbench` |
| `run-s302.sh` | S3.02 | `out-s302-partitioning.txt`, `-attach-hash.txt` | poda, **PK com a chave**, `DEFAULT` × `DETACH CONCURRENTLY`, retenção, `ATTACH` com/sem `CHECK`, *hash* |
| `run-s302-swap.sh` | S3.02 | `out-s302-swap.txt` | **troca** do ledger existente em particionado **com carga** (0 falhas) |
| `run-s303.sh` | S3.03 | `out-s303-replica.txt`, `-routing.txt` + `RoutingLab.java` | `pg_stat_replication`; conflito de recuperação (`hot_standby_feedback`); roteamento Spring, **token LSN**, réplica atrasada/parada |
| `run-s304.sh` | S3.04 | `out-s304-archive.txt` | permissões; ciclo de arquivamento (impressão digital → *detach* → CSV.gz → **restaurar e comparar** → manifesto → `DROP`); `period_balances` |
| `run-s305.sh` | S3.05 | `out-s305-search.txt`, `-write.txt`, `-scope.txt` | `ILIKE` × `pg_trgm` × *full-text* (português + `unaccent`); custo de escrita; escopo por conta |
| `run-s306.sh` | S3.06 | `out-s306-decoding.txt`, `-slot.txt`, `-outbox.txt` | *logical decoding* (`test_decoding`), `REPLICA IDENTITY`, `TOAST`, `peek` × `get`, ***slot* abandonado** e `max_slot_wal_keep_size`, outbox |
| `run-s307.sh` | S3.07 | `out-s307-brin.txt` | BRIN × B-tree em coluna de tempo (correlação 0,5 e 1,0) |
| `run-s308.sh` | S3.08 | `out-s308-analytics.txt` | agregado direto × visão materializada × resumo incremental; **dados atrasados** |
| `run-s309.sh` | S3.09 | `out-s309-migration.txt`, `-mutant.txt`, `-cases.txt` + `V18__expand_users_phone_e164.sql`, `PhoneNormalizer.java`, `phone-cases.tsv` | migração de coluna **sob carga**; **mutante** sem *trigger*; Java × SQL |
| `run-s309-generated.sh` | S3.09 | `out-s309-generated.txt` | coluna gerada `VIRTUAL`/`STORED` no PostgreSQL 18 |

## Avisos
- **Milissegundos variam** entre execuções (máquina compartilhada); as aulas citam **ordens de grandeza** e colam a saída.
- O `run-s301.sh` (seção D) e o `run-s309.sh` levam **minutos** (carga de 30 s por modo).
- **Nenhum** número de OpenSearch, Debezium, RDS, S3 ou outro produto foi medido aqui.
- Os telefones e e-mails são **sintéticos**; o `PhoneNormalizer` imprime amostras **só** por isso (em produção: **nunca** logar o número).
