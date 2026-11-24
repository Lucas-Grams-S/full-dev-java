# J5.CP — Checkpoint do módulo J5

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J5.01 a J5.15

## Parte 1 — Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Explique **inversão de controle** e **injeção de dependência** e dê **duas razões** para preferir a injeção **por construtor**. Qual erro um contêiner mostra quando há **dois** beans candidatos para um parâmetro, e como resolver?
2. O que faz `@SpringBootApplication` (as três anotações) e por que a classe principal fica no **pacote raiz**? Por que o `pom.xml` do PayFlow **não tem versões** nas dependências do Spring?
3. Qual a diferença entre `@Component`, `@Service` e `@Repository`? Por que o bean que você recebe às vezes tem o nome `...$$SpringCGLIB$$0`? Por que um prototype injetado em um singleton "não funciona" e como corrigir?
4. Como a autoconfiguração decide criar o `DataSource`? O que `@ConditionalOnMissingBean` garante? Como descobrir **por que** um bean existe (ou não)?
5. Liste a **ordem de precedência** de: argumento de linha de comando, `application.yml`, variável de ambiente e propriedade `-D`. O que significa "sem perfil = produção" no PayFlow e por que o `application.yml` base **não** tem URL de banco?
6. Descreva o caminho de uma requisição `POST /accounts/{id}/deposit` por **Tomcat → `DispatcherServlet` → … → resposta**. Por que o PayFlow devolve **303** (e não 302) depois do depósito? Por que `redirect:/x` não serve?
7. Por que `@ManyToOne` deve ser `fetch = LAZY`? O que é o **contexto de persistência** e o que o **dirty checking** faz? O que causa `LazyInitializationException` e por que o PayFlow desliga o `open-in-view`?
8. O que é uma **consulta derivada** (`findByOwnerIdOrderByCreatedAtAscIdAsc`) e quando se usa `@Query`? Quantas consultas fazem `Slice` e `Page`? Por que a ordenação de uma lista paginada precisa de **desempate**?
9. Como o `@Transactional` funciona por baixo? Dê **três** situações em que ele **não** desfaz ou **não** abre transação (e a correção de cada uma). Para que serve `REQUIRES_NEW`?
10. O que é o problema **N+1** e como você o detecta e corrige? Explique as **três camadas de validação** do PayFlow (Bean Validation, domínio, banco) e por que `Accept-Language` pode transformar `25,90` em `2590`. O que o PayFlow loga (e **não** loga), e por que `/actuator/health` retorna 503 com o banco parado mas o liveness continua `UP`?

<details><summary>Gabarito</summary>

1. **IoC**: o framework (e não o seu código) controla quando e como os objetos são criados e ligados; **DI** é a técnica em que ele entrega as dependências prontas. Construtor: campos `final` (objeto sempre válido e imutável) e teste sem contêiner (`new Servico(dublê)`); a documentação do Spring a recomenda; dependências obrigatórias ficam **explícitas**. Dois candidatos: `NoUniqueBeanDefinitionException` (dentro de `UnsatisfiedDependencyException`: "expected single matching bean but found 2: a,b"); resolve com `@Primary` ou `@Qualifier("nome")` (ou redesenho).
2. `@SpringBootConfiguration` (é uma `@Configuration`), `@EnableAutoConfiguration` (autoconfiguração) e `@ComponentScan` (varre **o pacote da classe e os de baixo**); fora do pacote raiz o scan não acha os beans. As versões vêm do **BOM** do `spring-boot-starter-parent` (4.1.2): o Boot escolhe versões compatíveis entre si (por exemplo, Flyway 12.4.0, Hibernate 7.4.13); sobrescrever só com motivo registrado.
3. Todos registram o bean por *scan*; `@Service` documenta a regra de negócio; `@Repository` indica acesso a dados e ativa a **tradução de exceções**. O `$$SpringCGLIB$$0` é um **proxy** (subclasse gerada) para `@Configuration`, tradução de exceções de `@Repository` e `@Transactional`; classes de bean não podem ser `final`. Prototype em singleton: o singleton é criado **uma vez** e guarda **um** prototype; corrija com `ObjectProvider<Proto>` e `getObject()`.
4. `DataSourceAutoConfiguration` é candidata (em `META-INF/spring/...AutoConfiguration.imports` do módulo `spring-boot-jdbc`); passa se `javax.sql.DataSource` está no classpath, se há o Hikari e se `spring.datasource.url` está definida; o `@ConditionalOnMissingBean` faz o Boot **recuar** se você declarou o seu `DataSource`. Para descobrir: `--debug` (relatório de condições) ou `/actuator/conditions`.
5. Da menor para a maior: **arquivos** (e o do perfil ativo vence o geral) < **variáveis de ambiente** < **propriedades `-D`** < **argumentos** (`--x=y`). Sem perfil, o PayFlow usa a configuração de **produção**: sem seed e sem URL; a aplicação **falha ao subir** (`Failed to configure a DataSource`) se o ambiente não fornecer `SPRING_DATASOURCE_*`, em vez de usar um banco errado ou subir dados de exemplo em produção. O perfil `dev` traz o banco local e o seed.
6. Tomcat → `DispatcherServlet` → `HandlerMapping` (achar o método por método HTTP + caminho) → interceptors (`AuthInterceptor.preHandle`) → `HandlerAdapter` (converte `{id}`, o formulário em `DepositForm`, injeta o `CurrentUser`) → validação (`@Valid`) → controller → `ModelAndView`/view (Thymeleaf) → resposta. **303** manda o navegador trocar para **GET**, então F5 não repete o depósito (Post/Redirect/Get); `redirect:/x` devolve **302** por compatibilidade com HTTP/1.0 (o `RedirectView` só envia 303 se o status for definido), por isso `Pages.redirect` define 303.
7. O padrão de `@ManyToOne` é EAGER (JOINs não pedidos); com LAZY o Hibernate coloca um proxy e só vai ao banco quando você usa outro campo; busque explicitamente (`JOIN FETCH`, `@EntityGraph`, DTO). Contexto de persistência: entidades gerenciadas por uma transação, com **identidade** (mesmo id, mesmo objeto), retrato e **dirty checking** (no flush, o `UPDATE` das alteradas, sem `save`). `LazyInitializationException`: acessar um proxy **sem sessão** (fora da transação). O `open-in-view` mantém a sessão aberta até a renderização: esconde consultas, prende a conexão do pool e mascara N+1.
8. O nome do método é a consulta (`findBy` + caminho `owner.id` + `OrderBy...`), validada **na inicialização** (um nome errado impede a aplicação de subir). `@Query` (JPQL, parâmetros nomeados) para `JOIN`s, agregações, projeções ou nomes ilegíveis. `Slice`: **1** consulta (busca `size + 1`, só `hasNext`); `Page`: **2** (conteúdo + `count(*)`). Sem desempate (`id`), dois itens com o mesmo `created_at` podem trocar de lugar entre consultas: itens repetidos ou perdidos entre páginas.
9. O Spring cria um **proxy** que abre a transação (`JpaTransactionManager`: um `EntityManager` + conexão presos à thread), delega, e faz flush + commit, ou rollback. **Não desfaz:** exceção **verificada** (commit; use `rollbackFor`); **não abre:** auto-invocação `this.metodo()` (mova para outro bean) e método `readOnly = true` que tenta gravar (mudanças descartadas em silêncio; use transação de escrita). Também: bean criado com `new`, método `private`/classe `final`. `REQUIRES_NEW` suspende a transação atual e cria uma **independente**, que confirma sozinha (auditoria que sobrevive ao rollback da principal).
10. N+1: uma consulta para a lista + uma por item (ou por conta/usuário distinto) para a associação preguiçosa; medido: **47** consultas contra **1**. Detecta-se com `logging.level.org.hibernate.SQL`, `Statistics` (`getPrepareStatementCount`) em testes (um teste que fixa o número) e APM; corrige-se com `JOIN FETCH`, `@EntityGraph`, projeção em DTO (1 consulta) ou, como rede de segurança, `default_batch_fetch_size` (5 consultas). Validação: **borda** (Bean Validation: `@NotNull @Positive @Digits(17,2)` no `DepositForm`), **domínio** (`Amounts` e `Account.deposit/withdraw`, que nunca presumem entrada válida) e **banco** (`CHECK`, `NOT NULL`, `UNIQUE`). Pelo `Accept-Language: en-US`, `25,90` virou **2590** (a vírgula é separador de milhar no padrão americano; depósito aceito com 303): por isso `locale-resolver: fixed` (pt-BR). Logs: níveis, `{}`, `requestId` (MDC) e **nenhum** e-mail/segredo; erro de negócio é `WARN` sem stack trace. Health: com o banco parado o indicador `db` fica `DOWN` e o geral devolve 503; o **liveness** não depende do banco (reiniciar não o conserta) e, por padrão, o `db` não está no grupo readiness.
</details>

## Parte 2 — Entrega prática (tag `payflow-J5`)
O repositório `payflow` vira um **monólito Spring Boot 4.1.2** com cadastro, contas e depósitos **persistidos via Spring Data JPA** (Hibernate 7.4), painel web em **Spring MVC** com os mesmos templates da J4, serviços transacionais, validação, logs com `requestId`, Actuator e **87 testes** contra PostgreSQL real.

Estrutura esperada (o que muda em relação à J4):
```
payflow/
├── pom.xml                                (parent spring-boot-starter-parent 4.1.2; sem versões nas dependências)
├── scripts/                               db-up.sh (J3), run-dev.sh (J5.05)
├── docs/spring-lab/                       DI à mão (MiniDi*, CreationOrder), notas e saídas de cada aula
├── src/main/java/com/payflow/
│   ├── PayflowApplication.java            @SpringBootApplication + @ConfigurationPropertiesScan
│   ├── config/                            ClockConfig, PayflowProperties, PayflowInfoContributor
│   ├── domain/                            User, Account, LedgerEntry, Transfer (+ enums, Amounts, exceções)
│   ├── repository/                        UserRepository, AccountRepository, LedgerEntryRepository,
│   │                                      TransferRepository, TransferSummary
│   ├── service/                           AccountService, TransferService, TransferHistoryService
│   └── web/                               Sessions, SessionCookies, CurrentUser, AuthInterceptor, WebConfig, Pages,
│                                          LoginController, AccountPagesController, StatementApiController,
│                                          DepositForm, WebErrors, RequestIdFilter
├── src/main/resources/
│   ├── application.yml, application-dev.yml, application-test.yml, messages.properties
│   ├── db/migration/V1..V4, db/seed/R__dev_seed.sql        (inalterados desde a J3)
│   └── templates/ (5, J4)  e  static/ (app.css, statement.js, J4)
└── src/test/java/com/payflow/             config/ jpa/ lab/ service/ support/ (ResetDatabase) web/   → 87 testes
```
Removidos em relação à J4: `com.payflow.db.*` (JDBC), `PayflowServer`, `Views`, `JsonWriter`, `WebApp`, `PayflowServerTest`, `Database`; e, dos laboratórios, `BeanTour`, `StartupBanner` e `PingController`.

Critérios de pronto:
- [ ] **DI e beans:** `docs/spring-lab/` com `MiniDi.java`, `MiniDiErrors.java` e `CreationOrder.java` rodando; `BeansLabTest` (8) verde; nenhum bean com campo mutável.
- [ ] **Spring Boot:** `pom.xml` com o parent 4.1.2 e **zero** `<version>` em dependências; `mvn spring-boot:run` sobe e o log mostra o Flyway validando 5 migrações; `mvn -DskipTests package` gera o JAR executável (que sobe com `java -jar`) **sem** o DevTools.
- [ ] **Autoconfiguração:** relatório `--debug` salvo e comentado; `AutoConfigLabTest` (3) verde; a classe `Database` da J3 removida.
- [ ] **Configuração:** perfis `dev` e `test`; **sem perfil, a aplicação não sobe** (sem URL); `PayflowProperties` (`record` validado com `@Validated`) ligado a `payflow.*`; metadados gerados com `annotationProcessorPaths`; as cinco execuções de precedência documentadas; nenhum segredo real no Git.
- [ ] **Spring MVC:** controllers, `AuthInterceptor` e `WebErrors` em lugar do servidor manual; templates e `static/` **intactos**; **303** no Post/Redirect/Get; 400/401/404/405/422 conforme as decisões 12 e 25; `X-Request-Id` na resposta.
- [ ] **JPA:** 4 entidades mapeadas (enums em `STRING`, `BigDecimal(19,2)`, `Instant`, `@Version`, `LedgerEntry` `@Immutable` com a hora do `Clock`); todos os `@ManyToOne` com `LAZY`; `ddl-auto: validate`; `open-in-view: false`.
- [ ] **Spring Data:** os 4 repositórios; extrato com `Slice` (1 consulta por página); `@Query` JPQL do saldo do livro-razão batendo com o saldo das 4 contas do seed.
- [ ] **Transações:** `AccountService` e `TransferService` com `@Transactional`; regras em `Account.deposit/withdraw`; `TransactionLabTest` (8) com os cenários A–H explicados; os **11 testes** da J3 portados (`TransferServiceTest` 6 + `PersistenceIntegrationTest` 5), sem `@Transactional` no teste.
- [ ] **N+1:** `TransferHistoryService` com **1** consulta, fixada por `TransferHistoryServiceTest`; `NPlusOneLabTest`/`NPlusOneBatchLabTest` com **47 / 1 / 1 / 1 / 5**.
- [ ] **Validação:** `DepositForm` com `@NotNull @Positive @Digits(17,2)` e `@NumberFormat`; `locale-resolver: fixed`; `1000000000000000000000` → **400**; `abc` com mensagem em português; `@Email`/`@NotBlank` na `User`.
- [ ] **Logging:** SLF4J, mensagens parametrizadas, `requestId` no MDC (`RequestIdFilter`), **nenhum** dado pessoal no log (`LoggingTest` 3 verde); `System.out` removido.
- [ ] **Operação:** Actuator com **só `health` e `info`** em produção (`env`, `beans`, `loggers`, `metrics`, `mappings`, `conditions`, `heapdump` = 404, verificado por `ActuatorTest`); endpoints extras só no `dev`; `build-info` no `/actuator/info`; 503 com o banco parado documentado.
- [ ] **Testes:** `mvn test` com **87 testes verdes** (JDK 25, banco `payflow_test`, nada de H2), todos os que usam o banco com `@ExtendWith(ResetDatabase.class)`.
- [ ] Commits em Conventional Commits (inglês), PR mesclado e tag enviada:
```bash
git tag payflow-J5
git push origin payflow-J5
```

<details><summary>Referência: o <code>pom.xml</code> final</summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.2</version>
    <relativePath/>
  </parent>

  <groupId>com.payflow</groupId>
  <artifactId>payflow</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <name>payflow</name>

  <properties>
    <java.version>25</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-flyway</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-devtools</artifactId>
      <scope>runtime</scope>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessorPaths>
            <path>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-configuration-processor</artifactId>
              <version>${spring-boot.version}</version>
            </path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>build-info</goal>            <!-- gera META-INF/build-info.properties (nome, versão, hora do build) -->
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```
</details>

<details><summary>Referência: os arquivos de configuração finais</summary>

`src/main/resources/application.yml`
```yaml
spring:
  application:
    name: payflow
  jpa:
    open-in-view: false                    # sem sessão aberta na camada web (J5.08)
    hibernate:
      ddl-auto: validate                   # o Flyway cria o esquema; o Hibernate só CONFERE (decisão 17)
  web:
    locale: pt-BR                          # Content-Language e formatação padrão
    locale-resolver: fixed                 # sempre pt-BR, ignorando o idioma do navegador (vírgula decimal)
  mvc:
    static-path-pattern: /static/**        # os templates referenciam /static/app.css e /static/statement.js
  flyway:
    locations: classpath:db/migration      # produção: só o esquema, nunca o seed

management:
  endpoints:
    web:
      exposure:
        include: health,info              # só o necessário (os outros mostram detalhes internos)
  info:
    env:
      enabled: true                       # expõe as propriedades "info.*" em /actuator/info

info:
  app:
    name: PayFlow
    description: Carteira digital simplificada (projeto de estudo)

logging:
  level:
    root: INFO
    com.payflow: INFO
  pattern:
    level: "%5p [%X{requestId:-}]"          # o nível e o requestId (vazio fora de uma requisição)

payflow:
  statement:
    page-size: 5
    max-page: 10000
  session:
    cookie-name: PAYFLOW_SESSION
```
`src/main/resources/application-dev.yml`
```yaml
# Perfil de desenvolvimento: banco local do Docker (J3.03) e dados de exemplo (seed).
# Estes valores são só para a máquina do desenvolvedor; produção usa variáveis de ambiente.
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payflow
    username: payflow
    password: payflow
  flyway:
    locations: classpath:db/migration,classpath:db/seed
  thymeleaf:
    cache: false                           # editar o template e recarregar a página, sem reiniciar

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers,mappings,conditions,flyway   # só em desenvolvimento
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.payflow: DEBUG                     # no desenvolvimento, os logs do PayFlow em detalhe
    org.hibernate.SQL: DEBUG               # e o SQL que o Hibernate gera (J5.07)
```
`src/main/resources/application-test.yml`
```yaml
# Perfil de testes: banco separado (payflow_test, J3.12) e seed.
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payflow_test
    username: payflow
    password: payflow
  flyway:
    locations: classpath:db/migration,classpath:db/seed
    clean-disabled: false
```
`src/main/resources/messages.properties`
```properties
deposit.amount.invalid=Informe um valor positivo com até 2 casas decimais, como 25,90.
typeMismatch.depositForm.amount=Informe um valor positivo com até 2 casas decimais, como 25,90.
```
</details>

<details><summary>Referência: os controllers finais (somam as mudanças das J5.06, J5.09, J5.10, J5.11, J5.13 e J5.14)</summary>

`LoginController.java`
```java
package com.payflow.web;

import com.payflow.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/** Login provisório (só e-mail) e logout. Termina na J8, com Spring Security. */
@Controller
public class LoginController {

    private final UserRepository users;
    private final Sessions sessions;
    private final SessionCookies cookies;

    public LoginController(UserRepository users, Sessions sessions, SessionCookies cookies) {
        this.users = users;
        this.sessions = sessions;
        this.cookies = cookies;
    }

    @GetMapping("/")
    public ModelAndView root() {
        return Pages.redirect("/accounts");
    }

    @GetMapping("/login")
    public ModelAndView form() {
        return Pages.page("login", HttpStatus.OK);
    }

    @PostMapping("/login")
    public ModelAndView login(@RequestParam(defaultValue = "") String email, HttpServletResponse response) {
        String normalized = email.trim().toLowerCase();
        return users.findByEmail(normalized)
                .map(user -> {
                    cookies.write(response, sessions.create(new CurrentUser(user.getId(), user.getEmail(), user.getFullName())));
                    return Pages.redirect("/accounts");
                })
                .orElseGet(() -> Pages.page("login", HttpStatus.UNAUTHORIZED,
                        "error", "Não encontramos esse e-mail.", "email", normalized));
    }

    @PostMapping("/logout")
    public ModelAndView logout(HttpServletRequest request, HttpServletResponse response) {
        cookies.read(request).ifPresent(sessions::destroy);
        cookies.clear(response);
        return Pages.redirect("/login");
    }
}
```
`AccountPagesController.java`
```java
package com.payflow.web;

import com.payflow.config.PayflowProperties;
import com.payflow.domain.Account;
import com.payflow.domain.AccountUnavailableException;
import com.payflow.service.AccountService;
import java.math.BigDecimal;
import com.payflow.domain.EntryType;
import com.payflow.domain.LedgerEntry;
import com.payflow.repository.LedgerEntryRepository;
import com.payflow.repository.AccountRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/** Páginas do painel: contas, extrato e depósito. O usuário da sessão chega pelo AuthInterceptor. */
@Controller
public class AccountPagesController {

    private static final Logger log = LoggerFactory.getLogger(AccountPagesController.class);

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AccountRepository accountRepository;       // JPA: contas (J5.09)
    private final LedgerEntryRepository ledger;               // JPA: extrato paginado (J5.10)
    private final AccountService accountService;              // regras de negócio e transação (J5.11)
    private final PayflowProperties properties;
    private final MessageSource messages;                       // textos do messages.properties (J5.13)

    public AccountPagesController(AccountRepository accountRepository, LedgerEntryRepository ledger,
                                  AccountService accountService, PayflowProperties properties,
                                  MessageSource messages) {
        this.accountRepository = accountRepository;
        this.ledger = ledger;
        this.accountService = accountService;
        this.properties = properties;
        this.messages = messages;
    }

    /** O que o template precisa para uma linha do extrato, já formatado para quem lê. */
    public record EntryView(String label, boolean credit, BigDecimal amount, String when) { }

    @GetMapping("/accounts")
    public ModelAndView list(@RequestAttribute(AuthInterceptor.USER) CurrentUser user) {
        return Pages.page("accounts", HttpStatus.OK, "user", user, "accounts", accountRepository.findByOwnerIdOrderByCreatedAtAscIdAsc(user.id()));
    }

    @GetMapping("/accounts/{id}")
    public ModelAndView statement(@RequestAttribute(AuthInterceptor.USER) CurrentUser user,
                                  @PathVariable UUID id,
                                  @RequestParam(defaultValue = "0") int page) {
        return statementPage(user, id, page, null, HttpStatus.OK);
    }

    @PostMapping("/accounts/{id}/deposit")
    public ModelAndView deposit(@RequestAttribute(AuthInterceptor.USER) CurrentUser user,
                                @PathVariable UUID id,
                                @Valid DepositForm form, BindingResult errors) {
        if (ownedAccount(user, id).isEmpty()) {
            return statementPage(user, id, 0, null, HttpStatus.OK);     // vira a página 404 abaixo
        }
        if (errors.hasErrors()) {                                        // entrada inválida: 400, mensagem do messages.properties
            String message = messages.getMessage(errors.getFieldError("amount"), LocaleContextHolder.getLocale());
            return statementPage(user, id, 0, message, HttpStatus.BAD_REQUEST);
        }
        try {
            accountService.deposit(id, form.amount());
            return Pages.redirect("/accounts/" + id);                    // Post/Redirect/Get: F5 não repete o depósito
        } catch (AccountUnavailableException e) {
            log.warn("Deposit refused: account={} reason={}", id, e.getMessage());     // erro de NEGÓCIO: WARN, sem stack trace
            return statementPage(user, id, 0, "Esta conta não aceita depósitos agora.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private ModelAndView statementPage(CurrentUser user, UUID id, int requestedPage, String formError, HttpStatus status) {
        Optional<Account> account = ownedAccount(user, id);
        if (account.isEmpty()) {
            return Pages.page("error", HttpStatus.NOT_FOUND, "user", user,
                    "title", "Conta não encontrada", "message", "Não encontramos essa conta.");
        }
        int size = properties.statement().pageSize();
        int page = Math.max(0, Math.min(requestedPage, properties.statement().maxPage()));
        Slice<LedgerEntry> slice = ledger.findByAccountIdOrderByCreatedAtDescIdDesc(id, PageRequest.of(page, size));
        List<EntryView> entries = slice.getContent().stream().map(this::view).toList();
        boolean hasMore = slice.hasNext();
        ModelAndView mav = Pages.page("statement", status, "user", user, "account", account.get(),
                "entries", entries, "page", page, "hasMore", hasMore);
        if (formError != null) {
            mav.addObject("formError", formError);
        }
        return mav;
    }

    /** Só devolve a conta se ela for do usuário da sessão; de outro dono = "não existe" (o IDOR é tema da J8). */
    private Optional<Account> ownedAccount(CurrentUser user, UUID id) {
        return accountRepository.findById(id).filter(a -> a.getOwner().getId().equals(user.id()));
    }

    private EntryView view(LedgerEntry e) {
        boolean credit = e.getType() == EntryType.CREDIT;
        String label = e.getTransfer() == null ? "Depósito"
                : credit ? "Transferência recebida" : "Transferência enviada";
        return new EntryView(label, credit, e.getAmount(), DATE_TIME.format(e.getCreatedAt().atZone(ZONE)));
    }
}
```
`StatementApiController.java`
```java
package com.payflow.web;

import com.payflow.config.PayflowProperties;
import com.payflow.domain.LedgerEntry;
import com.payflow.repository.LedgerEntryRepository;
import com.payflow.repository.AccountRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** O endpoint JSON que o statement.js consome (contrato estável: dinheiro em string, datas ISO-8601 UTC). */
@RestController
public class StatementApiController {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledger;
    private final PayflowProperties properties;

    public StatementApiController(AccountRepository accountRepository, LedgerEntryRepository ledger,
                                  PayflowProperties properties) {
        this.accountRepository = accountRepository;
        this.ledger = ledger;
        this.properties = properties;
    }

    public record EntryJson(String id, String type, String transferId, String amount, String createdAt) { }

    public record StatementJson(int page, boolean hasMore, List<EntryJson> entries) { }

    @GetMapping("/api/accounts/{id}/statement")
    public ResponseEntity<?> statement(@RequestAttribute(AuthInterceptor.USER) CurrentUser user,
                                       @PathVariable UUID id,
                                       @RequestParam(defaultValue = "0") int page) {
        boolean owned = accountRepository.findById(id).filter(a -> a.getOwner().getId().equals(user.id())).isPresent();
        if (!owned) {
            return ResponseEntity.status(404).body(Map.of("error", "account not found"));
        }
        int size = properties.statement().pageSize();
        int safePage = Math.max(0, Math.min(page, properties.statement().maxPage()));
        Slice<LedgerEntry> slice = ledger.findByAccountIdOrderByCreatedAtDescIdDesc(id, PageRequest.of(safePage, size));
        List<EntryJson> rows = slice.getContent().stream().map(this::json).toList();
        return ResponseEntity.ok(new StatementJson(safePage, slice.hasNext(), rows));
    }

    private EntryJson json(LedgerEntry e) {
        return new EntryJson(e.getId().toString(), e.getType().name(),
                e.getTransfer() == null ? null : e.getTransfer().getId().toString(),
                e.getAmount().toPlainString(),                    // dinheiro como string (decisão 11)
                e.getCreatedAt().toString());
    }
}
```
</details>

O restante do código (e as saídas esperadas) está nas aulas: [J5.01](J5.01-inversao-de-controle-e-injecao-de-dependencia.md) a [J5.15](J5.15-devtools-e-actuator-basico.md). Use este material para **conferir** a sua versão depois de tentar.

## Parte 3 — Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Explico IoC e DI, e escrevo beans por estereótipo e `@Bean`, com injeção por construtor | ☐ | ☐ | ☐ |
| Leio o relatório de condições e as mensagens de bean ausente, ambíguo e circular | ☐ | ☐ | ☐ |
| Configuro por `application.yml`, perfis, variáveis de ambiente e `@ConfigurationProperties` validado | ☐ | ☐ | ☐ |
| Escrevo controllers Spring MVC com interceptor e tratamento de erros em `@ControllerAdvice` | ☐ | ☐ | ☐ |
| Mapeio entidades JPA (enums, dinheiro, datas, `@Version`) e valido o esquema com `ddl-auto: validate` | ☐ | ☐ | ☐ |
| Explico lazy × eager, o contexto de persistência, dirty checking e a `LazyInitializationException` | ☐ | ☐ | ☐ |
| Crio repositórios Spring Data (consultas derivadas, `@Query`, `Slice`/`Page`) e leio o SQL gerado | ☐ | ☐ | ☐ |
| Uso `@Transactional` (rollback, `readOnly`, propagação) e reconheço a auto-invocação | ☐ | ☐ | ☐ |
| Detecto e corrijo N+1 medindo as consultas (`JOIN FETCH`, `@EntityGraph`, DTO) | ☐ | ☐ | ☐ |
| Valido a entrada com Bean Validation e protejo o domínio e o banco (três camadas) | ☐ | ☐ | ☐ |
| Escrevo logs com SLF4J (níveis, `{}`, MDC) sem vazar dados pessoais | ☐ | ☐ | ☐ |
| Uso o Actuator (health, info, loggers) e decido o que expor em produção | ☐ | ☐ | ☐ |

**Regra:** se ficou com mais de duas linhas em 0, repita as aulas correspondentes antes de seguir para o [J6](../J6-apis-rest/README.md). Atualize `00-guia/progresso.md` e o `cronograma.md`.

## Próximo
[J6 — APIs REST](../J6-apis-rest/README.md)
