# J8.CP — Checkpoint do módulo J8

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J8.01 a J8.07

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, OpenJDK 25.0.5, Maven 3.9.12, Spring Boot 4.1.2, Spring Security 7.1.2): `lab/security-lab` (**81 testes**, a segurança do PayFlow com usuários em memória), `lab/default-security-lab` (**8 testes**), `lab/PasswordLab.java`, `lab/InjectionLab.java` (H2), `lab/check-secrets.sh` e `lab/osv-check.sh`. **Não executados:** o repositório `payflow` do aluno (nada foi compilado contra ele), o OWASP Dependency-Check, o gitleaks, a chamada real ao Have I Been Pwned, os testes de navegador (CSP, CSRF, `SameSite`) e a leitura do texto da LGPD. Os números de "testes esperados" abaixo vêm de **contagem por leitura**, não de execução.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Qual a diferença entre autenticação e autorização, e entre `401` e `403`? Onde o Spring Security fica no caminho da requisição e o que decide qual `SecurityFilterChain` trata um pedido?
2. Por que SHA-256 não serve para senha e o que o BCrypt acrescenta (sal e custo)? Para que serve o prefixo `{bcrypt}`, e por que `BadCredentialsException` é a mesma para e-mail inexistente e senha errada?
3. Onde mora o estado na sessão e no JWT? O que a assinatura de um JWT garante e **não** garante? Por que o PayFlow usa **os dois**?
4. O que é *session fixation* e como o Spring Security a previne? Para que servem `HttpOnly`, `Secure` e `SameSite` no cookie da sessão?
5. Por que a **ordem** das regras de URL importa? Cite três motivos de um `@PreAuthorize` não valer e o que faz um `AccessDeniedException` virar `500`.
6. Como se previne SQL injection e por que o ORM não imuniza? O que o Thymeleaf escapa e o que não escapa? O que a CSP faz?
7. Explique um ataque CSRF, as duas defesas do PayFlow e por que a API Bearer não precisa de CSRF. Por que `GET` nunca pode alterar estado?
8. O que é IDOR? Por que `404` (e não `403`) para a conta de outra pessoa, e por que UUID não é autorização? Como provar que **todo** endpoint com `{id}` checa o dono?
9. Como o limite de tentativas de login funciona e qual o seu custo (DoS de conta)? O que diz o NIST sobre a política de senha? Por que o `toString` de um `record` de login é um risco e o que é *log injection*?
10. Onde um segredo **não** pode estar e o que fazer quando um vaza? O que "zero registros no OSV" **não** quer dizer? Cite dois cuidados de LGPD que viram decisão de código e o conflito entre eliminação e o livro-razão.

<details><summary>Gabarito</summary>

1. Autenticação confirma **quem** é; autorização decide **o que** pode. `401` = não autenticado (vem com `WWW-Authenticate`); `403` = autenticado sem permissão. O Spring Security é uma cadeia de *servlet filters* (`DelegatingFilterProxy` → `FilterChainProxy`) que roda **antes** do `DispatcherServlet`; cada `SecurityFilterChain` tem um `securityMatcher` e vale a **primeira** que casa (por isso `@Order`; a genérica por último). O último filtro, `AuthorizationFilter`, aplica as regras.
2. SHA-256 é rápido e sem sal: GPUs e dicionários pré-calculados quebram milhões por segundo. O BCrypt embute **sal** (aleatório por senha: dois `encode` diferentes, ambos conferem) e **custo** (cada +1 dobra o tempo: ~53 ms no 10, ~209 ms no 12 na máquina do autor). `{bcrypt}` diz qual algoritmo gerou o hash (permite trocar depois); sem ele, o delegante lança `IllegalArgumentException`. A exceção é a mesma (`hideUserNotFoundExceptions`, mais um hash de mentira para igualar o tempo) para não **enumerar usuários**.
3. Sessão: no servidor (`HttpSession`), o cookie é só o número; JWT: no próprio token (o cliente o carrega). A assinatura garante **integridade e origem**; **não** garante sigilo (qualquer um lê as claims) nem revogação (vale até `exp`). Sessão (revogável, `HttpOnly`) para o **navegador**; JWT (sem cookie, imune a CSRF) para **API** e clientes que não são navegador.
4. O atacante fixa um id de sessão conhecido na vítima e espera o login para herdá-la; o Spring **troca o id** ao autenticar (`changeSessionId`; no laboratório o cookie mudou de `ABD7...` para `1412...`). `HttpOnly`: o JavaScript não lê o cookie (um XSS não o rouba); `Secure`: só por HTTPS; `SameSite=Lax`: o navegador não o envia em requisições entre sites (exceto navegação de topo com método seguro).
5. A **primeira** regra que casa vale: uma regra geral antes da específica a **sombreia em silêncio** (o `USER` levou `200` em `/admin`). Já `anyRequest()` fora do fim falha na subida (`Can't configure requestMatchers after anyRequest`). `@PreAuthorize` não vale: (1) falta `@EnableMethodSecurity` (nenhum erro), (2) chamada **interna** (não passa pelo proxy), (3) método `private`/classe fora do Spring. O `403` vira `500` quando um `@ExceptionHandler(Exception.class)` engole o `AuthorizationDeniedException`; é preciso um tratador mais específico.
6. Parâmetros em toda consulta com dado externo (`?`, `:nome`, métodos derivados) e **lista permitida** para identificadores (`ORDER BY`); menor privilégio; validação só como camada extra. O ORM protege **se** você usar parâmetros (JPQL/nativa concatenadas são injeção). O Thymeleaf escapa em `th:text` e nos atributos, **não** em `th:utext`/`[(...)]`, e **não** barra `javascript:` em `th:href`. A CSP é uma lista do que a página pode carregar/executar: com `script-src 'self'`, um script injetado não roda.
7. A página do atacante faz o navegador da vítima logada enviar um `POST`; como o navegador anexa o cookie sozinho, o servidor o aceita. Defesas: o **token CSRF** por sessão em campo oculto (`th:action` o insere; sem ele, `403`) e o `SameSite=Lax`. A API autentica por cabeçalho `Authorization`, que o navegador não anexa sozinho. O filtro CSRF só confere métodos que mudam estado porque assume `GET` seguro; um `GET` que altera estado seria forjável por uma tag `<img>`.
8. IDOR: acessar um objeto por um id do cliente sem checar que ele é do usuário. `404` não confirma a existência (o `403` deixaria varrer ids); UUID v7 é parcialmente previsível e vaza em log/URL/`Referer`: só a checagem protege. A prova é uma **matriz de testes** (cada rota com `{id}` chamada pelo dono e por outro usuário) e, no desafio, um teste que garante que nenhuma rota nova fica fora da matriz.
9. Um decorador do `AuthenticationProvider` conta erros por e-mail normalizado e, após 5, recusa por 15 min com `429` antes de gastar BCrypt (inclui e-mails inexistentes: sem enumeração); memória limitada a 10 000 chaves. Custo: um atacante pode **trancar a conta da vítima** de propósito (mitigar: bloqueio curto, chave e-mail+IP, CAPTCHA, MFA). NIST SP 800-63B-4: **sem** regras de composição, aceitar senhas longas (≥64), barrar comuns e vazadas. O `toString` de um `record` imprime todos os componentes (`password=...`): sobrescreva-o. *Log injection*: valor do atacante com `\n` forja linhas de log (o `maskedName` da primeira versão deixou passar pelo "domínio" do e-mail; a correção troca caracteres de controle por `?` e limita o tamanho).
10. Não: Git, imagem Docker, log, JavaScript do navegador. Pode: variável de ambiente, arquivo montado (`configtree`), cofre. Vazou: **rotacionar/revogar primeiro**, investigar o uso, só então limpar o histórico (que não desfaz o vazamento); e a aplicação **não sobe** sem o segredo (falha rápida). "Zero registros no OSV" = o OSV não conhece falha publicada para aquela versão, **não** "é seguro" (novas aparecem depois: varredura recorrente). LGPD no código: **necessidade** (coletar o mínimo; tirar `email`/`name` do token se a interface não precisar), **não logar dado pessoal** (`Masking`, ids), mapa de dados em `docs/lgpd-notes.md`. Conflito: eliminação × livro-razão imutável (regra 5): anonimizar o titular e preservar o lançamento; decisão do jurídico e do produto.
</details>

## Parte 2: Entrega prática (tag `payflow-J8`)
O `payflow` troca o login provisório (decisão 24) por **autenticação e autorização de verdade**, fecha as falhas do diagnóstico da J8.01 e ganha uma trilha de auditoria.

```
src/main/java/com/payflow/
├── security/  SecurityConfig, JwtConfig, TokenService, PayflowUserDetails, PayflowUserDetailsService,
│              LoginThrottle, ThrottledAuthenticationProvider, ApiSecurityErrors, SeeOtherRedirectStrategy,
│              SecurityAudit, PasswordPolicy                                                          (J8.02 a J8.07)
├── web/       CurrentUser (Serializable), CurrentUserArgumentResolver, WebConfig (sem interceptor),
│              LoginController (só as páginas), Pages                                                 (J8.03)
├── web/api/   AuthController, AdminAccountsController, dto/LoginRequest, dto/TokenResponse           (J8.03, J8.04)
├── service/   AccountAdminService                                                                    (J8.04)
└── config/    PayflowProperties (Security no lugar de Session)                                       (J8.03)
removidos: AuthInterceptor, Sessions, SessionCookies, BridgeSecurityConfig, payflow.session.*
src/main/resources/ application.yml (server.servlet.session.*), application-dev.yml e -test.yml (segredo de laboratório),
                    templates/login.html (senha + th:action), fragments.html (logout com th:action), db/seed/R__dev_seed.sql ({bcrypt})
src/test/java/com/payflow/  security/AuthenticationTest · web/api/JwtApiTest, Tokens, AdminAccountsControllerWebTest,
                    AccountsControllerWebTest, IdorMatrixTest · security/SecurityAuditTest, PasswordPolicyTest, LoginThrottleTest ·
                    web/TemplateSafetyTest, RequestDtoRulesTest, LoginRequestTest · JpqlInjectionLabTest · AccountAdminServiceTest
                    + ApiClient e PayflowWebTest migrados; TransfersControllerWebTest com a segurança real
scripts/check-secrets.sh   docs/security-notes.md   docs/lgpd-notes.md   pom.xml (perfil security-scan)
```
Novas dependências (todas pelo BOM do Boot 4.1.2, **sem** `<version>`): `spring-boot-starter-security`, `spring-boot-starter-security-oauth2-resource-server`; de teste: `spring-boot-starter-security-oauth2-resource-server-test`. Plugin com `<version>`: `org.owasp:dependency-check-maven` **13.0.0** (perfil `security-scan`, fora do `verify`).

Critérios de pronto:
- [ ] **Login com senha:** `POST /api/v1/auth/token` devolve JWT (15 min, HS256, `iss`, `sub`, `roles`, `jti`) e o painel entra por formulário com senha; **nenhuma** rota depende de e-mail sozinho; `git grep -n "AuthInterceptor\|Sessions\|SessionCookies\|payflow.session" src/main` **vazio**.
- [ ] **Senhas:** `{bcrypt}` com 68 caracteres nas cinco linhas de `users` (`psql`); `AuthenticationTest` (4); o custo medido na **sua** máquina anotado.
- [ ] **Duas cadeias:** `/api/v1/**` stateless (Bearer, sem CSRF, com CORS) e painel com sessão, CSRF e CSP; `/error`, `/forbidden`, `/login`, `/static/**` e `health`/`info` públicos; **todo o resto exige login**.
- [ ] **Sessão:** cookie `PAYFLOW_SESSION` com `HttpOnly`, `SameSite=Lax` e `Secure` (produção); id **diferente** antes e depois do login; logout por `POST` com `303` e cookie apagado.
- [ ] **Papéis:** só `ADMIN` bloqueia/desbloqueia contas (`401`/`403`/`200` conferidos por `curl`); **cada barreira** (cadeia e `@PreAuthorize`) testada **sozinha**; `403` nunca `500`.
- [ ] **Injeção e XSS:** os três `git grep` da J8.05 sem consulta concatenada; `JpqlInjectionLabTest` (2); `TemplateSafetyTest` (2) verde e **vermelho** quando quebrado; `Content-Security-Policy` e os demais cabeçalhos presentes; console do navegador sem violações de CSP (manual).
- [ ] **CSRF:** `POST` sem token → `403` e saldo intacto (`PayflowWebTest`); experimento com a página de ataque (com e sem proteção) registrado.
- [ ] **IDOR:** `IdorMatrixTest` (4) verde; conta/transferência alheias respondem `404`; origem alheia em transferência responde `422`.
- [ ] **Autenticação sob ataque:** `401` ×5 e `429` no `curl` em laço; mesma resposta para e-mail inexistente e senha errada; cinco tokens ruins (adulterado, expirado, emissor errado, chave errada, `alg: none`) → `401`.
- [ ] **Auditoria:** `SecurityAudit` registra sucesso (id) e falha (e-mail mascarado + motivo); `SecurityAuditTest` (5) incluindo a tentativa de log forjado; `LoginRequest.toString` sem senha.
- [ ] **Segredos:** `scripts/check-secrets.sh` limpo; a aplicação **não sobe** sem `PAYFLOW_SECURITY_JWT_SECRET`; nenhum segredo real no Git.
- [ ] **Dependências e LGPD:** `osv-check.sh` das principais bibliotecas anotado com a data; perfil `security-scan` no `pom.xml`; `docs/lgpd-notes.md` com o mapa de dados; nenhum log do PayFlow tem e-mail ou nome.
- [ ] **64 métodos de teste novos** (**esperado, contado por leitura; não executado**): `AuthenticationTest` 4, `JwtApiTest` 14 (os 15 do laboratório, sem o do `ReportsController`), `AdminAccountsControllerWebTest` 5, `AccountsControllerWebTest` 3, `SecurityAuditTest` 5, `PasswordPolicyTest` 9, `LoginThrottleTest` 4, `PayflowPropertiesTest` (segurança) 4, `TemplateSafetyTest` 2, `RequestDtoRulesTest` 2, `IdorMatrixTest` 4, `JpqlInjectionLabTest` 2, `LoginRequestTest` 2, `AccountAdminServiceTest` 3, `depositWithoutCsrfTokenIsForbiddenAndChangesNothing` 1; mais os testes antigos migrados (`ApiClient`, `PayflowWebTest`, `TransfersControllerWebTest`).
- [ ] **Pendências herdadas** registradas em `docs/security-notes.md`: a J7.03 afirma que o `@ConfigurationPropertiesScan` vale na fatia (**não vale**, testado no laboratório); `ActuatorTest` (J5.15) espera `404` para os endpoints não expostos, mas agora o anônimo recebe o redirecionamento de login **antes**: ajuste a expectativa (ou autentique como `ADMIN`).
- [ ] Commits em Conventional Commits, PR mesclada e tag:
```bash
git tag payflow-J8
git push origin payflow-J8
```

## Parte 3: Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Explico autenticação × autorização, `401` × `403` e a cadeia de filtros do Spring Security | ☐ | ☐ | ☐ |
| Guardo senhas com BCrypt/`DelegatingPasswordEncoder` e implemento `UserDetailsService` | ☐ | ☐ | ☐ |
| Escolho sessão ou JWT conforme o cliente e emito/valido um JWT com HS256 | ☐ | ☐ | ☐ |
| Configuro papéis (`hasRole`, `@PreAuthorize`) na ordem certa e testo cada barreira sozinha | ☐ | ☐ | ☐ |
| Previno SQL injection e XSS (parâmetros, lista permitida, escape na saída, CSP) | ☐ | ☐ | ☐ |
| Explico e previno CSRF (token, `SameSite`) e sei quando desligá-lo | ☐ | ☐ | ☐ |
| Previno IDOR e escrevo uma matriz de testes de autorização | ☐ | ☐ | ☐ |
| Limito tentativas de login, evito enumeração e registro eventos de segurança sem vazar dados | ☐ | ☐ | ☐ |
| Gerencio segredos, limito entradas e rodo uma varredura de dependências | ☐ | ☐ | ☐ |
| Mapeio dados pessoais à LGPD e decido o que logar e guardar | ☐ | ☐ | ☐ |

**Regra:** com mais de duas linhas em 0, repita as aulas correspondentes antes do [J9](../J9-docker/README.md). Atualize `00-guia/progresso.md`.

## Próximo
[J9: Docker essencial](../J9-docker/README.md)
