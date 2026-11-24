# J6.CP — Checkpoint do módulo J6

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J6.01 a J6.09

## Parte 1 — Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Diferencie método **seguro** e **idempotente**; diga qual status para "criou", "saldo insuficiente", "conta que não é sua em `GET /accounts/{id}`" e "colisão de versão".
2. Por que a entidade JPA não vai no JSON? Cite quatro sintomas e o risco na entrada (*mass assignment*).
3. Por que o `ApiMapper` usa `unmappedTargetPolicy = ERROR` e por que o `pom.xml` precisa de **dois** processadores de anotação?
4. Onde fica a fronteira da transação na transferência e por que a conta alheia dá `422` no corpo do `POST /transfers`?
5. Qual a estrutura de um Problem Details (RFC 9457) e por que o `type` é o que o cliente deve testar? Por que o `WebErrors` do painel teve de ser alterado?
6. `Slice` × `Page`: qual a decisão do PayFlow e por quê? Por que `to` é exclusivo e por que ordenar com desempate pela chave?
7. Por que `(:from is null or ...)` falha no PostgreSQL e como a `Specification` resolve? Qual a pegadinha de `Specification.and(null)`?
8. Como funciona a idempotência do `POST /transfers`? O que acontece com 8 requisições simultâneas com a mesma chave e por que existe o `TransferSubmitter`?
9. Qual a armadilha do `usePathSegment(1)` sem predicado e a do springdoc com `version = "1"`? Por que desligar a documentação em produção?
10. Por que o preflight do CORS recebia `401`? Por que `Location` precisa ser exposto, e por que uma chamada externa **sem timeout** é perigosa (e como o PayFlow traduz as falhas dela)?

<details><summary>Gabarito</summary>

1. Seguro não altera estado (`GET`); idempotente repetido dá o mesmo estado (`GET`, `PUT`, `DELETE`). `201`+`Location`, `422`, `404`, `409`.
2. Vazamento (`passwordHash`), `LazyInitializationException`, recursão infinita, acoplamento ao banco; na entrada o cliente definiria campos que só o servidor deveria definir. DTOs mínimos evitam.
3. Para falhar a compilação se um campo do DTO ficar sem origem; no JDK 25 o `javac` não detecta processadores só pelo classpath, então o do Boot e o do MapStruct são declarados em `annotationProcessorPaths`.
4. No `TransferService.submit` (`@Transactional`), nunca no controller. No corpo, `422` uniforme evita enumerar contas; no caminho, `404`.
5. `type`, `title`, `status`, `detail`, `instance` + extensões; `type` é estável, texto muda. Rotas sem controller não passam por advices com seletor; o `WebErrors` delega ao `ApiProblems` para `/api/`.
6. `Slice` (sem `count`, com `hasNext`). `[from, to)` encadeia janelas sem perder/duplicar; desempate pela PK dá ordem total.
7. O driver não infere o tipo de `:from is null`; `Specification` só inclui o predicado se o filtro veio. `and(null)`/`allOf(null)` lançam `IllegalArgumentException`: use `Specification.unrestricted()`.
8. Chave `userId:chave`; repetição com mesmo corpo → `200` com a mesma transferência; corpo diferente → `422`. Sem o submitter, 7 de 8 viram `500` (violação do `UNIQUE` no commit); com ele, 1×`201` e 7×`200`.
9. O versionamento global derruba páginas com 500; o springdoc publica `/api/1/...` em vez de `/api/v1/...`. Documentação revela a superfície da API.
10. O `AuthInterceptor` cobria `/api/**` e o preflight não leva cookie. Sem `exposedHeaders` o JS não lê `Location`. Sem timeout a thread fica presa; falhas viram `504`/`502`/`422` sem repassar o texto externo.
</details>

## Parte 2 — Entrega prática (tag `payflow-J6`)
O `payflow` ganha uma **API REST v1** documentada, sobre os serviços da J5.

```
src/main/java/com/payflow/
├── client/        ExchangeRates, FrankfurterExchangeRates, ExchangeRate, ExchangeRateUnavailableException, UnsupportedCurrencyException
├── config/        OpenApiConfig (+ Exchange e Cors em PayflowProperties)
├── domain/        NotFoundException, InvalidTransferException, IdempotencyKeyReuseException (+ Amounts.normalize)
├── repository/    EntrySpecifications
├── service/       TransferOutcome, TransferSubmitter (+ TransferService.submit / findVisibleTo)
└── web/ (WebConfig com CORS e versionamento; AuthInterceptor/WebErrors delegam a api/)
    └── api/       AccountsController, TransfersController, ApiMapper (MapStruct), ApiErrors, ApiProblems,
                   BadRequestException, IdempotencyKeys, dto/ (Account/Entry/Transfer/ConvertedBalance Response,
                   DepositRequest, CreateTransferRequest, PageResponse, ProblemBody)
scripts/api-smoke.sh   docs/api-design.md   docs/api-review.md   docs/openapi.json
```
Novas dependências: `spring-boot-starter-restclient`, `mapstruct` 1.6.3 (+ processador), `springdoc-openapi-starter-webmvc-ui` 3.2.0, `swagger-parser` 2.1.48 (teste).

Critérios de pronto:
- [ ] Contrato da v1 (J6.01) implementado; entidade nunca no JSON; dinheiro em string com 2 casas.
- [ ] `ApiMapper` MapStruct com `unmappedTargetPolicy = ERROR`; dois processadores.
- [ ] `POST /transfers` atômico e idempotente (corrida: 1×201 + 7×200); 6 pedidos simultâneos: pelo menos um `201`, os outros `409` e **saldo = 830 − 100 × (nº de `201`)** (com o *pool* aquecido sai 1×201 + 5×409; com o perfil de teste, 2×201 + 4×409: veja a J6.03).
- [ ] Todo erro da API é `application/problem+json`; páginas continuam HTML.
- [ ] `GET .../entries` paginado com `Slice`, filtros por `Specification`, 400 para parâmetros inválidos.
- [ ] Versionamento `/api/v1` por predicado; legado do painel intacto.
- [ ] OpenAPI válida (teste), UI só em `dev`/`test`.
- [ ] CORS para origens configuradas (testado também em navegador); timeouts no `RestClient`.
- [ ] **43 testes novos** (`AccountsApiTest` 9, `TransfersApiTest` 9, `ErrorsApiTest` 6, `VersioningApiTest` 3, `OpenApiTest` 1 + `OpenApiDisabledTest` 1, `ExchangeApiTest` 5, `CorsApiTest` 4, `RestClientTimeoutLabTest` 3, `LedgerInvariantTest` 1, `OptionalFilterLabTest` 1; o laboratório de entidade no JSON da J6.02 é descartável e **não** entra no projeto) e `scripts/api-smoke.sh` com 15 `ok`.
- [ ] Commits em Conventional Commits, PR mesclado e tag:
```bash
git tag payflow-J6
git push origin payflow-J6
```


### Referência: classes que as aulas só citam pelo nome
> Acrescentado na **validação final** (2026-11-24): as aulas descrevem estas classes (campos na tabela da J6.02, exceções na tabela da J6.03 e no `ApiErrors`), mas não mostravam o código. As versões abaixo são as que **compilaram e passaram** nos 130 testes (87 da J5 + 43 da J6).

<details><summary>DTOs, exceções e cotação (9 arquivos)</summary>

`src/main/java/com/payflow/web/api/dto/EntryResponse.java`
```java
package com.payflow.web.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EntryResponse(
        UUID id,
        String type,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        UUID transferId,
        Instant createdAt) { }
```
`src/main/java/com/payflow/web/api/dto/TransferResponse.java`
```java
package com.payflow.web.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID targetAccountId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        String status,
        Instant createdAt) { }
```
`src/main/java/com/payflow/web/api/dto/ConvertedBalanceResponse.java`
```java
package com.payflow.web.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConvertedBalanceResponse(
        String baseCurrency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal convertedBalance,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal rate,
        LocalDate rateDate) { }
```
`src/main/java/com/payflow/client/ExchangeRate.java`
```java
package com.payflow.client;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma cotação: 1 base = rate quote, na data informada pela API. */
public record ExchangeRate(String base, String quote, BigDecimal rate, LocalDate date) { }
```
`src/main/java/com/payflow/client/ExchangeRateUnavailableException.java`
```java
package com.payflow.client;

/** A API de câmbio falhou, demorou demais (timeout = true) ou respondeu algo inválido. */
public class ExchangeRateUnavailableException extends RuntimeException {

    private final boolean timeout;

    public ExchangeRateUnavailableException(String message, boolean timeout, Throwable cause) {
        super(message, cause);
        this.timeout = timeout;
    }

    public boolean isTimeout() { return timeout; }
}
```
`src/main/java/com/payflow/client/UnsupportedCurrencyException.java`
```java
package com.payflow.client;

/** A API de câmbio não conhece a moeda pedida. */
public class UnsupportedCurrencyException extends RuntimeException {
    public UnsupportedCurrencyException(String currency) {
        super("Unsupported currency: " + currency);
    }
}
```
`src/main/java/com/payflow/domain/InvalidTransferException.java`
```java
package com.payflow.domain;

public class InvalidTransferException extends RuntimeException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
```
`src/main/java/com/payflow/domain/IdempotencyKeyReuseException.java`
```java
package com.payflow.domain;

public class IdempotencyKeyReuseException extends RuntimeException {
    public IdempotencyKeyReuseException(String message) {
        super(message);
    }
}
```
`src/main/java/com/payflow/web/api/BadRequestException.java`
```java
package com.payflow.web.api;

/** Parâmetro inválido que o Bean Validation não cobre (página, tamanho, direção, moeda): vira 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
```
</details>

<details><summary><code>WebErrors</code> e <code>AuthInterceptor</code> delegando à API (J6.04) e o <code>AccountsController</code> final (J6.01–J6.08)</summary>

`src/main/java/com/payflow/web/WebErrors.java`
```java
package com.payflow.web;

import com.payflow.web.api.ApiProblems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Transforma exceções em páginas de erro (template "error") com o status certo. */
@ControllerAdvice
public class WebErrors {

    private static final Logger log = LoggerFactory.getLogger(WebErrors.class);   // o Logback chega na J5.14

    private final ApiProblems problems;

    public WebErrors(ApiProblems problems) {
        this.problems = problems;
    }

    /** Sob /api/ não há controller (rota inexistente): a resposta é Problem Details, escrita pelo ApiProblems (J6.04). */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ModelAndView notFound(Exception e, HttpServletRequest request, HttpServletResponse response) {
        if (ApiProblems.isApi(request)) {
            problems.render(e, request, response);
            return null;                                             // resposta já escrita
        }
        return notFound();
    }

    private ModelAndView notFound() {
        return Pages.page("error", HttpStatus.NOT_FOUND,
                "title", "Página não encontrada", "message", "O endereço pedido não existe.");
    }

    /** Um parâmetro que não converte: {id} que não é UUID = recurso inexistente (404); ?page=abc = pedido inválido (400). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView badArgument(MethodArgumentTypeMismatchException e) {
        if ("id".equals(e.getName())) {
            return notFound();
        }
        return Pages.page("error", HttpStatus.BAD_REQUEST,
                "title", "Pedido inválido", "message", "O parâmetro '" + e.getName() + "' não é válido.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ModelAndView methodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request,
                                         HttpServletResponse response) {
        if (ApiProblems.isApi(request)) {
            problems.render(e, request, response);
            return null;
        }
        if (e.getSupportedHttpMethods() != null) {
            response.setHeader(HttpHeaders.ALLOW, String.join(", ",
                    e.getSupportedHttpMethods().stream().map(Object::toString).toList()));
        }
        return Pages.page("error", HttpStatus.METHOD_NOT_ALLOWED,
                "title", "Método não permitido", "message", "Esta página não aceita " + e.getMethod() + ".");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception e) {
        log.error("Unexpected error", e);                                  // o detalhe vai para o log, não para a tela
        return Pages.page("error", HttpStatus.INTERNAL_SERVER_ERROR,
                "title", "Algo deu errado", "message", "Não conseguimos concluir a operação. Tente de novo em instantes.");
    }
}
```
`src/main/java/com/payflow/web/AuthInterceptor.java`
```java
package com.payflow.web;

import com.payflow.web.api.ApiProblems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Exige sessão nas rotas protegidas. Com sessão, guarda o usuário no atributo "user" da requisição.
 * Sem sessão: páginas redirecionam para /login (303) e a API responde 401 em JSON.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER = "user";

    private final SessionCookies cookies;
    private final Sessions sessions;
    private final ApiProblems problems;

    public AuthInterceptor(SessionCookies cookies, Sessions sessions, ApiProblems problems) {
        this.cookies = cookies;
        this.sessions = sessions;
        this.problems = problems;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;                                          // o preflight (OPTIONS) não leva cookie: o CORS responde (J6.08)
        }
        Optional<CurrentUser> user = cookies.read(request).flatMap(sessions::userOf);
        if (user.isPresent()) {
            request.setAttribute(USER, user.get());
            return true;
        }
        if (ApiProblems.isApi(request)) {
            problems.write(response, HttpStatus.UNAUTHORIZED, "not-authenticated", "Not authenticated",
                    "Log in to use this API.");
        } else {
            response.setStatus(HttpStatus.SEE_OTHER.value());
            response.setHeader(HttpHeaders.LOCATION, "/login");
        }
        return false;                                         // false = a requisição NÃO chega ao controller
    }
}
```
`src/main/java/com/payflow/web/api/AccountsController.java`
```java
package com.payflow.web.api;

import com.payflow.client.ExchangeRate;
import com.payflow.client.ExchangeRates;
import com.payflow.config.PayflowProperties;
import com.payflow.domain.Account;
import com.payflow.domain.EntryType;
import com.payflow.domain.LedgerEntry;
import com.payflow.domain.NotFoundException;
import com.payflow.repository.AccountRepository;
import com.payflow.repository.EntrySpecifications;
import com.payflow.repository.LedgerEntryRepository;
import com.payflow.service.AccountService;
import com.payflow.web.AuthInterceptor;
import com.payflow.web.CurrentUser;
import com.payflow.web.api.dto.AccountResponse;
import com.payflow.web.api.dto.ConvertedBalanceResponse;
import com.payflow.web.api.dto.DepositRequest;
import com.payflow.web.api.dto.EntryResponse;
import com.payflow.web.api.dto.PageResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/accounts", version = "v1", produces = "application/json")
public class AccountsController {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    private final AccountService accountService;
    private final ApiMapper mapper;
    private final PayflowProperties properties;
    private final ExchangeRates exchangeRates;

    public AccountsController(AccountRepository accounts, LedgerEntryRepository ledger, AccountService accountService,
                              ApiMapper mapper, PayflowProperties properties, ExchangeRates exchangeRates) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.accountService = accountService;
        this.mapper = mapper;
        this.properties = properties;
        this.exchangeRates = exchangeRates;
    }

    @GetMapping
    public List<AccountResponse> list(@RequestAttribute(AuthInterceptor.USER) CurrentUser user) {
        return accounts.findByOwnerIdOrderByCreatedAtAscIdAsc(user.id()).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@RequestAttribute(AuthInterceptor.USER) CurrentUser user, @PathVariable UUID id) {
        return mapper.toResponse(owned(user, id));
    }

    @PostMapping("/{id}/deposits")
    public ResponseEntity<EntryResponse> deposit(@RequestAttribute(AuthInterceptor.USER) CurrentUser user,
                                                 @PathVariable UUID id, @Valid @RequestBody DepositRequest request) {
        owned(user, id);
        LedgerEntry entry = accountService.deposit(id, request.amount());
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + id + "/entries/" + entry.getId()))
                .body(mapper.toResponse(entry));
    }

    @GetMapping("/{id}/entries")
    public PageResponse<EntryResponse> entries(@RequestAttribute(AuthInterceptor.USER) CurrentUser user,
                                               @PathVariable UUID id,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
                                               @RequestParam(required = false) EntryType type,
                                               @RequestParam(required = false) Instant from,
                                               @RequestParam(required = false) Instant to,
                                               @RequestParam(defaultValue = "desc") String direction) {
        owned(user, id);
        if (page < 0 || page > properties.statement().maxPage()) {
            throw new BadRequestException("page must be between 0 and " + properties.statement().maxPage());
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_SIZE);
        }
        Sort.Direction dir = switch (direction.toLowerCase()) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BadRequestException("direction must be asc or desc");
        };
        var spec = Specification.allOf(EntrySpecifications.ofAccount(id), EntrySpecifications.ofType(type),
                EntrySpecifications.from(from), EntrySpecifications.before(to));
        Sort sort = Sort.by(dir, "createdAt").and(Sort.by(dir, "id"));          // desempate pela chave primária
        Slice<LedgerEntry> slice = ledger.findBy(spec, q -> q.slice(PageRequest.of(page, size, sort)));
        return PageResponse.of(slice.map(mapper::toResponse));
    }

    @GetMapping("/{id}/converted-balance")
    public ConvertedBalanceResponse convertedBalance(@RequestAttribute(AuthInterceptor.USER) CurrentUser user,
                                                     @PathVariable UUID id,
                                                     @RequestParam String currency) {
        Account account = owned(user, id);
        if (!currency.matches("[A-Z]{3}")) {
            throw new BadRequestException("currency must be a 3-letter ISO 4217 code, like USD");
        }
        ExchangeRate rate = exchangeRates.rate("BRL", currency);
        BigDecimal converted = account.getBalance().multiply(rate.rate()).setScale(2, RoundingMode.HALF_EVEN);
        return new ConvertedBalanceResponse("BRL", account.getBalance(), currency, converted, rate.rate(), rate.date());
    }

    private Account owned(CurrentUser user, UUID id) {
        return accounts.findById(id).filter(a -> a.getOwner().getId().equals(user.id()))
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
    }
}
```
</details>

### Referência: os testes da J6
As aulas mostram partes destes testes; as classes **completas** (as que rodaram) estão aqui. Pacote `com.payflow.api` (o `V2Lab` fica **fora** de `com.payflow` de propósito: um `@RestController` de teste dentro do pacote da aplicação é achado pelo *component scan* e registrado em **todos** os contextos; registrado duas vezes, o contexto não sobe com `There is already '...V2Lab' bean method`).

<details><summary>9 classes de teste</summary>

`src/test/java/com/payflow/api/AccountsApiTest.java`
```java
package com.payflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.payflow.support.ApiClient;
import com.payflow.support.ResetDatabase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(ResetDatabase.class)                     // banco do zero a cada teste (decisão 40)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccountsApiTest {

    static final String ANA_1 = "20000000-0000-0000-0000-000000000001";
    static final String BRUNO = "20000000-0000-0000-0000-000000000002";

    @LocalServerPort int port;
    ApiClient ana;

    @BeforeEach
    void login() throws Exception {
        ana = new ApiClient(port).login("ana@payflow.dev");
    }

    @Test
    void listsMyAccountsWithMoneyAsString() throws Exception {
        var r = ana.get("/api/v1/accounts");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        List<String> balances = JsonPath.read(r.body(), "$[*].balance");
        assertThat(balances).containsExactly("830.00", "0.00");
        assertThat((String) JsonPath.read(r.body(), "$[0].status")).isEqualTo("ACTIVE");
    }

    @Test
    void anotherUsersAccountIsNotFound() throws Exception {
        assertThat(ana.get("/api/v1/accounts/" + BRUNO).statusCode()).isEqualTo(404);
        assertThat(ana.get("/api/v1/accounts/" + ANA_1).statusCode()).isEqualTo(200);
    }

    @Test
    void withoutSessionTheApiIs401() throws Exception {
        assertThat(new ApiClient(port).get("/api/v1/accounts").statusCode()).isEqualTo(401);
    }

    static final String ANA_2 = "20000000-0000-0000-0000-000000000004";

    @Test
    void depositCreatesAnEntryAndReturns201WithLocation() throws Exception {
        var r = ana.post("/api/v1/accounts/" + ANA_2 + "/deposits", "{\"amount\":\"25.90\"}", Map.of());
        assertThat(r.statusCode()).isEqualTo(201);
        String location = r.headers().firstValue("Location").orElseThrow();
        assertThat(location).startsWith("/api/v1/accounts/" + ANA_2 + "/entries/");
        assertThat((String) JsonPath.read(ana.get("/api/v1/accounts/" + ANA_2).body(), "$.balance")).isEqualTo("25.90");
    }

    @Test
    void amountsAreNormalizedToTwoDecimals() throws Exception {
        var r = ana.post("/api/v1/accounts/" + ANA_2 + "/deposits", "{\"amount\":25.9}", Map.of());
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat((String) JsonPath.read(r.body(), "$.amount")).isEqualTo("25.90");
    }

    @Test
    void invalidBodiesAre400() throws Exception {
        for (String body : List.of("{\"amount\":\"-1\"}", "{\"amount\":\"1.234\"}", "{\"amount\":\"abc\"}", "{}", "not json")) {
            var r = ana.post("/api/v1/accounts/" + ANA_2 + "/deposits", body, Map.of());
            assertThat(r.statusCode()).as(body).isEqualTo(400);
        }
    }

    @Test
    void unknownFieldsInTheBodyAreIgnored() throws Exception {
        var r = ana.post("/api/v1/accounts/" + ANA_2 + "/deposits",
                "{\"amount\":\"5.00\",\"balance\":\"999999.00\",\"id\":\"x\"}", Map.of());
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat((String) JsonPath.read(ana.get("/api/v1/accounts/" + ANA_2).body(), "$.balance")).isEqualTo("5.00");
    }

    @Test
    void entriesArePagedFilteredAndOrdered() throws Exception {
        var r = ana.get("/api/v1/accounts/" + ANA_1 + "/entries?size=2");
        assertThat((Boolean) JsonPath.read(r.body(), "$.hasNext")).isTrue();
        assertThat((List<?>) JsonPath.read(r.body(), "$.items")).hasSize(2);
        assertThat((String) JsonPath.read(r.body(), "$.items[0].amount")).isEqualTo("20.00");   // o mais novo primeiro
        var p1 = ana.get("/api/v1/accounts/" + ANA_1 + "/entries?size=2&page=1");
        assertThat((Boolean) JsonPath.read(p1.body(), "$.hasNext")).isFalse();
        var credits = ana.get("/api/v1/accounts/" + ANA_1 + "/entries?type=CREDIT");
        assertThat((List<?>) JsonPath.read(credits.body(), "$.items")).hasSize(1);
        var range = ana.get("/api/v1/accounts/" + ANA_1 + "/entries?from=2026-01-10T00:00:00Z&to=2026-01-15T09:10:00Z");
        assertThat((List<?>) JsonPath.read(range.body(), "$.items")).hasSize(1);    // [from, to): o de 01-15 09:10 fica de fora
        var asc = ana.get("/api/v1/accounts/" + ANA_1 + "/entries?direction=asc");
        assertThat((String) JsonPath.read(asc.body(), "$.items[0].amount")).isEqualTo("1000.00");
    }

    @Test
    void invalidQueryParametersAre400() throws Exception {
        for (String q : List.of("size=0", "size=101", "page=-1", "type=XYZ", "direction=sideways", "from=ontem")) {
            var r = ana.get("/api/v1/accounts/" + ANA_1 + "/entries?" + q);
            assertThat(r.statusCode()).as(q).isEqualTo(400);
        }
    }

}
```
`src/test/java/com/payflow/api/TransfersApiTest.java`
```java
package com.payflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.payflow.support.ApiClient;
import com.payflow.support.ResetDatabase;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** API de transferências (J6.03, J6.04, J6.06): atomicidade, erros de negócio, idempotência e concorrência. */
@ExtendWith(ResetDatabase.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TransfersApiTest {

    static final String ANA_1 = "20000000-0000-0000-0000-000000000001";
    static final String BRUNO = "20000000-0000-0000-0000-000000000002";
    static final String CARLA_BLOCKED = "20000000-0000-0000-0000-000000000003";

    @LocalServerPort int port;
    ApiClient ana;

    @BeforeEach
    void login() throws Exception {
        ana = new ApiClient(port).login("ana@payflow.dev");
    }

    String body(String from, String to, String amount) {
        return "{\"sourceAccountId\":\"" + from + "\",\"targetAccountId\":\"" + to + "\",\"amount\":\"" + amount + "\"}";
    }

    HttpResponse<String> transfer(ApiClient who, String key, String from, String to, String amount) throws Exception {
        return who.post("/api/v1/transfers", body(from, to, amount), key == null ? Map.of() : Map.of("Idempotency-Key", key));
    }

    String balance(ApiClient who, String id) throws Exception {
        return JsonPath.read(who.get("/api/v1/accounts/" + id).body(), "$.balance");
    }

    @Test
    void transferMovesMoneyAndReturns201WithLocation() throws Exception {
        var r = transfer(ana, "k-1", ANA_1, BRUNO, "100.00");
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(balance(ana, ANA_1)).isEqualTo("730.00");
        var again = ana.get(r.headers().firstValue("Location").orElseThrow());
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(again.body(), "$.status")).isEqualTo("COMPLETED");
        var bruno = new ApiClient(port).login("bruno@payflow.dev");
        assertThat(balance(bruno, BRUNO)).isEqualTo("674.50");
        assertThat(bruno.get(r.headers().firstValue("Location").orElseThrow()).statusCode()).isEqualTo(200);   // destinatário também vê
        var carla = new ApiClient(port).login("carla@payflow.dev");
        assertThat(carla.get(r.headers().firstValue("Location").orElseThrow()).statusCode()).isEqualTo(404);  // terceiros não
    }

    @Test
    void businessErrorsAre422AndNothingChanges() throws Exception {
        var funds = transfer(ana, "k-4", ANA_1, BRUNO, "5000.00");
        assertThat(funds.statusCode()).isEqualTo(422);
        assertThat((String) JsonPath.read(funds.body(), "$.type")).isEqualTo("urn:payflow:problem:insufficient-funds");
        var same = transfer(ana, "k-5", ANA_1, ANA_1, "1.00");
        assertThat(same.statusCode()).isEqualTo(422);
        assertThat((String) JsonPath.read(same.body(), "$.type")).isEqualTo("urn:payflow:problem:invalid-transfer");
        var blocked = transfer(ana, "k-6", ANA_1, CARLA_BLOCKED, "1.00");
        assertThat(blocked.statusCode()).isEqualTo(422);
        assertThat((String) JsonPath.read(blocked.body(), "$.type")).isEqualTo("urn:payflow:problem:account-unavailable");
        var notMine = transfer(ana, "k-7", BRUNO, ANA_1, "1.00");                   // origem de outro usuário = "não existe"
        assertThat(notMine.statusCode()).isEqualTo(422);
        assertThat((String) JsonPath.read(notMine.body(), "$.type")).isEqualTo("urn:payflow:problem:account-unavailable");
        assertThat(balance(ana, ANA_1)).isEqualTo("830.00");                       // nada mudou
    }

    @Test
    void invalidBodyIs400WithTheFieldsThatFailed() throws Exception {
        var r = transfer(ana, "k-8", ANA_1, BRUNO, "-5");
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(r.body(), "$.type")).isEqualTo("urn:payflow:problem:validation-failed");
        assertThat((List<String>) JsonPath.read(r.body(), "$.errors[*].field")).containsExactly("amount");
    }

    @Test
    void missingOrInvalidIdempotencyKeyIs400() throws Exception {
        assertThat(transfer(ana, null, ANA_1, BRUNO, "1.00").statusCode()).isEqualTo(400);
        assertThat(transfer(ana, " ", ANA_1, BRUNO, "1.00").statusCode()).isEqualTo(400);
        assertThat(transfer(ana, "x".repeat(64), ANA_1, BRUNO, "1.00").statusCode()).isEqualTo(400);
        assertThat(transfer(ana, "x".repeat(63), ANA_1, BRUNO, "1.00").statusCode()).isEqualTo(201);
        assertThat(balance(ana, ANA_1)).isEqualTo("829.00");
    }

    @Test
    void sameKeyAndSameBodyReplaysWith200AndTheSameTransfer() throws Exception {
        var first = transfer(ana, "k-9", ANA_1, BRUNO, "10.00");
        var again = transfer(ana, "\"k-9\"", ANA_1, BRUNO, "10.00");               // com aspas: a mesma chave
        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(again.body(), "$.id")).isEqualTo(JsonPath.read(first.body(), "$.id"));
        assertThat(balance(ana, ANA_1)).isEqualTo("820.00");                       // debitou uma vez só
    }

    @Test
    void sameKeyWithADifferentBodyIs422() throws Exception {
        assertThat(transfer(ana, "k-10", ANA_1, BRUNO, "10.00").statusCode()).isEqualTo(201);
        var other = transfer(ana, "k-10", ANA_1, BRUNO, "11.00");
        assertThat(other.statusCode()).isEqualTo(422);
        assertThat((String) JsonPath.read(other.body(), "$.type")).isEqualTo("urn:payflow:problem:idempotency-key-reused");
    }

    @Test
    void keysAreScopedByUser() throws Exception {
        var bruno = new ApiClient(port).login("bruno@payflow.dev");
        assertThat(transfer(ana, "shared", ANA_1, BRUNO, "1.00").statusCode()).isEqualTo(201);
        assertThat(transfer(bruno, "shared", BRUNO, ANA_1, "2.00").statusCode()).isEqualTo(201);
    }

    /** Corrida pela mesma chave: 8 pedidos ao mesmo tempo = um 201 e sete 200, e o dinheiro sai uma vez. */
    @Test
    void concurrentRequestsWithTheSameKeyTransferOnlyOnce() throws Exception {
        List<Integer> statuses = race(8, i -> transfer(ana, "race", ANA_1, BRUNO, "10.00"));
        System.out.println("MESMA CHAVE, 8 simultâneas: " + statuses + "   saldo = " + balance(ana, ANA_1));
        assertThat(statuses).containsOnlyOnce(201).filteredOn(s -> s != 201).containsOnly(200);
        assertThat(balance(ana, ANA_1)).isEqualTo("820.00");
    }

    /**
     * Chaves diferentes, mesma conta: quem leu a mesma versão e chegou depois recebe 409 (@Version).
     * QUANTOS passam depende do tempo (pool aquecido: quase sempre 1); o que NUNCA muda é o invariante do saldo.
     */
    @Test
    void concurrentDifferentTransfersFromTheSameAccountNeverLoseMoney() throws Exception {
        List<ApiClient> clients = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            clients.add(new ApiClient(port).login("ana@payflow.dev"));        // cada thread com o seu cliente HTTP
        }
        List<Integer> statuses = race(6, i -> transfer(clients.get(i), "parallel-" + i, ANA_1, BRUNO, "100.00"));
        System.out.println("statuses: " + statuses + "  saldo da origem = " + balance(ana, ANA_1));
        assertThat(statuses).contains(201).filteredOn(s -> s != 201).containsOnly(409);
        int ok = (int) statuses.stream().filter(s -> s == 201).count();
        assertThat(new java.math.BigDecimal(balance(ana, ANA_1)))
                .isEqualByComparingTo(new java.math.BigDecimal("830.00").subtract(new java.math.BigDecimal(100 * ok)));
    }

    interface Call { HttpResponse<String> run(int i) throws Exception; }

    List<Integer> race(int n, Call call) throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(n)) {
            List<java.util.concurrent.Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int id = i;
                futures.add(pool.submit((Callable<Integer>) () -> { start.await(); return call.run(id).statusCode(); }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (var f : futures) {
                statuses.add(f.get());
            }
            return statuses;
        }
    }
}
```
`src/test/java/com/payflow/api/ErrorsApiTest.java`
```java
package com.payflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.payflow.support.ApiClient;
import com.payflow.support.ResetDatabase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** Todo erro da API é application/problem+json; as páginas continuam com erros em HTML (J6.04). */
@ExtendWith(ResetDatabase.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ErrorsApiTest {

    static final String ANA_1 = "20000000-0000-0000-0000-000000000001";

    @LocalServerPort int port;

    @Test
    void noSessionIs401ProblemDetails() throws Exception {
        var r = new ApiClient(port).get("/api/v1/accounts");
        assertThat(r.statusCode()).isEqualTo(401);
        assertThat(r.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
        assertThat((String) JsonPath.read(r.body(), "$.type")).isEqualTo("urn:payflow:problem:not-authenticated");
    }

    @Test
    void unknownRouteUnderApiIs404ProblemDetails() throws Exception {
        var ana = new ApiClient(port).login("ana@payflow.dev");
        var r = ana.get("/api/v1/nope");
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(r.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
    }

    @Test
    void wrongMethodIs405WithAllow() throws Exception {
        var ana = new ApiClient(port).login("ana@payflow.dev");
        var r = ana.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/accounts")).DELETE());
        assertThat(r.statusCode()).isEqualTo(405);
        assertThat(r.headers().firstValue("Allow").orElseThrow()).contains("GET");
        assertThat(r.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
    }

    @Test
    void wrongContentTypeIs415ProblemDetails() throws Exception {
        var ana = new ApiClient(port).login("ana@payflow.dev");
        var r = ana.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/accounts/" + ANA_1 + "/deposits"))
                .header("Content-Type", "text/plain;charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString("10.00")));
        assertThat(r.statusCode()).isEqualTo(415);
        assertThat(r.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
    }

    @Test
    void idThatIsNotAUuidIs404ProblemDetails() throws Exception {
        var ana = new ApiClient(port).login("ana@payflow.dev");
        var r = ana.get("/api/v1/accounts/not-a-uuid");
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat((String) JsonPath.read(r.body(), "$.type")).isEqualTo("urn:payflow:problem:not-found");
    }

    @Test
    void pagesStillRenderHtmlErrors() throws Exception {
        var client = HttpClient.newHttpClient();
        var r = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/nao-existe")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isIn(303, 404);                                  // sem sessão o painel manda para o login
        var ana = new ApiClient(port).login("ana@payflow.dev");
        var page = ana.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/accounts/not-a-uuid")));
        assertThat(page.statusCode()).isEqualTo(404);
        assertThat(page.headers().firstValue("Content-Type").orElseThrow()).startsWith("text/html");
    }
}
```
`src/test/java/com/payflow/api/VersioningApiTest.java`
```java
package com.payflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.support.ApiClient;
import com.payflow.support.ResetDatabase;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.accept.StandardApiVersionDeprecationHandler;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Versionamento pelo caminho (J6.06): v1 marcada obsoleta SÓ neste teste, uma v2 de mentira, v9 inexistente e o legado. */
@ExtendWith(ResetDatabase.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(VersioningApiTest.Lab.class)
class VersioningApiTest {

    @TestConfiguration
    static class Lab implements WebMvcConfigurer {

        @Override
        public void configureApiVersioning(ApiVersionConfigurer configurer) {
            var deprecation = new StandardApiVersionDeprecationHandler();
            deprecation.configureVersion("v1")
                    .setDeprecationDate(ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                    .setDeprecationLink(URI.create("https://docs.payflow.example/migrate-v2"), MediaType.TEXT_HTML)
                    .setSunsetDate(ZonedDateTime.of(2027, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC));
            configurer.setDeprecationHandler(deprecation);
        }

        @org.springframework.context.annotation.Bean
        lab.versioning.V2Lab v2Lab() {                  // fora de com.payflow: o component scan não o registra sozinho
            return new lab.versioning.V2Lab();
        }
    }

    @LocalServerPort int port;
    ApiClient ana;

    @BeforeEach
    void login() throws Exception {
        ana = new ApiClient(port).login("ana@payflow.dev");
    }

    @Test
    void v1AnswersWithDeprecationAndSunsetHeaders() throws Exception {
        var r = ana.get("/api/v1/accounts");
        System.out.println("V1: " + r.statusCode() + " deprecation=" + r.headers().allValues("Deprecation")
                + " link=" + r.headers().allValues("Link") + " sunset=" + r.headers().allValues("Sunset"));
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("Deprecation").orElseThrow()).isEqualTo("@1793491200");
        assertThat(r.headers().firstValue("Sunset").orElseThrow()).isEqualTo("Sat, 1 May 2027 00:00:00 GMT");
    }

    @Test
    void v2IsAnotherMappingAndUnknownVersionIs404() throws Exception {
        var v2 = ana.get("/api/v2/accounts");
        System.out.println("V2: " + v2.statusCode() + " " + v2.body() + " deprecation=" + v2.headers().allValues("Deprecation"));
        assertThat(v2.statusCode()).isEqualTo(200);
        assertThat(v2.body()).isEqualTo("{\"hello\":\"v2\"}");
        assertThat(v2.headers().firstValue("Deprecation")).isEmpty();
        var v9 = ana.get("/api/v9/accounts");
        System.out.println("V9: " + v9.statusCode() + " " + v9.headers().firstValue("Content-Type").orElse("") + " " + v9.body());
        assertThat(v9.statusCode()).isEqualTo(404);
    }

    @Test
    void legacyStatementOfThePanelHasNoVersion() throws Exception {
        var r = ana.get("/api/accounts/20000000-0000-0000-0000-000000000001/statement");
        System.out.println("Legado: " + r.statusCode());
        assertThat(r.statusCode()).isEqualTo(200);
    }
}
```
`src/test/java/lab/versioning/V2Lab.java`
```java
package lab.versioning;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Uma "v2" de mentira, só para o VersioningApiTest (registrada por @Bean no teste, não pelo component scan). */
@RestController
public class V2Lab {

    @GetMapping(path = "/api/v2/accounts", version = "v2")
    public Map<String, String> hello() {
        return Map.of("hello", "v2");
    }
}
```
`src/test/java/com/payflow/api/OpenApiDisabledTest.java`
```java
package com.payflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.support.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** Em produção (application.yml base) a documentação fica desligada: simulamos com as mesmas propriedades. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
@ActiveProfiles("test")
class OpenApiDisabledTest {

    @LocalServerPort int port;

    @Test
    void docsAreOffWhenDisabled() throws Exception {
        int docs = new ApiClient(port).get("/v3/api-docs").statusCode();
        int ui = new ApiClient(port).get("/swagger-ui/index.html").statusCode();
        System.out.println("DOCS OFF (springdoc.*.enabled=false): api-docs=" + docs + " ui=" + ui);
        assertThat(docs).isEqualTo(404);
        assertThat(ui).isEqualTo(404);
    }
}
```
`src/test/java/com/payflow/api/ExchangeApiTest.java`
```java
package com.payflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.payflow.support.ApiClient;
import com.payflow.support.ExchangeStub;
import com.payflow.support.ResetDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** /converted-balance contra uma API de câmbio FALSA (HTTP de verdade): sucesso, lentidão, falha, moeda inexistente (J6.08). */
@ExtendWith(ResetDatabase.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ExchangeApiTest {

    static final String ANA_1 = "20000000-0000-0000-0000-000000000001";
    static final ExchangeStub stub = newStub();

    static ExchangeStub newStub() {
        try {
            return new ExchangeStub();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void exchange(DynamicPropertyRegistry registry) {
        registry.add("payflow.exchange.base-url", stub::url);
        registry.add("payflow.exchange.connect-timeout", () -> "300ms");
        registry.add("payflow.exchange.read-timeout", () -> "500ms");
    }

    @AfterAll
    static void stop() {
        stub.close();
    }

    @LocalServerPort int port;
    ApiClient ana;

    @BeforeEach
    void login() throws Exception {
        stub.reset();
        ana = new ApiClient(port).login("ana@payflow.dev");
    }

    String path(String currency) {
        return "/api/v1/accounts/" + ANA_1 + "/converted-balance?currency=" + currency;
    }

    @Test
    void convertsTheBalance() throws Exception {
        var r = ana.get(path("USD"));
        System.out.println("ok        " + r.statusCode() + " " + r.body());
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(r.body(), "$.convertedBalance")).isEqualTo("146.50");
    }

    @Test
    void slowApiIs504() throws Exception {
        stub.delayMillis = 2_000;
        long t0 = System.nanoTime();
        var r = ana.get(path("USD"));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("lento     " + r.statusCode() + " em " + ms + " ms  " + r.body());
        assertThat(r.statusCode()).isEqualTo(504);
        assertThat((String) JsonPath.read(r.body(), "$.type")).isEqualTo("urn:payflow:problem:upstream-timeout");
        assertThat(ms).isLessThan(1_500);
    }

    @Test
    void brokenApiIs502() throws Exception {
        stub.status = 500;
        assertThat(ana.get(path("USD")).statusCode()).isEqualTo(502);
        stub.status = 200;
        stub.body = "<html>oops</html>";
        assertThat(ana.get(path("USD")).statusCode()).isEqualTo(502);
        stub.body = "{\"amount\":1.0,\"base\":\"BRL\",\"date\":\"2026-11-13\",\"rates\":{}}";
        var r = ana.get(path("USD"));
        System.out.println("500/HTML/sem taxa -> " + r.statusCode() + " " + r.body());
        assertThat(r.statusCode()).isEqualTo(502);
        assertThat((String) JsonPath.read(r.body(), "$.type")).isEqualTo("urn:payflow:problem:upstream-failure");
    }

    @Test
    void unknownCurrencyIs422() throws Exception {
        stub.status = 404;
        stub.body = "{\"message\":\"not found\"}";
        var r = ana.get(path("ZZZ"));
        System.out.println("moeda desconhecida (404 da API) -> " + r.statusCode() + " " + r.body());
        assertThat(r.statusCode()).isEqualTo(422);
    }

    @Test
    void lowercaseCurrencyIs400AndTheApiIsNotCalled() throws Exception {
        var r = ana.get(path("usd"));
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(stub.calls.get()).isZero();
    }
}
```
`src/test/java/com/payflow/api/CorsApiTest.java`
```java
package com.payflow.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.support.ApiClient;
import com.payflow.support.ResetDatabase;
import java.net.URI;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** CORS só para as origens configuradas (J6.08); o preflight passa pelo AuthInterceptor sem cookie. */
@ExtendWith(ResetDatabase.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "payflow.cors.allowed-origins=http://localhost:5173")
@ActiveProfiles("test")
class CorsApiTest {

    @LocalServerPort int port;

    HttpRequest.Builder preflight(String origin) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/transfers"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,idempotency-key");
    }

    @Test
    void preflightFromAnAllowedOriginIs200WithTheHeaders() throws Exception {
        var r = new ApiClient(port).send(preflight("http://localhost:5173"));
        System.out.println("preflight origem permitida: " + r.statusCode() + " " + r.headers().map());
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("Access-Control-Allow-Origin").orElseThrow()).isEqualTo("http://localhost:5173");
        assertThat(r.headers().firstValue("Access-Control-Allow-Credentials").orElseThrow()).isEqualTo("true");
        assertThat(r.headers().firstValue("Access-Control-Max-Age").orElseThrow()).isEqualTo("3600");
    }

    @Test
    void preflightFromAStrangeOriginIs403() throws Exception {
        var r = new ApiClient(port).send(preflight("https://evil.example"));
        System.out.println("preflight origem estranha: " + r.statusCode() + " " + r.body());
        assertThat(r.statusCode()).isEqualTo(403);
    }

    @Test
    void actualRequestFromAnAllowedOriginGetsTheAllowOriginHeader() throws Exception {
        var ana = new ApiClient(port).login("ana@payflow.dev");
        var r = ana.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/accounts"))
                .header("Origin", "http://localhost:5173"));
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("Access-Control-Allow-Origin").orElseThrow()).isEqualTo("http://localhost:5173");
        assertThat(r.headers().firstValue("Access-Control-Expose-Headers").orElseThrow()).contains("Location");
    }

    @Test
    void pagesHaveNoCors() throws Exception {
        var r = new ApiClient(port).send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
                .header("Origin", "http://localhost:5173"));
        assertThat(r.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }
}
```
`src/test/java/com/payflow/jpa/OptionalFilterLabTest.java`
```java
package com.payflow.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.domain.EntryType;
import com.payflow.domain.LedgerEntry;
import com.payflow.support.ResetDatabase;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Laboratório da J6.05: o JPQL "(:p is null or ...)" no PostgreSQL. Por que o extrato usa Specification. */
@ExtendWith(ResetDatabase.class)
@SpringBootTest
@ActiveProfiles("test")
class OptionalFilterLabTest {

    @Autowired EntityManager em;

    @Test
    void optionalParametersInJpqlFailInPostgres() {
        String jpql = """
                select e from LedgerEntry e
                where (:type is null or e.type = :type)
                  and (:from is null or e.createdAt >= :from)
                """;
        List<String> results = new ArrayList<>();
        for (EntryType type : Arrays.asList(EntryType.CREDIT, null)) {
            for (Instant from : Arrays.asList(Instant.parse("2026-01-01T00:00:00Z"), null)) {
                String outcome;
                try {
                    em.createQuery(jpql, LedgerEntry.class).setParameter("type", type).setParameter("from", from).getResultList();
                    outcome = "OK";
                } catch (RuntimeException e) {
                    outcome = e.getClass().getSimpleName() + ": " + e.getMessage().lines()
                            .filter(l -> l.contains("ERROR")).findFirst().orElse(e.getMessage());
                }
                String line = String.format("type=%-6s from=%-20s -> %s", type, from, outcome);
                System.out.println(line);
                results.add(outcome);
            }
        }
        assertThat(results).allMatch(r -> r.contains("could not determine data type of parameter"));
    }
}
```
</details>

## Parte 3 — Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Desenho endpoints REST (recursos, verbos, status, idempotência) e justifico | ☐ | ☐ | ☐ |
| Separo DTOs de entidades e mapeio (manual e MapStruct) | ☐ | ☐ | ☐ |
| Exponho uma operação transacional e traduzo desfechos em status | ☐ | ☐ | ☐ |
| Padronizo erros com Problem Details sem vazar internals | ☐ | ☐ | ☐ |
| Pagino, filtro e ordeno com limites e ordem estável | ☐ | ☐ | ☐ |
| Aplico versionamento e idempotência (inclusive sob concorrência) | ☐ | ☐ | ☐ |
| Documento e valido a API com OpenAPI | ☐ | ☐ | ☐ |
| Configuro CORS e consumo APIs externas com timeouts | ☐ | ☐ | ☐ |

**Regra:** com mais de duas linhas em 0, repita as aulas correspondentes antes do [J7](../J7-testes-qualidade/README.md). Atualize `00-guia/progresso.md`.

## Próximo
[J7 — Testes e qualidade](../J7-testes-qualidade/README.md)
