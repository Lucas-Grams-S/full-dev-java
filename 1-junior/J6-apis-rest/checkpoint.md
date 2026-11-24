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
- [ ] `POST /transfers` atômico e idempotente (corrida: 1×201 + 7×200); 6 pedidos simultâneos: 1×201 + 5×409.
- [ ] Todo erro da API é `application/problem+json`; páginas continuam HTML.
- [ ] `GET .../entries` paginado com `Slice`, filtros por `Specification`, 400 para parâmetros inválidos.
- [ ] Versionamento `/api/v1` por predicado; legado do painel intacto.
- [ ] OpenAPI válida (teste), UI só em `dev`/`test`.
- [ ] CORS para origens configuradas (testado também em navegador); timeouts no `RestClient`.
- [ ] **43 testes novos** (`AccountsApiTest` 8, `TransfersApiTest` 9, `ErrorsApiTest` 6, `VersioningApiTest` 3, `OpenApiTest` 1 + `OpenApiDisabledTest` 1, `ExchangeApiTest` 5, `CorsApiTest` 4, `RestClientTimeoutLabTest` 3, `LedgerInvariantTest` 1, `EntityJsonLabTest` 1, `OptionalFilterLabTest` 1) e `scripts/api-smoke.sh` com 15 `ok`.
- [ ] Commits em Conventional Commits, PR mesclado e tag:
```bash
git tag payflow-J6
git push origin payflow-J6
```

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
