# J7.CP — Checkpoint do módulo J7

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J7.01 a J7.09

> ⚠️ **Estado de verificação do módulo:** os exemplos de J7 foram **revisados por leitura**; a bateria prática de testes fica para o **fim do treinamento** (decisão do aluno). Foram **executados**: o laboratório `StatementLab` (J7.07), o laboratório `qa-lab` com Checkstyle e SpotBugs (J7.08), o *hook* `commit-msg` e o `ReleaseBump` (J7.09), a lógica pura de `Masking` e o *script* `coverage-summary.sh` (J7.06). **Não executados:** todos os testes JUnit/Spring/Testcontainers, a configuração do JaCoCo, a aplicação de Checkstyle/SpotBugs ao código do PayFlow e a migração dos testes antigos. Os números de "testes esperados" abaixo vêm de **contagem por leitura**, não de execução.

## Parte 1 — Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Em que camada da pirâmide ficam `AccountTest`, `TransfersControllerWebTest`, `RepositoriesJpaTest` e `TransferServiceTest` (J5.11)? Por que a forma de pirâmide?
2. Por que `assertThat(balance).isEqualTo(new BigDecimal("69.50"))` pode falhar com o valor certo e como corrigir? Que técnica escolhe **quais** valores testar?
3. Diferença entre stub, mock e fake. Por que o `TransferServiceUnitTest` usa `Account` real, e o que ele **não** consegue provar?
4. O que é *strict stubs*? Quando aparecem `UnnecessaryStubbingException` e `PotentialStubbingProblem`?
5. O que o `@WebMvcTest` registra, o que deixa de fora, e por que o `AuthInterceptor` precisou de `@Import` de `Sessions`, `SessionCookies` e `ApiProblems`? Por que o `401` vem antes do `400`?
6. Por que `@AutoConfigureTestDatabase(replace = NONE)` e o `flush` são indispensáveis no `RepositoriesJpaTest`? O que o rollback do teste **esconde**?
7. Com `@SpringBootTest(webEnvironment = RANDOM_PORT)`, por que `@Transactional` no teste **não** desfaz o que a requisição fez? O que entra na chave do cache de contexto?
8. O que o Testcontainers faz (porta, prontidão, Ryuk)? Por que declarar o container como **bean** com `@ServiceConnection`, e por que o `application-test.yml` perdeu o `datasource`?
9. Diferencie cobertura de linhas e de *branches*. Como escolher um piso de cobertura? Descreva o ciclo vermelho-verde-refatorar e "fake it".
10. O que cada uma das ferramentas (Checkstyle, SpotBugs, SonarLint) lê? Por que `a == b` entre `String`s foi pego só pelo SpotBugs? O que você prioriza em um *code review* e como rotula comentários? Para que serve `feat!:`?

<details><summary>Gabarito</summary>

1. Unidade (`AccountTest`), fatia web (`TransfersControllerWebTest`), fatia JPA (`RepositoriesJpaTest`), integração (`TransferServiceTest`). Muitos testes baratos e precisos na base, poucos grandes e caros no topo: custo e velocidade de feedback.
2. `isEqualTo` usa `equals`, que considera a **escala** (`69.5` ≠ `69.50`); use `isEqualByComparingTo` (ou compare a *string* quando a escala é parte do contrato). Partição de equivalência e valores-limite.
3. Stub devolve respostas prontas; mock também verifica interações; fake é uma implementação simples que funciona. A `Account` real mantém as regras do domínio sob teste. Não prova transação (rollback), SQL/consulta derivada, `UNIQUE` nem `@Version`.
4. O `MockitoExtension` é estrito por padrão: stub **não usado** → `UnnecessaryStubbingException`; chamada ao método stubado com **argumentos diferentes** → `PotentialStubbingProblem`. Pegam stub sobrando e caminho diferente do esperado.
5. Registra controllers, advices, conversores, interceptors e `WebMvcConfigurer`; deixa de fora `@Service`/`@Repository`/`@Component` e a persistência. As dependências do interceptor são `@Component` comuns (não registrados pela fatia), logo `@Import`. O interceptor roda antes da resolução de argumentos e da validação.
6. O Boot trocaria o `DataSource` por um banco embutido se houvesse um; o PayFlow nunca usa H2 (decisão 5). O `INSERT` só sai no `flush`; sem ele a constraint nunca é checada. O rollback esconde o commit real, o efeito de `UNIQUE`/`CHECK` sem flush, a versão do `@Version` e o `LazyInitializationException` (a transação do teste mantém a sessão aberta).
7. A requisição roda em outra thread (a do servidor), fora da transação do teste: o commit é real. Chave: perfis, propriedades de teste, classes, conjunto de `@MockitoBean`/`@MockitoSpyBean`, `@Import`, `@DynamicPropertySource`, tipo de fatia.
8. Sobe um container descartável com porta aleatória, espera o sinal de prontidão e usa o Ryuk para removê-lo. Como bean, o Spring o cria antes e o encerra depois dos beans que o usam, por contexto (o `@Container` o encerra ao fim da classe e o cache pode manter um contexto com a conexão morta). O `@ServiceConnection` entrega os detalhes de conexão (precedência sobre as propriedades); sem datasource no perfil, um teste que esqueceu o container **falha alto**.
9. Linha: executou ao menos uma instrução; *branch*: cada lado de cada decisão (uma linha com `? :` pode estar 100% coberta com 50% dos desvios). Piso: **medir** a cobertura atual e fixar poucos pontos abaixo. Ciclo: teste que falha (vermelho), mínimo para passar (verde), refatorar com tudo verde; "fake it" devolve a constante do teste e deixa o segundo exemplo forçar a generalização.
10. Checkstyle lê a **árvore do código-fonte** (estilo); SpotBugs lê o ***bytecode*** com tipos e fluxo de dados (padrões de bug); SonarLint analisa na IDE em tempo real. O `StringLiteralEquality` do Checkstyle só vê `==` com **literal**; o SpotBugs sabe que os dois operandos são `String`. Revisão: desenho, corretude, dinheiro/dados, testes, legibilidade; estilo é da máquina; rótulos `blocking`/`suggestion`/`nit`/`question`. `feat!:` é uma mudança incompatível (MAJOR).
</details>

## Parte 2 — Entrega prática (tag `payflow-J7`)
O `payflow` ganha uma **suíte em camadas**, cobertura medida e portões de qualidade. Sem mudança de comportamento da aplicação (a única classe nova de produção é `Masking`).

```
src/main/java/com/payflow/web/Masking.java                       (TDD, J7.06)
src/test/java/com/payflow/
├── support/   TestEntities, PostgresContainerConfig, PayflowTest, PostgresContainerTest   (J7.02, J7.05)
├── domain/    AmountsTest, AccountTest                                                     (J7.01)
├── service/   TransferServiceUnitTest                                                      (J7.02)
├── web/       MaskingTest                                                                  (J7.06)
├── web/api/   TransfersControllerWebTest                                                   (J7.03)
├── jpa/       RepositoriesJpaTest                                                          (J7.04)
└── ApplicationContextSmokeTest                                                             (J7.04)
+ todos os testes de banco da J5/J6 migrados para @PayflowTest (J7.05)
config/checkstyle/checkstyle.xml   config/spotbugs/exclude.xml   .editorconfig
scripts/coverage-summary.sh   scripts/migrate-tests-to-testcontainers.sh   scripts/hooks/commit-msg
.github/pull_request_template.md   docs/code-review-checklist.md
docs/testing-notes.md   docs/quality-notes.md   docs/clean-code-lab/StatementLab.java   docs/qa-lab/
```
Novas dependências de teste: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-testcontainers`, `org.testcontainers:testcontainers-postgresql` (todas pelo BOM). Novos plugins (com `<version>`): `jacoco-maven-plugin` 0.8.16, `maven-checkstyle-plugin` 3.6.0 (+ Checkstyle 14.4.0), `spotbugs-maven-plugin` 4.10.5.0; mais `maven-dependency-plugin` (do BOM) para o agente do Mockito.

Critérios de pronto:
- [ ] **43 métodos de teste novos** (62 execuções, **esperado, contado por leitura**): `AmountsTest` 3 (12), `AccountTest` 10 (12), `TransferServiceUnitTest` 8, `TransfersControllerWebTest` 7 (9), `RepositoriesJpaTest` 6, `ApplicationContextSmokeTest` 2, `PostgresContainerTest` 3, `MaskingTest` 4 (10).
- [ ] **Pirâmide:** testes de unidade rodam **sem** Spring e sem banco (nenhum `Started PayflowApplication` ao rodar `AmountsTest` e `AccountTest*`); a fatia web **sem** Flyway/Hikari/Tomcat nos logs.
- [ ] **Banco real, sem H2:** `@PayflowTest` (container `postgres:18` por `@ServiceConnection`); `application-test.yml` sem `spring.datasource`; o container local `payflow-pg` **parado** e `mvn test` verde.
- [ ] **`mvn verify`** gera o relatório do JaCoCo; o piso foi **medido** e registrado em `docs/testing-notes.md` (não é o `0.70` do exemplo); `ApiMapperImpl` e `PayflowApplication` excluídos com justificativa.
- [ ] **Qualidade:** `mvn validate` roda o Checkstyle; `mvn verify` roda o SpotBugs; a **primeira rodada** foi medida e triada em `docs/quality-notes.md`; o portão está ligado.
- [ ] **Clean code:** `StatementLab.java` executado (8 `== legacy`, 1 `!=`); cinco *smells* reais do PayFlow listados e dois corrigidos com testes verdes.
- [ ] **Revisão:** modelo de PR, lista do revisor e *hook* `commit-msg` ativos; você revisou a PR fictícia #12 por escrito.
- [ ] **Pendência herdada:** `Transfer.complete()` (erro da J5, registrado em "Correções pendentes" de `progresso.md`) foi acrescentado à entidade **antes** de rodar os testes novos (`TransferServiceUnitTest` e `RepositoriesJpaTest` dependem dele); a decisão foi anotada em `docs/testing-notes.md`.
- [ ] Commits em Conventional Commits, PR mesclada (modelo preenchido) e tag:
```bash
git tag payflow-J7
git push origin payflow-J7
```

## Parte 3 — Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Escrevo testes de unidade rápidos (AAA, AssertJ, valores-limite) | ☐ | ☐ | ☐ |
| Uso Mockito (dublês, *strict stubs*) e sei o que mocks **não** provam | ☐ | ☐ | ☐ |
| Escrevo testes de fatia (`@WebMvcTest`, `@DataJpaTest`) e leio erros de contexto | ☐ | ☐ | ☐ |
| Uso Testcontainers com PostgreSQL real e entendo o custo (contextos, containers) | ☐ | ☐ | ☐ |
| Meço e interpreto cobertura (linhas × *branches*) sem tratá-la como meta | ☐ | ☐ | ☐ |
| Pratico TDD (vermelho, verde, refatorar) em regras pequenas | ☐ | ☐ | ☐ |
| Refatoro com testes de caracterização e separo refatoração de mudança de comportamento | ☐ | ☐ | ☐ |
| Uso Checkstyle/SpotBugs/SonarLint, triando verdadeiro × falso positivo | ☐ | ☐ | ☐ |
| Reviso e recebo *code review* e escrevo Conventional Commits | ☐ | ☐ | ☐ |

**Regra:** com mais de duas linhas em 0, repita as aulas correspondentes antes do [J8](../J8-seguranca-essencial/README.md). Atualize `00-guia/progresso.md`.

## Próximo
[J8 — Segurança essencial](../J8-seguranca-essencial/README.md)
