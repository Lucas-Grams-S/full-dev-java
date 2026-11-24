# Versões de referência

> Verificado em **2026-11-12**. Reconfira a versão do que cada módulo usa **na execução que o gera** (as páginas mudam; o campo "Fonte" diz onde olhar).

| Tecnologia | Versão adotada | Observação | Fonte |
|---|---|---|---|
| Java (JDK) | **25 (LTS)** — lançado em 2025-09-16 | Java 27 é a versão mais recente (não LTS). Spring Boot 4.1 aceita Java 17 até 27. | [dev.java/download](https://dev.java/download/) (cita Java 27), [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Spring Boot | **4.1.2** | 4.2 estava prevista para novembro de 2026; reconferir em J5. A linha 3.5 teve o último patch aberto em junho de 2026 (fonte secundária: HeroDevs). | [spring.io/projects/spring-boot](https://spring.io/projects/spring-boot) |
| Spring Framework | **7.0.x** (7.0.8 na página do projeto; Boot 4.1.2 exige 7.0.10+) | Gerenciado pelo Boot; não fixar à mão. | [spring.io/projects/spring-framework](https://spring.io/projects/spring-framework) |
| Spring Security | **7.1.2** | | [spring.io/projects/spring-security](https://spring.io/projects/spring-security) |
| Spring Data JPA | **4.1.2** | | [spring.io/projects/spring-data-jpa](https://spring.io/projects/spring-data-jpa) |
| Hibernate ORM | **7.4.15.Final** (série 7.4 = estável mais recente) | 8.0 ainda em desenvolvimento (CR1). | [hibernate.org/orm/releases](https://hibernate.org/orm/releases/) |
| Servlet container | Tomcat 11.0.x (Servlet 6.1) | Padrão do Boot 4.1. | [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Maven | 3.9.12 instalado; Boot exige 3.6.3+ | | idem |
| Gradle | 8.14+ ou 9.x (só como alternativa) | | idem |
| PostgreSQL | **18** (18.7 na página de versionamento) | Cada major tem 5 anos de suporte; a 14 chega ao fim em 2026-11-12. | [postgresql.org/support/versioning](https://www.postgresql.org/support/versioning/) |
| JUnit | **JUnit 6** (geração atual; exige Java 17) | ⚠️ O prompt original cita JUnit 5. Número exato da versão não confirmado: conferir em docs.junit.org ao gerar J1. | [junit.org](https://junit.org/) |
| Testcontainers (Java) | **2.0.5** | Número tirado da página de releases do GitHub; o ano não aparece na página. | [github.com/testcontainers/testcontainers-java/releases](https://github.com/testcontainers/testcontainers-java/releases) |
| Kubernetes | **1.37.1** (linhas suportadas: 1.34 a 1.37); em 2026-11-14 `dl.k8s.io/release/stable.txt` = **v1.37.2** | Em 2026-11-12 a 1.34 já está perto do fim do suporte. | [kubernetes.io/releases](https://kubernetes.io/releases/) |
| Terraform | **1.17.2** | Lista de releases do HashiCorp. | [releases.hashicorp.com/terraform](https://releases.hashicorp.com/terraform/) |
| Apache Kafka | **4.4.0** (lançado em 2026-10-22; 4.3.2 é correção) | Fonte secundária (Red Hat Kafka Monthly Digest, outubro de 2026). ⚠️ Confirmar em kafka.apache.org ao gerar P3. | [developers.redhat.com](https://developers.redhat.com/index%2ephp/blog/2026/11/02/kafka-monthly-digest-october-2026) |
| Docker | 29.9.2 (instalado na máquina do aluno) | Compose v2 integrado. | `docker --version` |
| Serviços AWS | — | Reconferir nomes, limites e preços no módulo que usa cada serviço (J10, P9, S6). | docs.aws.amazon.com |

## Ambiente do aluno (detectado em 2026-11-12)
- JDK instalado: OpenJDK 17.0.21 → **é preciso instalar o JDK 25** (aula J0.02).
- Maven 3.9.12 ✔ · Docker 29.9.2 ✔ · Sistema: Linux.

## Mudanças de versão maior que costumam quebrar tutoriais antigos
- `javax.*` → `jakarta.*` (desde Spring Boot 3).
- `RestTemplate` → `RestClient` (Spring 6.1+); `WebClient` para o modelo reativo.
- Spring Security: configuração por `SecurityFilterChain` e lambdas; `WebSecurityConfigurerAdapter` foi removido.
- Spring Boot 4 / Framework 7: ⚠️ verificar nas notas de migração oficiais antes de usar qualquer biblioteca de terceiros (por exemplo, a versão da biblioteca JSON usada pelo Boot). Não afirmar de memória.

## Verificado em J3 (2026-11-12)
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| PostgreSQL (imagem Docker `postgres:18`) | **18.6** (`select version()`) | No 18 o volume vai em `/var/lib/postgresql` (PGDATA `/var/lib/postgresql/18/docker`); `uuidv7()` e *skip scan* são novos no 18 | [Docker Hub](https://hub.docker.com/_/postgres), [notas do 18](https://www.postgresql.org/docs/18/release-18.html) |
| Driver JDBC `org.postgresql:postgresql` | **42.7.14** | `getObject(col, Instant.class)` em `timestamptz` **não** é suportado (testado) | [Maven Central](https://repo1.maven.org/maven2/org/postgresql/postgresql/maven-metadata.xml) |
| Flyway (`flyway-core` e `flyway-database-postgresql`) | **13.12.2** | O módulo `flyway-database-postgresql` é obrigatório (`Unsupported Database` sem ele); `clean` desabilitado por padrão | [Maven Central](https://repo1.maven.org/maven2/org/flywaydb/flyway-core/maven-metadata.xml), [docs](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations) |
| `exec-maven-plugin` | 3.7.0 | Usado só para rodar `main` (`mvn exec:java`) | [Maven Central](https://repo1.maven.org/maven2/org/codehaus/mojo/exec-maven-plugin/maven-metadata.xml) |
| HikariCP | 7.1.0 (versão mais recente no Maven Central; **não usada** em J3) | Aparece só como visão em J3.11; o pool entra em J5/P2 | [Maven Central](https://repo1.maven.org/maven2/com/zaxxer/HikariCP/maven-metadata.xml) |

## Verificado em J4 (2026-11-13)
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Thymeleaf (`org.thymeleaf:thymeleaf`) | **3.1.7.RELEASE** | Standalone (sem Spring) na J4; o `@{/...}` exige contexto web (`IWebContext`), testado; o escape de `th:text` testado | [Maven Central](https://repo1.maven.org/maven2/org/thymeleaf/thymeleaf/maven-metadata.xml), [docs 3.1](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html) |
| `slf4j-simple` | **2.0.20** (a série 2.1 aparece só como `alpha`) | Destino dos logs do Thymeleaf e do Flyway até o Spring Boot trazer o Logback (J5) | [Maven Central](https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/maven-metadata.xml) |
| Chrome | **156.0.8078.47** | Laboratórios de HTML/CSS/JS e do axe, automatizado (headless) | `google-chrome --version` |
| Node.js | **22.22.1** (instalado) | O Angular CLI 22 exige `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0` e **recusou** rodar | `node -v`, `npm view @angular/core@22.2.6 engines` |
| axe-core / puppeteer-core | **4.14.0** / **25.17.0** | Só nos laboratórios de teste de acessibilidade | `npm ls` |
| React / React DOM | **19.3.1** | Só leitura e renderização no servidor (J4.07) | `npm view react version` |
| Angular (`@angular/core`) / CLI | **22.2.6** / 22.2.7 | Componente compilado com `ngc` (TypeScript **6.0.3**; o compilador do Angular 22 declara `typescript >=6.0 <6.1`) | `npm view @angular/compiler-cli@22.2.6 peerDependencies` |
| TypeScript | **7.0.2** (`tsc`) | Usado com o exemplo React | `npm view typescript version` |
| esbuild | 0.28.2 | Só para renderizar o componente React no Node | `npm view esbuild version` |
| WCAG | **2.2** (nível AA) | Critérios e Quick Reference | [w3.org/WAI/WCAG22/quickref](https://www.w3.org/WAI/WCAG22/quickref/) |
| PostgreSQL (imagem `postgres:18`) | 18.6 | Mesma da J3 | `select version()` |

## Verificado em J5 (2026-11-13)
Versões efetivas = **BOM do Spring Boot 4.1.2** (`spring-boot-dependencies-4.1.2.pom`); não fixar à mão. Registre aqui qualquer sobrescrita.
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Spring Boot | **4.1.2** (última estável; `4.2.0-RC2` e `4.1.3-SNAPSHOT` existiam; o Initializr listava 4.1.2.RELEASE como padrão) | `spring-boot-starter-webmvc` (o `starter-web` está deprecado); um starter de teste por tecnologia (`spring-boot-starter-*-test`); `spring-boot-starter-flyway` **não** inclui `flyway-core` nem o módulo do PostgreSQL | [Maven Central](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml), [Initializr](https://start.spring.io/) |
| Spring Framework | **7.0.10** | Gerenciado pelo Boot | `spring-boot-dependencies-4.1.2.pom` |
| Spring Data (BOM) / Spring Data JPA | `2026.0.2` / 4.1.2 | | idem |
| Hibernate ORM | **7.4.13.Final** (o BOM; a série 7.4 já tinha a 7.4.15, sem uso aqui) | `@Immutable` + `@Generated` falha ao gravar (J5.07/J5.11); `@Generated(event = INSERT)` com `RETURNING` | idem |
| Hibernate Validator / Jakarta Validation | **9.1.5.Final** / 3.1.1 | | idem |
| Tomcat embutido | 11.0.27 | | idem |
| HikariCP | **7.0.2** (o BOM; a J3 citava 7.1.0 do Maven Central) | Pool padrão do Boot | idem |
| Flyway | **12.4.0** (o BOM; a J3 fixou a 13.12.2) | Valida o histórico gravado pela 13.12.2 (testado) | idem |
| Thymeleaf | **3.1.5.RELEASE** (o BOM; a J4 fixou a 3.1.7) | Starter traz `thymeleaf-spring6` | idem |
| JUnit Jupiter | **6.0.3** (o BOM; a J3/J4 usaram a 6.1.3) | | idem |
| Jackson | **3.1.7**, pacote `tools.jackson` | Mudou de `com.fasterxml.jackson` | idem |
| Micrometer / Logback / SLF4J | 1.17.2 / 1.5.38 / 2.0.20 | | idem |
| PostgreSQL JDBC | 42.7.14 | Igual à J3 | idem |
| `spring-boot-devtools` | 4.1.2 | LiveReload **desligado por padrão** e **obsoleto desde o 4.1.0**; fora do JAR executável | [docs](https://docs.spring.io/spring-boot/reference/using/devtools.html) |
| Maven (plugin do compilador) | `maven-compiler-plugin` 3.15.0; `maven-surefire-plugin` 3.5.6 (do parent) | **JDK 25: o processamento de anotações não roda sem `annotationProcessorPaths`** (testado com `spring-boot-configuration-processor`) | `mvn help:effective-pom` |
| PostgreSQL | 18.6 (`postgres:18`) | Igual à J3 | `select version()` |

Mudanças do Boot 4 que quebram tutoriais antigos (conferidas): autoconfigurações em **módulos separados** (`org.springframework.boot.jdbc.autoconfigure.*`, `...flyway.autoconfigure.*`); `HealthIndicator`/`Health` em `org.springframework.boot.health.contributor` (módulo `spring-boot-health`); `LocalServerPort` em `org.springframework.boot.test.web.server`; Jackson 3.

## Verificado em J6 (2026-11-13)
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| MapStruct (`mapstruct`, `mapstruct-processor`) | **1.6.3** (a `1.7.0.Beta2` é beta) | **Fora do BOM do Boot**; declarar nos `annotationProcessorPaths` junto com o processador do Boot | [Maven Central](https://repo1.maven.org/maven2/org/mapstruct/mapstruct/maven-metadata.xml) |
| springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) | **3.2.0** (POM declara Spring Boot 4.1.2) | Fora do BOM; OpenAPI 3.1.0; `version` do mapeamento = texto do segmento (`v1`) | [Maven Central](https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml) |
| swagger-parser (teste) | 2.1.48 | Valida a especificação gerada | [Maven Central](https://repo1.maven.org/maven2/io/swagger/parser/v3/swagger-parser/maven-metadata.xml) |
| `spring-boot-starter-restclient` | 4.1.2 (BOM) | Traz `spring-boot-http-client`; timeouts por `HttpClientSettings` | Maven Central |
| Spring Framework (versionamento nativo) | 7.0.10 | `ApiVersionConfigurer.usePathSegment(int, Predicate)`, `StandardApiVersionDeprecationHandler` | [docs](https://docs.spring.io/spring-framework/reference/web/webmvc-versioning.html) |
| Spring Data JPA | 4.1.2 | `Specification.and(null)`/`allOf(null)` lançam exceção; use `unrestricted()` | testado |
| Idempotency-Key (IETF) | Internet-Draft **08** (rascunho) | Valor como string entre aspas; 400 sem chave, 422 corpo diferente, 409 em andamento | [datatracker](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/) |
| RFC 9457 (Problem Details) | RFC | Substitui a RFC 7807 | [rfc-editor](https://www.rfc-editor.org/rfc/rfc9457.html) |

## Verificado em J7 (2026-11-13)
Versões efetivas de teste = **BOM do Spring Boot 4.1.2** (`spring-boot-dependencies-4.1.2.pom`); plugins fora do BOM levam `<version>` explícita.
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| AssertJ | **3.27.7** (BOM; o Maven Central já lista `4.0.0-M1`, marco, **não usado**) | `isEqualByComparingTo` para `BigDecimal` | `spring-boot-dependencies-4.1.2.pom` |
| Mockito | **5.23.0** (BOM; o Maven Central lista 5.24.0, sem uso) | `MockitoExtension` (strict stubs); agente por `-javaagent` com `argLine` | idem |
| JUnit Jupiter | **6.0.3** (BOM) | `@ParameterizedTest`, `@Nested`, `@NullAndEmptySource` | idem |
| Testcontainers | **2.0.5** (BOM) | módulo `org.testcontainers:testcontainers-postgresql`; classe `org.testcontainers.postgresql.PostgreSQLContainer` (a `org.testcontainers.containers.PostgreSQLContainer` existe no mesmo JAR, antiga) | idem; `javap` no JAR |
| `spring-boot-starter-webmvc-test` / `spring-boot-starter-data-jpa-test` / `spring-boot-testcontainers` | 4.1.2 (BOM) | pacotes: `org.springframework.boot.webmvc.test.autoconfigure` (`@WebMvcTest`, `@AutoConfigureMockMvc`), `org.springframework.boot.data.jpa.test.autoconfigure` (`@DataJpaTest`), `org.springframework.boot.jdbc.test.autoconfigure` (`@AutoConfigureTestDatabase`), `org.springframework.boot.jpa.test.autoconfigure` (`TestEntityManager`), `org.springframework.boot.testcontainers.service.connection` (`@ServiceConnection`); **Flyway roda no `@DataJpaTest`** pela inicialização de `DataSource` (conferido nos `.imports`) | Maven Central (listagem dos JARs) |
| `@MockitoBean` | Spring Framework 7 | `org.springframework.test.context.bean.override.mockito.MockitoBean` | [docs](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-mockitobean.html) |
| JaCoCo (`jacoco-maven-plugin`) | **0.8.16** (24/10/2026) | suporte oficial ao Java 25 desde a 0.8.14, ao 27 na 0.8.16; **fora do BOM** | [changes](https://www.jacoco.org/jacoco/trunk/doc/changes.html), Maven Central |
| Checkstyle / `maven-checkstyle-plugin` | **14.4.0** / **3.6.0** | **funcionou no JDK 25** (executado no `qa-lab`); fora do BOM | Maven Central |
| SpotBugs / `spotbugs-maven-plugin` | **4.10.5** / **4.10.5.0** | analisou *bytecode* do release 25 (executado); fora do BOM | Maven Central |
| `maven-dependency-plugin` / `maven-surefire-plugin` | **3.10.0** / **3.5.6** (gerenciados pelo `spring-boot-starter-parent`) | `dependency:properties` cria `${org.mockito:mockito-core:jar}` para o agente | `spring-boot-starter-parent-4.1.2.pom` |
| PostgreSQL (imagem dos testes) | `postgres:18` | `uuidv7()` exige o 18; não usar `latest` | decisão 14 |

## Verificado em J8 (2026-11-13)
Versões efetivas de segurança = **BOM do Spring Boot 4.1.2** (`spring-boot-dependencies-4.1.2.pom`); o plugin do Dependency-Check, fora do BOM, leva `<version>` explícita.
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Spring Security | **7.1.2** | `spring-security-core`, `-web`, `-config`, `-crypto`, `-oauth2-jose`, `-oauth2-resource-server`, `-test`; executado no `lab/security-lab` (JDK 25) | `spring-boot-dependencies-4.1.2.pom` (`spring-security.version`) |
| `spring-boot-starter-security` / `-security-test` | 4.1.2 (BOM) | pacote do `EndpointRequest`: `org.springframework.boot.security.autoconfigure.actuate.web.servlet` | Maven Central (listagem do JAR `spring-boot-security`) |
| `spring-boot-starter-security-oauth2-resource-server` (e `...-test`) | 4.1.2 (BOM) | o nome antigo `spring-boot-starter-oauth2-resource-server` está **deprecado** (descrição do POM); com este *starter* no classpath o Boot **não** cria o usuário `user` com senha aleatória, salvo se `spring.security.user.*` for configurado | Maven Central; relatório de condições do Boot |
| `nimbus-jose-jwt` | **10.9.1** | assina e valida o JWT (vem pelo *starter*) | `mvn dependency:tree` |
| BCrypt (`spring-security-crypto`) | custo 10 (padrão) | medido na máquina do autor: 52,7 ms (custo 10), 208,5 ms (12), 837,6 ms (14); **recusa senha > 72 bytes**; o codificador delegante usa `{bcrypt}` por padrão (68 caracteres) | `PasswordLab` (executado) |
| Mudanças do Security 7 que quebram tutoriais antigos (conferidas) | | `DaoAuthenticationProvider(UserDetailsService)` pelo construtor; `DelegatingAuthenticationEntryPoint.builder()` (o construtor com `LinkedHashMap` e `setDefaultEntryPoint` estão **deprecados**); autoridade `FACTOR_PASSWORD` depois do login por senha; `WWW-Authenticate: Bearer resource_metadata="..."`; URL absoluta com `&continue` depois do login; `hasRole("ROLE_X")` recusado | `javap` nos JARs 7.1.2; `lab/security-lab` |
| Spring Boot (testes) | 4.1.2 | `@AutoConfigureMockMvc` em `org.springframework.boot.webmvc.test.autoconfigure`; `ConfigDataApplicationContextInitializer` em `org.springframework.boot.test.context`; o `@ConfigurationPropertiesScan` da classe principal **não** vale em `@WebMvcTest` (precisa de `@EnableConfigurationProperties`) | `lab/security-lab` (executado) |
| Jackson 3 (limites padrão) | 3.1.7 | `maxStringLength` 100 000 000, `maxNestingDepth` 500, `maxNumberLength` 1000, `maxDocumentLength` -1 (sem limite) | `StreamReadConstraints.defaults()` (executado) |
| H2 | 2.4.240 | **só** no `lab/InjectionLab.java` (decisão 5: nunca em teste do PayFlow) | `spring-boot-dependencies-4.1.2.pom` |
| OWASP Top 10 | **2025** | A01 Controle de acesso quebrado (inclui CSRF e SSRF), A02 Configuração incorreta, A03 Cadeia de suprimento de software, A04 Falhas criptográficas, A05 Injeção (inclui XSS), A06 Design inseguro, A07 Falhas de autenticação, A08 Integridade de software e dados, A09 Falhas de registro e alerta, A10 Condições excepcionais | [top10.owasp.org/2025](https://top10.owasp.org/2025/) |
| `org.owasp:dependency-check-maven` | **13.0.0** (a mais recente no Maven Central) | perfil `security-scan`; **não executado** (baixa o banco do NVD); `failBuildOnCVSS` padrão 11 (nunca falha) | [Maven Central](https://repo1.maven.org/maven2/org/owasp/dependency-check-maven/maven-metadata.xml), [documentação](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/configuration.html) |
| OSV (API `v1/query`) | n/a | consulta por pacote e versão; `lab/osv-check.sh` executado em 2026-11-13 (`log4j-core` 2.14.1: 8 registros; as bibliotecas do PayFlow: nenhum) | [osv.dev](https://osv.dev/) |
| gitleaks | n/a | só o README foi lido (`gitleaks git`; v8.19.0 aposentou `detect` e `protect`); **não executado** | [github.com/gitleaks/gitleaks](https://github.com/gitleaks/gitleaks) |

## Verificado em J9 (2026-11-13)
Imagens e ferramentas de container **executadas** no `lab/docker-lab` (Linux); as imagens base foram conferidas com `docker manifest inspect` e executadas.
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Docker Engine / CLI | **29.9.2** (cliente e servidor) | `docker build` com BuildKit e `buildx` 0.38.1; `docker build --check` disponível | `docker version` |
| Docker Compose | **v5.6.1** | `docker compose` (com espaço); o campo `version:` é **obsoleto** (aviso conferido); nome preferido do arquivo: `compose.yaml` | `docker compose version`, [documentação](https://docs.docker.com/compose/intro/compose-application-model/) |
| `maven` (imagem do estágio de build) | `maven:3.9.12-eclipse-temurin-25` | existe também `maven:3.9-eclipse-temurin-25` (tag móvel); a do PayFlow **fixa** a 3.9.12, igual ao Maven local | `docker manifest inspect` |
| `eclipse-temurin` (runtime) | `25-jre-alpine` (**JRE 25.0.5+8**, 306 MB) | tags verificadas: `25-jdk`, `25-jre`, `25-jre-noble`, `25-jre-alpine`; `25.0.5_10-jre` **não existe** como tag; a variante Ubuntu (`25-jre`, 480 MB) **não traz `curl` nem `wget`** e nenhuma variante JRE traz `jcmd`/`jstack` | `docker manifest inspect`, execução |
| Alpine / BusyBox | base do `25-jre-alpine` | `wget` do BusyBox serve ao `healthcheck`; `addgroup -S -g` / `adduser -S -u -G -H -s` | execução |
| `postgres` (imagem) | `postgres:18` → **18.6** | volume em `/var/lib/postgresql`; `POSTGRES_PASSWORD_FILE` suportado; as variáveis só valem ao criar o banco | `select version()`, [Docker Hub](https://hub.docker.com/_/postgres) |
| Spring Boot (container) | 4.1.2 | `java -Djarmode=tools -jar app.jar extract --layers --destination dir` (gera `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`); `server.shutdown=graceful` é o padrão (a documentação manda usar `immediate` para desligar) e foi **declarado** no laboratório; `configtree` por `SPRING_CONFIG_IMPORT` | execução, [documentação](https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html) |
| `spring-boot-starter-flyway` + `flyway-database-postgresql` | 4.1.2 (BOM) → Flyway 12.4.0 | migração e seed aplicados dentro do container | log do laboratório |
| JVM no container | `Picked up JAVA_TOOL_OPTIONS`; `MaxRAMPercentage=75` | sem limite: 14 392 MB de *heap* e 12 CPUs; com `--memory 512m --cpus 2`: 371 MB e 2 CPUs | `RuntimeController` do laboratório |
| JDK local (`jdb`) | 25 | usado só para conectar ao JDWP | execução |
| Hadolint, Docker Scout, Trivy, `spring-boot:build-image`, `docker debug` | n/a | **não executados** | |

## Verificado em J10 (2026-11-13)
Ferramentas de CI e nuvem; **nenhuma conta AWS foi usada** (a AWS foi emulada pelo moto 5.2.5 nos testes dos scripts).
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Runner `ubuntu-24.04` (`ubuntu-latest` = 24.04; existe `ubuntu-26.04`) | imagem `20261111.370.1`: Docker 28.0.4, Compose 2.38.2, Git 2.56.1, Java 8/11/17 (padrão)/21/**25.0.5**, **Maven 3.10.0**, Node 22.23.4 | o Maven do runner difere do 3.9.12 do projeto: usar o Wrapper | [runner-images](https://github.com/actions/runner-images), README da imagem Ubuntu 24.04 |
| `actions/checkout` / `setup-java` / `upload-artifact` / `cache` | **v7.0.1** / **v6.2.0** / **v7.0.2** / **v6.1.0** (todas `node24`) | fixadas por SHA nos workflows (SHAs na J10.02) | `api.github.com/.../releases/latest`, `git ls-remote` |
| `docker/setup-buildx-action` / `build-push-action` / `login-action` | v4.4.1 / v7.4.0 / v4.8.0 (`login-action` não usada: login por CLI) | | idem |
| `aws-actions/configure-aws-credentials` | **v6.3.0** (`node24`) | `role-to-assume` + `id-token: write` | idem |
| `actions/dependency-review-action` | v5.0.0 | público ou GitHub Advanced Security | README da action |
| Maven Wrapper (`maven-wrapper-plugin`) | **3.3.4** (`distributionType=only-script`, Maven **3.9.12**) | gerado e executado no laboratório | Maven Central; `mvn wrapper:wrapper` |
| JaCoCo | 0.8.16 | cobertura real do laboratório 72,2% (26/36 linhas); piso 0,70 | execução |
| actionlint / shellcheck | **1.7.12** / **0.11.0** (imagens Docker `rhysd/actionlint`, `koalaman/shellcheck`) | executados | Docker Hub |
| Caddy | **2.11** (imagem; release mais recente 2.11.9) | `request_body { max_size }`, `reverse_proxy`; `caddy validate` executado | Docker Hub, GitHub |
| Docker Compose (na EC2) | **v5.7.1** (release de 2026-11-11) + `.sha256` | baixado no `user-data` (não executado) | [releases](https://github.com/docker/compose/releases) |
| AWS CLI | **2.39.5** | `aws login` existe (credenciais temporárias do console) | `aws --version`, `aws login help` |
| RDS for PostgreSQL | **18.6** (a 19 também listada) | `--engine-version 18`: confirmar a menor com `describe-db-engine-versions` | [notas de versão](https://docs.aws.amazon.com/AmazonRDS/latest/PostgreSQLReleaseNotes/postgresql-versions.html) |
| Plano gratuito da AWS (contas desde 15/07/2025) | US$ 100 + até US$ 100 em créditos, 6 meses; vira Paid ao entrar no Organizations | instâncias elegíveis: `t3.micro`, `t3.small`, `t4g.micro`, `t4g.small`, `c7i-flex.large`, `m7i-flex.large` | [Free plan](https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier-plans.html), [EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-free-tier-usage.html) |
| IPv4 público | US$ 0,005 por IP por hora desde 2024-02-01 | | [blog da AWS](https://aws.amazon.com/blogs/aws/new-aws-public-ipv4-address-charge-public-ip-insights/) |
| GitHub Actions (cobrança) | repositório público: runners padrão **grátis**; Free privado: **US$ 12/mês** incluídos, Linux **US$ 0,006/min**, cache 10 GB/repo, artefatos 500 MB | | [billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions) |
| moto | 5.2.5 (`MOTO_IAM_LOAD_MANAGED_POLICIES=true` para políticas gerenciadas) | só no laboratório | PyPI |

## Verificado em P1 (2026-11-13)
| Item | Versão / valor | Observação | Fonte |
|---|---|---|---|
| JDK usado nos laboratórios | **OpenJDK 25.0.5** (Ubuntu, `25.0.5.0.3+1`); `Pinning.java` também no **21** | 12 núcleos, 18 GiB, máquina compartilhada | `java -version`, execução |
| Coletor padrão e ergonomia | G1 com 2+ CPUs; **Serial com 1 CPU** (`-XX:ActiveProcessorCount=1`); G1 com 2 CPUs mesmo com `-XX:MaxRAM=1g` | `G1HeapRegionSize` 1 MiB, `MaxGCPauseMillis` 200, `InitiatingHeapOccupancyPercent` 45, `ParallelGCThreads` 10 (de 12 núcleos) | `-XX:+PrintFlagsFinal` |
| ZGC | **geracional por padrão** (log `Minor`/`Major Collection`) | JEP 439 (21), 474 (23, padrão), 490 (24, remove o modo antigo) | openjdk.org/jeps |
| Shenandoah geracional | JEP 521 (Java 25) | presente nesta JVM (`UseShenandoahGC`) | openjdk.org/jeps/521 |
| Virtual threads | JEP 444 (21); **`synchronized` sem *pinning*: JEP 491 (24)** | provado: Java 21 fixa, Java 25 não | execução (`Pinning.java`) |
| `ScopedValue` | **final no 25** (JEP 506) | | openjdk.org/jeps/506 |
| `StructuredTaskScope` | **preview no 25** (JEP 505, 5ª preview); API: `open()`, `Joiner.awaitAllSuccessfulOrThrow/allSuccessfulOrThrow/anySuccessfulResultOrThrow/awaitAll/allUntil`, `Configuration.withTimeout`; **sem** `ShutdownOnFailure` | `--enable-preview` | `javap --module java.base`, execução |
| Recursos do Java 22–25 usados | `_` (JEP 456), Gatherers (485), Class-File API (484), construtor flexível (513), `import module` (511), arquivo compacto + `IO.println` (512), compact object headers (519), cache AOT (514/515) | `import module java.base` **não** inclui `java.net.http` | openjdk.org/jeps, execução |
| Compact Object Headers | `new Object()` de **16 → 8 bytes** (medido) | `-XX:+UseCompactObjectHeaders` | `ObjectSize.java` |
| Cache AOT | `-XX:AOTCacheOutput=` cria o cache (12 MiB num programa mínimo); **exige JAR** (diretório de classes é recusado) | ganho medido só num programa mínimo (0,14 s → 0,12 s) | execução |
| `spring.threads.virtual.enabled` | existe, **padrão `false`**, "Whether to use virtual threads"; faz o executor/agendador assíncrono virtual | não verifiquei na documentação do 4.1 que o servidor web embutido também adota virtuais | docs.spring.io (Boot 4.1.2) |
| Encapsulamento do JDK | JEP 261 (9), 396 (16, padrão), 403 (17, remove `--illegal-access`) | `InaccessibleObjectException … does not "opens java.lang"` reproduzida | openjdk.org/jeps, execução |
| `jlink` | runtime com 5 módulos: **55 MiB** sem `.debuginfo` (a instalação completa do JDK, sem `.debuginfo`: ~583 MiB) | Ubuntu inclui `libjvm.debuginfo` de ~734 MiB (ignorado) | execução |
| Módulos do JDK instalado | **69** (`java --list-modules`) | | execução |
| Spring Boot (referência) | 4.1.2 (sem mudança) | nenhum código Spring foi compilado na P1 | |
| JMH | **1.37** (usado na S2.08; antes da S2 era só citado) | ver "Referências do módulo S2" | |

## Verificado em P2 (2026-11-13)
| Item | Versão / valor | Observação | Fonte |
|---|---|---|---|
| PostgreSQL (imagem `postgres:18`) | **18.6** | `deadlock_timeout` padrão **1 s**; *skip scan* de B-tree e `Index Searches` no `EXPLAIN` são do 18; **`EXPLAIN ANALYZE` inclui `BUFFERS` automaticamente** no 18 | `select version()`, [notas da versão 18](https://www.postgresql.org/docs/18/release-18.html), [`deadlock_timeout`](https://www.postgresql.org/docs/18/runtime-config-locks.html) |
| Spring Boot / Spring Framework / Spring Data JPA / Spring Data Redis | 4.1.2 / **7.0.10** / 4.1.2 / **4.1.2** | `@Retryable` e `@EnableResilientMethods` (`org.springframework.resilience.annotation`) e `RetryTemplate` (`org.springframework.core.retry`) são **nativos do Framework 7** (sem Spring Retry) | `javap` nos JARs, [documentação](https://docs.spring.io/spring-framework/reference/core/resilience.html) |
| Hibernate ORM | **7.4.13.Final** (o BOM) | PostgreSQL: `PESSIMISTIC_WRITE` → `for no key update of …` (+ `nowait`); a dica `jakarta.persistence.lock.timeout=500` **foi honrada** (507 ms); paginação com *fetch join* de coleção usa **subconsulta com `offset … fetch first`** (sem aviso de paginação em memória); `MultipleBagFetchException` na **execução** | `SqlShapeIT`, `FetchIT`, `LockTimeoutIT` |
| HikariCP | **7.0.2** (o BOM) | padrões do README: `maximumPoolSize` 10, `minimumIdle` = max, `connectionTimeout` 30 000 ms (mín. 250), `maxLifetime` 1 800 000 ms, **`keepaliveTime` 120 000 ms**, `leakDetectionThreshold` 0 | [README](https://github.com/brettwooldridge/HikariCP) (conferido) |
| Driver `org.postgresql:postgresql` | 42.7.14 | `prepareThreshold` padrão **5**; `reWriteBatchedInserts=true` na URL | [pgjdbc](https://jdbc.postgresql.org/documentation/server-prepare/) |
| Flyway (BOM do Boot) | **12.4.0** (o `flyway-core` do BOM; 13.12.2 também está no `.m2`) | **`CREATE INDEX CONCURRENTLY` trava a migração para sempre com a configuração padrão**; `flyway.postgresql.transactional.lock=false` (Boot: `spring.flyway.postgresql.transactional-lock`) resolve | `FlywayConcurrentIT`; [propriedades do Boot](https://docs.spring.io/spring-boot/appendix/application-properties/index.html) |
| Redis (imagem `redis:8`) | **8.10.3** | `maxmemory-policy` padrão **`noeviction`** (`CONFIG GET`); `allkeys-lru` recomendado para cache | `redis-server --version`, [eviction](https://redis.io/docs/latest/develop/reference/eviction/) |
| Lettuce | **7.5.2.RELEASE** (o BOM) | cliente padrão do Spring Data Redis | `mvn dependency:list` |
| Jackson | **3.1.7** (`tools.jackson`) | o *starter* de Redis **não** traz o Jackson 3: `spring-boot-starter-jackson`; `GenericJacksonJsonRedisSerializer` sem tipagem devolve `LinkedHashMap`; usar `JacksonJsonRedisSerializer<T>` | `mvn dependency:list`, execução |
| Spring Data Redis: escritas do cache | **assíncronas por padrão** (Lettuce); `RedisCacheWriter.create(f, w -> w.immediateWrites())`; trava **por cache** (`cacheLocking`) | `sync = true` **não** serializa a carga com o escritor padrão | código-fonte do JAR + [documentação](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html) |
| Propriedades do Boot (cache/Redis) | `spring.cache.redis.time-to-live` (**padrão: nunca expira**), `cache-null-values` (padrão `true`), `key-prefix`, `enable-statistics` (`false`); `spring.data.redis.connect-timeout`/`timeout`/`password`/`ssl.enabled` | | [propriedades do Boot](https://docs.spring.io/spring-boot/appendix/application-properties/index.html) |
| `hibernate-jcache` + Caffeine JCache | 7.4.13.Final + **3.3.1** (fora do BOM; o `caffeine` do BOM é 3.2.4) | só no `SecondLevelCacheIT` | `mvn dependency:list` |
| jOOQ | **3.21.9** (o BOM); Community Edition: Apache 2.0; exige Java 21+ | **não executado** | `spring-boot-dependencies-4.1.2.pom`, [Boot: jOOQ](https://docs.spring.io/spring-boot/reference/data/sql.html), jooq.org |
| MongoDB / DynamoDB | **não executados**; só documentação (transações multi-documento no MongoDB; `TransactWriteItems`, consistência de leitura no DynamoDB) | limites de itens/tamanho **não confirmados** | páginas citadas na P2.10 |
| Maven do laboratório | Maven **3.9.12** (do sistema); JDK 25.0.5 | `maven-surefire-plugin` 3.5.6 só roda `*Test` por padrão: os `*IT` pedem `-Dtest='*IT'` | execução |
| Máquina do laboratório | 12 núcleos, 18 GiB, **compartilhada** (a *load average* chegou a 39) | os tempos variam muito entre rodadas | `uptime`, `nproc` |

## Verificado em P3 (2026-11-14)
Versões efetivas de mensageria = **BOM do Spring Boot 4.1.2** (`spring-boot-dependencies-4.1.2.pom`: `spring-kafka` 4.1.2, `spring-amqp` 4.1.2, `kafka` 4.2.2, `reactor-bom` 2025.0.8, `testcontainers` 2.0.5); o ShedLock, fora do BOM, leva `<version>`.
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Apache Kafka (broker, imagem `apache/kafka:latest`) | **4.3.1** (lida dos JARs da imagem; KRaft, 1 nó) | `versoes.md` (J-nível) citava 4.4.0 por fonte secundária; **a 4.4.0 não foi usada no broker**. Cliente `kafka-clients` do BOM **4.2.2** (o Maven Central já lista 4.4.0). Em Docker, a porta anunciada precisa existir **dentro** do contêiner (senão transações travam) | `unzip -l` na imagem; `spring-boot-dependencies-4.1.2.pom`; [Maven Central](https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/maven-metadata.xml) |
| Spring for Apache Kafka / `spring-boot-starter-kafka` | **4.1.2** / 4.1.2 | `JacksonJsonSerializer`/`JacksonJsonDeserializer` (Jackson 3) no JAR; `AckMode` padrão `BATCH`; DLT padrão `<tópico>-dlt`; `@RetryableTopic` sufixa pelo **valor do atraso** (`-retry-500`) | `javap`; [docs](https://docs.spring.io/spring-kafka/reference/) |
| Spring AMQP (`spring-rabbit`) / `spring-boot-starter-amqp` | **4.1.2** / 4.1.2 | `JacksonJsonMessageConverter` (Jackson 3); *prefetch* padrão **250** (`DEFAULT_PREFETCH_COUNT`); o Maven Central lista `4.2.0-RC1` (não usada) | `javap`; Maven Central |
| RabbitMQ (`rabbitmq:4-management-alpine`) | **4.4.0**, Erlang/OTP 29 | **Fila não durável e não exclusiva é recusada** (`transient_nonexcl_queues is deprecated`); o site rabbitmq.com responde 403 ao `curl` | `rabbitmqctl status`; execução |
| Propriedade do Boot 4 | `spring.rabbitmq.listener.simple.retry.max-retries` (padrão 3) | `max-attempts` **não tem efeito** (4 entregas medidas) | [propriedades](https://docs.spring.io/spring-boot/appendix/application-properties/index.html); execução |
| ShedLock (`shedlock-spring`, `shedlock-provider-jdbc-template`) | **7.11.0** | Funcionou com Spring Framework 7.0.x; `JdbcTemplateLockProvider` + `usingDbTime()` | [Maven Central](https://repo1.maven.org/maven2/net/javacrumbs/shedlock/shedlock-spring/maven-metadata.xml); execução |
| Boot: `spring.task.scheduling.pool.size` / `spring.task.execution.pool.core-size` / `queue-capacity` | **1** / **8** / ilimitada | Sem efeito com `spring.threads.virtual.enabled=true` (padrão `false`) | tabela de propriedades do Boot |
| Project Reactor (`reactor-core`) | BOM **2025.0.8** | Só Reactor puro (sem WebFlux/Netty) | `spring-boot-dependencies-4.1.2.pom` |
| `spring-boot-starter-websocket` / `spring-boot-starter-webmvc-test` | 4.1.2 (BOM) | `@AutoConfigureMockMvc` em `org.springframework.boot.webmvc.test.autoconfigure` | execução |
| `testcontainers-kafka` / `testcontainers-rabbitmq` | 2.0.5 (existem no Maven Central; **não usados**) | Para os testes do PayFlow (por leitura) | Maven Central |
| PostgreSQL (`postgres:18`) | 18.x | `uuidv7()` no *outbox* | `select version()` |
| Padrões do cliente Kafka 4.2.2 | `acks=all`, `enable.idempotence=true`, `enable.auto.commit=true`, `auto.offset.reset=latest`, `isolation.level=read_uncommitted`, `max.poll.interval.ms=300000` | medidos pelo `ConfigDef` | `KafkaDefaultsIT` |

## Verificado em P4 (2026-11-14)
Versões efetivas = **BOM do Spring Boot 4.1.2**; o **Spring Modulith não é gerenciado pelo Boot** (importe `spring-modulith-bom`); o ArchUnit leva `<version>`.
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Spring Modulith (`spring-modulith-bom`, `starter-core`, `starter-test`, `events-api`) | **2.1.2** | O `.pom` do `spring-modulith-core` 2.1.2 aponta para `spring-boot-autoconfigure` **4.1.2** e `archunit` **1.4.2**. O Maven Central lista `2.2.0-M1/-M2/-RC1` (**não usadas**). `starter-core` **não** traz `@ApplicationModuleListener` (falta `events-api`) | [Maven Central](https://repo.maven.apache.org/maven2/org/springframework/modulith/spring-modulith-bom/maven-metadata.xml); execução; [documentação](https://docs.spring.io/spring-modulith/reference/fundamentals.html) |
| ArchUnit (`com.tngtech.archunit:archunit`) | **1.4.2** (a do Modulith 2.1.2) | A mais nova no Maven Central é a **1.5.1** (não usada: mantenha **uma** versão no classpath). `GeneralCodingRules` tem `NO_CLASSES_SHOULD_USE_FIELD_INJECTION`, `NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS`, `NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING` (`javap`); `archRule.failOnEmptyShould` padrão **true** (executado) | [Maven Central](https://repo.maven.apache.org/maven2/com/tngtech/archunit/archunit/maven-metadata.xml); execução |
| `@ApplicationModuleTest` | modo padrão **`STANDALONE`** (lido no código-fonte da anotação) | O teste **precisa estar no pacote do módulo** | `spring-modulith-test-2.1.2-sources.jar`; execução |
| `ApplicationModules.of(String)` | — | **Exclui classes de teste** por padrão; `of(String, ImportOption)` com `location -> true` as inclui | execução |
| H2 (`com.h2database:h2`) | BOM do Boot 4.1.2 | **Só no laboratório** (transação real sem Docker); H2 nunca em testes de integração do PayFlow (decisão 5) | execução |
| `jmolecules-ddd` | 2.0.1 existe no Maven Central (**não usado**) | Anotações de DDD (`@AggregateRoot`...) integráveis ao Modulith: **fora do roteiro** | [Maven Central](https://repo.maven.apache.org/maven2/org/jmolecules/jmolecules-ddd/maven-metadata.xml) |

## Verificado em P5 (2026-11-14)
Versões efetivas = **BOM do Spring Boot 4.1.2** (`spring-boot-dependencies-4.1.2.pom`: `grpc-java` 1.84.1, `protobuf-java` 4.36.2, `protobuf-maven-plugin` 5.1.11, `spring-grpc` 1.1.2, `jackson` 3.1.x); **Resilience4j, Spring Cloud e Pact não vêm do BOM do Boot** (levam `<version>`/BOM próprio).
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Resilience4j (`resilience4j-spring-boot4`) | **2.4.0** | Módulo **específico do Boot 4** (existe também `-spring-boot3`). As **anotações** exigem `spring-boot-starter-aspectj`; ordem padrão `Retry ( CircuitBreaker ( RateLimiter ( TimeLimiter ( Bulkhead ) ) ) )` | [Maven Central](https://repo.maven.apache.org/maven2/io/github/resilience4j/resilience4j-spring-boot4/maven-metadata.xml); execução |
| Spring Cloud (trem) | **2025.1.4** (usado só no `gateway-lab`) | Declara **Boot 4.0.9** (`spring-cloud-dependencies-2025.1.4.pom`). **2026.0.0-RC2** (pré-lançamento) declara **Boot 4.2.0-RC2**. **Nenhum trem declara o Boot 4.1**; o Gateway 5.0.4 **funcionou** no 4.1.2 (combinação não declarada) | `.pom` do BOM; [Supported Versions](https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions) (lista 2025.1 ↔ Boot 4.0.x) |
| Spring Cloud Gateway (`spring-cloud-starter-gateway-server-webmvc`) | **5.0.4** (do trem 2025.1.4) | O Maven Central lista `5.1.0-RC2` (**não usada**). Propriedades do Boot 4: `spring.cloud.gateway.server.webmvc.*`; timeouts via `spring.http.clients.*`; `trusted-proxies` **necessário** para `X-Forwarded-*`; destino lento/fora → **HTTP 500** | [docs](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html); execução |
| Spring Cloud Circuit Breaker (`…-resilience4j`) | **5.0.3** (trem 2025.1.4) | Usado pelo filtro `CircuitBreaker` do gateway; **`statusCodes` explícito** para contar 503 | execução |
| Spring Cloud Config / Netflix (Eureka) / Kubernetes / Commons (LoadBalancer) | **5.0.6** / **5.0.3** / **5.0.4** / **5.0.4** (trem 2025.1.4) | **Lidos no `.pom`, não executados** (P5.06) | `spring-cloud-dependencies-2025.1.4.pom` |
| Spring Cloud Contract (`spring-cloud-contract-verifier`) | **5.0.3** existe no Maven Central | **Não executado** (P5.09) | Maven Central |
| Pact JVM (`au.com.dius.pact.consumer:junit5`, `…provider:junit5`) | **4.7.7** | Consumidor `@Pact`/`PactConsumerTestExt`; provedor `@Provider`/`@PactFolder`/`@State`; `@PactDirectory` está em `au.com.dius.pact.core.model.annotations`; **`pact_do_not_track=true`** desliga métricas anônimas | execução; [docs](https://docs.pact.io/implementation_guides/jvm/consumer/junit5) |
| Starters gRPC do Boot | `spring-boot-starter-grpc-server` / `-client` **4.1.2** | BOM do Boot gerencia `spring-grpc` **1.1.2**, `grpc-java` **1.84.1**, `protobuf-java` **4.36.2**; propriedade `spring.grpc.server.port` | `spring-boot-dependencies-4.1.2.pom`; execução |
| `protobuf-maven-plugin` | **`org.xolstice.maven.plugins` 0.6.1** (usado) | **`io.github.ascopes:protobuf-maven-plugin` 5.1.11 (BOM do Boot) e 5.1.12 falharam** com Maven 3.9.12 (`Guice/NullInjectedIntoNonNullable … ProtocResolver`); causa **não investigada**. Requer `os-maven-plugin` 1.7.1 e os executáveis `protoc` 4.36.2 e `protoc-gen-grpc-java` 1.84.1 (200 no Maven Central) | execução |
| Jackson 3 (`tools.jackson.databind`) | **3.1.7** (BOM do Boot) | `FAIL_ON_UNKNOWN_PROPERTIES` **desligado de fábrica** (no Jackson 2 era ligado); `READ_UNKNOWN_ENUM_VALUES_AS_NULL` agora em **`EnumFeature`**; `JsonMapper.rebuild()` | `javap`; execução |
| Maven do laboratório | Maven **3.9.12**; JDK **25.0.5** | 4 módulos; ~40 s por `mvn test` | execução |

## Verificado em P6 (2026-11-14)
Versões efetivas = **BOM do Spring Boot 4.1.2** (`spring-boot-dependencies-4.1.2.pom`: `micrometer` 1.17.2, `micrometer-tracing` 1.7.2, `opentelemetry` 1.62.0, `prometheus-client` 1.7.0); **Prometheus, Grafana e Jaeger** são imagens Docker (versões lidas dos binários/`/api/health`).
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Micrometer / Micrometer Tracing | **1.17.2** / **1.7.2** | `micrometer-registry-prometheus` **precisa ser declarado** (não vem do `starter-actuator`) | BOM do Boot; execução |
| `spring-boot-starter-opentelemetry` | **4.1.2** | Traz `spring-boot-micrometer-tracing-opentelemetry`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp` **e** `micrometer-registry-otlp` (métricas OTLP **ligadas por padrão**: desligar com `management.otlp.metrics.export.enabled=false`) | `.pom` do starter; execução |
| OpenTelemetry Java | **1.62.0** | `opentelemetry-sdk-testing` (`InMemorySpanExporter`) para testes; um bean `SpanProcessor` é usado pelo Boot | BOM; execução |
| Propriedades de tracing no Boot 4.1 | `management.tracing.sampling.probability` (padrão **0.1**), `management.opentelemetry.tracing.export.otlp.endpoint`/`.transport` (padrão `http`), `.export.schedule-delay` (**5 s**), `management.opentelemetry.tracing.sampler` (`parent-based-trace-id-ratio`) | `management.otlp.tracing.*` e `management.tracing.opentelemetry.export.*` aparecem como **antigos/substituídos**; propagação `produce=W3C`, `consume=W3C,B3,B3_MULTI` | metadados `spring-configuration-metadata.json` dos JARs |
| Observação de mensageria | `spring.kafka.template.observation-enabled`, `spring.kafka.listener.observation-enabled`, `spring.rabbitmq.template.observation-enabled`, `spring.rabbitmq.listener.simple.observation-enabled` | Todas com padrão **`false`** (**lido, não executado**) | metadados |
| `spring-boot-starter-restclient` | **4.1.2** | **Necessário** para existir o bean `RestClient.Builder` (e portanto cliente instrumentado); `RestClient.create()` **não** é instrumentado (traço quebra: medido) | execução |
| Logging estruturado | `logging.structured.format.console` = `ecs`/`logstash`/`gelf`; `logging.structured.json.rename[campo]`/`add.campo` | Use **colchetes** em `rename[...]` (sem eles a caixa se perde); **vazio** volta ao texto; o formato é **global da JVM** (testes com formatos diferentes precisam de JVMs separadas) | execução |
| `spring.task.execution.propagate-context` | padrão `false` | Liga `ContextPropagatingTaskDecorator` (traço atravessa `@Async`; o `requestId` **não**) | `TaskExecutorConfigurations` (código-fonte); execução |
| Padrões de gestão do Boot 4.1 | `management.endpoints.web.exposure.include` = `health`; `management.endpoint.health.probes.enabled` = `true`; `management.endpoint.health.show-details` = `never`; `management.tracing.exemplars.include` = `sampled-traces`; `management.metrics.web.client.max-uri-tags` = 100 | | metadados |
| Pacote do `HealthIndicator` no Boot 4 | `org.springframework.boot.health.contributor.{Health,HealthIndicator}`; `LocalManagementPort` em `org.springframework.boot.test.web.server` | | execução |
| Prometheus | **3.16.0** (`prom/prometheus:latest` em 2026-11-14) | retenção local padrão **15 dias**; `promtool check rules`/`test rules`/`check config` | `promtool --version`; `prometheus --help` |
| Grafana | **13.3.0** (`grafana/grafana:latest`) | provisionamento por arquivo; `GF_SECURITY_ADMIN_PASSWORD__FILE` **funciona** (medido) | `/api/health`; execução |
| Jaeger | **v2.23.0** (`jaegertracing/jaeger:latest`) | OTLP/HTTP em **4318**; a **busca** da API v1 (`/api/traces?service=`) dá **404**: usar `/api/v3/traces?query.*` (resposta OTLP/JSON); `GET /api/traces/<id>` funciona | execução |
| Spring Security 7 na porta de gestão | Boot 4.1.2 | A cadeia da aplicação **vale também na porta de gestão** (anônimo 401 no `/actuator/prometheus`); papel próprio `METRICS` para o *scrape* | `mgmt-security-lab` |
| Maven do laboratório | Maven **3.9.12**; JDK **25.0.5**; `surefire` com `reuseForks=false` | 31 testes; ~7 min o `run-all.sh` completo | execução |

## Verificado em P7 (2026-11-14)
Fontes: execução local (**medido**), `dl.k8s.io`, API de *releases* do GitHub, Maven Central e documentação oficial (**lida**). **Nenhum componente de Kubernetes foi executado** (o ambiente não permitiu instalar `kind`/`kubectl`/`helm`).

| Item | Versão / valor | Observação | Fonte |
|---|---|---|---|
| Kubernetes (estável) | **v1.37.2** | `dl.k8s.io/release/stable.txt` | `curl` |
| kind | **v0.33.0** (2026-08-26) | binário e `sha256` baixados e conferidos, **mas não instalados/executados** (negado pelo ambiente); a documentação atual do kind usa o **Cloud Provider KIND** para Ingress/LoadBalancer | GitHub API; kind.sigs.k8s.io |
| Helm | **v4.3.2** (2026-11-13) | Helm 4: `--atomic` → `--rollback-on-failure`, `--force` → `--force-replace`, `--server-side` padrão **true**, `--wait` com estratégias | GitHub API; helm.sh (lido) |
| metrics-server | **v0.9.1** (2026-10-29) | não instalado | GitHub API |
| Gateway API | **v1.7.0** (2026-10-29) | citada, não usada | GitHub API |
| ingress-nginx | última *release* `controller-v1.15.1` (2026-03-19) | **aposentado**: manutenção "melhor esforço" só até **março de 2026** | GitHub API; kubernetes.io (blog 2025-11-11) |
| Traefik | v3.7.19 (2026-11-13) | citado como alternativa, não usado | GitHub API |
| Jib (`jib-maven-plugin`) | **3.5.2** | `dockerBuild` executado sobre o PayFlow Lite: 527 MB, 10 camadas, usuário root por padrão | Maven Central; execução |
| Buildpacks (`spring-boot:build-image`) | Boot 4.1.2; *builder* `paketobuildpacks/builder-noble-java-tiny` (1,4 GB), *run* `ubuntu-noble-run-tiny` 0.0.159; `bellsoft-liberica` 11.10.5 | 686 MB, 20 camadas; calculador de memória (`-Xmx188175K` com 768M) | execução |
| Imagem JRE | `eclipse-temurin:25-jre` (Java 25.0.5) | JVM sob limites: ver `jvm-lab/out-jvm.txt` | execução |
| Redimensionamento de pods em execução | **estável desde o Kubernetes 1.35** | `resizePolicy` por recurso | kubernetes.io (lido) |
| HPA (padrões) | tolerância **0,1**; subida: janela 0, +100% ou +4 pods / 15 s; descida: janela **300 s**, −100% / 15 s | `autoscaling/v2` | kubernetes.io (lido) |
| Sondas | `startupProbe` com `failureThreshold × periodSeconds` cobrindo o pior caso de partida | exemplo de 30 × 10 = 300 s na documentação | kubernetes.io (lido) |
| `preStop` | tipos: `exec`, `httpGet`, `sleep` (`tcpSocket` *deprecated*) | `terminationGracePeriodSeconds` padrão 30 s | kubernetes.io (lido) |
| Ingress (API) | `networking.k8s.io/v1` **congelada** (sem plano de remoção); o projeto recomenda a **Gateway API** | | kubernetes.io (lido) |
| Spring Boot e Kubernetes | `configtree:` cita ConfigMaps/Secrets montados; `health.group.*.additional-path` (`server:`/`management:`); `preStop` na documentação do Actuator | **lido, não executado** | docs.spring.io |
| Maven do laboratório | Maven **3.9.12**; JDK **25.0.5** | | execução |


## Verificado em P8 (2026-11-14)
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Terraform | **1.17.1** instalado (a mais recente era **1.17.2**) | `required_version >= 1.14`; S3 com `use_lockfile` (DynamoDB **obsoleto**), `terraform test` com `mock_provider`, blocos `moved`/`import` | `terraform version`; docs do backend S3 (lido) |
| AWS provider (`hashicorp/aws`) | **6.73.0** (`~> 6.0`) | ~772 MB por `.terraform`; `.terraform.lock.hcl` **vai para o Git** | `terraform init` |
| moto (emulador de AWS) | imagem `motoserver/moto:latest` | prova sintaxe e ordem, **não** o comportamento da AWS; 2 diferenças no 2º `plan` (tags do S3, ID do SG) | execução |
| Trivy | **0.76.2**, `sha256:8f6bb947…` (= `latest` em 2026-11-14) | contêiner fixado por *digest*; `docker save | --input` (sem *socket*) | `trivy --version`; `docker images --digests` |
| `actionlint` / `shellcheck` | **1.7.12** / **0.11.0** | imagens Docker `rhysd/actionlint`, `koalaman/shellcheck` | execução |
| `hashicorp/setup-terraform` | **v4.0.1** (`dfe3c3f8…`) | `terraform_wrapper: false` | `git ls-remote --tags` |
| `azure/setup-helm` | **v5.1.0** (`2f5e7bc6…`) | **não executado** | `git ls-remote --tags` |
| `actions/attest` | **v4.2.2** (`1e69f48a…`) | entradas `subject-name`, `subject-digest`, `push-to-registry` conferidas no `action.yml`; **não executado** | `git ls-remote --tags`, `action.yml` |
| `actions/download-artifact` | **v8.0.2** (`9000827c…`) | | `git ls-remote --tags` |
| OpenFeature SDK Java (`dev.openfeature:sdk`) | **1.23.0** (a mais recente no Maven Central) | API do README: `getInstance`, `setProviderAndWait`, `getClient`, `getBooleanValue`; **não executado** | Maven Central, README do repositório |
| Helm (nos *workflows*) | **v4.3.2** | `--rollback-on-failure`; **não executado** | P7.08 |
| nginx / PostgreSQL (laboratórios) | `nginx:1.29-alpine` / `postgres:18` | `split_clients`; *expand/contract* real | execução |
| GitHub: checks obrigatórios | **bem-sucedido, ignorado ou neutro** passam | um *job* pulado conta como aprovado | docs.github.com (lido) |
| OpenGitOps | **v1.0.0**: declarativo, versionado e imutável, obtido automaticamente, reconciliado continuamente | | opengitops.dev (lido) |
| DORA | 3 de vazão (tempo de entrega da mudança, frequência, tempo de recuperação) + 2 de instabilidade (taxa de falha, retrabalho) | | dora.dev (lido) |

## Verificado em P9 (2026-11-14)
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| moto (emulador de AWS) | **5.2.6.dev** (imagem `motoserver/moto:latest`) | **emulação de API**: não executa contêineres nem funções, **não avalia alarmes nem filtros de log**, não impõe políticas; **travou** em `aws_appautoscaling_target`, `aws_sqs_queue_redrive_allow_policy` e na réplica do RDS pelo Terraform; ignora `snap_start`; a resposta de `promote-read-replica` ainda mostra a origem | `pip`/execução |
| AWS provider (`hashicorp/aws`) | **6.73.0** (`~> 6.0`) | `secret_string_wo` (*write-only*) exige Terraform ≥ 1.11 | `terraform init`; docs do provedor (lido) |
| Provedor `random` | **3.9.1** (`~> 3.7`) | `ephemeral "random_password"` | `terraform init` |
| AWS SDK for Java v2 | **2.59.1** (BOM; a mais recente no Maven Central em 2026-11-13); `url-connection-client` | clientes **sem** *endpoint* no código: `AWS_ENDPOINT_URL` | Maven Central; execução |
| Spring Cloud AWS | **4.2.2** (a mais recente no Maven Central); tabela do repositório: **4.x ↔ Spring Cloud 2025.1 ↔ Spring Boot 4.0.x**, Spring Framework 7.0.x | **não adotado** (Boot 4.1.2 está fora da tabela: o mesmo achado da P5); SDK v2 atrás de portas | Maven Central; github.com/awspring/spring-cloud-aws (lido) |
| `aws-lambda-java-core` | **1.4.1** (a mais recente no Maven Central) | contrato `RequestHandler` | Maven Central |
| Lambda | *runtime* **`java25`** (e `java21`, `java17.al2023`...), Amazon Linux 2023, fim de suporte em **2029**; SnapStart: **Java 11+**, só versões publicadas/*aliases*, **não** com *provisioned concurrency*, EFS nem armazenamento efêmero > 512 MB | CPU proporcional à memória | docs.aws.amazon.com (lido) |
| ECS/Fargate | CPU 512 aceita **1 a 4 GB**; `stopTimeout` máx. **120 s**; *circuit breaker* em 2 estágios (valida ELB/AWS Cloud Map/contêiner); segredos injetados **só na partida** | `secrets.valueFrom`: `ARN:chave-json:estágio:versão` | docs.aws.amazon.com (lido) |
| ALB | sub-rede **/27 ou maior** com **8 IPs livres**; *deregistration delay* padrão **300 s**; *health check* `Matcher` padrão **200** | | docs.aws.amazon.com (lido) |
| VPC | bloco **/16 a /28**; **5 IPs reservados** por sub-rede; *endpoint* de gateway (S3) **sem cobrança adicional**; NAT público exige Elastic IP | exemplo de preço na página: NAT **US$ 0,045/h** e **US$ 0,045/GB**; *endpoint* de interface **US$ 0,01/GB** (primeiro PB) | docs.aws.amazon.com e aws.amazon.com/vpc/pricing, /privatelink/pricing (lidos) |
| EKS | **US$ 0,10/cluster/h** (suporte padrão, ~14 meses) e **US$ 0,60** (estendido); *Pod Identity* sem OIDC; *access entries*; **Auto Mode** | **nenhum cluster real criado** | aws.amazon.com/eks/pricing; docs.aws.amazon.com (lidos) |
| SQS | padrão: *at-least-once*, ordem "melhor esforço"; *visibility timeout* **30 s** (padrão) a **12 h**; retenção **4 dias** (padrão) a **14 dias**; mensagem até **1 MiB**; ~**120.000** em voo; DLQ conta a retenção do **envio original**; FIFO alta vazão: **3.000/s** com *batching* ou **300/s** por partição | 1 milhão de requisições grátis por mês | docs.aws.amazon.com; aws.amazon.com/sqs/pricing (lidos) |
| SNS | entrega **crua** retira os metadados e limita a **10** atributos de mensagem; tópicos FIFO | | docs.aws.amazon.com (lido) |
| EventBridge | **dois** barramentos na documentação: *Custom Event Bus* (retenção, ordem, *replay*; **recomendado para aplicações novas**) e *Classic* (regras e alvos); o laboratório usa o **clássico** | ⚠️ suporte do provedor ao novo **não verificado** | docs.aws.amazon.com (lido) |
| Secrets Manager / Parameter Store | RDS rotaciona a senha mestra **a cada 7 dias** por padrão (muda a senha **no banco**); Parameter Store **padrão: sem cobrança adicional**; a documentação do Parameter Store **recomenda** o Secrets Manager para credenciais | | docs.aws.amazon.com (lido) |
| CloudWatch | retenção de logs **indefinida** por padrão; métricas **OTLP** = "caminho recomendado"; EMF para métrica + evento de log; **alarmes PromQL** e compostos | | docs.aws.amazon.com (lido) |
| RDS | Multi-AZ (instância): *standby* **não** atende leitura; *failover* **60–120 s**; réplica PostgreSQL com primário ocioso reporta lag de até **5 min**; logs de transação ao S3 a cada **5 min** (PITR); TTL de DNS da JVM **≤ 60 s** recomendado | | docs.aws.amazon.com (lido) |
| PostgreSQL (laboratório) | **18.6** (`postgres:18`), replicação por *streaming* (`pg_basebackup -R`) | `recovery_min_apply_delay` só para **simular** lag | execução |
| JVM (laboratório) | OpenJDK **25.0.5**; `eclipse-temurin:25-jre`; DNS: `networkaddress.cache.ttl` **não definido** → renova em **30 s** (medido) | Serial GC com 0,5 vCPU | execução |
| JUnit / AssertJ / Surefire (laboratório) | **6.0.3** / **3.27.7** / **3.5.4** | | Maven |
| Docker | **29.9.2** | | `docker --version` |

## Verificado em P10 (2026-11-14)
| Tecnologia | Versão usada/testada | Observação | Fonte |
|---|---|---|---|
| Keycloak | **26.8.2** (`quay.io/keycloak/keycloak:26.8.2`; a mais recente no Maven Central e no Quay em 2026-11-14; publicada em 2026-11-05 no Maven Central) | `start-dev` (HTTP, banco embutido) **só** no laboratório; descoberta anuncia `implicit` e `password`; `code_challenge_methods_supported = [plain, S256]` (o cliente exige S256); **importar um realm com `clientScopes` NÃO cria os escopos embutidos** (token sem `sub`/`realm_access`) | `curl` ao Maven Central/Quay; execução |
| Spring Security | **7.1.2** (a do Boot 4.1.2) | `oauth2ResourceServer().jwt()`; `OAuth2ClientHttpRequestInterceptor`; `WWW-Authenticate` ganha `resource_metadata` (RFC 9728); `spring-security-oauth2-authorization-server` também em 7.1.2 (**não usado**) | `~/.m2`; execução |
| Propriedades do Resource Server (Boot 4.1.2) | `spring.security.oauth2.resourceserver.jwt.{issuer-uri, jwk-set-uri, audiences, jws-algorithms, public-key-location, principal-claim-name, authority-prefix, authorities-claim-name, authorities-claim-expressions}` | com `jwk-set-uri` **e** `issuer-uri`, o JWKS vem do primeiro e o emissor ainda é validado; só com `issuer-uri`, a descoberta é **preguiçosa** | metadados do `spring-boot-security-oauth2-resource-server`; execução |
| Boot 4: `RestClient.Builder` | exige `spring-boot-starter-restclient`; `AutoConfigureMockMvc` em `org.springframework.boot.webmvc.test.autoconfigure`; `JsonNode`/`ObjectMapper` em `tools.jackson.databind` (Jackson 3) | sem o starter o contexto não sobe | execução |
| Nimbus JOSE+JWT | **10.9.1** | assina/verifica os tokens de teste | `mvn`; SBOM |
| HikariCP / PostgreSQL JDBC | os do Boot 4.1.2 / 42.7.x (`postgresql` no `~/.m2`: 42.7.14) | `HikariConfigMXBean.setUsername/setPassword` valem para conexões **novas** | execução |
| PostgreSQL (laboratório) | **18** (`postgres:18`) | verificador **SCRAM-SHA-256**; `pg_authid` ilegível para a aplicação | execução |
| CycloneDX Maven plugin | **2.9.1**; formato **CycloneDX 1.6** (JSON); o Trivy gera **1.7** | grava hashes SHA3 que o Trivy ignora (256 avisos) | `~/.m2`; execução |
| Trivy | **0.76.2** (por *digest*, como na P8); banco de vulnerabilidades de **2026-11-14 13:05 UTC** | `trivy sbom` e `trivy image --format cyclonedx` | execução |
| cosign | **v3.1.4** (`ghcr.io/sigstore/cosign/cosign:latest`: ⚠️ deveria ser fixado por *digest*) | `--tlog-upload=false` exige **também** `--use-signing-config=false`; ambas obsoletas; mensagem de chave errada fala em *attestations*; saída fala em "transparency log verified offline" mesmo com `--insecure-ignore-tlog` | execução |
| `registry:2` | pull de 2026-11-14 | registro local (loopback, sem TLS) **só** no laboratório | execução |
| Maven: JAR reprodutível | `project.build.outputTimestamp` | dois *builds* **idênticos** com a propriedade, **diferentes** sem ela (só o JAR; a imagem **não** foi testada) | execução |
| RFC (conferidos em 2026-11-14) | 6749 (OAuth 2.0), 7636 (PKCE), 6750 (Bearer), 7519 (JWT), 9068 (perfil JWT de access token), 9207 (`iss`), 9700 (BCP de segurança do OAuth 2.0, **janeiro de 2025**; §2.4: *password* "MUST NOT be used"), 9728 (metadados do servidor de recursos), 8693 (troca de token), 7662 (introspecção), 8725 (BCP de JWT) | OAuth 2.1 ainda é *draft* (conferir em oauth.net/2.1) | `curl` ao rfc-editor.org |
| Secrets Manager: estratégias de rotação | ⚠️ página carregada por JavaScript: **conteúdo não lido** | o experimento do banco justifica "usuários alternados"; **não** é citação da AWS | `curl` |
| Docker / JDK | **29.9.2** / **25.0.5** | | `docker --version`; `java -version` |

## Referências do módulo S1 (verificadas em 2026-11-14)
| Item | Valor | Observação | Fonte |
|---|---|---|---|
| JDK do laboratório | **OpenJDK 25.0.5** (`java X.java`, sem Maven e sem Docker) | 13 programas de Java puro | `java -version` |
| Rascunho IETF `Idempotency-Key` | **draft-ietf-httpapi-idempotency-key-header-08** (atualizado em 2026-10-22; expira em 2027-04-25) | **ainda rascunho**: 400 ausente, 409 em andamento, 422 corpo diferente | [datatracker](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/) |
| Rascunho IETF `RateLimit` | **draft-ietf-httpapi-ratelimit-headers-11** (`RateLimit-Policy`, `RateLimit`) | versões antigas usavam `RateLimit-Limit/Remaining/Reset` | [datatracker](https://datatracker.ietf.org/doc/draft-ietf-httpapi-ratelimit-headers/) |
| PostgreSQL 18 | `max_prepared_transactions` **0** por padrão (2PC desligado) | `PREPARE TRANSACTION` | [doc](https://www.postgresql.org/docs/18/runtime-config-resource.html) |
| Redis Cluster | **16384** *hash slots* (`CRC16(chave) mod 16384`) | *hash tags* para chaves no mesmo *slot* | [doc](https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/) |
| ALB | `load_balancing.algorithm.type` = `round_robin`, `least_outstanding_requests`, `weighted_random` | nome no *provider* Terraform **a conferir** | [doc](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/edit-target-group-attributes.html) |
| Bibliotecas novas | **nenhuma** (o S1 não executou código com Maven) | Bucket4j citado, **versão a escolher** no fim do treinamento | — |

## Referências do módulo S2 (verificadas em 2026-11-14)
| Item | Valor | Observação | Fonte |
|---|---|---|---|
| JDK do laboratório | **OpenJDK 25.0.5** (`java X.java`, JFR, `jcmd`, `jfr view`) | 13 programas de Java puro; 12 núcleos, máquina compartilhada | `java -version` |
| JMH | **1.37** (Maven Central: `latest`/`release` = 1.37) | `jmh-core` + `jmh-generator-annprocess`; `maven-compiler-plugin` 3.14.0, `maven-shade-plugin` 3.6.0; padrão do JMH: **5 *forks*, 5 aquecimentos e 5 medições de 10 s** (conferido em `-h`) | Maven Central, execução |
| JFR (JDK 25) | `default` amostra a pilha a **20 ms**; `profile` a **10 ms** (`lib/jfr/*.jfc`); `jfr view` com *views* prontas | JEP 518 (amostragem cooperativa) e JEP 509 (CPU-time, experimental) são do **JDK 25** | JEPs (Release 25), execução |
| ZGC | `-XX:+UseZGC` é **geracional por padrão** desde o JDK 23 (JEP 474); o modo não geracional foi **removido** no JDK 24 (JEP 490) | base: JEP 439 (JDK 21) | openjdk.org/jeps |
| Cache AOT | JEP 483 (JDK 24: *Class Loading & Linking*), JEP 514 (JDK 25: *Command-Line Ergonomics*), JEP 515 (JDK 25: *Method Profiling*); `-XX:AOTCacheOutput=` e `-XX:AOTCache=` | medido num programa minúsculo (227 → 116 ms) | openjdk.org/jeps, execução |
| k6 | **`grafana/k6:1.8.3`** (Docker Hub, tag `1.8.3`) | `constant-arrival-rate`; `thresholds` falhando ⇒ **código de saída 99** (medido); imagem **removida** ao final | Docker Hub, execução |
| GraalVM | **`ghcr.io/graalvm/native-image-community:25`** (CE) | o *entrypoint* da imagem **é** o `native-image`; `--no-fallback`; compilação do `MiniApp` ~44 s, ~1,9 GB, binário 32 MB; imagem **removida** ao final | execução |
| CRaC | **`azul/zulu-openjdk:25-jdk-crac`** (Zulu 25.38, motor padrão **`warp`**, sem privilégios) | `jcmd JDK.checkpoint`, `-XX:CRaCCheckpointTo`/`-XX:CRaCRestoreFrom`; **socket de escuta aberto impede o checkpoint**; imagem **removida** ao final | Docker Hub, execução |
| async-profiler | **4.6** (README do GitHub, lido em 2026-11-14) | `asprof -d 30 -f flamegraph.html <PID>`; modos `cpu`/`wall`/`alloc`/`lock`; **não executado** | github.com/async-profiler |
| Spring (CRaC e native) | Spring Framework: CRaC exige JVM com suporte (**Linux**), `org.crac` ≥ 1.4.0 e as *flags*; Spring Boot: **mundo fechado**, reflexão declarada | só a **documentação** foi lida; **não executado** | docs.spring.io |
| Gatling | `injectOpen`/`injectClosed`; asserções com `percentile(...)` | **não executado**; sintaxe do exemplo **a conferir** | docs.gatling.io |
| Bibliotecas novas no PayFlow | **nenhuma executada** (`perf-bench/` com JMH 1.37 **por leitura**) | k6 1.8.3 fixada **por leitura** | — |

## Referências do módulo S3 (verificadas em 2026-11-14)
| Item | Valor | Observação | Fonte |
|---|---|---|---|
| PostgreSQL do laboratório | **18.6** (`postgres:18`, Debian 13; imagem já existia) | `wal_level=logical` no contêiner principal; `pgbench` 18.6, `test_decoding`, `pg_trgm`, `unaccent` da própria imagem | `postgres --version`, execução |
| JDK do laboratório | **OpenJDK 25.0.5** (`java X.java`) | `RoutingLab` (spring-jdbc) e `PhoneNormalizer` (Java puro) | `java -version` |
| Spring (laboratório) | **spring-jdbc / tx / core / beans / context / aop / expression 7.0.10** + `commons-logging` 1.3.5 + `jspecify` 1.0.1 | só para `AbstractRoutingDataSource` e `LazyConnectionDataSourceProxy` (**não** é Spring Boot) | `~/.m2`, execução |
| Driver JDBC | **PostgreSQL 42.7.14** | no `~/.m2` | `~/.m2` |
| PostgreSQL 18: `NOT NULL ... NOT VALID` | `ADD CONSTRAINT nome NOT NULL coluna NOT VALID` + `VALIDATE CONSTRAINT` | linha nova com `NULL` é recusada **antes** da validação; `ADD` pede `ACCESS EXCLUSIVE` (ms), `VALIDATE` pede `SHARE UPDATE EXCLUSIVE` | execução |
| PostgreSQL 18: colunas geradas | `VIRTUAL` **não aceita função do usuário** (`Virtual generated columns that make use of user-defined functions are not yet supported`); `STORED` **reescreve** a tabela | execução |
| Partições | PK/`UNIQUE` **devem conter a chave**; `DETACH ... CONCURRENTLY` **recusado** com partição `DEFAULT`; `ATTACH` com `CHECK` validado **sem varredura** | execução; docs `ddl-partitioning` |
| *Logical decoding* | `wal_level=logical`; `pg_create_logical_replication_slot('s','test_decoding')`; `max_slot_wal_keep_size` invalida o *slot* (`wal_status = lost`, `invalidation_reason = wal_removed`) | `test_decoding` lido; **`pgoutput` não exercitado** | execução, docs PostgreSQL 18 |
| Debezium | **3.7** (página de versões: "latest stable"; a lista de versões testadas cita PostgreSQL **14–18**, `decoderbufs` e `pgoutput`; **confirmar para a 3.7**) | propriedades da S3.06 (`connector.class`, `topic.prefix`, `slot.name`, `publication.autocreate.mode`, `snapshot.mode`, `heartbeat.interval.ms`, `column.exclude.list`, `EventRouter`) **conferidas** na documentação; **não executado** | debezium.io (docs e releases) |
| OpenSearch | **3.9.0** (*release* mais recente do repositório no GitHub, publicado em 2026-09-29); documentação em `docs.opensearch.org` | `OPENSEARCH_INITIAL_ADMIN_PASSWORD` obrigatória na imagem Docker; `vm.max_map_count = 262144`; analisadores e consulta `match` com `fuzziness` conferidos; **não executado** | GitHub API, docs.opensearch.org |
| Bibliotecas novas no PayFlow | **nenhuma executada** (`ReportingDataSourceConfig` e `PhoneNormalizer` **por leitura**) | libphonenumber **não** executada | — |

## Referências do módulo S4 (verificadas em 2026-11-14)
| Item | Valor | Observação | Fonte |
|---|---|---|---|
| JDK do laboratório | **OpenJDK 25.0.5** (`java X.java`; o `java` padrão da máquina é o 17) | `SloLab`, `DocLint`, `ChaosLab`, `RpoRtoLab`, `AzLab`, `CapacityLab` (Java puro; `ChaosLab` usa `com.sun.net.httpserver` e `java.net.http`) | `java -version`, execução |
| Prometheus / `promtool` | **3.16.0** (`prom/prometheus:latest`; imagem já existia) | `check rules`, `test rules` (`alert_rule_test`, `promql_expr_test`), mutantes; rodar com `--entrypoint promtool` | `promtool --version`, execução |
| AWS CLI | **2.39.5** (instalada; **só `help`**, nenhuma chamada à AWS) | `restore-db-instance-to-point-in-time` (`--restore-time` ou `--use-latest-restorable-time`), `create-db-snapshot`, `copy-db-snapshot` (`--source-region`, `--kms-key-id`), `start-db-instance-automated-backups-replication` (`--source-db-instance-arn`, `--backup-retention-period`) | `aws rds <cmd> help` |
| Alertas por taxa de queima | **14,4× (1 h + 5 min, 2% do orçamento), 6× (6 h + 30 min, 5%), 1× (3 d + 6 h, 10%)** | janela curta ≈ 1/12 da longa; exemplo de 100% de erro por 5 min a cada 10 min gasta 35% do orçamento sem disparar o alerta de janela curta | SRE Workbook, *Alerting on SLOs*, tabelas 5-6 e 5-8 |
| Gatilhos de postmortem | indisponibilidade/degradação acima de um limiar; **perda de dado**; **intervenção do plantão**; resolução lenta; **falha de monitoramento** | "blameless": foco nas causas contribuintes, sem acusar pessoa ou time | SRE Book, *Postmortem Culture* |
| Papéis de incidente | comando, operação (e comunicação e planejamento) | baseado no Incident Command System; "separação recursiva de responsabilidades" | SRE Book, *Managing Incidents* |
| Plantão | no máximo **25%** do tempo em plantão e **≥ 50%** em engenharia | regra do SRE Book | SRE Book, *Being On-Call* |
| Princípios do caos | estado estável → hipótese → variáveis do mundo real → tentar refutar | "quanto mais difícil perturbar o estado estável, mais confiança" | principlesofchaos.org |
| AWS FIS | *stop condition* = alarme do CloudWatch; experimento parado **não retoma**; ações `aws:ecs:stop-task`, `aws:ecs:task-network-latency`, `aws:rds:reboot-db-instances` (`forceFailover`), `aws:network:disrupt-connectivity` | **não executado**; preço **não verificado** | docs FIS |
| RDS: PITR | restaura para um ponto no tempo criando **nova instância** (a original não muda); `LatestRestorableTime` no `describe-db-instances` | PITR e logs a cada ~5 min vêm da P9.08 | docs RDS |
| AWS Backup Vault Lock | WORM; modo *governance* (removível por quem tem permissão) e *compliance* (imutável após o período de carência, nem a AWS altera) | **não executado** | docs AWS Backup |
| DR na AWS | quatro estratégias: **backup e restauração**, ***pilot light***, ***warm standby***, **multi-site ativo/ativo**; "planos de dados têm metas de disponibilidade maiores que os de controle" | custos e RTO/RPO **não** citados (não verificados) | whitepaper de DR |
| ECS e AZ | tarefas de serviço usam *spread* por `ecs.availability-zone` (melhor esforço); desde **05/09/2025** o ECS liga o rebalanceamento por AZ para serviços elegíveis | **não executado** | docs ECS |
| `predict_linear` | `predict_linear(v range-vector, t scalar)`: regressão linear simples; **só *gauges***; mínimo de 2 amostras | a janela do intervalo é a decisão (S4.07) | docs Prometheus |
| Bibliotecas novas no PayFlow | **nenhuma** (artefatos de operação por leitura) | `DocLint` é uma ferramenta de laboratório | — |

## Referências do módulo S5 (verificadas em 2026-11-23)
| Tecnologia | Versão / fato | Observação | Fonte |
|---|---|---|---|
| JDK / `keytool` | **OpenJDK 25.0.5** (`/usr/lib/jvm/java-25-openjdk-amd64`); o `java` padrão da máquina é o 17 e **muda o resultado** | o `MtlsLab` gerou CA e folhas (EC P-256) com o `keytool` do JDK 25; TLS 1.3 `TLS_AES_256_GCM_SHA384`; `jdk.tls.disabledAlgorithms` recusa TLS 1.1 | execução |
| PostgreSQL | **18** (`postgres:18`) | `GRANT` por coluna, função `SECURITY DEFINER` com piso, `VALID UNTIL`; **imagem oficial: `127.0.0.1` em `trust`** | execução; postgresql.org/docs/18 |
| Trivy | **0.76.2** (imagem `ghcr.io/aquasecurity/trivy:0.76.2`; banco baixado em 2026-11-23) | `fs --scanners vuln/secret/misconfig`; `--ignorefile`, `--exit-code`, `--ignore-unfixed` existem (`--help`); **429 do Maven Central** em pasta com `pom.xml` | execução |
| Prometheus / `promtool` | **3.16.0** (`prom/prometheus:latest`) | regras de auditoria: 5 testes, 2 mutantes | execução |
| `actionlint` | **1.7.12** (`rhysd/actionlint`) | `security.yml` passa; mutante reprovado | execução |
| OWASP ASVS | **5.0.0** (publicada em 30/05/2025) | requisitos verificáveis | owasp.org |
| OWASP Top 10 | **2025** (o endereço `owasp.org/Top10/` redireciona para ela) | | owasp.org |
| OWASP ZAP | imagem `ghcr.io/zaproxy/zaproxy:stable`; `zap-baseline.py` **passivo**; opções `-c`, `-I` | **não executado** | zaproxy.org |
| AWS Encryption SDK for Java | `com.amazonaws:aws-encryption-sdk-java` **3.0.2** (Maven Central, 2025-06-04) | **não executado** | repo1.maven.org |
| AWS SDK for Java v2 (`kms`) | **2.59.7** (Maven Central, 2026-11-21); o BOM do PayFlow segue em 2.59.1 (P9) | **não executado**; atualize o BOM com o resto | repo1.maven.org |
| AWS KMS | rotação automática padrão **365 dias**, o KMS **retém** o material antigo; exclusão **7–30 dias** (padrão 30); contexto = AAD, não secreto, aparece no CloudTrail | **não executado** | docs.aws.amazon.com/kms |
| Amazon RDS (criptografia) | cifra armazenamento, logs, *backups* automáticos, réplicas e *snapshots* (AES-256, KMS); **a chave KMS não muda depois** | **não executado** | docs.aws.amazon.com/AmazonRDS |
| ALB mTLS | modos **verify** e **passthrough**; cabeçalhos `X-Amzn-Mtls-*` | **não executado** | docs.aws.amazon.com/elasticloadbalancing |
| CloudTrail | validação de integridade: SHA-256 + assinatura SHA-256 com RSA; resumos encadeados | **não executado** | docs.aws.amazon.com/awscloudtrail |
| S3 Object Lock | modos *governance* e *compliance* | **não executado** | docs.aws.amazon.com/AmazonS3 |
| Spring Boot 4.1 (SSL) | `server.ssl.bundle`, `server.ssl.client-auth`, `spring.ssl.bundle.pem.*.reload-on-update` (Tomcat/Netty), `RestClientSsl` (`org.springframework.boot.restclient.autoconfigure`), `SslHealthIndicator` + `management.health.ssl.certificate-validity-warning-threshold` | **não executado** | docs.spring.io |
| NIST SP 800-38D | §8.3: com IV aleatório, **≤ 2³²** chamadas por chave | texto do PDF lido | csrc.nist.gov |
| ANPD | comunicação de incidente em **3 dias úteis** (Res. CD/ANPD 15/2024), complementação em 20 dias úteis | página da ANPD | gov.br/anpd |
| Keycloak | *OAuth 2.0 Mutual TLS Certificate Bound Access Tokens*; eventos de usuário e de administração (**não armazenados por padrão**) | **não executado** | keycloak.org |
| LGPD (texto), PCI DSS v4.0.1 (texto) | **não conferidos** (planalto.gov.br sem resposta; PCI exige cadastro) | números de artigos e requisitos são de conhecimento prévio | — |
| Bibliotecas novas no PayFlow | **nenhuma obrigatória** (AWS Encryption SDK se adotar cifra por campo; SpotBugs + FindSecBugs opcional) | | — |

## Referências do módulo S6 (verificadas em 2026-11-23)
| Tecnologia | Versão / fato | Observação | Fonte |
|---|---|---|---|
| JDK | **OpenJDK 25.0.5.1** (`/usr/lib/jvm/java-25-openjdk-amd64`; atualizado desde a S5) | lançador de código-fonte com **vários arquivos** (JEP 458): `java FinOpsLab.java` acha `PriceBook.java` | execução |
| AWS Price List Bulk API | arquivos públicos por oferta/região (`pricing.us-east-1.amazonaws.com/offers/v1.0/aws/<oferta>/current/<região>/index.csv`) e *Savings Plans* (`.../savingsPlan/v1.0/aws/AWSComputeSavingsPlan/...`), **sem credenciais** | ofertas usadas publicadas de **08 a 23/11/2026**; EC2 e *Savings Plans* ~300 MB por região (lidos em fluxo); `lab/prices.csv` com 100 preços | execução; docs.aws.amazon.com/awsaccountbilling |
| Preços citados (`us-east-1` / `sa-east-1`) | Fargate ARM vCPU-h **0,03238 / 0,0557**, GB-h **0,00356 / 0,00612**; RDS PostgreSQL `db.m7g.large` Multi-AZ **0,337 / 0,442** por hora; *endpoint* de interface **0,01 / 0,021** por hora; NAT **0,045 / 0,093** por hora e por GB; CloudWatch Logs ingestão **0,50 / 0,90** por GB; Lambda ARM **0,0000133334** por GB-s e **0,0000002** por requisição (iguais nas duas); entre AZs **0,01**/GB por sentido | **mudam**: refaça com `fetch-prices.py` | Price List |
| Compute Savings Plan | Fargate ARM 1 ano sem adiantamento: **−21%** (`sa-east-1`); 1 ou 3 anos; tudo, parte ou nada adiantado; "até 72%"; vale para EC2, Fargate e Lambda | **não comprado** | Price List; docs Savings Plans |
| Spot Instance Advisor | `m7g.large`: economia **58%** (`us-east-1`) e **38%** (`sa-east-1`), faixa de interrupção **"> 20%"** (lido em 23/11/2026); página: "até 90%", média histórica **< 5%** | faixa **mensal**, não taxa por hora | spot-advisor-data.json; aws.amazon.com/ec2/spot/instance-advisor |
| AWS Well-Architected Framework | **atualizado em 16/11/2026**: documento único; **Cloud Foundations** (FND01–FND07) + **Cloud Workloads** (6 pilares, seções *Understanding trade-offs*); níveis HRI/MRI | `the-pillars-of-the-framework.html` dá **404**; páginas antigas por pilar ainda **200** | docs.aws.amazon.com/wellarchitected |
| Azure WAF / Google Cloud WAF | Azure: **5** pilares na matriz da página; Google: **6** pilares (inclui "segurança, privacidade e conformidade" e "otimização de desempenho") + perspectivas IA/ML e serviços financeiros | | learn.microsoft.com; cloud.google.com |
| AWS Lambda | memória **128–10.240 MB**; **1.769 MB = 1 vCPU**; duração arredondada para **1 ms**; **tempo limite 30 min** (cota atual); um ambiente de execução por requisição simultânea | **não executado** | docs Lambda; preços do Lambda |
| AWS Fargate / EC2 | Fargate: por segundo com **mínimo de 1 minuto**, desde o *pull* da imagem; Fargate Spot: aviso de **2 min** (SIGTERM + EventBridge), **não** troca por sob demanda sozinho; EC2: cobrança por segundo; Spot: aviso de **2 min** | **não executado** | docs ECS/EC2; preços |
| AWS Organizations (cotas) | **≥ 1 e ≤ 10 SCPs** por raiz/OU/conta; SCP até **10.240 caracteres**; RCP até 5 por entidade (inclui `RCPFullAWSAccess`); **10 contas** por padrão (ajustável); OUs até **5 níveis** | **não executado**; o 1º desenho do laboratório violava o mínimo de 1 SCP | docs Organizations |
| SCP / IAM | SCP **não concede**; Allow em **todos** os níveis; conta de gestão e papéis vinculados a serviço **não** afetados; *root* das contas-membro **afetado**; chave de condição ausente: positivo = falso, negado = verdadeiro | modelado em `LandingZoneLab` | docs Organizations/IAM |
| Tags de custo | chaves e valores **sensíveis a maiúsculas**; ativação até **24 h + 24 h**; *tag policy* **não avalia** recurso sem tag | | docs Billing/Organizations/Tag Editor |
| AWS Budgets / Cost Anomaly Detection | Budgets: real e previsão, atualizado **até 3× por dia** (8–12 h); Anomaly Detection: aprendizado de máquina, monitores e assinaturas | **não executado** | docs Cost Management |
| Data Exports / FOCUS | **CUR 2.0** recomendado; exportações **FOCUS 1.0 e 1.2** com colunas AWS; especificação FOCUS na **1.4** | **não executado** | docs CUR; focus.finops.org |
| Transferência de dados | 100 GB/mês grátis para a internet (agregado); isenção para quem **sai da AWS** via suporte (março de 2024); `sa-east-1 → us-east-1` **US$ 0,138/GB** × `us-east-1 → sa-east-1` **US$ 0,02/GB** | | blog AWS; Price List |
| Terraform / provedor AWS | **1.17.1** / **6.73.0** (cache da P8) | `validate` em `finops` e `organization` + mutante; `aws_ce_cost_allocation_tag`, `aws_budgets_budget`, `aws_ce_anomaly_monitor`/`_subscription`, `aws_organizations_*` | execução |
| AWS SDK for Java v2 | **2.59.1** (`~/.m2`, P9): SNS, SQS, Secrets Manager | 7 classes de exemplo **compiladas**; S3 não estava no cache | execução |
| Bibliotecas novas no PayFlow | **nenhuma** (artefatos de arquitetura e custo; ArchUnit já existe desde a P4) | | — |
