# Checkpoint de nível Júnior

> ⏱️ ~9h (6 sessões de 1h30: ver `00-guia/cronograma.md`, "CK-J.1" a "CK-J.6") · 🟢 Júnior · Pré-requisitos: J0 a J10 (todos os módulos do nível)

> ⚠️ **Estado de verificação:** por decisão do aluno, os **testes práticos do PayFlow ficam para o fim do treinamento**. Esta execução foi uma **revisão por leitura e por varredura automática** (links, ordem de pré-requisitos, horas, cobertura da matriz). **Não** foi compilado o PayFlow do aluno nem rodado o desafio técnico abaixo: ele é um **roteiro de avaliação**, não uma solução testada. O que cada módulo executou de verdade está no registro do módulo em `00-guia/progresso.md` e no aviso de cada `checkpoint.md`.

## 🎯 Para que serve
Você terminou 11 módulos (99 aulas, 165 h). Este checkpoint responde a uma pergunta só: **estou pronto para trabalhar como dev Java júnior em um time e para começar o nível Pleno?** Ele tem quatro partes, em ordem:

| Parte | O que é | Sessões | Como é avaliada |
|---|---|---|---|
| 1 | Revisão de entrada: **o que a revisão do nível encontrou** (leia antes de começar) | — | leitura |
| 2 | **Projeto PayFlow** avaliado por checklist (tag `payflow-J10`) | 1 | autoavaliação com evidência |
| 3 | **Quiz** por módulo (J0–J10) + **quiz integrador** de 20 questões | 2 | gabarito |
| 4 | **Simulado de entrevista júnior** (20 perguntas, 40 min) | 1 | respostas-modelo |
| 5 | **Desafio técnico (take-home) com tempo limite de 4 h** | 2 | rubrica de 100 pontos |
| 6 | **Autoavaliação pela matriz de competências** e decisão de avançar | — | tabela |

---

## Parte 1: O que a revisão do nível encontrou

Revisão feita em 2026-11-13 sobre `1-junior/` (99 aulas, 11 `README.md`, 11 `checkpoint.md`).

| Verificação | Resultado |
|---|---|
| **Links internos** (links relativos entre arquivos `.md`) em `1-junior`, `00-guia`, `projeto`, `recursos` e no `README.md` | ✅ **0 quebrados** |
| **Links externos** (440 URLs únicas, `curl` com redirecionamento) | ✅ **434 responderam 200**. Os 6 restantes **não são links de estudo**: o *namespace* XML `maven.apache.org/POM/4.0.0` (404 de propósito), o emissor OIDC `token.actions.githubusercontent.com` (404 na raiz), o IMDS `169.254.169.254` (só existe dentro da EC2), o `wrong.host.badssl.com` (certificado errado de propósito, J2.03), `docs.payflow.example` (exemplo fictício) e um `https://gist` em texto de exemplo |
| **Pré-requisitos no cabeçalho** (99 aulas) | ✅ **nenhuma aula aponta para uma aula posterior ou inexistente**; nenhuma aula do Júnior tem P/S como pré-requisito |
| **Horas** | ✅ 99 aulas de ~1h30 + 11 checkpoints de módulo = **110 sessões × 1h30 = 165 h**; mais as 6 sessões deste checkpoint (9 h) = **174 h**, o número do `README.md` raiz. O cronograma lista as 110 sessões de módulo e as 6 partes do checkpoint nas semanas 1 a 24 |
| **Conceito usado antes de ensinado** (varredura por termo em código e prosa) | ⚠️ **um problema real, já registrado**: J1.05–J1.07 usam `Map`/`HashMap`, `BigDecimal` e `Clock` antes de J1.11/J1.15 (Correções pendentes, "J1 (ordem de pré-requisitos)"); e J1.07 usa **lambda** antes da J1.13, com uma linha avisando "detalhes em J1.13". Todos os outros usos antecipados (por exemplo `@Controller` em J4.06, `@RestController` em J2.06, `Testcontainers` em J7.01, `Dockerfile` em J8.07) são **menções sinalizadas** ("Nada de Spring ainda", "veja J7.04", tabelas "hoje → futuro"), não uso em código que o aluno precise executar |
| **Consistência de nomes** (`User`, `Account`, `Transfer`, `LedgerEntry`, `com.payflow`) | ✅ iguais em todas as aulas lidas pelos *greps*; as divergências conhecidas estão em "Correções pendentes" (a `Transfer.complete()` da J5) |
| **Competências da matriz sem aula** | ⚠️ **uma**: "Mensageria e assincronia: entende fila vs síncrono" (ver tabela da Parte 6). Registrada em Correções pendentes |

### O que você precisa fazer **antes** dos testes práticos finais
Estas pendências estão em `00-guia/progresso.md`, "Correções pendentes", e **não foram corrigidas por conta própria** (regra do treinamento). As que mais afetam um aluno que for compilar o PayFlow:

1. **`Transfer.complete()`** não existe nas aulas da J5 e é chamado por `TransferService.execute` (J5.11, J7). Sem ela o PayFlow **não compila**. Correção sugerida: `public void complete() { this.status = TransferStatus.COMPLETED; }`.
2. **`@WebMvcTest` não enxerga `@ConfigurationPropertiesScan`** (J7.03): use `@EnableConfigurationProperties(PayflowProperties.class)` no teste (a J8.04 já faz isso).
3. **`ActuatorTest` da J5.15** espera `404`; com a J8 o anônimo recebe `302` para o login.
4. **Ambiente:** o `java` padrão da máquina pode ser o 17; o `mvn` das aulas pede `JAVA_HOME` do JDK 25. Containers de teste (`payflow-pg`) devem ser recriados pelas aulas.
5. As listas **"a validar no fim do treinamento"** (J7, J8, J9, J10) viram o **roteiro dos testes práticos finais**.

---

## Parte 2: Projeto PayFlow avaliado por checklist

**Entrega:** o repositório `payflow` na tag **`payflow-J10`** (o Júnior não tem tag própria: o PayFlow do aluno **é** a tag da J10). Marque `[x]` só com **evidência** (comando e saída, captura de tela ou link do commit). "Parece que funciona" não vale.

### 2.1 Build, versões e repositório
- [ ] `./mvnw verify` termina com `BUILD SUCCESS` em uma máquina **limpa** (clone novo), com JDK 25 e Docker no ar. Evidência: saída do comando.
- [ ] O `pom.xml` usa `spring-boot-starter-parent` **4.1.x** e **não** repete versões das dependências do Spring (J5.02).
- [ ] `git log --oneline` mostra commits em **Conventional Commits** e há uma tag `payflow-J0` … `payflow-J10` por módulo (`git tag -l 'payflow-*'`).
- [ ] Nenhum segredo no repositório: `scripts/ci/check-secrets.sh` passa e o `.gitignore` cobre `secrets/`, `.env` e `target/` (J8.07, J10.02).

### 2.2 Dados (J3, J5)
- [ ] O esquema vem **só** das migrações Flyway (`db/migration`, `V1`…): `ddl-auto: validate`; apagar o banco e subir a aplicação recria tudo.
- [ ] Quatro tabelas: `users`, `accounts`, `transfers`, `ledger_entries`, com PK `uuid` (`uuidv7()`), FKs, `UNIQUE (email)`, `UNIQUE (idempotency_key)` e `NUMERIC(19,2)` para dinheiro.
- [ ] Você sabe mostrar, com `EXPLAIN`, uma consulta do extrato **usando índice** e explicar por que (J3.09).
- [ ] O extrato **não** tem N+1: o log do Hibernate mostra o número esperado de consultas (J5.12).

### 2.3 Regras de negócio e API (J5, J6)
- [ ] Dinheiro **sempre** `BigDecimal`, escala 2, `HALF_EVEN`; em JSON, **string** (`"110.00"`).
- [ ] `POST /api/v1/transfers` é **atômica** (débito, crédito, lançamentos e `Transfer` na mesma transação) e **idempotente** (`Idempotency-Key`: repetir devolve a mesma transferência, não duplica).
- [ ] Erros em **Problem Details** (RFC 9457): 400 entrada inválida, 404 não encontrado, 409 conflito, 422 regra de negócio (saldo insuficiente).
- [ ] Paginação estável (ordenação com desempate por `id`), filtros e OpenAPI publicados pelo springdoc.
- [ ] CORS só para as origens configuradas e `RestClient` com **timeouts** de conexão e de leitura.

### 2.4 Testes e qualidade (J7)
- [ ] Os testes cobrem as quatro camadas: **unidade** (sem Spring), **fatia web** (`@WebMvcTest`), **fatia JPA** (`@DataJpaTest`) e **integração** com **PostgreSQL real** (Testcontainers).
- [ ] Existe pelo menos um teste que **falha** quando você quebra a regra (prove: comente a validação de saldo e veja o teste vermelho).
- [ ] JaCoCo gera o relatório e o `verify` falha abaixo do piso **medido** no seu projeto (não o `0.70` de exemplo).
- [ ] Checkstyle e SpotBugs rodam no `verify`; toda supressão tem um comentário com o motivo.

### 2.5 Segurança (J8)
- [ ] Senhas com **BCrypt** (`{bcrypt}`); nunca em log, em `toString` ou em resposta.
- [ ] **Duas cadeias** de segurança: painel com sessão + CSRF + CSP; API com **JWT** (HS256, 15 min). Anônimo na API recebe **401**; autenticado sem permissão recebe **403**.
- [ ] **IDOR:** o usuário A não lê nem movimenta a conta do usuário B (há teste que prova, em cada rota com `{id}`).
- [ ] Papéis `USER` e `ADMIN`: só `ADMIN` bloqueia/desbloqueia contas (`@PreAuthorize`).
- [ ] Segredos por **variável de ambiente ou arquivo**, nunca no `application.yml` versionado; Dependency-Check configurado.

### 2.6 Container, CI e nuvem (J9, J10)
- [ ] `docker compose up --build --wait`: app e banco `healthy`; o banco **não** publica a porta 5432; a aplicação roda como **usuário não root** (uid 10001).
- [ ] O `Dockerfile` é multi-stage, usa cache de camadas e **não** copia `target/`, `.git` nem `secrets/` (`.dockerignore`).
- [ ] O `ci.yml` roda `./mvnw verify` a cada PR; *actions* fixadas por SHA, `permissions` mínimas, sem `pull_request_target`; a `main` está protegida por *ruleset* (PR obrigatória + CI verde).
- [ ] 💸 **Só se você fez a parte AWS:** alerta de custo criado **antes** de qualquer recurso, MFA ativo, nada com `0.0.0.0/0` na entrada, e **`91-verify-empty.sh` provando que não ficou nada** depois do `90-teardown.sh`. Se não fez, escreva "não executado" ao lado: é honesto e aceitável.

### 2.7 Documentação
- [ ] `README.md` do `payflow` com: o que é, como rodar (3 comandos), como testar, decisões (ADRs 0001–0012) e **o que você aprendeu/errou**.
- [ ] `docs/ci-notes.md`, `cloud-notes.md` e `deploy-notes.md` existem e batem com o que o repositório faz.

**Nota do projeto:** conte os itens marcados com evidência.
- **≥ 80%** ✅ pronto · **60–79%** ⚠️ pronto com plano de reforço (liste os itens abertos e a aula que os cobre) · **< 60%** ❌ refaça os módulos indicados antes de seguir.

---

## Parte 3: Quiz

### 3.1 Quiz por módulo
Cada módulo tem o seu quiz de 10 questões com gabarito no `checkpoint.md` dele. Refaça **sem consultar** e anote a nota. Se alguma ficar abaixo de 7, releia as aulas da coluna da direita.

| Módulo | Quiz | Nota (0–10) | Se < 7, releia |
|---|---|---|---|
| J0 | [`J0-ambiente-ferramentas/checkpoint.md`](J0-ambiente-ferramentas/checkpoint.md) | ___ | J0.01–J0.06 |
| J1 | [`J1-java-logica/checkpoint.md`](J1-java-logica/checkpoint.md) | ___ | J1.01–J1.16 |
| J2 | [`J2-como-a-web-funciona/checkpoint.md`](J2-como-a-web-funciona/checkpoint.md) | ___ | J2.01–J2.06 |
| J3 | [`J3-banco-de-dados/checkpoint.md`](J3-banco-de-dados/checkpoint.md) | ___ | J3.01–J3.12 |
| J4 | [`J4-front-end-essencial/checkpoint.md`](J4-front-end-essencial/checkpoint.md) | ___ | J4.01–J4.08 |
| J5 | [`J5-spring-boot/checkpoint.md`](J5-spring-boot/checkpoint.md) | ___ | J5.01–J5.15 |
| J6 | [`J6-apis-rest/checkpoint.md`](J6-apis-rest/checkpoint.md) | ___ | J6.01–J6.09 |
| J7 | [`J7-testes-qualidade/checkpoint.md`](J7-testes-qualidade/checkpoint.md) | ___ | J7.01–J7.09 |
| J8 | [`J8-seguranca-essencial/checkpoint.md`](J8-seguranca-essencial/checkpoint.md) | ___ | J8.01–J8.07 |
| J9 | [`J9-docker/checkpoint.md`](J9-docker/checkpoint.md) | ___ | J9.01–J9.04 |
| J10 | [`J10-ci-nuvem-intro/checkpoint.md`](J10-ci-nuvem-intro/checkpoint.md) | ___ | J10.01–J10.07 |

### 3.2 Quiz integrador (20 questões, ~45 min, sem consulta)
Estas questões **cruzam módulos**: é assim que os problemas aparecem no trabalho.

1. Um `POST /api/v1/transfers` retorna `500` com `LazyInitializationException` no log. Em que camada o problema nasce, por que o PayFlow desliga `open-in-view` e qual a correção (cite duas)? *(J5.08, J5.12, J6.02)*
2. Dois usuários transferem da mesma conta ao mesmo tempo. O que o `@Transactional` sozinho garante e o que **não** garante? O que o `@Version` do `Account` faz hoje e o que ele **ainda não** resolve? *(J3.10, J5.07, J5.11)*
3. O cliente reenvia o mesmo `POST` por *timeout* de rede. Como a `Idempotency-Key` evita a transferência dupla, e o que deve voltar na segunda chamada (status e corpo)? *(J2.02, J6.06)*
4. Por que `double` nunca representa dinheiro, e por que o PayFlow usa `HALF_EVEN` e envia o valor como **string** no JSON? *(J1.15, J2.04, J6.02)*
5. Um teste passa na sua máquina e falha no CI. Liste **quatro** causas prováveis (pense em hora, ordem, banco, Docker, `JAVA_HOME`). *(J1.16, J7.05, J10.01)*
6. A consulta do extrato ficou lenta com 1 milhão de lançamentos. Descreva o diagnóstico (`EXPLAIN`, índice, `OFFSET`) e a correção. *(J3.09, J5.10, J6.05)*
7. Diferencie `401`, `403` e `404` e diga quando devolver `404` **em vez de** `403` de propósito. *(J2.02, J8.01, J8.06)*
8. O que é **IDOR**? Escreva o teste (em palavras) que o pegaria no endpoint `GET /api/v1/accounts/{id}`. *(J8.06)*
9. Por que o JWT é adequado à API e a sessão ao painel no PayFlow? Em que lugar do navegador cada um fica e que ataque cada lugar facilita? *(J2.04, J8.03, J8.05)*
10. Um `INSERT` falha com `duplicate key value violates unique constraint`. Como a aplicação deve responder à API (status, Problem Details) e onde se traduz essa exceção? *(J3.04, J5.09, J6.04)*
11. O que `@SpringBootApplication` faz e o que acontece se a classe principal for movida para um pacote filho? *(J5.02, J5.03)*
12. Por que o `Dockerfile` copia o `pom.xml` **antes** do `src`, e o que o `.dockerignore` evita? *(J9.02, J9.04)*
13. A aplicação dentro do Compose não conecta ao banco com `jdbc:postgresql://localhost:5432/...`. Por quê e qual o endereço certo? *(J9.03)*
14. Por que o `ci.yml` usa `permissions` mínimas e fixa *actions* por SHA? Cite um ataque que isso reduz. *(J10.02)*
15. O que é a **responsabilidade compartilhada** e o que continua sendo seu na EC2 e no RDS? *(J10.03, J10.05)*
16. Em uma revisão de PR, você encontra `System.out.println`, `catch (Exception e) {}` e um método de 80 linhas. Como comenta cada um (tom, severidade, sugestão)? *(J7.07, J7.09)*
17. Qual a diferença entre `@WebMvcTest` e `@SpringBootTest`, e quando cada um é a escolha certa? *(J7.03, J7.04)*
18. Por que dado pessoal (e-mail, senha) não entra em log, e o que a LGPD pede de mínimo ao PayFlow? *(J5.14, J8.07)*
19. Um colega pergunta "por que não usar H2 nos testes?". Responda com a decisão do projeto. *(J7.05, decisão 5)*
20. Você viu `OutOfMemoryError: Java heap space` num container com 512 MB. Quais as primeiras três coisas que você confere? *(J9.02, J9.04)*

<details>
<summary>Gabarito do quiz integrador (respostas-modelo)</summary>

1. A entidade foi carregada com a associação `LAZY`, a transação **fechou** e alguém tocou na associação fora dela (no *controller*, na serialização). `open-in-view` desligado faz o erro aparecer cedo (e evita conexão presa durante a renderização). Correções: buscar o que precisa na consulta (`JOIN FETCH` / `@EntityGraph`) ou devolver um **DTO** montado dentro da transação.
2. `@Transactional` dá atomicidade e isolamento **da transação** (tudo ou nada), mas duas transações concorrentes podem ler o mesmo saldo e escrever por cima (*lost update*) em um isolamento fraco. O `@Version` detecta a escrita concorrente e faz a segunda falhar com exceção de *locking* otimista; **não** resolve filas de retentativa, nem *deadlocks*, nem o bloqueio explícito (isso é P2).
3. A chave é única no banco (`UNIQUE (idempotency_key)`): na repetição a aplicação **encontra** a transferência já feita e a devolve (200, mesmo corpo) em vez de executar de novo (a primeira vez devolve 201). Duas requisições simultâneas com a mesma chave são resolvidas pela restrição `UNIQUE`.
4. `double` é binário e não representa 0,1 exatamente (`0.1 + 0.2 != 0.3`); dinheiro exige decimal exato (`BigDecimal`). `HALF_EVEN` (arredondamento bancário) reduz o viés acumulado. JSON `number` é lido por muitos clientes como `double`: **string** com 2 casas preserva o valor.
5. Fuso/hora (`Instant`, `Clock` fixo), ordem de execução dos testes ou estado compartilhado, versão do JDK (`JAVA_HOME`), Docker/Testcontainers indisponível ou imagem do banco diferente, dependência de arquivo local ou variável de ambiente, concorrência/tempo (`sleep`), *locale* (vírgula decimal).
6. `EXPLAIN (ANALYZE)` mostra `Seq Scan`: falta um índice em `(account_id, created_at, id)` (a ordem do índice acompanha o filtro e a ordenação). Paginação por `OFFSET` fica mais lenta quanto mais fundo; **keyset** (`WHERE (created_at, id) < (:c, :i)`) ou `Slice` com limite. Confirme com `EXPLAIN` de novo.
7. `401` = "não sei quem é você" (sem credencial ou inválida); `403` = "sei quem é, mas não pode"; `404` = "não existe". Devolver `404` no lugar de `403` esconde a **existência** do recurso de quem não é dono (evita enumerar IDs), que é a escolha em contas de outro usuário.
8. IDOR: referência direta a objeto sem checar o dono (`/accounts/{id}` devolve a conta de qualquer um). Teste: autentique como A, peça a conta de B e **espere 404/403 sem dados no corpo**; repita para cada rota com `{id}`.
9. Sessão no painel: cookie `HttpOnly; Secure; SameSite`, o servidor guarda o estado (exige **CSRF**). JWT na API: sem estado no servidor, enviado no `Authorization: Bearer`; guardá-lo em `localStorage` o expõe ao **XSS**; sessão em cookie é exposta ao **CSRF**, daí os tokens e o `SameSite`.
10. Violação de restrição única vira `DataIntegrityViolationException` (traduzida pelo `@Repository`); o `@RestControllerAdvice` a mapeia para **409** (ou 422, conforme a regra) em Problem Details com um `detail` seguro (sem nome de constraint nem SQL).
11. Une `@SpringBootConfiguration`, `@EnableAutoConfiguration` e `@ComponentScan`; o scan varre o pacote da classe **e os de baixo**. Em um pacote filho, os beans nos pacotes irmãos **não são encontrados** e a aplicação falha por falta de bean.
12. Para o **cache de camadas**: as dependências (que mudam pouco) ficam numa camada reutilizada; só o `src` invalida a camada seguinte. O `.dockerignore` mantém `target/`, `.git`, segredos e lixo fora do contexto (build mais rápido e sem vazar).
13. Dentro do container, `localhost` é **o próprio container**. No Compose use o **nome do serviço** (`db`), resolvido pela rede interna do Compose; a porta 5432 não precisa ser publicada no host.
14. `permissions` mínimas limitam o que um passo comprometido faz com o `GITHUB_TOKEN`; fixar por **SHA** impede que uma *tag* movida pelo autor da *action* injete código (ataque de cadeia de suprimentos). Também: *script injection* e `pull_request_target`.
15. O provedor cuida **da** nuvem (instalações, hardware, hipervisor); você cuida **na** nuvem: na EC2, sistema operacional, *patches*, rede, firewall, aplicação, dados e IAM; no RDS, o provedor cuida do SO e do motor, você cuida de acesso, esquema, senhas, *backup*/retenção e custo.
16. `println`: *nit/suggestion* ("use o logger com nível; assim dá para filtrar"); `catch` vazio: **blocker** ("engole o erro: registre ou relance com a causa"); método de 80 linhas: *suggestion* ("extraia X e Y; ajuda nos testes"), sempre com **motivo** e um caminho, sem personalizar.
17. `@WebMvcTest` sobe só a camada web (controllers, filtros, conversores), com serviços *mockados*: rápido, para contrato HTTP e validação. `@SpringBootTest` sobe o contexto inteiro: para integração ponta a ponta (com Testcontainers). Prefira a fatia e use `@SpringBootTest` com moderação.
18. Logs vão para sistemas de terceiros, são retidos e lidos por muita gente; dado pessoal em log é vazamento e infração. A LGPD pede, no mínimo: finalidade e **minimização** dos dados, base legal, direitos do titular (acesso, correção, eliminação) e segurança. Detalhes na J8.07 (sem afirmar números de artigo de memória).
19. H2 não é o banco de produção: difere em tipos, SQL e comportamento transacional e **esconde** bugs; a decisão 5 é Testcontainers com **PostgreSQL 18 real**.
20. (1) O limite do container e o `-XX:MaxRAMPercentage` (75%), (2) se o *heap* é realmente pequeno para a carga (dump/ perfil, `docker stats`), (3) vazamento (coleção que cresce) ou consulta que traz a tabela inteira. Código de saída **137** indica morto por falta de memória.

</details>

**Nota:** 18–20 ✅ · 14–17 ⚠️ (reforce os módulos das questões erradas) · < 14 ❌ (refaça os quizzes por módulo).

---

## Parte 4: Simulado de entrevista júnior

**Regras do simulado:** 20 perguntas, **40 minutos**, em voz alta e sem consulta, de preferência gravado ou com um colega. Cada resposta deve caber em **1 a 2 minutos**. Uma boa resposta tem **definição → por que importa → exemplo do PayFlow**. Não vale "decorei": diga "não sei, mas eu investigaria assim". As respostas-modelo estão abaixo, **fechadas**: tente antes.

**Blocos:** Java (1–6) · Web/HTTP e SQL (7–11) · Spring/APIs (12–15) · Testes/segurança/DevOps (16–19) · Comportamental (20).

1. `==` e `equals`: diferença, e o contrato com `hashCode`.
2. `String` é imutável: por que, e quando usar `StringBuilder`?
3. `ArrayList` ou `LinkedList`? `HashMap` por dentro.
4. Checked e unchecked: quando usar cada uma?
5. Por que `BigDecimal` para dinheiro?
6. O que é um `Stream`? `map` e `flatMap`? Quando **não** usar?
7. O que acontece quando eu digito uma URL e aperto Enter?
8. `PUT`, `PATCH`, `POST`: diferenças. O que é idempotente?
9. `INNER JOIN` e `LEFT JOIN`; `WHERE` e `HAVING`.
10. O que é uma transação? Explique ACID.
11. O que é um índice? Quando ele **não** ajuda?
12. O que é injeção de dependência e por que preferir o construtor?
13. Como o `@Transactional` funciona e uma situação em que ele **não** funciona.
14. O que é o problema N+1?
15. DTO ou entidade na API? Por quê?
16. Pirâmide de testes: unidade, fatia e integração. O que é um *mock* e o que **não** se deve mockar?
17. Cite três itens do OWASP Top 10 e como se previne cada um.
18. Hash de senha: por que BCrypt e não SHA-256 puro?
19. O que é uma imagem Docker e por que o container não deve rodar como root? O que é CI?
20. Conte sobre um bug que você teve. Como você pede ajuda quando está travado?

<details>
<summary>Respostas-modelo</summary>

1. `==` compara **referências** (ou valores, nos primitivos); `equals` compara o **conteúdo** conforme o contrato da classe. Se dois objetos são `equals`, **têm** de ter o mesmo `hashCode`; se violar, o objeto "some" de um `HashSet`/`HashMap`. No PayFlow, `Account` define a igualdade pelo `id`, não por todos os campos.
2. Imutável permite compartilhar com segurança (*string pool*), usar como chave de `Map` e ser *thread-safe*. Concatenar em laço cria um objeto por volta; use `StringBuilder` (mutável) dentro do laço.
3. `ArrayList` (array redimensionável): acesso por índice O(1), bom na maioria dos casos; `LinkedList` raramente vence. `HashMap`: *buckets* por `hashCode`, colisões em lista/árvore; O(1) médio; por isso `equals`/`hashCode` coerentes.
4. *Checked* para falhas **recuperáveis** que o chamador deve tratar (E/S); *unchecked* para erro de programação ou de regra (`IllegalArgumentException`, `InsufficientFundsException`). Nunca engula a exceção; preserve a **causa**.
5. `double` é binário e inexato (`0.1 + 0.2`); `BigDecimal` é decimal exato, com escala e arredondamento explícitos (`HALF_EVEN`); construa a partir de **String**, não de `double`.
6. Pipeline sobre uma coleção (fonte → operações intermediárias *lazy* → terminal). `map` transforma 1→1, `flatMap` 1→N achatado. Não use quando um laço simples é mais legível, ou com efeitos colaterais, ou para I/O.
7. DNS resolve o nome em IP; TCP conecta (e TLS negocia, se HTTPS); o navegador envia `GET`; o servidor responde (status, *headers*, corpo); o navegador renderiza e pede os recursos extras.
8. `POST` cria/executa (não idempotente por padrão); `PUT` **substitui** o recurso inteiro; `PATCH` altera **parte**. Idempotente = repetir tem o **mesmo efeito** de fazer uma vez (`GET`, `PUT`, `DELETE`); em pagamentos evita a cobrança dupla, daí a `Idempotency-Key`.
9. `INNER` devolve só o que casa nos dois lados; `LEFT` mantém todas as linhas da esquerda (com `NULL` quando não casa). `WHERE` filtra **linhas** antes de agrupar; `HAVING` filtra **grupos** depois do `GROUP BY`.
10. Unidade de trabalho "tudo ou nada". **A**tomicidade, **C**onsistência, **I**solamento, **D**urabilidade. Exemplo: a transferência debita, credita e lança no *ledger* ou não faz nada.
11. Estrutura (B-tree) que evita varrer a tabela. Não ajuda em tabela pequena, baixa seletividade, `LIKE '%x'`, função na coluna ou quando a coluna não está no filtro; e custa escrita e espaço.
12. O objeto **recebe** o que precisa em vez de criar. Por construtor: campos `final`, objeto sempre válido, dependências explícitas e **fácil de testar** sem Spring; falta de bean falha na subida.
13. O Spring cria um **proxy** que abre a transação, chama o método e faz *commit*/*rollback*. Não funciona em **chamada interna** (`this.metodo()`), em método não público/`final` ou quando a exceção é *checked* (não desfaz por padrão).
14. Uma consulta para a lista e **uma a mais por item** para a associação preguiçosa. Detecta-se no log SQL; corrige com `JOIN FETCH`, `@EntityGraph` ou projeção/DTO.
15. **DTO**: contrato estável, sem expor/serializar a entidade (campos internos, `LAZY`, `password_hash`), validação própria e evolução da API sem mexer no banco.
16. Muitos testes de unidade (rápidos), alguns de fatia, poucos de integração/E2E. *Mock* substitui uma dependência para isolar a unidade; **não** mocke o que você está testando, tipos simples, nem o banco que você quer provar (use Testcontainers).
17. Injeção (consulta parametrizada), controle de acesso quebrado/IDOR (checar o dono em toda rota), XSS (escapar saída, CSP), CSRF (token/`SameSite`), componentes vulneráveis (Dependency-Check, Dependabot), segredos expostos.
18. SHA-256 é rápido (bom para atacante). BCrypt é **lento de propósito**, com *salt* embutido e custo ajustável; SHA-256 puro sem sal permite tabelas arco-íris e força bruta barata.
19. Imagem = *template* imutável em camadas; container = instância em execução. Sem root, uma falha na aplicação não vira controle do contêiner/host. CI = a cada PR, compila e testa em ambiente limpo e barra o que quebra.
20. Estrutura STAR (situação, tarefa, ação, resultado) com um bug **real**: como reproduziu, como isolou (log, *debugger*, teste que falha), a correção e o teste que ficou. Ao pedir ajuda: o que você quer, o que tentou, a mensagem de erro completa e um exemplo mínimo (J0.06).

</details>

**Como se avaliar:** nota 0 (não sabia), 1 (disse algo vago), 2 (respondeu com exemplo). **34–40** ✅ · **26–33** ⚠️ · **< 26** ❌. Registre as perguntas em que tirou 0 e releia a aula da coluna "Aula" de `recursos/perguntas-entrevista.md`.

---

## Parte 5: Desafio técnico (take-home) com tempo limite

> ⏱️ **Limite: 4 horas corridas** (cronometre; pare quando acabar; o que não ficou pronto vai para "o que faria com mais tempo"). Pode consultar a documentação oficial; **não** copie o PayFlow: o objetivo é provar que você sabe **construir do zero**. Faça em um repositório novo, com `git init` e commits pequenos em Conventional Commits.

### Enunciado: **Vales (voucher-service)**
Uma loja precisa de uma API para emitir e resgatar **vales de presente** (gift vouchers).

**Requisitos funcionais**
1. `POST /api/v1/vouchers`: cria um vale com `code` (único, gerado pelo servidor), `value` (decimal, **> 0**), `expiresAt` (instante no futuro) e `status = ACTIVE`. Resposta `201` com `Location`.
2. `GET /api/v1/vouchers/{code}`: devolve o vale (`404` se não existir).
3. `POST /api/v1/vouchers/{code}/redeem`: resgata o vale. **Regras:** só `ACTIVE` e não expirado pode ser resgatado; um vale resgatado **não pode** ser resgatado de novo. Repetir o resgate com a mesma `Idempotency-Key` devolve o **mesmo** resultado, sem erro.
4. `GET /api/v1/vouchers?status=&page=&size=`: lista **paginada** (padrão 20, máximo 100), ordenada de forma estável (`createdAt` e desempate por `id`).
5. Erros em **Problem Details** (400 validação, 404, 409 já resgatado ou chave conflitante, 422 expirado).

**Requisitos não funcionais**
- Java 25, Spring Boot 4.1.x, Maven (com Wrapper), **PostgreSQL** com **Flyway** (`ddl-auto: validate`).
- Valor em `BigDecimal` (escala 2); no JSON, **string**.
- **Testes:** pelo menos unidade (regra de resgate), fatia web e **um teste de integração com Testcontainers** que prove que **dois resgates simultâneos do mesmo vale resultam em um só sucesso** (use `@Version` ou `UPDATE ... WHERE status = 'ACTIVE'` e **explique** a escolha no README).
- **Segurança mínima:** `POST` exige autenticação básica ou JWT de um usuário `ADMIN` (qualquer mecanismo do Spring Security que você saiba justificar); nada de segredo no código.
- `Dockerfile` multi-stage e `compose.yaml` (app + banco), e um `ci.yml` que roda `./mvnw verify`.
- **README** com: como rodar em 3 comandos, decisões tomadas, **trade-offs**, e "o que faria com mais tempo".

**Fora do escopo** (não gaste tempo): telas, mensageria, cache, microsserviços, nuvem.

### Rubrica (100 pontos)

| Critério | Pontos | O que o avaliador procura |
|---|---|---|
| **Corretude das regras** (resgate, expiração, idempotência) | 20 | Casos de borda: vale expirado no limite, repetição da chave, dois resgates concorrentes |
| **Modelagem e banco** (Flyway, constraints, índices) | 10 | `UNIQUE (code)`, `CHECK (value > 0)`, `NUMERIC(19,2)`, `TIMESTAMPTZ`, índice para a listagem |
| **API REST** (verbos, status, `Location`, paginação, Problem Details) | 15 | Contrato coerente, erros úteis e **sem vazar** detalhes internos |
| **Testes** (camadas, concorrência real, nomes claros) | 20 | O teste de concorrência **falha** se você remove a proteção; sem `Thread.sleep` frágil |
| **Segurança** (autenticação, validação, segredos, IDOR/papéis) | 10 | Senha com BCrypt, sem segredo versionado, entrada validada |
| **Qualidade do código** (nomes, tamanho dos métodos, sem *code smells*) | 10 | Camadas simples, DTOs, sem lógica no controller |
| **DevOps** (`Dockerfile`, Compose, CI) | 5 | Roda com `docker compose up`; CI verde |
| **README e comunicação** (decisões, trade-offs, honestidade) | 10 | Diz o que **não** ficou pronto e por quê |

**Bônus (até +5, não passa de 100):** `EXPLAIN` da listagem no README, OpenAPI e *correlation id* nos logs.

**Penalidades:** não compila ou `verify` vermelho (−15); segredo no Git (−10); `double` para dinheiro (−10); passou do tempo sem avisar (−5).

**Resultado:** **≥ 80** ✅ nível júnior sólido · **65–79** ⚠️ ok com plano de reforço (liste as lacunas) · **< 65** ❌ refaça os módulos ligados aos critérios com menos pontos e repita o desafio com **outro** enunciado (por exemplo, "cupons de desconto").

<details>
<summary>Dicas de planejamento (leia só se travar)</summary>

- **Divida as 4 h:** 20 min lendo e planejando; 30 min esquema + migração; 80 min domínio e API; 60 min testes (comece pelo de concorrência); 30 min Docker/CI; 20 min README. Se estourar, **corte o bônus e o CI**, não os testes.
- **Concorrência:** `UPDATE vouchers SET status='REDEEMED', redeemed_at=now() WHERE code=:c AND status='ACTIVE' AND expires_at > now()` devolve **1** linha afetada ou **0**; com 0, descubra o motivo (não existe, já resgatado, expirado) e responda 404/409/422. Isso é correto sob concorrência sem *lock* explícito.
- **Idempotência:** guarde `idempotency_key` com `UNIQUE` e o resultado; na repetição, devolva o resultado salvo.
- **Teste de concorrência:** `ExecutorService` com N tarefas liberadas por um `CountDownLatch`; o esperado é **exatamente 1** sucesso.
- O **código** de resgate (código aleatório) precisa de um gerador seguro (`SecureRandom`), nunca sequencial.

</details>

---

## Parte 6: Autoavaliação pela matriz de competências

A matriz (`00-guia/matriz-de-competencias.md`) lista o comportamento observável de um júnior. A coluna "Aulas" abaixo é a **tabela "competência → aulas que cobrem"** gerada nesta revisão. Marque ✅ (faço sozinho) / ⚠️ (faço com ajuda) / ❌ (não faço).

| Competência (comportamento do júnior) | Aulas que cobrem | Cobertura | ✅ ⚠️ ❌ |
|---|---|---|---|
| **Linguagem Java:** OO, Collections, Streams, exceções e `BigDecimal` | J1.01–J1.09 (OO, exceções), J1.10–J1.11 (Collections), J1.13 (Streams), J1.15 (`BigDecimal`), J1.12 e J1.14 (generics, `Optional`, records) | ✅ completa | ☐ |
| **Lógica e estruturas de dados:** escolhe `List`/`Set`/`Map`, estima Big-O simples | J1.10, J1.11 ("como escolher"), J1.16 (Big-O), `recursos/exercicios-logica.md` | ✅ completa | ☐ |
| **Git e ferramentas:** branch, PR, conflitos, commits claros | J0.04, J0.05, J7.09 (code review e Conventional Commits), J10.01 (PR + CI), J0.01–J0.03 (terminal, Maven, IntelliJ) | ✅ completa | ☐ |
| **Web e HTTP:** ciclo da requisição, status codes e headers | J2.01–J2.06, J6.01 (design de endpoints), J6.06 (idempotência), J6.08 (CORS) | ✅ completa | ☐ |
| **Banco de dados:** JOINs, agregações, transações/ACID, Flyway | J3.01–J3.03 (modelagem e banco), J3.04–J3.08 (SQL), J3.09 (índices), J3.10 (ACID), J3.11 (JDBC), J3.12 (Flyway) | ✅ completa | ☐ |
| **Spring e persistência (JPA):** CRUD com Spring Data, validação, *profiles* | J5.01–J5.06 (Spring e Boot), J5.07–J5.12 (JPA, Data, JPQL, `@Transactional`, N+1), J5.13 (validação), J5.05 (*profiles*) | ✅ completa | ☐ |
| **APIs REST:** endpoints, DTOs, erros padronizados e OpenAPI | J6.01–J6.09 | ✅ completa | ☐ |
| **Testes e qualidade:** unidade, fatia, Testcontainers | J7.01–J7.06, J1.16 (JUnit), J7.07–J7.09 (qualidade e revisão) | ✅ completa | ☐ |
| **Segurança:** Spring Security, BCrypt, JWT, OWASP Top 10 | J8.01–J8.07 | ✅ completa | ☐ |
| **Front-end:** HTML/CSS/JS, Thymeleaf, `fetch` | J4.01–J4.08 | ✅ completa | ☐ |
| **Mensageria e assincronia:** entende fila vs síncrono | só **fundamentos indiretos**: `Queue`/`Deque` como estrutura (J1.11), chamadas assíncronas no navegador (J4.05) e latência de chamadas síncronas externas (J6.08). **Não há aula sobre fila de mensagens** (começa em P3) | ⚠️ **parcial** (lacuna registrada) | ☐ |
| **Arquitetura:** segue a estrutura do projeto | J5.02 (estrutura), J5.03 (beans, camadas), J5.06 (MVC), J6.02 (DTO e camadas), ADRs 0001–0012 em `projeto/adrs/` | ✅ completa | ☐ |
| **Sistemas distribuídos:** (não exigido no Júnior) | — | ➖ n/a | — |
| **Observabilidade e operação:** lê logs e stack traces | J0.06 (stack traces), J5.14 (logging), J5.15 (Actuator), J9.04 (logs e depuração em container) | ✅ completa | ☐ |
| **Containers:** containeriza a app com Docker e Compose | J9.01–J9.04 | ✅ completa | ☐ |
| **CI/CD e IaC:** configura CI com testes por PR | J10.01, J10.02, J10.06 (entrega) | ✅ completa | ☐ |
| **Nuvem e custo:** usa EC2, RDS e S3; sabe desligar para não gastar | J10.03–J10.07 (💸 e destruição provada) | ✅ completa | ☐ |
| **Performance:** evita consultas óbvias lentas | J3.09 (índices e `EXPLAIN`), J5.10 (paginação), J5.12 (N+1), J6.05 (paginação e filtros) | ✅ completa | ☐ |
| **Liderança e comunicação:** pede ajuda, recebe code review | J0.05, J0.06 (pedir ajuda), J7.09 (code review) | ✅ completa | ☐ |

**18 competências** exigidas do Júnior: **17 com cobertura completa e 1 parcial** (mensageria). Fechar a lacuna de mensageria antes do Pleno é simples: leia o início de **P3** quando chegar lá; ela não bloqueia o resto.

### Decisão de avançar
| Resultado | Decisão |
|---|---|
| Projeto ≥ 80% **e** quizzes ≥ 14/20 **e** simulado ≥ 34 **e** desafio ≥ 80, sem ❌ na matriz | ✅ **Avance para P1** |
| Alguma parte em ⚠️, sem ❌ | ⚠️ **Avance** com um **plano de reforço** de 1 semana: 3 itens, a aula e a data |
| Qualquer parte em ❌ | ❌ **Repita os módulos indicados** (e o desafio, com outro enunciado) antes de seguir |

> **Consolide no `progresso.md`**: nota de cada parte, itens em ⚠️/❌ e o plano de reforço. Os **testes práticos finais** do PayFlow (compilar, rodar a suíte, subir o Compose, o CI de verdade e a parte AWS) entram na lista "a validar no fim do treinamento".

## ➡️ Próximo
[P1 — Java a fundo](../2-pleno/P1-java-a-fundo/README.md) (o próximo módulo, a ser gerado quando você pedir)
