# ADR 0005 — Front-end do PayFlow: renderização no servidor (Thymeleaf) com JavaScript pontual

- **Status:** aceita (J4, 2026-11-13)
- **Contexto:** o PayFlow precisa de um painel (login, contas, extrato, depósito). O time é de Java; as telas são formulários e uma tabela; as regras (saldo, dono da conta, validação) já vivem no servidor. Há duas formas comuns de fazer o painel: páginas montadas no servidor ou uma SPA (Angular/React) falando com uma API.
- **Opções:** (1) SPA com Angular ou React, API JSON para tudo; (2) renderização no servidor com Thymeleaf e um trecho de JavaScript com `fetch` para o "carregar mais" do extrato; (3) HTML estático com JavaScript puro para tudo.
- **Decisão:** opção 2. Páginas por **Thymeleaf** (`th:text`, fragmentos, escape por padrão), formulários com **Post/Redirect/Get** e **um** endpoint JSON (`GET /api/accounts/{id}/statement?page=N`) usado só por `statement.js`. O link "Mais antigos" **funciona sem JavaScript** (aprimoramento progressivo). O endpoint JSON é desenhado como **contrato estável** (dinheiro em string, datas ISO-8601 UTC, decisão 11).
- **Consequências:**
  - (+) Uma só linguagem principal, um só deploy, regras e autorização no servidor; XSS reduzido pelo escape automático do `th:text`.
  - (+) Se surgir um app de celular ou um painel rico, a API JSON já existe e não muda; o front vira um novo cliente.
  - (−) Cada ação recarrega a página; interações muito ricas (tempo real, arrastar e soltar) exigiriam JavaScript maior ou uma SPA.
  - (−) A formatação do extrato existe **duas vezes** (Java para a página, JavaScript para o "Mais antigos"); aceito porque é pequena e coberta por teste de ponta a ponta.
  - Na J5 o roteador manual (`PayflowServer`) é substituído por Spring MVC; os templates e o `statement.js` permanecem.
