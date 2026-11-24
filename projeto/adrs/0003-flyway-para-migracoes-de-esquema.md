# ADR 0003 — Flyway para migrações de esquema; seed de desenvolvimento repetível

- **Status:** aceita (J3, 2026-11-12)
- **Contexto:** o esquema precisa ser versionado, reproduzível e aplicado igual em todos os ambientes. O Spring Boot (J5) integra Flyway e Liquibase.
- **Opções:** (1) Flyway com SQL puro; (2) Liquibase (XML/YAML/SQL); (3) scripts manuais; (4) `ddl-auto` do Hibernate.
- **Decisão:** Flyway, com **SQL puro**, em `src/main/resources/db/migration` (`V<n>__descrição.sql`, o *location* padrão do Spring Boot). O seed de dev/teste é a migração **repetível e idempotente** `src/main/resources/db/seed/R__dev_seed.sql` (`ON CONFLICT (id) DO NOTHING`), em *location* separado e habilitado apenas em dev/teste. `clean` só em banco de teste (`cleanDisabled(false)` apenas no `DbTestSupport`). O Hibernate **não** gera o esquema.
- **Consequências:**
  - (+) Histórico auditável no Git; mesmo esquema em dev, teste, CI e produção; testes de migração a cada execução.
  - (+) SQL puro: aproveita tudo o que o PostgreSQL oferece e é o que o time lê em revisões.
  - (−) Disciplina: nunca editar migração aplicada (*roll-forward*), renumerar em colisões, nomes exatos (nome errado é ignorado em silêncio).
  - (−) Mudanças sem *downtime* exigem planejamento em etapas (S3, *expand/contract*).
