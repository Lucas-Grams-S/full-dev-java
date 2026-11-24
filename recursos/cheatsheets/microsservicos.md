# Cheatsheet — Microsserviços e integração (P5)

> Valores **executados** em `2-pleno/P5-microsservicos-integracao/lab/integration-lab/` (Spring Boot 4.1.2, Resilience4j 2.4.0, Spring Cloud 2025.1.4/Gateway 5.0.4, Pact JVM 4.7.7, JDK 25). Os números de tempo são **do laboratório** (máquina compartilhada): **não** são recomendações de produção. Veja a aula indicada.

## Dividir ou não (P5.01, P5.02)
| Pergunta | Se a resposta for "não" |
|---|---|
| Há **dor medida** (deploy, escala, falha, time)? | fique no monólito modular |
| A fronteira existe no código (API, **dono dos dados**, `verify()`)? | modularize antes |
| O resto **vive** se esta parte cair? | redesenhe (assíncrono, *fallback*) |
| Posso **implantar sozinho** e continuar correto se os vizinhos caírem? | é um **monólito distribuído** |
Disponibilidade em cadeia síncrona = **produto**: 5 × 99,9% = 99,50%; 10 = 99,00%; 20 = 98,02%.

## Padrões que o laboratório mediu
| Situação | Resultado medido | Aula |
|---|---|---|
| Cliente HTTP do JDK **sem** *timeout* de leitura | esperou **2.003 ms** (servidor de 2.000 ms) | P5.07 |
| *Read timeout* 300 ms | falha em **302 ms** (`HttpTimeoutException`) | P5.07 |
| 3 camadas × 3 tentativas contra serviço que falha sempre | **27** chamadas por requisição | P5.07 |
| `POST` repetido após *timeout* (servidor processou) | **3** mensagens (sem idempotência) | P5.07 |
| `404` × 20 com configuração **padrão** do *breaker* | **OPEN**; com `ignoreExceptions` → CLOSED | P5.08 |
| 5 respostas **200** de 150 ms (limite 100 ms) | **OPEN** (lentidão conta) | P5.08 |
| *Bulkhead* 5, 20 chamadas simultâneas | 5 executadas, 15 rejeitadas | P5.08 |
| *Rate limiter* 10/s, 25 chamadas instantâneas | 10 permitidas, 15 negadas | P5.08 |
| `TimeLimiter` 200 ms | libera o chamador, **não** cancela o trabalho | P5.07 |
| Fallback no `@CircuitBreaker` (interna) | `@Retry` **não age** (1 chamada, não 3) | P5.07 |
| Fallback **não público** | `IllegalAccessException` intermitente (1–2 em 30 → 500) | P5.07 |
| Tomcat de 10 *threads*, 30 checkouts, serviço de 2 s, **sem** proteção | `/ping` sem resposta em 1 s; mediana **4.024 ms**; 30 chamadas ao serviço | P5.10 |
| Idem **com** timeout + retry + breaker + bulkhead + fallback | `/ping` 3 ms; 30/30 com 200; mediana ~37–42 ms; **8** chamadas ao serviço | P5.10 |

## Resilience4j (P5.07, P5.08)
| Item | Regra |
|---|---|
| Dependências | `resilience4j-spring-boot4` + `spring-boot-starter-aspectj` (anotações) + actuator |
| Ordem dos aspectos | `Retry ( CircuitBreaker ( RateLimiter ( TimeLimiter ( Bulkhead ( f ) ) ) ) )` |
| `fallbackMethod` | na anotação **mais externa** (`@Retry`) e **`public`** |
| `@CircuitBreaker` | `recordExceptions` (5xx, E/S, *timeout*) e `ignoreExceptions` (4xx) **explícitos**; `slowCallDurationThreshold` |
| Estados | CLOSED → OPEN (recusa na hora) → HALF_OPEN (poucas sondas) → CLOSED; a passagem OPEN→HALF_OPEN é **preguiçosa**; fechar **zera** a janela |
| `@Bulkhead` | `L = λ × W` com folga 2–3×; `max-wait-duration: 0` (rejeita) |
| `@RateLimiter` | por **instância** (N réplicas = N× o limite) |
| *Retry* | ≤ 3 no total, *backoff* exponencial + *jitter*, **uma** camada, nunca 4xx, só idempotente |
| Pior caso do *retry* | soma de tempos **cabe** no prazo do chamador |

## Gateway WebMVC (P5.05)
| Item | Observado |
|---|---|
| Dependências | `spring-cloud-starter-gateway-server-webmvc` + BOM `spring-cloud-dependencies` **2025.1.4** (+ `…circuitbreaker-resilience4j`) |
| Combinação | trem 2025.1 → Boot 4.0.x; **nenhum** trem declara o Boot 4.1 (funcionou no 4.1.2, **não declarado**) |
| `X-Forwarded-*` | só com `spring.cloud.gateway.server.webmvc.trusted-proxies` (nunca `.*` em produção) |
| Timeouts | `spring.http.clients.connect-timeout` / `read-timeout`; estouro → **HTTP 500** (não 502/504) |
| `CircuitBreaker` | `args.statusCodes` obrigatório para contar 503; `fallbackUri: forward:/…` |
| `Retry` | forma `name`/`args` (a curta não vincula); `methods: GET,POST` repete POST (**perigoso**) |
| Segurança | **nenhuma** rota `/internal/**`; o JWT é validado **em cada serviço** |

## gRPC e Protobuf (P5.03, P5.04)
| Regra | Evidência |
|---|---|
| Número de campo **nunca** reutilizado (`reserved`) | `int32 score=2` lido como `string reason=2` → dado some **sem erro** |
| Campo novo = número novo | leitor antigo preserva como desconhecido; leitor novo vê o padrão |
| Enum com `0 = UNSPECIFIED` | valor novo → `UNRECOGNIZED` no cliente antigo |
| **Prazo obrigatório** | sem *deadline*: esperou 1.504 ms; com 200 ms: `DEADLINE_EXCEEDED` em 215 ms |
| Dinheiro | `string` (decisão 11), nunca `double` |
| Boot 4.1 | `spring-boot-starter-grpc-server`/`-client`; `spring.grpc.server.port`; BOM: grpc-java 1.84.1, protobuf-java 4.36.2, spring-grpc 1.1.2 |
| Plugin Maven | `ascopes` 5.1.11/5.1.12 **falhou** com Maven 3.9.12; usado `xolstice` 0.6.1 + `os-maven-plugin` |

## Eventos e compatibilidade (P5.04)
| Mudança do produtor | Jackson 3 (Boot) |
|---|---|
| Campo **novo** | ignorado (`FAIL_ON_UNKNOWN_PROPERTIES` = false, também de fábrica) |
| Campo **renomeado/removido** | `null` **silencioso** → valide os obrigatórios |
| **Enum** novo | `InvalidFormatException` → `UNKNOWN`/DLT ou `EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL` |
| Dinheiro como número | passa por `BigDecimal`, mas via `double` perde precisão → **string** |
Quebra de verdade: **expandir → migrar → contrair** (tópico/versão nova em paralelo).

## Pact (P5.09)
| Item | Regra |
|---|---|
| Consumidor | `@Pact` **um por cenário** (várias interações num método exigem todas em cada teste); `@PactDirectory`; *matchers* de tipo/regex |
| Provedor | `@Provider` + `@PactFolder` + `@TestTemplate`/`PactVerificationContext` + `@State` |
| Prova | `rename`/`type` **falham** (`missing the following keys: channels`; `Type mismatch`); `additive` **passa** |
| Higiene | `pact_do_not_track=true` (métricas anônimas por padrão) |
| Não executado | Spring Cloud Contract, Pact Broker, `can-i-deploy`, pactos de mensagem |
