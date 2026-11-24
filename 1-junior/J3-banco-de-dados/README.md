# J3 — Bancos de dados relacionais

> 🟢 Júnior · ⏱️ 19.5h (13 sessões de 1h30) · Status: ✅ concluído (aulas geradas em 2026-11-12)

## 🎯 Objetivo do módulo
Modelar, consultar e versionar um banco PostgreSQL, e acessá-lo com JDBC de forma segura.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [J3.01](J3.01-modelagem-entidades-relacionamentos-er.md) | Modelagem: entidades, relacionamentos e diagrama ER | 1h30 |
| [J3.02](J3.02-chaves-constraints-normalizacao.md) | Chaves, constraints e normalização (1FN a 3FN) | 1h30 |
| [J3.03](J3.03-postgresql-docker-primeiros-comandos.md) | PostgreSQL com Docker e primeiros comandos | 1h30 |
| [J3.04](J3.04-sql-ddl-dml.md) | SQL I: DDL e DML | 1h30 |
| [J3.05](J3.05-sql-select-where-order-limit.md) | SQL II: SELECT, `WHERE`, `ORDER BY`, `LIMIT` | 1h30 |
| [J3.06](J3.06-sql-joins.md) | SQL III: JOINs | 1h30 |
| [J3.07](J3.07-sql-agregacoes-group-by-subqueries.md) | SQL IV: agregações, `GROUP BY` e subqueries | 1h30 |
| [J3.08](J3.08-sql-ctes-funcoes-de-janela.md) | SQL V: CTEs e funções de janela básicas | 1h30 |
| [J3.09](J3.09-indices-e-explain.md) | Índices e `EXPLAIN` básico | 1h30 |
| [J3.10](J3.10-transacoes-e-acid.md) | Transações e ACID | 1h30 |
| [J3.11](J3.11-jdbc-e-sql-injection.md) | JDBC puro e SQL injection com `PreparedStatement` | 1h30 |
| [J3.12](J3.12-migracoes-flyway.md) | Migrações com Flyway | 1h30 |
| [J3.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Ambiente de referência: JDK 25, Maven 3.9.12, PostgreSQL 18 (Docker `postgres:18`), driver `org.postgresql:postgresql` 42.7.14, Flyway 13.12.2, JUnit 6.1.3, AssertJ 3.27.7 (ver `00-guia/versoes.md`). As aulas J3.03 a J3.12 pressupõem o container `payflow-pg` no ar.
>
> **Aviso de ordem:** J3.02 mostra um trecho de `CREATE TABLE` **só para leitura** (a sintaxe é ensinada em J3.04) e J3.03 usa `docker run` como ferramenta (Docker é estudado a fundo em J9); ambos estão sinalizados nas aulas.

## 🏗️ Entrega no PayFlow
Esquema do PayFlow (`users`, `accounts`, `transfers`, `ledger_entries`) em migrações Flyway, com seed e acesso JDBC, testado contra um PostgreSQL real. O repositório `payflow` passa a ser um **projeto Maven** (`com.payflow`). Tag `payflow-J3`.

## ➡️ Próximo
[J4 — Front-end essencial para dev backend](../J4-front-end-essencial/README.md)
