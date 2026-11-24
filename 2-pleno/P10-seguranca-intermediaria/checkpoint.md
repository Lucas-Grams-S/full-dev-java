# P10.CP — Checkpoint do módulo P10

> ⏱️ ~1h30 · 🔵 Pleno · Pré-requisitos: P10.01 a P10.07

> ✅ **Validação final (2026-11-24):** módulo executado no PayFlow reconstruído; resultado e o que continua não executado no [README do módulo](README.md). Os avisos "não executado" / "por leitura" abaixo descrevem a escrita original.

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** nesta geração (Linux, Docker 29.9.2, JDK 25.0.5, Spring Boot 4.1.2 / Spring Security 7.1.2, Keycloak 26.8.2, PostgreSQL 18, Trivy 0.76.2, cosign 3.1.4, CycloneDX Maven plugin 2.9.1): (1) o **Resource Server** em `lab/identity-lab` com **54 testes verdes** — **23** sem Keycloak (chaves RSA de teste + JWKS local) e **31** contra o **Keycloak** e o **PostgreSQL** reais (`out-suite.txt`); (2) **8 mutantes** das regras de segurança (**6 reprovados, 2 sobreviveram**, ambos explicados: `out-mutants.txt`); (3) o **fluxo Authorization Code + PKCE** inteiro, o **refresh com rotação**, o **bloqueio por tentativas** e a **política de senha** do realm; (4) o **Client Credentials** pelo Spring (token **reutilizado**); (5) a **rotação de credenciais do banco** (usuário único × dois alternados × ordem errada) e o **menor privilégio**; (6) a **API com o Keycloak indisponível** (`out-outage.txt`); (7) **SBOM** (Maven e imagem), *scan* com **falha plantada**, **assinatura**, **tag trocada**, **portão de *deploy*** e **atestação** (`lab/supply-chain-lab/out-*.txt`) e a **reprodutibilidade do JAR**; (8) o **modelo de ameaças** (26 ameaças) e o **verificador** com `--self-test`. **Não executado:** o **PayFlow do aluno** (revisado por leitura), o painel com `oauth2Login`, o Keycloak **em produção** (HTTPS, banco externo, alta disponibilidade), a **migração das senhas** da J8, a **troca de token** e o **mTLS**, **KMS**, o **Secrets Manager com Lambda de rotação**, o **ECS/AWS real**, a assinatura **sem chave** (*keyless*) e uma **sessão de *threat modeling* com pessoas**. O laboratório faz **os dois lados** (cliente e servidor) numa aplicação só. **Os testes práticos acontecem no fim do treinamento.** A **lista "a validar no fim do treinamento"** está em `00-guia/progresso.md`.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Quem são os **quatro papéis** do OAuth 2.0 e **qual deles é o PayFlow** quando atende o app e quando chama o serviço de notificações? Qual a diferença entre **access**, **ID** e **refresh token**, e por que o ID token **não** vai no `Authorization`?
2. Percorra o **Authorization Code + PKCE** e diga **qual ataque** cada um de `code_challenge`, `redirect_uri`, `state`, `nonce` e `iss` impede. O que os testes mostraram para o **código roubado** e para o **`redirect_uri` do atacante**?
3. Na importação do realm, declarar `clientScopes` no JSON **tirou** o `sub` e os papéis do token. **Por quê**, como o PayFlow **se protege** desse tipo de erro e como as **claims** do usuário de teste ficaram idênticas aos ids do PayFlow?
4. O que o Spring confere num JWT **por padrão**, o que **você** acrescenta e como o **escopo** e o **papel** viram autoridades? Quando a resposta é **401**, **403** ou **404**? Que **surpresa** o `WWW-Authenticate` trouxe no Spring Security 7?
5. Dos **8 mutantes**, **2 sobreviveram**. Quais, **por que** sobreviveram e o que cada um **ensina**?
6. O usuário fez **logout** e a API **continuou** aceitando o *access token*. **Por quê**, qual a **decisão** do PayFlow e qual o **trade-off** da alternativa (introspecção)? E o que a API faz quando o **Keycloak está fora do ar**?
7. Por que **um cliente por serviço** no Keycloak? Por que o `/internal` confere **escopo e `azp`**? O que um **cliente `reporting`** com token válido **recebe**, e **como** o PayFlow se autentica **na AWS**?
8. Mostre, **com os resultados**, por que trocar a senha do **usuário único** quebra **depois**, como a rotação por **dois usuários alternados** evita isso, **qual a ordem** e **o que a "ordem errada" prova**. O que o **menor privilégio** impôs ao livro-razão?
9. Qual a diferença entre **SBOM do Maven** e **SBOM da imagem**? Por que **plantar uma falha** antes de confiar no `0`? O que **aconteceu** com a **tag trocada** e o que fazem os **três códigos de saída** do portão? Por que o digest **muda** entre *builds* e o que resolve?
10. Explique o **STRIDE por elemento**, para que serve o **verificador** do modelo (e o que o `--self-test` prova), a diferença entre **aceitar** e **ignorar** um risco e o que as **16 lacunas** significam.

<details><summary>Gabarito</summary>

1. **Dono do recurso** (Alice), **cliente** (o app), **servidor de autorização** (Keycloak), **servidor de recursos** (a API). O PayFlow é **servidor de recursos** para o app (valida o token) e **cliente** quando chama o serviço de notificações (pede um token de serviço). **Access:** escrito para a **API** (`aud=payflow-api`), curto, vai no `Authorization`. **ID:** escrito para o **app** (`aud=payflow-web`), diz **quem logou**. **Refresh:** só para o **Keycloak**, renova o access (com **rotação**). O ID token é um JWT **assinado pela mesma chave** — a assinatura confere —, mas **`typ=ID`** e **`aud` do cliente**: a API recusa (**`The aud claim is not valid`**).
2. O cliente gera `code_verifier` e envia `code_challenge = SHA256(verifier)`; o login acontece **no Keycloak**; o código volta pelo navegador; o cliente o troca com o **verifier**. **`code_challenge`**: **código roubado** é inútil (teste: `400 invalid_grant — PKCE verification failed: Code mismatch`). **`redirect_uri` exato**: o código **não vai** para o atacante (teste: `redirect_uri=http://evil.example/callback` → **`400` e nenhuma página de login**). **`state`**: o **cliente** descarta o código de **outro fluxo** (CSRF). **`nonce`**: o ID token **antigo** não é reaproveitado (o `nonce` voltou igual). **`iss`** (RFC 9207): o cliente sabe **qual servidor** respondeu. O código também é **de uso único** (`Code not valid` na segunda troca).
3. Um realm importado que **declara** `clientScopes` **não cria** os escopos **embutidos** (`basic`, `roles`, `profile`, `email`), que carregam os *mappers* de `sub` e papéis; o cliente os listava, mas eles **não existiam**: o token veio **sem `sub`, sem `realm_access`, sem e-mail**. Correção: o JSON declara só realm, clientes, papéis e usuários; os **escopos próprios** nascem pela **Admin REST API** (`kc-setup.py`, idempotente). Proteção: **pedir o token e conferir as claims** (`KeycloakRealmIT`), não confiar no JSON. O usuário importado com `"id"` **mantém esse id como `sub`** (medido: `sub=11111111-1111-4111-8111-111111111111`), por isso `users.id = sub`.
4. **Padrão:** assinatura (JWKS, RS256) e **tempo** (`exp`/`nbf`, tolerância de relógio — 30 s passa, 90 s não); com `issuer-uri`, o **emissor**. **Acrescento:** **`aud`** (`audiences: payflow-api`), **`typ=Bearer`** (`TokenTypeValidator`), o **conversor** (`scope` → `SCOPE_x`, `realm_access.roles` → `ROLE_x`) e o **dono** (`sub`) no caso de uso. **401**: não autenticado/token inválido (`invalid_token`); **403**: autenticado sem escopo/papel (`insufficient_scope`); **404**: objeto alheio (IDOR). **Surpresa:** o `401` sem token traz `Bearer resource_metadata="…/.well-known/oauth-protected-resource"` (**RFC 9728**), não `Bearer` puro — **compare com `startsWith`**.
5. **M2** (sem `jws-algorithms: RS256`): o **padrão** do decodificador por `jwk-set-uri` **já é RS256**; a linha **documenta**, não é o que segura a confusão de algoritmo. **M4a** (regra da cadeia frouxa, fica o `@PreAuthorize`): **defesa em profundidade** — o `@PreAuthorize` sozinho barra o usuário; o **M4b** (as duas barreiras fora) reprova. **Lição:** sobrevivente **não é bug**: é pergunta "**este controle é redundante ou o teste é fraco?**"; e **cada barreira deve ser testada sozinha** (decisão 60).
6. O JWT é **autocontido**: a API só confere **assinatura e prazo**, **não pergunta** ao Keycloak. Medido: `logout=204`, `refresh-after-logout=400 Session not active`, **introspecção `active:false`**, **API `200`**. **Decisão (ADR 0022):** *access token* de **5 min** + refresh revogado; o intervalo é **risco aceito (T07)**. **Introspecção:** revogação imediata, mas **uma ida ao Keycloak por requisição** (latência e disponibilidade dele entram em toda chamada) — só nos endpoints de risco. **Keycloak fora:** com o JWKS **em cache**, tokens válidos **seguem** (`200`); se a API **nunca** resolveu o emissor, **qualquer requisição com token** dá **`403` vazio** + `JwtDecoderInitializationException` no log (**deveria ser `503`**: T21, **aberta**); ao voltar, a API **se recupera sozinha**.
7. **Um cliente por serviço:** revogação, **auditoria** (`azp` no log) e **menor escopo** por serviço; um cliente compartilhado faria o comprometimento do relatório abrir as notificações. **Escopo e `azp`:** **defesa em profundidade** — um erro do realm (dar `internal:notify` a outro cliente) **não** abre o `/internal`; o **M5** provou o teste. O `reporting` **obtém** um token válido e **recebe `403 insufficient_scope`**. **AWS:** por **IAM** (a ***task role***, credenciais **temporárias**), **não OAuth**.
8. **Usuário único:** depois do `ALTER ROLE … PASSWORD`, a conexão **aberta continua** (`still works`) e a **primeira conexão nova** falha (`failed after 1502 ms`, o `connectionTimeout`) — **horas depois**, longe da causa. **Alternados:** (1) altera o **inativo**, (2) o pool passa a usá-lo (`HikariConfigMXBean`), (3) **só depois** invalida o antigo: **194 consultas, 0 falhas**, conexões novas como `app_b`, credencial antiga **recusada**. **Ordem errada** (invalidar antes): **`failed>0`** (54 ok, 1 falha): **prova que a medição enxerga falhas**, então o `0` **significa** algo. **Menor privilégio:** a aplicação **não faz DDL** e **não faz `UPDATE`/`DELETE`/`TRUNCATE` em `ledger_entries`**: a **regra 5** passa a ser imposta **pelo banco**; a conta de migração é **outra**.
9. **Maven:** o que o `pom` resolveu (**64** bibliotecas, CycloneDX 1.6); **imagem:** **tudo** (**177** componentes: SO, bibliotecas dentro do JAR, binários; CycloneDX 1.7) — o achado de **2 falhas corrigíveis** estava **no SO da base**, que o SBOM do Maven **nem lista**. **Plantar** `log4j-core 2.14.1`: **2 CRITICAL** (CVE-2021-44228 e CVE-2021-45046), `exit=1` — só assim o `0` anterior **vale**. **Tag trocada:** verificar **pela tag** → `no signatures found`; o **digest original** **continuou verificando**. **Portão:** `0` (digest assinado), `1` (sem assinatura válida), `2` (**referência por tag**, recusada antes). **Digest muda** porque o **JAR** tem *timestamps*: com `project.build.outputTimestamp` **dois builds dão o mesmo SHA-256** (só o **JAR**, na mesma máquina; **imagem não testada**).
10. **Processo:** STRIDE (S T R I D E); **armazenamento:** **TRID**; **fluxo:** **TID**; **entidade externa:** **SR**. O **verificador** garante que **toda "mitigada" aponta para evidência que existe** (teste/arquivo/saída) e que **aceita/aberta** tem **dono e data não vencida**; o `--self-test` **renomeia** um teste e **avança a data**, e **ambos são acusados** (o verificador **consegue falhar**). **Aceitar** é decisão **registrada** (dono, motivo, revisão); **ignorar** é **não saber**. As **16 lacunas** são **letras aplicáveis sem ameaça registrada** — "**ninguém perguntou**", **não** "não há ameaça".
</details>

## Parte 2: Entrega prática (tag `payflow-P10`)
O `payflow` passa a **autenticar com o Keycloak**: a API vira **Resource Server** (RS256, `aud`, `typ`, escopos e papéis), os serviços se autenticam por **Client Credentials**, a **senha do banco** é rotacionada por **dois usuários alternados** com **menor privilégio**, a **imagem** sai com **SBOM**, **assinatura** e **portão de *deploy***, e o sistema ganha um **modelo de ameaças verificado**. **Sem** mudar o contrato HTTP `/api/v1`, a lógica de **dinheiro** (uma transação) nem a regra de que o **dono vem do `sub`**.
```
payflow/
├── src/main/java/com/payflow/security/
│   ├── SecurityConfig.java              apiChain (escopo+papel) · internalChain (escopo+azp) · deny-rest        (P10.03–04)
│   ├── KeycloakAuthoritiesConverter.java   scope → SCOPE_x · realm_access.roles → ROLE_x                       (P10.03)
│   ├── TokenTypeValidator.java          só typ=Bearer                                                          (P10.03)
│   └── ApiSecurityErrors.java           MANTIDO (Problem Details) · SAEM: TokenService, JwtConfig, LoginThrottle, PasswordPolicy
├── src/main/java/com/payflow/web/CurrentUser.java      id = sub · e-mail e nome pelas claims                    (P10.03)
├── src/main/java/com/payflow/client/{ServiceClientConfig,InternalApiClient}.java   client_credentials             (P10.04)
├── src/main/resources/db/migration/V12__users_password_hash_nullable.sql   expandir (a V13 contrai depois)        (P10.03)
├── src/test/java/…/{ResourceServerTest, KeycloakRealmIT, AuthorizationCodePkceIT, RealmPolicyIT, …}              (P10.01–04)
├── deploy/keycloak/{payflow-realm.json, kc-setup.py, kc-up.sh}   configuração do IdP COMO CÓDIGO, idempotente    (P10.02)
├── scripts/ops/rotate-db-user.sh        roteiro dos 6 passos da rotação (staging)                              (P10.05)
├── scripts/ci/verify-before-deploy.sh   portão: digest + assinatura (chamado pelo ecs-deploy.sh)               (P10.06)
├── .github/workflows/release.yml        + SBOM da imagem, cosign sign/attest do DIGEST                          (P10.06)
├── docs/
│   ├── identity.md                      papéis OAuth, claims usadas e para quê, o que mora no token             (P10.01)
│   ├── resource-server.md               tabela claim → conferência → teste; migração da J8                      (P10.03)
│   ├── service-auth.md                  matriz de chamadas (quem chama quem, mecanismo, escopo, onde mora o segredo) (P10.04)
│   ├── secrets.md                       inventário, janela de coexistência, exceção da decisão 169 FECHADA      (P10.05)
│   ├── supply-chain.md                  SBOM por release, gate, chave × keyless, build reprodutível            (P10.06)
│   └── threat-model/{threats.csv, check-threat-model.py}   modelo de ameaças + verificador no CI              (P10.07)
└── projeto/adrs/0022-seguranca-intermediaria-do-payflow.md   (já redigido nesta geração)
```
Critérios de pronto:
- [ ] **Contrato HTTP `/api/v1`** inalterado; os testes de J5–P9 **continuam verdes** (os da J8 que usavam `TokenService`/`jwt()` migrados); **uma migração nova (V12)**, **só** `DROP NOT NULL` (a próxima livre passa a ser **V13**).
- [ ] A API **recusa**, com **um teste cada**: **expirado, outro emissor, outra audiência, ID token, `alg=none`, HS256 com a chave pública, assinatura de outra chave, `sub` adulterado**; e **aceita** a **rotação de chave** do JWKS **sem reiniciar**.
- [ ] `transfers:write` só para criar transferência; `GET` exige `transfers:read`; `/admin/**` com **duas barreiras**, **cada uma testada sozinha**; o **dono vem do `sub`** (teste do Bob lendo a conta da Alice → 404).
- [ ] **8 mutantes** rodados; os **dois sobreviventes** **explicados** em `docs/resource-server.md` (ou **mortos** com testes novos).
- [ ] **`/internal`** exige **escopo e `azp`**; **um cliente por serviço**; o relé do Outbox usa o gerenciador **de serviço**; **nenhum `client_secret` no Git**.
- [ ] A API **responde `503`** (não `403` vazio) quando o decodificador **não resolve o emissor**; há **alarme** para essa taxa.
- [ ] **Conta da aplicação sem DDL e sem `UPDATE`/`DELETE`/`TRUNCATE` em `ledger_entries`** (teste que **tenta e falha**); rotação **com carga: 0 falhas**; **ordem errada: > 0**; **janela calculada** com os valores reais.
- [ ] **`PAYFLOW_SECURITY_JWT_SECRET` removido** (config, Compose, *task definition*) e a **exceção da decisão 169 riscada**.
- [ ] SBOM **da imagem arquivado por release**; **`trivy sbom` reprova** a falha plantada; o portão **recusa tag**, **recusa sem assinatura** e **aceita assinado**; **digest implantado = digest escaneado e assinado**; `project.build.outputTimestamp` configurado.
- [ ] `threats.csv` com **dono e data** nos aceitos/abertos; verificador **no CI**; **lacunas de `supply-chain` e `internal` reduzidas**; **T21 fechada ou com data**.
- [ ] `docs/*.md` escritos; **ADR 0022** revisado; tag `payflow-P10`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Pleno: **≥ 2** em todos e **3** nos marcados com ⭐.

| Competência | Aula(s) | 0–3 |
|---|---|---|
| **OAuth 2.0/OIDC**: papéis, tokens (access/ID/refresh), **Authorization Code + PKCE** e o que cada parâmetro impede | P10.01 | ⭐ |
| **Escopo × papel × audiência × dono** (quatro perguntas diferentes) | P10.01, P10.03 | ⭐ |
| **Keycloak**: realm, clientes (público × confidencial), escopos padrão × opcionais, *mappers*, configuração **como código** | P10.02 | ⭐ |
| **Ler as claims que o servidor emite** (e não confiar no JSON de configuração) | P10.02 | |
| **Resource Server**: validadores (`iss`, `aud`, `typ`, algoritmo, tempo), conversor de autoridades, **401 × 403 × 404** | P10.03 | ⭐ |
| **Testar a validação de tokens** (chaves de teste, JWKS local) **e provar com mutantes** | P10.03 | ⭐ |
| **JWT autocontido × revogação**; rotação de chave; Keycloak indisponível | P10.03 | |
| **Client Credentials** (cliente e servidor), **um cliente por serviço**, escopo + `azp`; **IAM** na AWS | P10.04 | ⭐ |
| **Rotação de credenciais do banco**: usuário único × alternados, **ordem**, **janela de coexistência** | P10.05 | ⭐ |
| **Menor privilégio no banco** (aplicação ≠ migração ≠ mestra) e **eliminar** segredos | P10.05 | ⭐ |
| **SBOM** (Maven × imagem), *scan* **com prova de que enxerga**, **assinatura por digest**, **portão de *deploy*** | P10.06 | ⭐ |
| **Build reprodutível**, atestação e *keyless* (conceito) | P10.06 | |
| **Threat modeling**: DFD, **STRIDE por elemento**, tratamento e **dono/data**; modelo **verificável** | P10.07 | ⭐ |

**Se algum ⭐ ficou abaixo de 2:** refaça `lab/identity-lab` (`run-all.sh`) lendo cada saída; **escreva do zero** um `TestTokens` e três testes de token inválido; reproduza a rotação (`SecretRotationIT`) **trocando a ordem**; **plante uma falha** num SBOM e **troque uma tag** no registro local antes de seguir para o Checkpoint de nível Pleno.

## Parte 4: Perguntas de entrevista (simulado rápido)
Responda em voz alta, 2 minutos cada: (1) "Explique o Authorization Code com PKCE e o que o PKCE impede." (2) "Como sua API valida um JWT? O que você confere além da assinatura?" (3) "O usuário fez logout e o token ainda funciona: é bug?" (4) "Como um serviço chama outro com segurança? Chave de API serve?" (5) "Como rotacionar a senha do banco sem derrubar a aplicação?" (6) "Um desenvolvedor commitou uma chave: o que você faz?" (7) "O que é um SBOM e por que implantar por digest?" (8) "Faça um *threat model* de um endpoint de estorno." (9) "Keycloak, Cognito ou construir o próprio servidor?" (10) "Como saber que seu modelo de ameaças não é um papel de gaveta?"

## ➡️ Próximo
[✅ Checkpoint de nível Pleno](../checkpoint-pleno.md): a **revisão do nível** (links, ordem de pré-requisitos, soma das horas e a tabela **competência da matriz → aulas**), o **incidente simulado** (logs, métricas e traços), o quiz por módulo e o simulado de entrevista pleno. **Não comece antes de pedir.**
