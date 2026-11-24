# J3.CP — Checkpoint do módulo J3

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J3.01 a J3.12

## Parte 1 — Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Qual a diferença entre um relacionamento **1:N** e **N:N**? Onde fica a chave estrangeira em cada caso?
2. Por que o PayFlow usa um `id` UUID como chave primária **e** uma restrição `UNIQUE` em `users.email`?
3. Uma tabela `transfers(id, source_account, owner_name, owner_email, amount)` está em qual forma normal? O que viola a 3FN e como corrigir? Dê um caso em que **desnormalizar** é justificável.
4. Escreva (em palavras ou SQL) como listar os usuários **sem conta**. Por que mover `a.status = 'ACTIVE'` do `ON` para o `WHERE` em um `LEFT JOIN` muda o resultado?
5. O que `WHERE transfer_id = NULL` devolve? E `id NOT IN (SELECT transfer_id FROM ledger_entries)` quando há depósitos (com `transfer_id` nulo)?
6. Como conferir, com SQL, que o saldo de cada conta é igual à soma dos lançamentos? Qual o resultado esperado e por que o `LEFT JOIN` é importante?
7. O que é um índice B-tree e qual o custo de ter muitos? Cite **três** razões para o banco **não** usar um índice existente. Em um índice `(account_id, created_at DESC)`, o que importa na ordem das colunas?
8. Explique ACID com a transferência do PayFlow. O que acontece se o servidor cair entre o débito e o crédito? E se o `INSERT` do lançamento falhar depois do débito?
9. Por que `PreparedStatement` impede SQL injection? Por que `ORDER BY ?` não funciona e como permitir ordenação escolhida pelo cliente? Por que usar `BigDecimal`/`OffsetDateTime` ao ler `NUMERIC`/`TIMESTAMPTZ`?
10. Por que não se edita uma migração do Flyway já aplicada? O que acontece com um arquivo chamado `v5_add_phone.sql`? Quando `flyway.clean()` é aceitável?

<details><summary>Gabarito</summary>

1. 1:N: uma linha do lado "1" se liga a várias do lado "N" (um usuário, várias contas); a chave estrangeira fica no lado **N** (`accounts.owner_id`). N:N: várias de cada lado (contas conjuntas); precisa de uma **tabela associativa** com duas chaves estrangeiras (e atributos da ligação, se houver).
2. O UUID é uma chave **substituta**: estável, sem significado de negócio, copiada nas FKs, não enumerável (IDOR, J8). O e-mail é uma chave **candidata** que muda e identifica o login: o banco precisa impedir duplicidade com `UNIQUE`. Sem o `UNIQUE`, a chave substituta permitiria dois logins com o mesmo e-mail.
3. 1FN e 2FN (chave simples), mas **viola a 3FN**: `owner_name` e `owner_email` dependem da **conta** (`source_account`), não da transferência (dependência transitiva `transfer → conta → dono`). Correção: tirar os dados do dono para `users`/`accounts` e guardar só a chave da conta. Desnormalizar é justificável com leitura medida como gargalo e um mecanismo que garanta a consistência: o `accounts.balance` (soma dos lançamentos) conferido pela consulta de reconciliação, ou o `transfers.amount` como fotografia imutável.
4. `SELECT u.full_name FROM users u LEFT JOIN accounts a ON a.owner_id = u.id WHERE a.id IS NULL` (ou `NOT EXISTS`). No `ON`, a condição só decide **quais contas casam**; os usuários continuam aparecendo (com `NULL`). No `WHERE`, ela roda **depois** do join e **descarta** as linhas sem conta ativa, transformando o `LEFT JOIN` em `INNER JOIN`.
5. `= NULL` nunca é verdadeiro: devolve **0 linhas** (use `IS NULL`). O `NOT IN` com uma subquery que contém `NULL` também devolve **0 linhas** (a comparação com `NULL` é "desconhecida"). Use `NOT EXISTS` ou filtre `IS NOT NULL` na subquery.
6. Agrupar os lançamentos por conta (créditos − débitos), juntar com `accounts` por `LEFT JOIN` e listar as contas em que `balance <> COALESCE(soma, 0)`. O resultado saudável é **vazio** (0 linhas). O `LEFT JOIN` e o `COALESCE` garantem que contas **sem lançamentos** (saldo deve ser 0) também sejam conferidas; com `JOIN`, uma conta com saldo indevido e sem lançamentos passaria despercebida.
7. Uma estrutura ordenada (árvore balanceada) que permite achar linhas em O(log n) sem varrer a tabela. Custo: espaço e escrita mais lenta (cada insert/update/delete mantém todos os índices: medimos ~11x com três índices). Não é usado por: tabela pequena; consulta que devolve grande parte da tabela (pouca seletividade); função/expressão aplicada à coluna; `LIKE '%x'`; filtrar só a 2ª coluna de um composto; estatísticas desatualizadas. Em `(a, b)`: igualdade primeiro (`a`), ordenação/faixa depois (`b`); serve a `a` e `a + b`, em geral não só a `b`.
8. A: tudo ou nada (débito, crédito, lançamentos e status juntos); C: as constraints (`CHECK balance >= 0`) são respeitadas ou a transação falha; I: quem consulta não vê o débito não confirmado; D: depois do `COMMIT`, sobrevive a falhas (WAL). Se o servidor cair antes do `COMMIT`, nada foi confirmado e o banco descarta a transação ao reiniciar: nenhuma conta fica "meio debitada". Se o `INSERT` do lançamento falhar, o `rollback` desfaz **também** o débito e o crédito.
9. O SQL é fixo e os valores vão **separados** (mensagens diferentes do protocolo): o valor nunca vira parte da estrutura. Um `?` é um **valor**, não um nome de coluna; use uma **lista permitida** (mapa de nomes públicos para colunas reais) e uma direção restrita a `ASC`/`DESC`. `BigDecimal` mantém a exatidão (`double` é aproximado); `OffsetDateTime` mantém o instante (`getTimestamp` depende do fuso da JVM).
10. O Flyway guarda o checksum: editar causa `Migration checksum mismatch`, e mesmo "consertando" com `repair` os bancos onde a versão antiga já rodou **não** mudam (divergem). Cria-se uma versão nova. `v5_add_phone.sql` (minúscula e um underscore) **não casa** com o padrão `V<n>__descrição.sql` e é **ignorado em silêncio**. `clean` apaga tudo e vem desabilitado por padrão: só em **banco de teste**, habilitado de propósito (`cleanDisabled(false)`).
</details>

## Parte 2 — Entrega prática (tag `payflow-J3`)
O repositório `payflow` agora é um **projeto Maven** com o esquema do PayFlow em **migrações Flyway**, seed de desenvolvimento e acesso **JDBC** testado.

Estrutura esperada:
```
payflow/
├── pom.xml
├── README.md                       (inclui "Banco local" e "Como rodar os testes")
├── scripts/db-up.sh, psql.sh       (J3.03)
├── docs/
│   ├── er-diagram.md               (J3.01)
│   ├── db-constraints.md           (J3.02, J3.04)
│   ├── queries/                    (statement.sql, statement_with_counterparty.sql, reconciliation.sql, statement_running_balance.sql)
│   ├── performance-notes.md        (J3.09)
│   ├── flyway-notes.md             (J3.12)
│   ├── lab_entries.sql, transfer.sql
├── src/main/resources/db/migration/   V1__create_users.sql ... V4__create_indexes.sql
├── src/main/resources/db/seed/        R__dev_seed.sql
├── src/main/java/com/payflow/db/      Database, Amounts, User, Account, LedgerEntry, Transfer,
│                                      DataAccessException, InsufficientFundsException, AccountUnavailableException,
│                                      JdbcUserRepository, JdbcAccountRepository, TransferService, Demo
└── src/test/java/com/payflow/db/      DbTestSupport, RepositoryTest, TransferServiceTest, demo/{JdbcTour, SqlInjectionDemo, CrossTransfersDemo}
```

Critérios de pronto:
- [ ] **Modelo:** `docs/er-diagram.md` com as 4 tabelas, os dois relacionamentos `accounts` → `transfers` e a ligação opcional `transfers` → `ledger_entries`.
- [ ] **Banco local:** `scripts/db-up.sh` idempotente; PostgreSQL **18** no Docker com volume em `/var/lib/postgresql`; `pg_isready` ok.
- [ ] **Esquema:** migrações `V1` a `V4` (tabelas, constraints com nomes `pk_/uk_/ck_/fk_`, `NUMERIC(19,2)`, `TIMESTAMPTZ`, UUID `uuidv7()`, índices nas FKs e `(account_id, created_at DESC)`) e `R__dev_seed.sql` repetível e idempotente em `db/seed`.
- [ ] **Regras no banco:** regra 1 (`CHECK balance >= 0`), regra 4 (`UNIQUE idempotency_key`), FKs **sem** `ON DELETE CASCADE`; as regras 2, 3 e 5 documentadas em `docs/db-constraints.md`.
- [ ] **Consultas** de `docs/queries/` executadas no seed e conferidas; a conferência (regras 2 e 3) devolve **0 linhas**.
- [ ] **Índices:** FKs sem índice = 0 (consulta da J3.09); `docs/performance-notes.md` com os **seus** tempos de Seq Scan × Index Scan na tabela de laboratório.
- [ ] **JDBC:** `Database`, repositórios e `TransferService` com `PreparedStatement` em todo SQL, `try-with-resources`, `BigDecimal`/`OffsetDateTime`/`UUID`, exceções com causa; transferência em **uma** transação com *rollback*, idempotência e valor com no máximo 2 casas.
- [ ] **SQL injection:** a demonstração vulnerável só em `src/test`; os 3 ataques funcionam nela e **falham** na versão com parâmetros.
- [ ] **Flyway:** `Demo` num schema vazio aplica 5 migrações e na segunda execução não aplica nada; `flyway_schema_history` com 5 linhas `success = t`.
- [ ] **Testes:** `mvn test` com **11 testes** verdes (JDK 25, JUnit 6.1.3, AssertJ), banco `payflow_test` separado e **PostgreSQL real** (nada de H2); `cleanDisabled(false)` só no `DbTestSupport`.
- [ ] **Limite conhecido registrado:** `docs/flyway-notes.md` ou o README descreve o *deadlock* das transferências cruzadas (`40P01`) e que a correção entra em P2.
- [ ] **Sem segredos:** a senha `payflow` é só do Docker local; nada de credenciais reais no repositório.
- [ ] Commits em Conventional Commits (inglês), PR mesclado e tag enviada:
```bash
git tag payflow-J3
git push origin payflow-J3
```

## Parte 3 — Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Modelo uma situação com entidades, relacionamentos (1:1, 1:N, N:N) e diagrama ER | ☐ | ☐ | ☐ |
| Escolho chaves e constraints e normalizo até a 3FN (sabendo quando desnormalizar) | ☐ | ☐ | ☐ |
| Subo um PostgreSQL local com Docker e uso o `psql` | ☐ | ☐ | ☐ |
| Escrevo DDL e DML corretos e leio as mensagens de erro das constraints | ☐ | ☐ | ☐ |
| Escrevo `SELECT` com filtros, ordenação determinística, paginação e `NULL` corretos | ☐ | ☐ | ☐ |
| Escrevo JOINs (incluindo anti-join e múltiplos apelidos) e evito o *fan-out* | ☐ | ☐ | ☐ |
| Escrevo agregações, subqueries, CTEs e funções de janela básicas | ☐ | ☐ | ☐ |
| Crio índices com critério e leio um `EXPLAIN` básico | ☐ | ☐ | ☐ |
| Uso transações e explico ACID, autocommit e estado abortado | ☐ | ☐ | ☐ |
| Uso JDBC com `PreparedStatement` e transações, e explico SQL injection e a defesa | ☐ | ☐ | ☐ |
| Versiono o esquema com Flyway e entendo checksum, ordem e `clean` | ☐ | ☐ | ☐ |

**Regra:** se ficou com mais de duas linhas em 0, repita as aulas correspondentes antes de seguir para o [J4](../J4-front-end-essencial/README.md). Atualize `00-guia/progresso.md` e o `cronograma.md`.

## Referência do que foi construído
O código completo e testado está nas aulas: [J3.11](J3.11-jdbc-e-sql-injection.md) (`pom.xml` sem Flyway, `Database`, registros, exceções, repositórios, `TransferService`, testes e demonstrações) e [J3.12](J3.12-migracoes-flyway.md) (a versão **final**, com Flyway, migrações, seed e `DbTestSupport`). Use-o para **conferir** a sua versão depois de tentar; o objetivo é você escrever a sua. Falta apenas o código do `JdbcTour`, citado na J3.11 (conferido com PostgreSQL 18.6, JDK 25.0.5 e o driver 42.7.14):

<details><summary><code>src/test/java/com/payflow/db/demo/JdbcTour.java</code></summary>

```java
package com.payflow.db.demo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Passeio pelos tipos do JDBC e pelos erros mais comuns (rode com o PostgreSQL do J3.03 no ar). */
public class JdbcTour {
    static final String URL = "jdbc:postgresql://localhost:5432/payflow";

    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection(URL, "payflow", "payflow");
             PreparedStatement ps = c.prepareStatement("SELECT id, balance, created_at FROM accounts WHERE id = ?")) {
            ps.setObject(1, UUID.fromString("20000000-0000-0000-0000-000000000001"));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                System.out.println("id           = " + rs.getObject("id", UUID.class));
                System.out.println("BigDecimal   = " + rs.getBigDecimal("balance"));
                System.out.println("double       = " + rs.getDouble("balance"));
                System.out.println("OffsetDateTime = " + rs.getObject("created_at", OffsetDateTime.class));
                System.out.println("getTimestamp = " + rs.getTimestamp("created_at") + "  (depende do fuso da JVM!)");
                try {
                    System.out.println("Instant      = " + rs.getObject("created_at", Instant.class));
                } catch (SQLException e) {
                    System.out.println("Instant      -> " + e.getMessage());
                }
                System.out.println("-> Instant   = " + rs.getObject("created_at", OffsetDateTime.class).toInstant());
            }
            System.out.println("autoCommit padrão = " + c.getAutoCommit());
        }

        System.out.println("-- erros");
        try {
            DriverManager.getConnection("jdbc:postgresql://localhost:5999/payflow", "payflow", "payflow");
        } catch (SQLException e) {
            System.out.println("E1 " + e.getMessage() + " | SQLState=" + e.getSQLState());
        }
        try {
            DriverManager.getConnection(URL, "payflow", "errada");
        } catch (SQLException e) {
            System.out.println("E2 " + e.getMessage() + " | SQLState=" + e.getSQLState());
        }
        try {
            DriverManager.getConnection("jdbc:postgresql://localhost:5432/naoexiste", "payflow", "payflow");
        } catch (SQLException e) {
            System.out.println("E3 " + e.getMessage() + " | SQLState=" + e.getSQLState());
        }
        try (Connection c = DriverManager.getConnection(URL, "payflow", "payflow")) {
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM account WHERE id = ?")) {
                ps.setObject(1, UUID.randomUUID());
                ps.executeQuery();
            } catch (SQLException e) {
                System.out.println("E4 " + e.getMessage().replace("\n", " ") + " | SQLState=" + e.getSQLState());
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM accounts WHERE id = ? AND owner_id = ?")) {
                ps.setObject(1, UUID.randomUUID());
                ps.executeQuery();
            } catch (SQLException e) {
                System.out.println("E5 " + e.getMessage());
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users (email, full_name, password_hash) VALUES (?, 'x', 'y')")) {
                ps.setString(1, "ana@payflow.dev");
                ps.executeUpdate();
            } catch (SQLException e) {
                System.out.println("E6 " + e.getMessage().replace("\n", " ") + " | SQLState=" + e.getSQLState());
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT balance FROM accounts WHERE id = ?")) {
                ps.setObject(1, UUID.randomUUID());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.getBigDecimal(1);                       // esqueceu o rs.next()
                }
            } catch (SQLException e) {
                System.out.println("E7 " + e.getMessage() + " | SQLState=" + e.getSQLState());
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT balance FROM accounts WHERE id = ?")) {
                ps.setObject(1, UUID.fromString("20000000-0000-0000-0000-000000000001"));
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    rs.getBigDecimal("saldo");                 // coluna que não existe no SELECT
                }
            } catch (SQLException e) {
                System.out.println("E8 " + e.getMessage() + " | SQLState=" + e.getSQLState());
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT ?::numeric(19,2) AS v")) {
                ps.setBigDecimal(1, new BigDecimal("10.005"));
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    System.out.println("10.005 gravado em NUMERIC(19,2) = " + rs.getBigDecimal(1)
                            + "  (o banco arredonda; Java HALF_EVEN daria "
                            + new BigDecimal("10.005").setScale(2, java.math.RoundingMode.HALF_EVEN) + ")");
                }
            }
        }
    }
}
```
</details>

## O que vem a seguir
O banco existe, tem esquema versionado e é acessado com JDBC. Na [J4](../J4-front-end-essencial/README.md) você constrói o lado do navegador (HTML, CSS, JavaScript) e consome a API; na [J5](../J5-spring-boot/README.md), o Spring Boot assume a conexão, o pool, as migrações (os mesmos arquivos em `db/migration`) e o mapeamento objeto-relacional (JPA), e você verá o que o framework faz **por você** em cima do que aprendeu aqui.
