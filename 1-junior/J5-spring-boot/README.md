# J5 — Spring Framework e Spring Boot

> 🟢 Júnior · ⏱️ 24h (16 sessões de 1h30) · Status: ✅ concluído (aulas geradas em 2026-11-13)

## 🎯 Objetivo do módulo
Construir um monólito Spring Boot com persistência JPA, validação e configuração por profiles.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [J5.01](J5.01-inversao-de-controle-e-injecao-de-dependencia.md) | Inversão de controle e injeção de dependência (mini-container à mão) | 1h30 |
| [J5.02](J5.02-spring-initializr-estrutura-e-starters.md) | Spring Initializr, estrutura do projeto e starters | 1h30 |
| [J5.03](J5.03-beans-escopos-ciclo-de-vida-e-estereotipos.md) | Beans: escopos, ciclo de vida e estereótipos | 1h30 |
| [J5.04](J5.04-como-funciona-a-autoconfiguracao.md) | Como funciona a autoconfiguração | 1h30 |
| [J5.05](J5.05-application-yml-profiles-e-configurationproperties.md) | `application.yml`, profiles e `@ConfigurationProperties` | 1h30 |
| [J5.06](J5.06-spring-mvc-e-o-padrao-mvc.md) | Spring MVC e o padrão MVC | 1h30 |
| [J5.07](J5.07-jpa-e-hibernate-i-entidades-e-mapeamento.md) | JPA e Hibernate I: entidades e mapeamento | 1h30 |
| [J5.08](J5.08-jpa-ii-relacionamentos-lazy-eager-e-entitymanager.md) | JPA II: relacionamentos, lazy vs eager e `EntityManager` | 1h30 |
| [J5.09](J5.09-spring-data-jpa-repositorios-e-consultas-derivadas.md) | Spring Data JPA: repositórios e consultas derivadas | 1h30 |
| [J5.10](J5.10-jpql-e-paginacao.md) | JPQL e paginação | 1h30 |
| [J5.11](J5.11-transactional-na-pratica.md) | `@Transactional` na prática | 1h30 |
| [J5.12](J5.12-problema-n-mais-1.md) | Problema N+1 | 1h30 |
| [J5.13](J5.13-bean-validation.md) | Bean Validation | 1h30 |
| [J5.14](J5.14-logging-com-slf4j-e-logback.md) | Logging com SLF4J/Logback | 1h30 |
| [J5.15](J5.15-devtools-e-actuator-basico.md) | DevTools e Actuator (básico) | 1h30 |
| [J5.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Ambiente de referência: JDK 25, Maven 3.9.12, **Spring Boot 4.1.2** (Spring Framework 7.0.10, Spring Data JPA 4.1.2, **Hibernate ORM 7.4.13.Final**, Hibernate Validator 9.1.5.Final, Tomcat 11.0.27, Jackson 3.1.7, Thymeleaf 3.1.5, Flyway 12.4.0 e JUnit 6.0.3: **as versões do BOM do Boot**, ver `00-guia/versoes.md`), PostgreSQL 18 (Docker `postgres:18`, container `payflow-pg`; bancos `payflow` e `payflow_test`), Node não é usado. As aulas pressupõem `bash scripts/db-up.sh` (J3.03).
>
> **Avisos de ordem (conceitos usados antes de serem formalmente ensinados):** (1) a partir da J5.03, os laboratórios são **testes JUnit** com `@SpringBootTest`, `@Transactional` em teste, `ApplicationContextRunner` e `OutputCaptureExtension`, que o módulo **J7** ensina a fundo; cada aula explica o que usa; (2) a J5.07 introduz uma **extensão do JUnit** (`ResetDatabase`) para o banco de teste; (3) a J5.11 e a J5.12 citam *threads* e concorrência (P1) apenas em exercícios opcionais; (4) a J5.13 e a J5.14 mencionam LGPD e *log injection* (J8 aprofunda).
>
> **Mudanças em relação às versões que você fixou na J3/J4:** o Boot escolhe as versões das bibliotecas (Flyway 12.4.0 no lugar da 13.12.2; Thymeleaf 3.1.5 no lugar da 3.1.7; JUnit 6.0.3 no lugar da 6.1.3). Foi verificado que o Flyway 12.4.0 valida o histórico gravado pela 13.12.2.

## 🏗️ Entrega no PayFlow
Monólito `com.payflow` (Spring Boot 4.1.2) com cadastro, contas e depósitos persistidos via **Spring Data JPA**, painel em **Spring MVC** (mesmos templates da J4), serviços transacionais, **Bean Validation**, logs com `requestId`, Actuator e **87 testes**. O JDBC da J3 e o servidor manual da J4 foram **removidos**. Tag `payflow-J5`.

## ➡️ Próximo
[J6 — APIs REST](../J6-apis-rest/README.md)
