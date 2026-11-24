# ADR 0007 — Configuração por ambiente: sem perfil = produção; `dev` e `test` abrem o que é conveniente

- **Status:** aceita (J5, 2026-11-13)
- **Contexto:** o mesmo JAR roda na máquina do desenvolvedor, nos testes, em homologação e em produção. O seed de dados de exemplo (ADR 0003) **nunca** pode rodar em produção; senhas reais não podem estar no Git; e o Spring Boot **não falha** por placeholder não resolvido (`url: ${PAYFLOW_DB_URL}` ficou como texto literal e o erro apareceu longe da causa).
- **Opções:** (1) `application.yml` com valores de desenvolvimento por padrão e perfil `prod` que sobrescreve; (2) `application.yml` base **seguro** (produção), sem URL de banco e sem seed, e perfis `dev` e `test` que **acrescentam** o necessário; (3) placeholders `${VAR}` no arquivo base.
- **Decisão:** opção 2. **Sem perfil ativo, a aplicação é "produção"**: nenhuma URL/usuário/senha de banco no arquivo (vêm de `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, convertidas pelo *relaxed binding*), `flyway.locations` só com o esquema, Actuator expondo **só `health` e `info`**, sem SQL nem `DEBUG`. O perfil **`dev`** traz o banco local do Docker (credenciais descartáveis), o seed, o SQL do Hibernate, `DEBUG` do PayFlow, o cache do Thymeleaf desligado e os endpoints extras do Actuator. O perfil **`test`** aponta para `payflow_test`, com seed e `clean` do Flyway permitido. Configuração tipada em `PayflowProperties` (`@Validated`, `kebab-case`).
- **Consequências:**
  - (+) Esquecer de configurar o ambiente **falha alto e cedo** (`Failed to configure a DataSource`) em vez de usar um banco errado ou subir dados de exemplo em produção; o `/actuator/info` mostra `devSeedEnabled` para conferência.
  - (+) Segredos reais ficam fora do Git; os valores do `dev` são só da máquina do desenvolvedor.
  - (−) Rodar localmente exige o perfil (`bash scripts/run-dev.sh` ou `SPRING_PROFILES_ACTIVE=dev`); a mensagem de erro sem perfil é a análise de falha do Boot, não um texto próprio.
  - (−) Nada impede alguém de ativar `dev` em produção; a proteção é operacional (a plataforma de deploy define as variáveis) e o `info` ajuda a detectar.
  - Não usar `${VAR}` sem valor padrão no arquivo base: o placeholder não resolvido vira texto literal.
