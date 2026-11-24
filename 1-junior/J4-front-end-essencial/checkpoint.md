# J4.CP — Checkpoint do módulo J4

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J4.01 a J4.08

## Parte 1 — Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Um formulário de depósito envia, mas o servidor recebe `amount` nulo. Cite **três** causas possíveis no HTML. Por que o botão "Cancelar" de um formulário pode **enviá-lo** sem você querer?
2. Por que login e depósito usam `POST` e não `GET`? O que o navegador manda no corpo de um `POST` de formulário (formato e codificação), e o que acontece com `25,90` e com `bruno@payflow.dev`?
3. Uma caixa tem `width: 200px; padding: 20px; border: 5px solid`. Qual a largura total com `content-box` e com `border-box`? Qual a diferença entre `justify-content` e `align-items` e quando usar flexbox ou grid?
4. O que a regra `grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr))` faz? Sem o `<meta name="viewport">`, o que acontece em um celular? O que é *mobile-first*?
5. Em JavaScript, quanto valem `'5' + 3`, `'5' - 3`, `0.1 + 0.2`, `Number('25,90')` e `Number('')`? Por que o PayFlow manda dinheiro como string e só usa `Number` para **exibir**?
6. Mostre a diferença de segurança entre `el.textContent = nome` e `el.innerHTML = nome`, e entre `th:text` e `th:utext` do Thymeleaf. O que é XSS e qual a defesa principal em uma página renderizada no servidor?
7. Qual a ordem de saída? `console.log('A'); setTimeout(() => console.log('D'), 0); Promise.resolve().then(() => console.log('C')); console.log('B');`. Explique com microtarefas e tarefas.
8. Um `fetch` a uma conta inexistente devolve 404. Cai no `catch`? Como tratar corretamente? O que significa `TypeError: Failed to fetch` com a mensagem "blocked by CORS policy" e onde se conserta?
9. Depois de `POST /accounts/{id}/deposit` com sucesso, o servidor responde `303 Location: ...`. Por quê? Que status o servidor devolve para valor inválido (`abc`), para conta bloqueada e para uma conta que pertence a outro usuário, e por quê?
10. Compare SSR (Thymeleaf) e SPA (Angular/React) para o painel do PayFlow. O que o TypeScript garante e o que **não** garante (`as StatementPage`)? Cite **três** itens de acessibilidade que o painel cumpre e **um** que o axe não detecta.

<details><summary>Gabarito</summary>

1. (a) Campo **sem `name`**; (b) campo `disabled` ou **fora** do `<form>`; (c) `name` com erro de digitação (`ammount`). (Checkbox desmarcado também não envia nada.) Um `<button>` dentro do formulário **sem `type`** é `submit` por padrão; o "Cancelar" precisa de `type="button"`.
2. `GET` põe os dados na URL (histórico, logs, *referer*, reenvio sem aviso) e é para **ler**; `POST` altera estado e leva os dados no corpo. O corpo é `application/x-www-form-urlencoded` (`name=valor&...`) com URL-encoding: `25,90` → `25%2C90` e `bruno@payflow.dev` → `bruno%40payflow.dev` (espaço vira `+`). Medido com `nc -l` no Chrome 156.
3. `content-box`: 200 + 2×20 + 2×5 = **250 px**; `border-box`: **200 px** (medido). `justify-content` distribui no eixo **principal** e `align-items` no **cruzado**. Flexbox para **uma dimensão** (barras, grupos); grid para **duas** (a página, galerias de cartões); usam-se juntos.
4. Cria **quantas colunas couberem**, cada uma com no mínimo 16rem e no máximo uma fração do espaço, sem media query (medido: 4 colunas em 1200 px, 2 em 700 px, 1 em 390 px). Sem o `viewport`, o celular finge ter ~980 px de largura e **encolhe** a página. *Mobile-first*: escrever o estilo base para a tela pequena e **acrescentar** regras com `min-width` para telas maiores.
5. `"53"`, `2`, `0.30000000000000004`, `NaN`, `0` (medidos no Node 22). `Number` é `double`: perde exatidão e a vírgula brasileira não é entendida. O servidor calcula com `BigDecimal` e manda **string**; no navegador, `Number` só serve para `Intl.NumberFormat` (formatar).
6. `textContent` insere **texto**; `innerHTML` interpreta HTML (`<img onerror=...>` executou o alerta no teste: 0 alertas × 1). `th:text` **escapa** (`&lt;script&gt;`); `th:utext` não. XSS é script de terceiro executando no navegador da vítima via dado não confiável; defesa principal: **escapar a saída por contexto** (Thymeleaf faz por padrão) e inserir dados como texto no JavaScript.
7. `A`, `B`, `C`, `D`. O código síncrono (`A`, `B`) roda primeiro; as **microtarefas** (`then`) esvaziam antes da próxima **tarefa**; `setTimeout(…, 0)` é tarefa e vem por último.
8. **Não**: o `fetch` só rejeita em falha de rede/CORS/abort; um 404 é uma resposta. Tratar com `if (!response.ok) throw new Error(...)` (e `401` → login). A mensagem de CORS significa que a **origem** (esquema+host+porta) da página difere da da API e o servidor não autorizou (`Access-Control-Allow-Origin`): o navegador bloqueou a leitura. Conserta-se **no servidor** (ou com proxy de desenvolvimento), nunca desligando a segurança do navegador.
9. **Post/Redirect/Get**: o `303` manda o navegador fazer um `GET`; assim F5 não repete o depósito (e duplicaria o dinheiro). Entrada inválida → **400**; regra de negócio violada (conta bloqueada) → **422**; conta de outro usuário → **404** (não confirma que existe; a autorização completa é J8). O status descreve o resultado para testes, monitoramento e clientes; o corpo (página com a mensagem) é para o humano.
10. SSR: uma linguagem, um deploy, regras no servidor, funciona sem JS, escape por padrão; SPA: interatividade rica e API reutilizável, com mais JavaScript, dois deploys, CORS e token no navegador. O PayFlow fica com SSR + JS pontual (ADR 0005), com a API como contrato estável. TypeScript confere **tipos em tempo de compilação** (propriedade errada, `number` no lugar de `string`), mas os tipos são **apagados**; `as StatementPage` é uma promessa, não valida o JSON recebido. Acessibilidade cumprida: `label` em todo campo, link de salto e foco visível, contraste ≥ 4,5:1, `lang`/`title`/`main`/`h1`, erro em `role="alert"` (qualquer três). O axe **não** detecta, por exemplo, um `<div onclick>` usado como botão, o foco invisível por `outline: none` ou um link "clique aqui".
</details>

## Parte 2 — Entrega prática (tag `payflow-J4`)
O repositório `payflow` ganha o **painel web**: login provisório, contas, extrato com depósito e "Mais antigos" por `fetch`, renderizados com **Thymeleaf** sobre o servidor JDK, o acesso JDBC da J3 e **21 testes**.

Estrutura esperada (o que muda em relação à J3):
```
payflow/
├── pom.xml                              (+ thymeleaf 3.1.7.RELEASE, slf4j-simple 2.0.20)
├── web-prototype/                       login.html, accounts.html, statement.html, app.css, statement.js   (J4.01–J4.05)
├── docs/
│   ├── frontend-lab/                    01-transfer-form.html, 02-box-model.html, 03-grid-responsivo.html,
│   │                                    04-dom-events.html, 05-async.mjs, ApiStub.java, 08-broken.html, a11y.mjs,
│   │                                    react-ts/ (types.ts, api.ts, StatementTable.tsx, ssr.tsx, tsconfig.json)
│   ├── frontend-notes.md                (J4.07)
│   └── a11y-review.md                   (J4.08)
├── src/main/java/com/payflow/web/       Sessions, Views, JsonWriter, PayflowServer, WebApp
├── src/main/resources/templates/        fragments, login, accounts, statement, error (.html)
├── src/main/resources/static/           app.css, statement.js
└── src/test/java/com/payflow/web/       PayflowServerTest
```

Critérios de pronto:
- [ ] **HTML:** as páginas têm `lang="pt-BR"`, `charset`, `viewport`, `title`, um `main`, **um** `h1`; todo campo com `label for`/`id` e `name`; formulários de alteração com `method="post"`; dinheiro como `type="text"` + `inputmode="decimal"` (nunca `number`).
- [ ] **CSS:** `app.css` com variáveis em `:root`, `box-sizing: border-box`, cabeçalho em flexbox, cartões em grid com `auto-fill/minmax`, tabela em `.table-wrap`; sem `!important`, sem `id` como seletor, sem cor fora de `:root`; sem rolagem horizontal em 390, 768 e 1200 px.
- [ ] **JavaScript:** `statement.js` com `defer`, `<template>` para as linhas, `textContent` (nunca `innerHTML` com dado externo), `Intl` pt-BR, guarda `loading`, `response.ok`, tratamento de 401 e de erro com `role="status"`/`aria-live`; dinheiro só **formatado**, nunca somado.
- [ ] **`fetch` testado** contra o `ApiStub` com 500, 401 e API lenta (clique duplo → uma carga).
- [ ] **Thymeleaf:** 5 templates, fragmentos para `head`/`header`/`footer`, `th:text` em tudo que vem do modelo e **nenhum `th:utext`**.
- [ ] **Servidor:** `PayflowServer` com sessão por cookie `HttpOnly; SameSite=Lax`, PRG (303), 400/401/404/422 conforme a aula, conta de outro usuário → 404 (página, API e depósito), arquivos estáticos só com o padrão permitido.
- [ ] **Testes:** `mvn test` com **21 testes** verdes (11 da J3 + 10 do painel), JDK 25, banco `payflow_test`, nada de H2.
- [ ] **XSS:** um usuário com `full_name` = `<img src=x onerror=alert(1)>` aparece como **texto** no cabeçalho.
- [ ] **TypeScript:** `docs/frontend-lab/react-ts` passa em `npx tsc -p .`, com a saída do render e o `bad.ts` mostrando os 3 erros.
- [ ] **Acessibilidade:** `docs/a11y-review.md` com os relatórios do axe (página quebrada: 7 violações; painel: 0), a tabela de contraste, a ordem do Tab e o teste só com teclado.
- [ ] **Decisão registrada:** `docs/frontend-notes.md` (SSR × SPA aplicado ao PayFlow) e um ADR de front-end no formato dos anteriores.
- [ ] **Sem segredos** e sem `node_modules/` versionado (`.gitignore`).
- [ ] Commits em Conventional Commits (inglês), PR mesclado e tag enviada:
```bash
git tag payflow-J4
git push origin payflow-J4
```

## Parte 3 — Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Escrevo HTML semântico e formulários com `label`, `name` e validação nativa, e explico o que o navegador envia | ☐ | ☐ | ☐ |
| Leio e ajusto CSS: cascata, box model, flexbox e grid | ☐ | ☐ | ☐ |
| Faço uma página responsiva (viewport, `auto-fill/minmax`, media query mobile-first) | ☐ | ☐ | ☐ |
| Manipulo o DOM e trato eventos (inclusive delegação) sem `innerHTML` com dado externo | ☐ | ☐ | ☐ |
| Consumo a API com `fetch`/`async`/`await`, tratando erro HTTP, rede, timeout e CORS | ☐ | ☐ | ☐ |
| Prevejo a ordem de execução (síncrono, microtarefa, tarefa) | ☐ | ☐ | ☐ |
| Renderizo páginas no servidor com Thymeleaf (`th:text`, `th:each`, `th:if`, fragmentos) e explico o escape | ☐ | ☐ | ☐ |
| Aplico Post/Redirect/Get e os status HTTP certos em formulários | ☐ | ☐ | ☐ |
| Leio TypeScript, um componente React e um componente Angular simples | ☐ | ☐ | ☐ |
| Reviso a acessibilidade básica (teclado, foco, contraste, `label`, axe) | ☐ | ☐ | ☐ |

**Regra:** se ficou com mais de duas linhas em 0, repita as aulas correspondentes antes de seguir para o [J5](../J5-spring-boot/README.md). Atualize `00-guia/progresso.md` e o `cronograma.md`.

## Referência do que foi construído
O código completo e testado está nas aulas: [J4.05](J4.05-promises-async-await-fetch.md) (`statement.js` e `ApiStub`), [J4.06](J4.06-thymeleaf-renderizacao-no-servidor.md) (`pom.xml`, `Views`, `Sessions`, `PayflowServer`, `JsonWriter`, `WebApp` e os 5 templates) e [J4.02](J4.02-css-box-model-flexbox.md) + [J4.03](J4.03-css-grid-responsividade.md) (`app.css` em duas metades) e [J4.08](J4.08-acessibilidade-e-revisao-do-painel.md) (o bloco final de acessibilidade do `app.css`). Use-o para **conferir** a sua versão depois de tentar. Falta aqui só o teste de ponta a ponta citado na J4.06 (conferido com PostgreSQL 18.6, JDK 25.0.5, Thymeleaf 3.1.7.RELEASE e JUnit 6.1.3: `mvn test` → 21 testes, 0 falhas):

<details><summary><code>src/test/java/com/payflow/web/PayflowServerTest.java</code></summary>

```java
package com.payflow.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

/** Testa o painel de ponta a ponta: HTTP real, Thymeleaf real, PostgreSQL real (banco payflow_test). */
class PayflowServerTest {

    private static final String ANA_ACCOUNT = "20000000-0000-0000-0000-000000000001";
    private static final String BRUNO_ACCOUNT = "20000000-0000-0000-0000-000000000002";

    private HttpServer server;
    private HttpClient client;
    private String base;

    @BeforeEach
    void start() throws Exception {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl("jdbc:postgresql://localhost:5432/payflow_test");
        ds.setUser("payflow");
        ds.setPassword("payflow");
        Flyway flyway = Flyway.configure().dataSource(ds)
                .locations("classpath:db/migration", "classpath:db/seed")
                .cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        server = PayflowServer.create((DataSource) ds, 0, false);
        server.start();
        base = "http://localhost:" + server.getAddress().getPort();
        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)    // queremos VER o 303
                .build();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).build(), BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String field, String value) throws Exception {
        String body = field + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
        return client.send(HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(body)).build(), BodyHandlers.ofString());
    }

    private void loginAsAna() throws Exception {
        assertThat(post("/login", "email", "ana@payflow.dev").statusCode()).isEqualTo(303);
    }

    @Test
    void pagesRequireLogin() throws Exception {
        var response = get("/accounts");
        assertThat(response.statusCode()).isEqualTo(303);
        assertThat(response.headers().firstValue("Location")).contains("/login");
        assertThat(get("/api/accounts/" + ANA_ACCOUNT + "/statement").statusCode()).isEqualTo(401);
    }

    @Test
    void loginSetsAnHttpOnlyCookieAndShowsTheBalance() throws Exception {
        var login = post("/login", "email", "ana@payflow.dev");
        assertThat(login.headers().firstValue("Set-Cookie").orElseThrow())
                .contains("PAYFLOW_SESSION=", "HttpOnly", "SameSite=Lax");
        var page = get("/accounts");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Ana Silva", "R$ 830,00", "Ativa");
    }

    @Test
    void unknownEmailIsRejectedAndTheOutputIsEscaped() throws Exception {
        var response = post("/login", "email", "<script>alert(1)</script>@x.com");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).doesNotContain("<script>alert(1)");
        assertThat(response.body()).contains("&lt;script&gt;alert(1)&lt;/script&gt;@x.com");
    }

    @Test
    void statementShowsFormattedEntries() throws Exception {
        loginAsAna();
        var body = get("/accounts/" + ANA_ACCOUNT).body();
        assertThat(body).contains("15/01/2026 06:10", "Transferência enviada", "− R$ 20,00",
                "Depósito", "+ R$ 1.000,00");
        assertThat(body).doesNotContain("id=\"load-more\"");         // só 3 lançamentos: sem próxima página
    }

    @Test
    void depositFollowsPostRedirectGet() throws Exception {
        loginAsAna();
        var response = post("/accounts/" + ANA_ACCOUNT + "/deposit", "amount", "25,90");
        assertThat(response.statusCode()).isEqualTo(303);
        assertThat(response.headers().firstValue("Location")).contains("/accounts/" + ANA_ACCOUNT);
        assertThat(get("/accounts/" + ANA_ACCOUNT).body()).contains("Saldo: R$ 855,90", "+ R$ 25,90");
    }

    @Test
    void invalidDepositShowsAnErrorAndChangesNothing() throws Exception {
        loginAsAna();
        for (String bad : new String[] {"abc", "-5", "0", "1,234", ""}) {
            var response = post("/accounts/" + ANA_ACCOUNT + "/deposit", "amount", bad);
            assertThat(response.statusCode()).as("amount '%s'", bad).isEqualTo(400);
            assertThat(response.body()).contains("role=\"alert\"", "Informe um valor positivo");
        }
        assertThat(get("/accounts/" + ANA_ACCOUNT).body()).contains("Saldo: R$ 830,00");
    }

    @Test
    void anotherUsersAccountLooksLikeItDoesNotExist() throws Exception {
        loginAsAna();
        assertThat(get("/accounts/" + BRUNO_ACCOUNT).statusCode()).isEqualTo(404);
        assertThat(get("/api/accounts/" + BRUNO_ACCOUNT + "/statement").statusCode()).isEqualTo(404);
        assertThat(post("/accounts/" + BRUNO_ACCOUNT + "/deposit", "amount", "5").statusCode()).isEqualTo(404);
    }

    @Test
    void apiReturnsMoneyAsStringAndPaginates() throws Exception {
        loginAsAna();
        for (int i = 1; i <= 4; i++) {
            post("/accounts/" + ANA_ACCOUNT + "/deposit", "amount", i + ",00");
        }
        var page0 = get("/api/accounts/" + ANA_ACCOUNT + "/statement?page=0");
        assertThat(page0.headers().firstValue("Content-Type")).contains("application/json; charset=utf-8");
        assertThat(page0.body()).contains("\"hasMore\":true", "\"amount\":\"4.00\"", "\"transferId\":null");
        var page1 = get("/api/accounts/" + ANA_ACCOUNT + "/statement?page=1");
        assertThat(page1.body()).contains("\"hasMore\":false", "\"amount\":\"1000.00\"");
    }

    @Test
    void servesStaticFilesWithTheRightTypeAndRejectsOthers() throws Exception {
        var css = get("/static/app.css");
        assertThat(css.statusCode()).isEqualTo(200);
        assertThat(css.headers().firstValue("Content-Type")).contains("text/css; charset=utf-8");
        assertThat(get("/static/..%2fpom.xml").statusCode()).isEqualTo(404);
        assertThat(get("/static/missing.js").statusCode()).isEqualTo(404);
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        loginAsAna();
        assertThat(get("/accounts").statusCode()).isEqualTo(200);
        post("/logout", "x", "y");
        assertThat(get("/accounts").statusCode()).isEqualTo(303);
    }
}
```
</details>

## Próximo
[J5 — Spring Framework e Spring Boot](../J5-spring-boot/README.md)
