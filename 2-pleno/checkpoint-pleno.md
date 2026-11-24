# Checkpoint de nível Pleno

> ⏱️ ~9h (6 sessões de 1h30: ver `00-guia/cronograma.md`, "CK-P.1" a "CK-P.6") · 🔵 Pleno · Pré-requisitos: P1 a P10 (todos os módulos do nível)

> ⚠️ **Estado de verificação:** por decisão do aluno, os **testes práticos do PayFlow ficam para o fim do treinamento**. Esta execução foi uma **revisão por leitura e por varredura automática** (links, ordem de pré-requisitos, horas, cobertura da matriz). **Não** foi compilado o PayFlow do aluno, **não** foi aplicado nenhum manifesto, **não** foi tocada a AWS, e o **incidente da Parte 5 é sintético** (escrito para o exercício: os números dele **não são medições**). O que cada módulo executou de verdade está no registro do módulo em `00-guia/progresso.md` e no aviso de cada `checkpoint.md`; o incidente **medido** de verdade é o da [P6.07](P6-observabilidade/P6.07-investigando-um-incidente-simulado.md).

## 🎯 Para que serve
Você terminou 10 módulos (97 aulas, 160,5 h, mais os 10 checkpoints de módulo). Este checkpoint responde a uma pergunta só: **estou pronto para conduzir uma funcionalidade de ponta a ponta em um time (do desenho ao deploy e à investigação em produção) e para começar o nível Sênior?** Ele tem seis partes, em ordem:

| Parte | O que é | Sessões | Como é avaliada |
|---|---|---|---|
| 1 | Revisão de entrada: **o que a revisão do nível encontrou** e as **pendências** que ela consolidou (leia antes de começar) | CK-P.1 | leitura |
| 2 | **Projeto PayFlow** avaliado por checklist (tag `payflow-P10`) | CK-P.1 | autoavaliação com evidência |
| 3 | **Quiz** por módulo (P1–P10) + **quiz integrador** de 20 questões | CK-P.2 | gabarito |
| 4 | **Simulado de entrevista pleno** (20 perguntas técnicas) + **design de um serviço** (45 min) | CK-P.3 e CK-P.4 | respostas-modelo e rubrica |
| 5 | **Incidente simulado**: investigar com **logs, métricas e traces** | CK-P.5 | roteiro e gabarito |
| 6 | **Autoavaliação pela matriz de competências** (tabela competência → aulas) e decisão de avançar | CK-P.6 | tabela |

---

## Parte 1: O que a revisão do nível encontrou

Revisão feita em 2026-11-14 sobre `2-pleno/` (**97 aulas**, 10 `README.md`, 10 `checkpoint.md`) e sobre `00-guia/`, `projeto/`, `recursos/` e o `README.md` raiz.

| Verificação | Resultado |
|---|---|
| **Links internos** (links relativos entre arquivos `.md`, fora dos blocos de código; 314 links) | ✅ **0 quebrados** |
| **Links externos** (559 URLs únicas em `2-pleno/`, `curl` com redirecionamento) | ✅ **557 responderam 200**. As **2** restantes são `rabbitmq.com/docs/confirms` e `rabbitmq.com/docs/quorum-queues`: **403 ao `curl`** (o site recusa o cliente; o registro da P3 já dizia isso e as duas páginas foram conferidas por outra via naquela execução). **Não** troquei o *User-Agent* para contornar. |
| **Pré-requisitos no cabeçalho** (97 aulas) | ✅ **todas as 97 aulas têm o campo**; **nenhuma** aponta para uma aula **posterior, igual ou inexistente** (varredura de cada código `J`/`P`/`S` citado, inclusive faixas como `P5.01–P5.05`) |
| **Horas** | ✅ P1 21 h + P2 18 h + P3 21 h + P4 18 h + P5 16,5 h + P6 12 h + P7 15 h + P8 13,5 h + P9 13,5 h + P10 12 h = **160,5 h** (97 aulas + 10 checkpoints de módulo = **107 sessões × 1h30 = 160,5 h**); mais as 6 sessões deste checkpoint (**9 h**) = **169,5 h**, o número do `README.md` raiz (e `cronograma.md`: semanas 24 a 46) |
| **Conceito usado antes de ensinado** | ⚠️ a varredura **por cabeçalho** (acima) não acha nada; a varredura **por termo** é feita por leitura nos registros de cada módulo ("Avisos de ordem" dos `README.md`, que **declaram** as retomadas: por exemplo P10.01 retoma o ADR 0010 da J8.03, P9.08 retoma a decisão 152 da P7.06). Os usos **antecipados** que existem são **menções sinalizadas** ("veja P9.05", tabelas "hoje → futuro"), não código que o aluno execute antes da aula. **Não** achei um segundo caso como o da J1 (Correções pendentes) |
| **Consistência de nomes** (`User`, `Account`, `Transfer`, `LedgerEntry`, `com.payflow`, `payflow-J…/P…`) | ✅ iguais nos registros e nos `checkpoint.md`; as divergências conhecidas estão em "Correções pendentes" (decisão 29 **substituída** pela 115/122 na P4; decisão 39/56 **revisadas** pela 138 na P6; 58/64 **superadas** pela 172 na P10) |
| **Competências da matriz sem aula** | ✅ **nenhuma no Pleno** (tabela da Parte 6). A lacuna de **mensageria** deixada no Júnior **foi fechada pela P3** |

### O que o nível deixou **aberto** (consolidado: leia antes dos testes práticos)
Nada abaixo foi corrigido por conta própria (regra do treinamento: **registrar e avisar**). Estas são as pendências que mais afetam quem for **executar** o PayFlow no fim:

| # | Pendência | Onde está | Como tratar |
|---|---|---|---|
| 1 | **Decisão 152 incompleta:** "6 × pool 10 < `max_connections`" ignora o **excedente do deploy** (`maximum_percent = 200` no ECS, `maxSurge` no Kubernetes). A regra correta é `tarefas × (maximum_percent/100) × pool + administração + Flyway ≤ max_connections` (**decisão 171**, P9.08). **E, durante a rotação de senha da P10.05, dois pools coexistem na mesma tarefa**: a conta **dobra de novo** se você recriar o `HikariDataSource` em vez de usar o `HikariConfigMXBean` | `progresso.md` (P9); P9.08; P10.05 (exercício 3) | **Decisão do aluno:** adotar a fórmula da 171 como substituta da 152. A **Parte 5** usa a conta; o item 2.9 do checklist a cobra |
| 2 | **T21** (Keycloak fora do ar → `503`, não `403` vazio) e **T13** (eventos de administração do realm): **abertas**, com dono e data no `threats.csv` | P10.07 | fechar **T21** (teste do `503` + alarme) antes de declarar o Resource Server pronto; **T13** é ligar e coletar os eventos de administração |
| 3 | **cosign** do laboratório da P10 usa `:latest` (a CLI **mudou** entre a 2 e a 3); o Trivy já está por *digest* | P10.06 | fixar **por digest** no `release.yml` e no laboratório |
| 4 | **Kubernetes não executado** (P7): `kind`, `kubectl` e `helm` não foram instalados (o ambiente negou). **Todos** os manifestos, o chart e o roteiro dos 7 experimentos estão "por leitura" | P7 | ordem de validação no `progresso.md` ("P7 (a validar…)"): `helm lint` → `--dry-run=server` → roteiro → … |
| 5 | **GitHub Actions real, AWS real, ECS, EKS, Lambda/SnapStart, RDS (Multi-AZ, *failover*, PITR, Proxy), Keycloak em produção, mTLS, KMS e rotação gerenciada** não foram executados (o moto emula só a **API**) | P8, P9, P10 | lista "a validar no fim do treinamento" de cada módulo; 💸 **alerta de custo antes** de criar qualquer coisa |
| 6 | **Erro achado na P7** (registrado na P8): o *template* do chart renderiza `repo:tag` e não aceita *digest* | `progresso.md` (P8) | a P8 usa `_image.tpl`; **corrigir o chart da P7** antes do passo 4 acima |
| 7 | **Pendências herdadas do Júnior** que ainda bloqueiam a compilação: `Transfer.complete()` ausente (J5), `@WebMvcTest` × `@ConfigurationPropertiesScan` (J7.03), `ActuatorTest` da J5.15 (302 em vez de 404), ordem J1.05–J1.07 | `progresso.md` | resolver **antes** de qualquer teste prático |
| 8 | **Cache de ferramentas** deixado no disco da máquina do laboratório: `~/.cache/p8-trivy` (~3,2 GB) e `~/.cache/p8-tf-plugins` (~0,8 GB) | `progresso.md` (P8) | apague quando não for mais rodar os laboratórios |

> **Regra de ouro do nível:** para cada módulo, o que foi **executado** (laboratório com Docker, Maven, moto, Keycloak, Prometheus) é evidência; o que foi **lido** é **hipótese** até o teste prático. Quando algo "da aula" divergir do que você obtém, **a execução manda**: corrija a aula e registre.

---

## Parte 2: Projeto PayFlow avaliado por checklist

**Entrega:** o repositório `payflow` na tag **`payflow-P10`** (e as tags `payflow-P1` … `payflow-P9` no caminho: `git tag -l 'payflow-*'`). Marque `[x]` só com **evidência** (comando e saída, captura de tela ou link do commit). "Parece que funciona" não vale. Onde a aula diz **"não executado"**, escreva isso ao lado até executar: é honesto e aceitável.

> **Estado em que o PayFlow chega aqui (por leitura):** monólito **modular** (Spring Modulith, hexágono pragmático) com **Outbox**, **Kafka/RabbitMQ**, **Redis**, **PostgreSQL 18**, o **serviço de notificações extraído** e o **gateway**, **observabilidade** completa (ECS/JSON, Micrometer, Prometheus, Grafana, OpenTelemetry), **imagem** por *digest*, **Kubernetes/Helm** e **ECS/Fargate**, **Terraform** (5 módulos), **CI/CD** com *scan*/SBOM/assinatura, **identidade no Keycloak** (Resource Server) e **modelo de ameaças** versionado. Contrato HTTP `/api/v1` **inalterado** desde a J6; dinheiro continua **uma transação** (decisão 104).

### 2.1 Linha de base (build, versões, repositório)
- [ ] `./mvnw verify` termina com `BUILD SUCCESS` em uma máquina **limpa** (clone novo), JDK 25 e Docker no ar. Evidência: saída do comando. *(Depende das pendências 7 e 4 da Parte 1.)*
- [ ] A **suíte inteira** (J5–P10) está verde **de uma vez**: lição repetida na P2, P5 e P10 (teste "passando pelo motivo errado" só aparece com a suíte completa).
- [ ] `git tag -l 'payflow-*'` mostra **J0 … J10 e P1 … P10**; commits em Conventional Commits; nenhum segredo no repositório (`scripts/ci/check-secrets.sh`).
- [ ] **Migrações** `V1 … V12` aplicam em um banco vazio (`ddl-auto: validate`); a **V13** (remover `users.password_hash`) **só** depois que todos os usuários estiverem no Keycloak.

### 2.2 Java, concorrência e dados (P1, P2)
- [ ] `ConcurrentTransfersInvariantsTest` **verde com PostgreSQL real** e **vermelho** quando você tira o `@Version` **e** a verificação de saldo (prova de que o teste detecta a corrida).
- [ ] `DeadlockPreventionTest` verde e **vermelho** se trocar `lockAllOrdered` pela ordem dos argumentos; a fachada de *retry* repete **só** `ConcurrencyFailureException`, com `idempotency_key` **a mesma**.
- [ ] Nenhum `Seq Scan` em `ledger_entries` nas consultas do extrato; índices criados com `CONCURRENTLY` (`transactional-lock: false`); extrato por **cursor**.
- [ ] `StatementCountTest` verde (e vermelho se trocar `@EntityGraph` por `LAZY`); nenhuma coleção ilimitada mapeada; cache de 2º nível **desligado**.
- [ ] Cache do saldo: leitor concorrente **sem dado velho** (`BalanceCacheTest`), `immediateWrites`, `CacheErrorHandler` e *timeouts* curtos; `@Cacheable` **depois** da checagem de dono.
- [ ] `LoginThrottle` (se ainda existir) com teste de concorrência — **a P10 o substitui pelo realm** (decisão 172): confirme que **saiu** e que o teste **saiu junto**.

### 2.3 Comunicação e arquitetura (P3, P4, P5)
- [ ] **Nenhum** passo que altera dinheiro é assíncrono; todo consumidor é **idempotente**, com teste de reentrega (3× e concorrente) que **falha** sem a proteção.
- [ ] **Outbox:** o relé usa `for update skip locked`; *rollback* não deixa linha; `PayflowOutboxBacklog` alarma; DLQ com *runbook* e alarme.
- [ ] `/webhooks/**` verifica HMAC dos **bytes**, janela de tempo e `eventId` único: **3 reenvios = 1 crédito**.
- [ ] `ModularityTests.verify()` e **ArchUnit** no CI; **cada regra** tem um **exemplo ruim** que a faz falhar; `allowedDependencies` em todos os módulos; nenhuma tabela com **dois donos**.
- [ ] **Com o serviço de notificações parado, a transferência funciona** (teste); toda chamada remota tem **timeout de conexão e de leitura** (teste com servidor lento); *retry* só de operação idempotente, **uma camada**; *circuit breaker* com `record`/`ignore` **explícitos** e alerta quando abre.
- [ ] **Pacto** do consumidor verificado **no CI do provedor**, com a prova de que **renomear** campo falha e **acrescentar** passa; o **gateway** não roteia `/internal/**`.

### 2.4 Observabilidade (P6)
- [ ] Log JSON (ECS) com `requestId`, `traceId`, `spanId`; **`ERROR` uma vez**; 404/405/400 **não** viram `ERROR`; o log da *thread* assíncrona **tem** os ids (teste que falha sem o `MdcTaskDecorator`).
- [ ] `/actuator/*` **só** na 8081 (não publicada); `prometheus` exige o papel `METRICS` (um teste por barreira); teste de **cardinalidade** (150 contas = **1** série de `uri`).
- [ ] Os **9 alertas** (e os 12 da P9) com `severity`, `summary` e `runbook`; `promtool check rules` **e** `promtool test rules` no CI, com caso **negativo**.
- [ ] *Readiness* = `readinessState` + `db`; *liveness* = só o processo; nenhuma sonda no `health` agregado.

### 2.5 Containers e Kubernetes (P7) *(⚠️ Kubernetes não executado)*
- [ ] Imagem com **tag imutável**, **uid numérico**, sem segredo (`docker history`); orçamento de memória com **números medidos**; `MaxRAMPercentage`, coletor e `ExitOnOutOfMemoryError` **de propósito**.
- [ ] `helm lint`, `helm template` e `kubectl apply --dry-run=server` **sem erro**; no kind, os **7 experimentos** com **esperado × obtido**; *rollout* sob tráfego **sem 5xx**; HPA sobe de 2 para > 2; `OOMKilled` reproduzido.
- [ ] Namespace com **Pod Security `restricted`**; Secrets como **arquivos** (`configtree`); a 8081 **fora** do Service e do Ingress.

### 2.6 CI/CD, IaC e nuvem (P8, P9) *(⚠️ GitHub e AWS reais não executados; 💸)*
- [ ] `actionlint` e `shellcheck` limpos; `ci-passed` recusa `skipped`/`cancelled`; a `main` exige **só** `ci-passed`; imagem **escaneada antes do push**, tag imutável, SBOM guardado, ações e Trivy **por SHA/digest**.
- [ ] Deploy **por digest**, aprovação em produção, *smoke test* que **reprova** segurança desligada, **reversão automática**; migrações *expand/contract*; *flags* com **dono e vencimento**.
- [ ] **5 módulos Terraform** com `terraform test` verdes (22 casos) e **3 mutantes** reprovando; *state* remoto com bloqueio, versionado e cifrado; `trivy config` limpo; **nenhum segredo no *state***.
- [ ] *Task role* com **menor privilégio**; `check-network.sh` reprova a rota criada à mão; **alarme por DLQ e por idade de fila**; réplica **só** para `reporting`; `networkaddress.cache.ttl` declarado.
- [ ] 💸 **Só se você fez a parte AWS:** alerta de custo **antes** de criar recurso, MFA, `terraform destroy` + conferência de que **não sobrou nada**. Se não fez, escreva "não executado".

### 2.7 Segurança (P10)
- [ ] A API **recusa**, **um teste cada**: expirado, outro emissor, outra audiência, ID token, `alg=none`, HS256 com a chave pública, assinatura de outra chave, `sub` adulterado; e **aceita** a rotação de chave do JWKS.
- [ ] `/internal/**` exige **escopo e `azp`**; **um cliente por serviço**; nenhum `client_secret` no Git; a conta da aplicação **não** faz DDL nem `UPDATE`/`DELETE`/`TRUNCATE` em `ledger_entries` (teste que **tenta e falha**).
- [ ] **T21 fechada** (a API responde `503`, não `403` vazio, com o Keycloak fora) **ou** com data; **T13** com dono e data; `check-threat-model.py` no CI.
- [ ] Rotação de senha do banco **com carga: 0 falhas**; a **ordem errada** falha (prova); SBOM da **imagem** por release; o portão **recusa tag** e **recusa sem assinatura**; **digest implantado = digest escaneado e assinado**.

### 2.8 Documentação e decisões
- [ ] `README.md` do `payflow` com: o que é, como rodar (3 comandos), como testar, **ADRs 0001–0022**, e **o que você aprendeu/errou**.
- [ ] Cada módulo deixou seus `docs/*.md` e **runbooks** (P3: 7 notas; P4: 5; P5: 4; P6: `observability.md` + runbooks + postmortem; P7: `kubernetes.md`; P8: 3 + runbook; P9 e P10: os seus) e batem com o que o repositório faz.

### 2.9 A conta de conexões, fechada (item **obrigatório** do nível)
Este é o único item que atravessa **P2, P7, P9 e P10** e que a decisão 152 deixou errado. Preencha **com os seus números** (`SHOW max_connections;` no banco; `maximum-pool-size` e `maximum_percent`/`maxSurge` do seu deploy):

| Termo | Fonte | Seu valor |
|---|---|---|
| `max_connections` do **primário** | `SHOW max_connections;` | ___ |
| Tarefas no **teto** do HPA (N) | P7.06 / `autoscaling` | ___ |
| Excedente do deploy (`maximum_percent/100`, ou `1 + maxSurge`/N) | P9.02 / P7.03 | ___ |
| `maximum-pool-size` (P) | P2.06 | ___ |
| Conexões de administração, Flyway e monitoração (A) | P2.06 / P9.08 | ___ |
| **Pico no deploy** = N × excedente × P + A | decisão 171 | ___ **≤** `max_connections` ? |
| Durante **rotação de senha** com `HikariConfigMXBean` | P10.05 | **igual** ao pico acima (as conexões migram) |
| Durante rotação **recriando o pool** | P10.05, exercício 3 | **até 2×** na tarefa em rotação (some o pool antigo ao novo) |

Exemplo da aula (P9.08, valores **de exemplo**): N = 6, excedente 2, P = 10 → **120 > 100**: as tarefas novas **falham na *readiness*** e o *deploy* é revertido; `maximum_percent = 150` (9 tarefas × 10 = 90) ou `maximumPoolSize = 6` resolvem. **Réplica de leitura:** conta **por banco** (o `reporting` tem o *pool* dele na réplica, que tem o `max_connections` **dela**).

**Nota do projeto:** conte os itens marcados **com evidência** (os "não executado" **não** contam como feitos, mas também não como erro).
- **≥ 80%** dos itens **executáveis** ✅ pronto · **60–79%** ⚠️ pronto com plano de reforço (liste os itens abertos e a aula que os cobre) · **< 60%** ❌ refaça os módulos indicados antes de seguir.

---

## Parte 3: Quiz

### 3.1 Quiz por módulo
Cada módulo tem o seu quiz de 10 questões com gabarito no `checkpoint.md` dele. Refaça **sem consultar** e anote a nota. Se alguma ficar abaixo de 7, releia as aulas da coluna da direita.

| Módulo | Quiz | Nota (0–10) | Se < 7, releia |
|---|---|---|---|
| P1 | [`P1-java-a-fundo/checkpoint.md`](P1-java-a-fundo/checkpoint.md) | ___ | P1.01–P1.13 |
| P2 | [`P2-banco-persistencia-avancados/checkpoint.md`](P2-banco-persistencia-avancados/checkpoint.md) | ___ | P2.01–P2.11 |
| P3 | [`P3-comunicacao-sincrona-assincrona/checkpoint.md`](P3-comunicacao-sincrona-assincrona/checkpoint.md) | ___ | P3.01–P3.13 |
| P4 | [`P4-arquitetura-aplicacoes/checkpoint.md`](P4-arquitetura-aplicacoes/checkpoint.md) | ___ | P4.01–P4.11 |
| P5 | [`P5-microsservicos-integracao/checkpoint.md`](P5-microsservicos-integracao/checkpoint.md) | ___ | P5.01–P5.10 |
| P6 | [`P6-observabilidade/checkpoint.md`](P6-observabilidade/checkpoint.md) | ___ | P6.01–P6.07 |
| P7 | [`P7-containers-kubernetes/checkpoint.md`](P7-containers-kubernetes/checkpoint.md) | ___ | P7.01–P7.09 |
| P8 | [`P8-cicd-iac/checkpoint.md`](P8-cicd-iac/checkpoint.md) | ___ | P8.01–P8.08 |
| P9 | [`P9-nuvem-intermediaria/checkpoint.md`](P9-nuvem-intermediaria/checkpoint.md) | ___ | P9.01–P9.08 |
| P10 | [`P10-seguranca-intermediaria/checkpoint.md`](P10-seguranca-intermediaria/checkpoint.md) | ___ | P10.01–P10.07 |

### 3.2 Quiz integrador (20 questões, ~50 min, sem consulta)
Estas questões **cruzam módulos**: é assim que os problemas aparecem no trabalho. Cada resposta indica as aulas em que o assunto foi **medido ou explicado**.

1. Duas transferências simultâneas saem da mesma conta e o saldo final está **certo**, mas uma delas demora 30 s e a outra falha com `deadlock detected`. Explique o que o `lockAllOrdered` muda, por que a fachada de *retry* fica **fora** da transação e o que ela reenvia. *(P2.02, P2.03, P1.09)*
2. O pool do banco tem 10 conexões e a API começa a devolver `503`. Diga **três** causas possíveis (uma delas **não** é "pool pequeno"), o que o log `Connection is not available… (total=10, active=10…)` informa e que alerta da P6 acusaria. *(P2.06, P6.04, P3.01)*
3. O mesmo evento do Outbox é entregue **duas vezes** ao notificador. Isso é um bug do broker? Diga qual garantia de entrega o PayFlow assume, onde a **idempotência** mora e como o teste de reentrega prova que ela funciona. *(P3.06, P3.07, P3.09)*
4. Por que o ouvinte do Outbox é **síncrono** e o de notificação usa `@ApplicationModuleListener`? Que falha cada escolha evita e que prova (teste) existe para o *rollback*? *(P3.09, P4.07)*
5. Um colega propõe "marcar a classe `Account` com `@Service` e injetar o repositório JPA no controller para ganhar tempo". Que **três** regras de arquitetura isso quebra e qual teste automático barraria? *(P4.01, P4.02, P4.10)*
6. O serviço de notificações fica lento. O *checkout* continua funcionando? Por que **sim** no PayFlow, e o que teria acontecido com `RestTemplate` sem *timeout* e `@Retry` empilhado em duas camadas? *(P5.07, P5.08, P5.10)*
7. O Pact do consumidor passou, mas o gateway devolve `404` para a rota nova em produção. O que o contrato **não** cobre e que teste de roteamento do gateway cobre? Que **risco de versão** o laboratório da P5 documentou? *(P5.05, P5.09)*
8. Um alerta de **p99** dispara, mas o p50 e a média estão normais. Descreva o caminho **métrica → escopo → traço → log** e por que a **conta** é atributo de *span* e **não** etiqueta de métrica. *(P6.02, P6.05, P6.07)*
9. Por que `/actuator/prometheus` fica na porta 8081 e exige o papel `METRICS`? O que a **cadeia de segurança** da 8080 tem a ver com a 8081? *(P6.02, J8.01)*
10. O pod foi **`OOMKilled`** (exit 137) e o *heap* parecia pequeno. Liste **quatro** consumidores de memória fora do *heap* e o que fixar para a JVM falhar de forma **legível**. *(P7.02, P7.09, P1.01)*
11. Por que a *readiness* inclui o banco e a *liveness* **não**? O que acontece, em cadeia, se você pôr o banco na *liveness* e ele ficar fora 2 minutos? *(P6.06, P7.05)*
12. A imagem foi **escaneada** e passou, mas o que subiu no ambiente é "a mesma tag". Por que **tag não é identidade**, o que `digest`, assinatura e SBOM da imagem garantem juntos e o que o portão `verify-before-deploy.sh` recusa? *(P8.02, P9.02, P10.06)*
13. O *canary* a 5% mostra erros "normais" e você promove para 100%; os erros explodem. Que **duas** armadilhas de medição a P8 mostrou (sobre a taxa da versão **nova** e o **ciclo fechado** da carga)? Que gatilho de reversão **numérico** você escreve **antes**? *(P8.03)*
14. A *feature flag* nova liga o ramo B para 10% dos usuários. Por que o *hash* precisa de **sal**, por que testar **os dois ramos** contra as invariantes do *ledger* e quando a *flag* **morre**? *(P8.04)*
15. O `terraform apply` quer **destruir** o banco. Que três proteções você já tem (e onde), o que o `plan` mostra e por que **ler o `plan`** foi o que descobriu o recurso *tainted* no laboratório da P9? *(P8.05–P8.07, P9.08)*
16. O deploy do PayFlow no ECS quebra **só** na sexta, no pico. Faça a conta (N, excedente, pool, `max_connections`), diga por que a **readiness** barra as tarefas novas e duas mudanças que resolvem **sem** reduzir a capacidade em regime. *(P9.02, P9.08, P2.06, P7.06)*
17. O consumidor SQS recebe a mesma mensagem duas vezes e o alarme de DLQ **nunca** dispara. Por quê (visibilidade, `maxReceiveCount`, idempotência) e qual alarme **você** acrescenta (idade da fila)? *(P9.04, P9.07, P3.08)*
18. O usuário fez *logout*, e a API aceitou o token por mais 4 minutos. Foi bug? Qual a decisão (e a ameaça **T07**), e o que fazer para **encurtar** a janela sem introspecção a cada chamada? *(P10.03, P10.07)*
19. Você rotaciona a senha do banco com **um** usuário: tudo "funciona" por horas e então a primeira conexão nova falha. Explique o **porquê**, a rotação por **dois usuários alternados** (ordem dos passos) e a **conta de conexões** durante a janela. *(P10.05, P9.08)*
20. Em 5 linhas, escolha entre **corrigir** e **aceitar** o risco T21 (Keycloak fora do ar). Que **evidência** muda uma ameaça de "aberta" para "mitigada" no `threats.csv`, e o que o verificador faz com uma data de revisão vencida? *(P10.07, P10.03)*

<details>
<summary>Gabarito do quiz integrador (respostas-modelo)</summary>

1. `lockAllOrdered` trava as contas **sempre na mesma ordem** (por id), então duas transferências cruzadas (A→B e B→A) não esperam uma à outra em círculo: **sem ciclo, sem deadlock**. A fachada de *retry* fica **fora** da transação porque, depois do erro, a transação **já foi desfeita**: repetir **dentro** dela é repetir numa transação abortada. Ela repete só `ConcurrencyFailureException`, com limite, atraso e *jitter*, e reenvia **a mesma** `idempotency_key` (o resultado é o mesmo, nunca uma segunda transferência). Os 30 s indicam espera por trava (ou `lock_timeout` alto): um limite curto transforma espera infinita em erro tratável.
2. Causas: (a) **transação longa** segurando a conexão; (b) **chamada remota dentro de `@Transactional`** (a conexão fica presa esperando a rede: a regra "nenhum `@Transactional` chama `RestClient`"); (c) **vazamento** de conexão ou uma **consulta lenta** (índice faltando) que ocupa o pool; só em último caso, pool pequeno **para a carga**. O log diz que o pool está **cheio e todo em uso** (`active = total`) e que ninguém conseguiu uma conexão em `connection-timeout`; o que se procura é **quem segura**. O alerta é o `PayflowDbPoolSaturated` (espera constante = saturação, página). A API traduz para `503` com Problem Details.
3. Não é bug: o PayFlow assume **pelo menos uma vez** (*at-least-once*) e **não** prometeu *exactly-once*. A idempotência mora **no consumidor** (chave do evento + restrição única/`processed_messages`; o efeito é aplicado **uma vez** mesmo com a entrega repetida). O teste entrega o mesmo evento **3 vezes e de forma concorrente** e afirma **um** efeito; e **falha** quando você remove a proteção (prova de que o teste detecta).
4. O ouvinte do **Outbox é síncrono** porque a linha do Outbox **tem de nascer na mesma transação** da transferência: se o *commit* falha, a linha some junto (**teste: *rollback* não deixa linha**); um ouvinte `@TransactionalEventListener` (depois do *commit*) perderia o evento se a JVM caísse entre o *commit* e o ouvinte. O `@ApplicationModuleListener` (assíncrono, depois do *commit*, com o registro de publicação do Modulith) serve para **efeitos colaterais tolerantes a atraso** (notificação, antifraude) e exige **idempotência**, porque pode reentregar.
5. Quebra: (1) **o controller não pode depender de persistência** (P4.01: camadas); (2) o **domínio e as portas** não importam `org.springframework.data` (P4.02, hexágono pragmático da decisão 116); (3) `@Service` em entidade mistura **papéis** e viola o **SRP** (P4.03); também o `@Autowired` em campo (P4.10). **Barra:** o ArchUnit no CI (regra "controller não acessa repositório" com o **exemplo ruim** que prova que ela falha) e o `ModularityTests.verify()`.
6. O *checkout* **continua** porque a transferência **não chama** o notificador de forma síncrona: ele reage a **eventos do Outbox** (teste: com o serviço **parado**, a transferência funciona; decisão 135 a). Sem *timeout*, cada chamada presa **segura uma *thread*** até esgotar o servidor (falha em **cascata**, P5.10); com `@Retry` em **duas camadas** (cliente **e** gateway) o número de tentativas **multiplica** (3 × 3 = 9) e amplifica a carga sobre o serviço que já está lento. *Timeout* **menor** que o do chamador, *retry* **numa camada**, *breaker* com alerta.
7. O contrato (Pact) prova que **consumidor e provedor concordam sobre o corpo e os campos**; **não** prova que o **gateway roteia** a rota nova (predicado, `StripPrefix`, `trusted-proxies`). Isso é o **teste de roteamento no CI**. O laboratório documentou o **risco de versão**: **nenhum trem do Spring Cloud declara o Boot 4.1** (2025.1 → 4.0.9; 2026.0-RC2 → 4.2) e o gateway funcionou assim mesmo; por isso o teste de roteamento é **a rede de segurança** em cada atualização.
8. **Métrica:** p99 alto e p50 normal ⇒ **segmento**, não "tudo lento" (média e p50 mentem). **Escopo:** "todos ou alguns?" — o painel agrega; pergunte ao Jaeger por traços ≥ 1 s e olhe o atributo `payflow.source.account`. **Traço:** abra **um** traço lento e veja **qual *span*** concentra o tempo. **Log:** o `traceId` da resposta (e o `requestId` do ticket) leva à linha do `ERROR`. A conta é **atributo de span** (um registro por requisição amostrada, pesquisável) e **não** etiqueta (cada conta cria séries: 1.000 contas = 1.000 séries de **um** contador, e quebra o Prometheus).
9. O Actuator expõe dados internos (métricas, às vezes *heap*, *loggers*): fica em uma **porta de gestão não publicada** (8081), **fora** do Service e do Ingress, e `prometheus` exige `METRICS` (anônimo 401, `USER` 403, `METRICS` 200: **uma barreira testada por vez**). **A cadeia de segurança vale também na porta de gestão** (P6): "porta diferente" é **uma camada**, não a **única**; por isso a 8081 tem as suas barreiras testadas.
10. Fora do *heap*: **metaspace**, **pilhas das threads**, **memória direta** (NIO/Netty, `MaxDirectMemorySize`), **código compilado (code cache)** e o **GC** (estruturas do coletor), além do **próprio SO** (cache de arquivos). Fixe: `MaxRAMPercentage` **de propósito** (não o padrão), o coletor, `MaxDirectMemorySize` e **`ExitOnOutOfMemoryError`** (a JVM morre com `OutOfMemoryError` **legível** em vez de ser morta pelo *kernel*, exit **137** sem causa visível); **limite de memória = requisição**. Meça com NMT.
11. A **readiness** diz "posso receber tráfego agora?": se o banco cai, **tirar o pod do Service** é o certo. A **liveness** diz "meu processo está travado?": se o banco cair e a *liveness* depender dele, o **Kubernetes reinicia todos os pods** a cada falha, derruba o que funcionava (inclusive o que não precisa do banco), e a **volta** dos pods **sobrecarrega o banco** que acabou de voltar (tempestade de reconexões): uma **falha em cascata criada pela sonda**.
12. **Tag é mutável** (alguém reenvia outra imagem com a mesma tag: no laboratório da P10, a **tag trocada** passou a apontar para outro conteúdo); **digest** é o **conteúdo**. O **scan** vale para aquele digest; a **assinatura** diz **quem** aprovou **aquele** digest; o **SBOM da imagem** diz **o que há dentro**. O portão `verify-before-deploy.sh` **recusa tag** (só digest), **recusa sem assinatura** e **aceita assinado**; e **digest implantado = digest escaneado e assinado**. (O digest **muda** entre *builds* não reprodutíveis; a data do `project.build.outputTimestamp` ajuda.)
13. (1) A taxa que importa é a **da versão nova**, **não** a agregada: com 5% do tráfego, a taxa global **dilui** o erro; compare **canary × estável** em janelas iguais. (2) Carga de **ciclo fechado** (cada usuário espera a resposta) **subconta** falhas quando o servidor fica lento (a carga **cai sozinha**): compare **janelas** e use carga de **taxa fixa**. **Gatilho numérico** antes: "taxa de 5xx do canary > 2× a do estável por 5 min **ou** p99 > X ms ⇒ reverte". Verifique **sempre o rollback** (um `0%` inválido no nginx foi achado no laboratório).
14. O **sal** impede que o mesmo usuário caia **sempre** no mesmo grupo em **todas** as *flags* (correlação) e permite **re-sortear** por *flag*; o *hash* **estável** garante que o usuário não "pisque" entre ramos. Testam-se os **dois ramos** contra as **invariantes do *ledger*** (saldo = Σ lançamentos; dinheiro = uma transação) porque a *flag* **não pode mudar o que é correto**, só **como**. A *flag* de **release** **morre** quando a promoção termina: tem **dono e vencimento** (teste de CI que reprova *flag* vencida).
15. `prevent_destroy` no **balde do *state***, no **banco** e no **ECR** (produção); `deletion_protection` + *snapshot* final no RDS (P9.08); o `plan` vai mostrar `-/+ destroy and then create` (ou `-` sozinho) **antes**; **a revisão humana do `plan`** é o controle. No laboratório da P9, ler o `plan` mostrou o recurso ***tainted* com dados do primário** (a réplica) — o que **nenhum** teste automatizado mostrou. **Regra:** nunca `apply` sem ler; nunca `-auto-approve` em produção sem *plan* aprovado.
16. Exemplo da aula: **N = 6**, excedente **2** (`maximum_percent = 200`), **P = 10**: **6 × 2 × 10 = 120** (+ administração e Flyway) **> 100**. As tarefas **novas** não conseguem abrir o pool, **falham na *readiness*** (o banco) e o *circuit breaker* do ECS **reverte** o deploy: **só no teto**, por isso **só no pico**. Resolvem sem reduzir a capacidade: **`maximum_percent = 150`** (9 × 10 = 90 ≤ 100), **`maximumPoolSize` = ⌊0,8 × 100 / 12⌋ = 6**, `max_connections` maior (custa memória) ou **RDS Proxy**. (A decisão 152 não considerava o excedente: pendência 1 da Parte 1.)
17. Reentrega por **visibilidade**: se o consumidor não apaga a mensagem antes de `visibilityTimeout` (processamento lento), a fila **a devolve**; o `maxReceiveCount` só manda para a DLQ **depois de N recebimentos sem apagar**: processar **devagar, mas com sucesso depois da 2ª entrega** não chega à DLQ, então o alarme de DLQ **fica em silêncio**. **Idempotência** (`processed_messages`) torna a segunda entrega **inócua**. Acrescente o alarme de **idade da mensagem mais antiga** (`ApproximateAgeOfOldestMessage`): a fila **atrasada** aparece mesmo sem DLQ.
18. Não é bug: o JWT é **autocontido**; a API confere **assinatura e prazo**, **não pergunta** ao Keycloak. Medido na P10: `logout=204`, `refresh-after-logout=400`, introspecção `active:false` e a **API ainda `200`**. A decisão (ADR 0022): *access token* de **5 min** + refresh **revogado** no logout, e o intervalo restante é **risco aceito T07** (dono e data de revisão). Encurtar: `exp` menor (mais refresh), **lista de revogação** curta de `jti` em cache para os casos sensíveis (dinheiro), ou **introspecção só em rotas críticas** (custa uma chamada e **acopla** a API ao Keycloak: a T21).
19. Com **um** usuário, `ALTER ROLE … PASSWORD` **não** derruba conexões abertas (o PostgreSQL só autentica no *handshake*): tudo parece bem por horas; a **primeira conexão nova** (reciclagem por `max-lifetime`, escalonamento, reinício) **falha** (`failed after 1502 ms`, o `connectionTimeout`, medido) — longe da causa. **Alternados** (`app_a`/`app_b` no grupo `payflow_app`, permissões **no grupo**): (1) altera a senha do **inativo**; (2) o pool passa a usá-lo (`HikariConfigMXBean`, as conexões **migram**); (3) só **depois** altera a do antigo. **Ordem errada** (alterar o que está em uso antes): falha (prova do laboratório). **Conta:** com o MXBean, a **mesma** do pico (N × excedente × P); recriando o pool, **dobra** na tarefa em rotação.
20. **Corrigir** quando o dano é alto e a correção é barata: T21 (Keycloak fora ⇒ `403` vazio, **API indisponível por engano**) tem correção **pequena** (responder `503` + alarme) e dano **médio-alto** ⇒ **feche**. **Aceitar** quando o custo supera o dano e há **dono, prazo e revisão** (T07). **"Mitigada" exige evidência existente** (teste, arquivo ou saída) que o verificador **confere**: ameaça marcada mitigada **sem** evidência reprova o CI; **data de revisão vencida** também reprova (no `--self-test`, com data futura, acusou T07 e T13 vencidas). **Ignorar** (sem dono, sem data) **não** é aceitar.

</details>

**Nota:** 18–20 ✅ · 14–17 ⚠️ (reforce os módulos das questões erradas) · < 14 ❌ (refaça os quizzes por módulo).

---

## Parte 4: Simulado de entrevista pleno

> O simulado tem **duas fases**: **(A)** 20 perguntas técnicas (40 min) e **(B)** o **design de um serviço** (45 min, quadro branco ou papel). Em voz alta, sem consulta, de preferência gravado ou com um colega.

### 4.A Perguntas técnicas (20 perguntas, 40 minutos)
**Regras:** cada resposta cabe em **1 a 2 minutos**. Uma boa resposta tem **definição → por que importa → exemplo com número ou fato medido → quando NÃO usar**. No pleno cobra-se o **trade-off**: "depende" sem dizer **de quê** vale 0. As respostas-modelo estão **fechadas**: tente antes.

**Blocos:** Java e concorrência (1–4) · Banco (5–8) · Mensageria e arquitetura (9–13) · Operação, nuvem e segurança (14–19) · Comportamental (20).

1. Explique o **Java Memory Model** em uma frase e dê um exemplo de bug que `volatile` **não** resolve.
2. G1, Parallel ou ZGC: como você escolhe e **como lê** um log de GC para decidir?
3. *Virtual threads*: quando ajudam, quando **não** e o que é *pinning*?
4. Como você reproduziria e corrigiria uma **condição de corrida** que "só acontece em produção"?
5. Dê um exemplo de cada anomalia: *dirty read*, *non-repeatable read*, *phantom*, *lost update*. Qual o isolamento padrão do PostgreSQL?
6. **Otimista** ou **pessimista**? Dê um critério e um caso do PayFlow para cada.
7. Como você lê um `EXPLAIN ANALYZE` e decide um índice? Cite um índice **parcial** ou de **cobertura** e quando ele **não** vale a pena.
8. Cache com Redis: que problemas aparecem (invalidação, *stampede*, dado velho) e **o que você nunca** cacheia?
9. *At-least-once*, *at-most-once* e "*exactly-once*": o que cada uma significa e como se obtém o **efeito** de *exactly-once*?
10. Padrão **Outbox**: o problema que resolve, como funciona e **o que ele não resolve**.
11. **Retentativas** e DLQ: quando **não** repetir, como calcular o *backoff* e o que fazer com a DLQ cheia.
12. Hexagonal, SOLID e DDD: o que você **realmente usa** no dia a dia e onde isso é **exagero** (o PayFlow escolheu um hexágono "pragmático": por quê)?
13. Monólito modular ou microsserviços? Dê **critérios** (e o que o PayFlow extraiu, e por quê).
14. **Timeout**, *retry*, *circuit breaker* e *bulkhead*: qual problema cada um resolve e que erro de **configuração** os torna inúteis ou perigosos?
15. Logs, métricas e traços: o que cada um responde e por que **alta cardinalidade** vai no *span* e não na métrica?
16. O que acontece no Kubernetes quando a *liveness* falha? E a *readiness*? Que parâmetros você acerta para um **deploy sem 5xx**?
17. *Rolling*, *blue/green* e *canary*: diferenças, custo e **como decide reverter**.
18. Terraform: o que é o *state*, por que o remoto precisa de **bloqueio, versionamento e cifra**, e o que fazer quando alguém muda o recurso **à mão**?
19. OAuth 2.0 e OIDC: diferença, Authorization Code + PKCE em uma frase e **como a API valida** um JWT (e o que **ela** acrescenta ao padrão).
20. Descreva um **incidente** em que você participou (ou o da P6.07): como investigou, o que **não** funcionou e que **ação com dono** saiu dele.

<details>
<summary>Respostas-modelo</summary>

1. O JMM define **quando a escrita de uma thread é visível a outra** (relação *happens-before*). `volatile` garante **visibilidade e ordem** de **uma** variável, **não atomicidade** de uma sequência: `contador++` com `volatile` ainda perde atualização; `if (!lock) { lock = true; … }` (*check-then-act*, como o do `LoginThrottle` da J8.06) continua corrida. Use `AtomicLong`/`synchronized`/lock, ou um estado **imutável** trocado por referência.
2. **G1** (padrão, equilíbrio), **Parallel** (vazão máxima, pausas longas: lotes), **ZGC** (pausas curtas em *heap* grande, custa CPU/memória). A escolha **não** é por moda: **meça** com o log de GC (`-Xlog:gc*`): pausa máxima, frequência, **taxa de alocação**, tempo em GC (% do total) e o *heap* **depois** de cada coleta (se **cresce**, é vazamento, não GC). **Em contêiner com 1 CPU o padrão vira Serial** (medido na P1/P7): confira o que a JVM **escolheu**.
3. Ajudam em **I/O bloqueante** com **muita concorrência** (cada requisição, uma thread barata). **Não** ajudam em **CPU-bound**, nem tiram o gargalo do **pool de conexões** (10 conexões continuam 10) — ligar sem dimensionar o Hikari e sem `Semaphore` em chamadas externas **move** o gargalo. ***Pinning***: a thread virtual **prende a portadora** durante o bloqueio e perde o ganho. Até o Java 21, `synchronized` fixava; no 24+ (JEP 491) não fixa mais (**provado** com a mesma classe nas duas JVMs, P1.10); ainda fixam métodos nativos e funções estrangeiras. Diagnóstico: o evento do JFR `jdk.VirtualThreadPinned`.
4. **Reproduzir** com um teste que **aumenta a chance**: muitas threads liberadas por um `CountDownLatch`/barreira, **sempre com `timeout`**, repetido; confirmar que ele **falha** na versão ruim. **Corrigir** com a ferramenta menor possível (atômico, trava, `@Version`, restrição única no banco) e **provar** que o teste agora passa e **volta a falhar** se você remover a correção. Em produção: invariante verificável (saldo = Σ lançamentos) como **conciliação**.
5. *Dirty read*: ler dado **não confirmado** (o PostgreSQL **não** permite, mesmo em `READ UNCOMMITTED`). *Non-repeatable*: ler a mesma linha duas vezes e ver **outro valor** (alguém confirmou entre as leituras). *Phantom*: a mesma consulta devolve **linhas novas**. *Lost update*: duas transações leem `saldo = 100`, ambas gravam e **uma sobrescreve a outra**. Padrão: **`READ COMMITTED`**; `REPEATABLE READ` do PostgreSQL é **snapshot** (falha de serialização `40001` em vez de esperar: `CannotAcquireLockException` no Spring, medido na P2).
6. **Otimista** (`@Version`): **conflito raro**, custo só no conflito, mas o perdedor **repete** (precisa de *retry* idempotente); caso: edição de perfil. **Pessimista** (`for update`/`for no key update`): **conflito frequente** ou custo alto de refazer; caso: **débito/crédito da transferência** (travar as contas **em ordem de id**). O PayFlow **mantém os dois** (decisão: `@Version` + `lockAllOrdered`).
7. Procure **onde o tempo vai**: `Seq Scan` em tabela grande com filtro seletivo, estimativa × real **muito diferentes** (estatística velha), `Sort` em disco, `Nested Loop` com milhões de voltas. Índice **composto** na ordem **igualdade → ordenação**; **parcial** (`WHERE status = 'PENDING'`) quando só uma fração é consultada; **cobertura** (`INCLUDE`) para *Index Only Scan*. **Não vale** em tabela pequena, baixa seletividade ou coluna que muda o tempo todo (custa **escrita** e espaço); crie com `CONCURRENTLY` (e `transactional-lock: false` no Flyway).
8. **Invalidação** (a parte difícil), ***stampede*** (muitos pedindo o mesmo item expirado: *lock* de recarga, TTL com *jitter*), **dado velho** (leitor concorrente durante o *commit*; `immediateWrites`). **Nunca** cacheie: o que **autoriza** (checar dono **antes** do cache), dado pessoal sem necessidade, ou **o saldo como fonte da verdade** (o cache guarda `BalanceView`, **nunca** a entidade).
9. *At-most-once*: pode **perder**, nunca duplica. *At-least-once*: **nunca** perde, pode **duplicar**. "*Exactly-once*" **de entrega** não existe em rede arbitrária; o que existe é **exactly-once de efeito**: **at-least-once + consumidor idempotente** (ou transações dentro do mesmo sistema, como Kafka→Kafka). O PayFlow assume *at-least-once* e **valida a idempotência por teste**.
10. Problema: gravar no banco **e** publicar no broker não é atômico (**dual write**): o *commit* pode passar e a publicação falhar (ou o contrário). **Outbox:** grave o evento **na mesma transação** do dado; um **relé** (`for update skip locked`) publica depois e marca como enviado. **Não resolve:** duplicata (o relé pode reenviar: consumidor **idempotente**), ordem global, nem o **atraso** (monitore `outbox_pending`).
11. **Não repita** erro de **dado** (mensagem inválida: vai **direto** ao DLT), nem operação **não idempotente** sem chave. *Backoff* **exponencial com *jitter***, com **teto** menor que `max.poll.interval.ms` (ou o consumidor é expulso). **DLQ cheia** é **incidente**: alarme, *runbook* (inspecionar, corrigir a causa, **reprocessar** com idempotência) e dono; **não** é lixeira.
12. Uso **todo dia**: separar **caso de uso** de **adaptador** (portas), **SOLID** no que muda (Strategy para regras de limite), **value objects** (`Money`) e **agregado** pequeno com `@Version`. **Exagero:** DDD estratégico em um domínio que cabe num time, camada de mapeamento para **toda** classe, "porta" para cada tabela. O PayFlow escolheu o hexágono **pragmático** (decisão 116: `domain` só importa `jakarta.persistence`) porque **trocar de ORM** não é o risco real; o que se paga é a **testabilidade** e a **regra de dependência** (ArchUnit).
13. **Modular primeiro**; divida quando houver **razão**: escala **independente**, **ritmo de entrega** diferente, **isolamento de falha** ou **dono/time** separado — e **nunca** o dinheiro (**uma transação**, decisão 104). O PayFlow extraiu o **serviço de notificações** (já era assíncrono, falha **isolável**, dono próprio) e **não** extraiu `account`/`transfer`. Custo de dividir: rede, versões, contrato, observabilidade e **consistência eventual**.
14. ***Timeout***: limita a espera (sem ele, a *thread* fica presa para sempre). ***Retry***: falha **transitória** (só **idempotente**, *backoff* + *jitter*, **uma camada**). ***Breaker***: para de chamar o que está falhando (dá folga e falha rápido). ***Bulkhead***: isola recursos (uma dependência lenta não consome todas as *threads*; dimensione por `L = λ × W`). **Erros:** *retry* sem *timeout*; *retry* em **duas camadas** (multiplica); `fallback` privado ou na anotação **errada** (não dispara); *breaker* que conta **4xx** como falha; *timeout* do cliente **maior** que o do chamador.
15. **Métricas** (agregado: **que** há problema, **desde quando**); **traços** (**onde** o tempo/erro foi, **por onde** a requisição passou); **logs** (**o que** o código viu naquele ponto). **Alta cardinalidade** (conta, usuário, `traceId`) em **etiqueta** cria uma série por valor e **derruba** o Prometheus (150 contas = 1 série de `uri` é o **teste**); como **atributo de span**, custa um registro por requisição **amostrada** e é pesquisável.
16. *Liveness* falha ⇒ o `kubelet` **reinicia o contêiner**; *readiness* falha ⇒ o pod **sai do Service** (sem reiniciar). Deploy sem 5xx: `readinessProbe` verdadeira, `preStop sleep` (tempo de o Service e o Ingress tirarem o pod), `terminationGracePeriodSeconds ≥ preStop + timeout-per-shutdown-phase + folga`, `maxUnavailable: 0`, *shutdown* gracioso do Spring, e (por leitura) PDB.
17. ***Rolling***: troca aos poucos, **barato**, misturando versões (exige compatibilidade). ***Blue/green***: duas pilhas completas, troca **instantânea** e **rollback imediato**, **custa o dobro** e exige banco compatível. ***Canary***: **fração** do tráfego na versão nova, **detecta** problema com **dano pequeno**, exige **métrica boa** e roteamento. **Reverter** por **gatilho numérico escrito antes** (taxa de 5xx da versão **nova**, p99, saturação do pool), e **teste do próprio rollback**.
18. O *state* é o **mapa** entre o código e os recursos reais (ids, atributos, **segredos em texto**). Remoto com **bloqueio** (dois `apply` não se pisam: `use_lockfile`, medido: segundo recebe `412`), **versionamento** (voltar de um *state* corrompido) e **cifra** (tem segredo). **Alteração à mão** = *drift*: `terraform plan` o mostra; decida **codificar** a mudança ou **desfazê-la**; `import` para trazer recurso existente, `moved` para renomear sem destruir. **Nunca** edite o *state* à mão.
19. **OAuth 2.0** = **autorização delegada** (um cliente age em nome do dono do recurso, com *token*); **OIDC** = camada de **identidade** por cima (**ID token**, `sub`, *userinfo*). **Authorization Code + PKCE**: o cliente prova (`code_verifier`) que **é o mesmo** que iniciou o fluxo, então o **código roubado é inútil**; o login acontece **no servidor de identidade**. A **API** (servidor de recursos) valida **assinatura (JWKS), `exp`/`nbf` e emissor** (o Spring faz) e **você acrescenta**: `aud` (`payflow-api`), `typ=Bearer` (recusa ID token), o **conversor** (escopo → `SCOPE_x`, papel → `ROLE_x`) e o **dono pelo `sub`** (IDOR).
20. Estrutura **STAR** com o incidente (real, ou o da P6.07): **sintoma** (p99 × 300 com p50 igual), **escopo** (uma conta, pelo atributo no span), **causa** (risco lento + *timeout* longo + chamada síncrona no caminho crítico), **o que não funcionou** (degradação de segmento não deu página; o 500 raiz não era `ERROR` no traço), **ações com dono e prazo**. **Sem culpa**: "o sistema permitia".

</details>

**Como se avaliar:** nota 0 (não sabia), 1 (disse algo vago), 2 (respondeu com **exemplo e trade-off**). **34–40** ✅ · **26–33** ⚠️ · **< 26** ❌. Registre as perguntas em que tirou 0 e releia a aula na coluna "Aula" de `recursos/perguntas-entrevista.md`.

### 4.B Design de um serviço (45 minutos)

> **Enunciado: "Cobranças recorrentes" (`recurring-service`).** O PayFlow quer permitir que um usuário **agende uma transferência recorrente** (por exemplo, R$ 50,00 todo dia 5, da conta dele para a de um beneficiário). Desenhe o serviço: **dados, API, fluxo, falhas, observabilidade, segurança e entrega**. Você tem **45 minutos**: 5 de perguntas ao entrevistador, 25 de desenho, 10 de falhas e trade-offs, 5 de "o que eu faria depois".

**Perguntas que você **deve** fazer antes de desenhar** (e **anotar** as suposições): quantos agendamentos (10 mil? 10 milhões?); o que fazer se o saldo faltar no dia (tentar de novo? por quanto tempo? avisar?); fuso horário e "dia 31" em fevereiro; quem pode **cancelar** e até quando; o serviço **mexe no dinheiro** ou **pede** a transferência ao PayFlow?; há requisito legal de aviso prévio?

**Roteiro de resposta (o que a rubrica procura):**

| # | Tema | O que um bom desenho diz | Aulas |
|---|---|---|---|
| 1 | **Fronteira** | O serviço **agenda e dispara**, mas **a transferência é feita pelo PayFlow** (uma transação, decisão 104): o recorrente **não** toca no saldo | P5.01, P5.02, P4.08 |
| 2 | **Modelo** | `recurring_transfers` (dono, origem, destino, valor `NUMERIC(19,2)`, regra de recorrência, **próxima execução em UTC + fuso do usuário**, estado) e `recurring_runs` (**uma linha por ocorrência**, `UNIQUE (schedule_id, due_date)`) | P2.04, P2.05, J3.12 |
| 3 | **Disparo** | `@Scheduled` + **ShedLock** (`lockAtMostFor` justificado) ou fila de ocorrências devidas lida com `for update skip locked`; **nunca** dois nós disparando a mesma ocorrência | P3.02, P3.09 |
| 4 | **Idempotência** | A `Idempotency-Key` da chamada ao PayFlow é **derivada** da ocorrência (`schedule_id + due_date`): repetir (rede, *retry*, falha do nó) **não** duplica | J6.06, P3.07 |
| 5 | **Falhas** | *Timeout* e *retry* **idempotente** com *backoff*+*jitter*; saldo insuficiente é **erro de negócio** (422: **não** repetir em laço; política de nova tentativa **com limite** e aviso); erro de dado → **DLQ** com alarme e *runbook* | P5.07, P3.08, P5.08 |
| 6 | **Eventos** | Resultado publicado por **Outbox** (notificar: `RecurringRunSucceeded/Failed`); consumidores **idempotentes** | P3.09, P4.07 |
| 7 | **API** | `POST/GET/DELETE /api/v1/recurring-transfers` (dono pelo `sub`, **404** para o de outro: IDOR), paginação por cursor, Problem Details, contrato **versionado**; dinheiro **string** | J6, J8.06, P10.03 |
| 8 | **Segurança** | Token do usuário na API; **Client Credentials** (um cliente `payflow-recurring`, menor escopo `transfers:write`) para chamar o PayFlow **em nome do agendamento** (não há usuário na sessão: **como** autorizar? limite por valor, **consentimento** gravado); sem `client_secret` no Git; **STRIDE** do fluxo | P10.01, P10.04, P10.05, P10.07 |
| 9 | **Observabilidade** | Métricas **RED** + de negócio (`recurring.runs{outcome}`, **atraso** do agendador = `now - due`), alarme de **atraso** e de **DLQ**; `traceId` ligando agendador → PayFlow; **conta como atributo de span**, nunca etiqueta | P6.02–P6.05 |
| 10 | **Entrega** | Imagem por **digest**, *readiness* = banco; *rollout* sem 5xx; **conta de conexões** (N × excedente × P ≤ `max_connections`); migração **expand/contract**; *flag* para ligar por grupo; **reversão** com gatilho numérico | P7.05, P8.03, P8.04, P9.08 |
| 11 | **Trade-offs** | Serviço **separado** (dono, escala, isolamento) × módulo no monólito (**menos** rede e consistência eventual): **justifique**; o que **não** fazer agora (calendário complexo, múltiplos beneficiários) | P5.01, P4.09 |

**Rubrica (100 pontos):**

| Critério | Pontos | O que o avaliador procura |
|---|---|---|
| **Entendimento do problema** (perguntas, suposições escritas) | 10 | perguntou antes de desenhar; escreveu o que **assumiu** |
| **Fronteira e dados** | 15 | o **dinheiro não sai** do PayFlow; `UNIQUE` por ocorrência; fuso e "dia 31" |
| **Corretude sob falha** (idempotência, 2 nós, *retry*, saldo insuficiente) | 25 | chave derivada; nenhum disparo duplo; erro de negócio ≠ erro técnico |
| **Operação** (métricas, alarmes, *runbook*, atraso) | 15 | alarme de **atraso** e de DLQ; `traceId` ponta a ponta |
| **Segurança** (IDOR, consentimento, escopo mínimo, segredos) | 15 | **quem autoriza** quando não há usuário na sessão |
| **Entrega e custo** (deploy, conexões, migração, *flag*, reversão) | 10 | a conta de conexões; *expand/contract* |
| **Comunicação** (diagrama, trade-offs, "o que não faria") | 10 | disse **o que não resolve** e **por quê** |

**Resultado:** **≥ 80** ✅ pleno sólido · **65–79** ⚠️ ok com plano de reforço · **< 65** ❌ refaça os módulos ligados aos critérios com menos pontos e repita com **outro** enunciado (por exemplo, "**cupons de cashback**" ou "**limites por conta**").

<details>
<summary>Armadilhas comuns (leia só depois de tentar)</summary>

- **Disparar dentro de uma transação** que também chama o PayFlow por HTTP (conexão presa: P2.06). O disparo **lê** a ocorrência, **sai da transação**, chama, e **grava** o resultado.
- **Calcular a próxima data somando 30 dias** (o dia 31, fevereiro, horário de verão): guarde a **regra** e calcule a próxima ocorrência **a partir da regra**, em UTC, com o fuso do usuário **explícito**.
- **Repetir a cobrança em laço quando falta saldo** (o usuário toma tarifa, o banco toma carga): limite de tentativas, **aviso**, e **parar**.
- **Um único cliente OAuth "serviço" com todos os escopos** (P10.04): o comprometimento do recorrente abre **tudo**.
- **Alarme só de erro**: o agendador **parado** não gera erro nenhum (**silêncio** é falha): alarme de **atraso** (`now - próxima execução`) e de "nenhuma execução em N minutos".

</details>

---

## Parte 5: Incidente simulado (logs, métricas e traces)

> ⚠️ **Leia isto primeiro.** O dossiê abaixo é **sintético**: foi **escrito para este exercício**, **não** medido. Os **números** (taxas, horários, contagens) são **inventados e rotulados como tais**; os **mecanismos** que ele usa (a conta de conexões, a *readiness* com o banco, o alarme de *pool*, o Outbox, o `traceId` no Problem Details) vêm de aulas em que foram **medidos** (P2.06, P6.04, P6.07, P7.05, P9.08). O incidente **medido de verdade** (pilha Prometheus + Grafana + Jaeger, `./incident.sh`) é o da [P6.07](P6-observabilidade/P6.07-investigando-um-incidente-simulado.md); se quiser sentir os sinais **reais**, rode-o antes (Docker; ~3 min) e depois faça este. **Não use as saídas abaixo como referência de comportamento real**; use-as como **exercício de raciocínio**.

### 5.1 O que fazer (sessão CK-P.5, ~1h30)
1. **Leia só o enunciado e os sinais** (5.2 e 5.3). **Não** abra o gabarito.
2. Responda as **8 perguntas** (5.4) **por escrito**, na ordem. Cronometre **45 min**.
3. Compare com o gabarito (5.5). Reescreva o que errou.
4. Escreva o **postmortem sem culpa** (modelo na 5.6) com **ações com dono e prazo**, e o **runbook** (5.7), **sem** olhar o gabarito do runbook.
5. Registre no `docs/` do PayFlow: `docs/postmortem-ck-p-deploy-no-pico.md`.

### 5.2 Enunciado
**Sexta-feira, 17h38.** O PayFlow roda no ECS/Fargate (P9.02) com **6 tarefas** (o teto do HPA: é o horário de pico), `deployment_maximum_percent = 200`, `maximum-pool-size = 10` por tarefa e um RDS PostgreSQL com **`max_connections = 100`** (valor do exercício). O *pipeline* (P8) publicou uma **nova revisão** da tarefa: **mesma aplicação**, só mudou o *digest* da imagem (uma atualização de dependência da P10.06). O *deploy* (rolling, com o *circuit breaker* do ECS ligado) começa às **17h40**.

**17h47.** O plantão recebe **duas páginas**: `PayflowHighErrorRatio` (5xx > 2% com tráfego) e, **seis minutos depois**, `PayflowOutboxBacklog`. O suporte cola no canal a reclamação de uma usuária, com o **`traceId`** que a resposta de erro devolveu. Você é a pessoa de plantão e **ninguém sênior está disponível**.

**Restrições de dinheiro (regras do projeto):** **nunca** altere saldo ou lançamentos à mão; reparação só por **lançamento de compensação** (P3.13); **nunca** reinicie o banco "para ver se melhora".

### 5.3 Os sinais (sintéticos)
**Linha do tempo dos alertas** (horário local; `for` já considerado):

| Hora | Alerta | Severidade |
|---|---|---|
| 17h47 | `PayflowHighErrorRatio` | página |
| 17h48 | `PayflowDbPoolSaturated` | página |
| 17h53 | `PayflowOutboxBacklog` | página |
| 17h59 | `PayflowTransfersSlow` | ticket |

**Métricas** (janelas de 1 min; **valores inventados**):

```text
                                   17h38   17h44   17h49   17h56   18h06
http_server_requests p50 (s)       0.012   0.013   0.014   0.014   0.012
http_server_requests p99 (s)       0.140   0.210   30.100  30.050  0.150
taxa de 5xx (%)                    0.1     0.2     11.8    9.6     0.2
hikaricp_connections_pending       0       0       14      11      0       (tarefas antigas)
payflow_outbox_pending             3       4       61      212     6
Keycloak: latência do JWKS p99     38 ms   41 ms   40 ms   39 ms   38 ms
```

**Banco** (consulta ao `pg_stat_activity` por quem **tem** a conexão, às 17h49; **inventada**):

```text
 application_name            | conexões
-----------------------------+---------
 payflow (revisão antiga)    |   58
 payflow (revisão NOVA)      |   40
 flyway / admin / monitoração|    2
 total                       |  100   (max_connections = 100)
```

**Log** de uma tarefa **antiga** às 17h49 (JSON/ECS, formatado para leitura; **inventado**):

```text
17:49:12.404 ERROR unexpected failure | requestId=ticket-suporte-9021 | traceId=4f1c…a7 |
   error.type=org.springframework.jdbc.CannotGetJdbcConnectionException
   caused by: org.postgresql.util.PSQLException: FATAL: sorry, too many clients already
17:49:12.405 WARN  HikariPool-1 - Connection is not available, request timed out after 30000ms (total=10, active=0, idle=0, waiting=14)
```

**Log** de uma tarefa **nova** (17h42–17h58; **inventado**):

```text
17:42:31.220 INFO  Started PayflowApplication in 18.4 seconds
17:42:33.901 WARN  readiness: db DOWN | FATAL: sorry, too many clients already
17:42:48.902 WARN  readiness: db DOWN | FATAL: sorry, too many clients already
```

**Traço** da requisição da usuária (`traceId` do ticket; **inventado**):

```text
http post /api/v1/transfers      30011 ms  status=500  outcome=SERVER_ERROR
  payflow.transfer                    0 ms  (não chegou a começar)
  jdbc.connection.acquire         30008 ms  otel.status_code=ERROR  exception=CannotGetJdbcConnectionException
```

**Fatos do ECS** (também inventados): às 17h40 o serviço passou de 6 para **12** tarefas; **3** das 6 tarefas novas ficaram `RUNNING` mas **nunca** `HEALTHY` no *target group* (readiness com o banco). Às 17h58, o *circuit breaker* **reverteu** o *deploy* para a revisão anterior.

### 5.4 Perguntas (8)
1. **Triagem.** Pela **ordem dos alertas** e pelo que as métricas dizem do **p50**, isto é lentidão geral, de um segmento ou **falha de um recurso compartilhado**? O que **muda** na sua primeira ação se a resposta for "recurso compartilhado"?
2. **Escopo.** Quem está sendo afetado: **todos** os usuários, um grupo ou só quem passou pelas tarefas novas? Que dado das 5.3 **prova** isso?
3. **Traço e log.** Onde o tempo foi gasto na requisição da usuária? O que o **traço** mostra que o **log sozinho** esconderia (pense em `payflow.transfer` com 0 ms)? Por que isso importa para a pergunta "**o dinheiro dela está certo?**"
4. **Hipóteses.** Liste **quatro** hipóteses plausíveis (inclua **o Keycloak** e **o código novo**) e, para **cada uma**, o **dado** das 5.3 que a **descarta ou sustenta**.
5. **Causa.** Escreva a **conta** (N, excedente, pool, `max_connections`) que explica o incidente. Qual **decisão** do treinamento estava **incompleta** e por que o problema só apareceu **na sexta**, **no pico**?
6. **Mitigação.** O que você faz **agora**, em ordem? Diga **o que não fazer** (cite três ações tentadoras e **por que** são ruins aqui).
7. **Verificação do dano.** Depois da mitigação, como você **prova** que o dinheiro está correto e que o Outbox **drenou**? O que é **dado perdido** e o que é **atraso**?
8. **Sistema.** Escreva **cinco ações** (com dono e prazo) que impedem a **recorrência**, **uma** que melhora a **detecção** e **uma** de **processo** (sem apontar pessoas).

### 5.5 Gabarito (leia **depois**)

<details>
<summary>Gabarito do incidente</summary>

1. **Recurso compartilhado.** O **p50** quase não mexe (0,012 → 0,014 s) enquanto o **p99** vai a **30 s** e o 5xx a ~12%: a cauda é o `connection-timeout` do *pool* (30 s) e a maior parte das requisições que **conseguem** conexão é rápida. O alerta de **`PayflowDbPoolSaturated`** (**causa acionável**, P6.04) disparou **um minuto depois** do de erro: **o banco/pool** é o suspeito nº 1. **Muda a ação:** não se "escala" a aplicação (mais tarefas = **mais conexões**: piora), não se reinicia o banco; **para-se a causa** (o *deploy*) e **protege-se o recurso**.
2. **Todos** os que passam pelas tarefas **antigas** (que são as que **servem** usuários: as novas nunca ficaram saudáveis). A prova é a **consulta ao `pg_stat_activity`**: **40 conexões estão com as tarefas novas, que não atendem ninguém** (readiness `DOWN`), e as antigas, que precisam crescer o *pool* sob o pico, **batem** em `too many clients already`. Os `hikaricp_connections_pending` (14 e 11) estão nas **antigas**. (Um segmento por conta, como na P6.07, **não** é o caso: o erro vem do **recurso**, não do dado.)
3. O tempo (30 s) está **todo** em `jdbc.connection.acquire`, **antes** de a transação começar: o `payflow.transfer` tem **0 ms**. **Dinheiro:** a transferência **nunca começou**; nada foi debitado nem lançado (o 500 é **antes** da transação: não há meia-transferência). O log, sozinho, só diz `CannotGetJdbcConnectionException` e uma `PSQLException`: **não** diz **onde** o tempo foi nem que a transação **não começou**. O traço mostra as duas coisas. (Com a `Idempotency-Key`, a usuária **pode repetir com segurança**.)
4. (a) **Código novo** (regressão): **descartada** como causa principal: mesma aplicação, só mudou o *digest* (dependência); e o **erro é de conexão**, não de lógica; o **que** precisa ser conferido é se a atualização de dependência mudou o *pool* (**verifique** o `maximum-pool-size` efetivo). (b) **Keycloak:** **descartada**: a latência do JWKS fica em ~40 ms **o tempo todo** e o erro é `FATAL` do **PostgreSQL**, não 403 vazio/503 de emissor (T21). (c) **Banco lento** (consulta cara, *lock*): **descartada**: `active=0` no *pool* antigo (`total=10, active=0, idle=0, waiting=14`): as conexões **nem existem**, não estão "em uso por consulta lenta". (d) **Excesso de conexões por causa do *deploy***: **sustentada**: 100 de 100; 40 nas tarefas **novas** (não prontas); coincide com o horário de início do *deploy*. (e) **Vazamento de conexão** (a transação longa segura): **descartada** por `active=0`.
5. **N = 6, excedente = `maximum_percent/100` = 2, P = 10:** pico = 6 × 2 × 10 = **120**, + administração/Flyway **> `max_connections` = 100**. As tarefas novas começam a abrir *pools* e **disputam** com as antigas; as novas não ficam **prontas** (a readiness inclui o banco), **mas continuam segurando** as conexões que conseguiram (40); as antigas, ao subir o *pool* sob o pico, recebem `too many clients already`. **Decisão incompleta:** a **152** (P7.06: "6 × pool 10 < `max_connections`") **ignora o excedente do *deploy*** (decisão **171** da P9.08 corrige). **Só na sexta no pico:** com 2 tarefas (fora do pico), 2 × 2 × 10 = 40 < 100: o *deploy* **funciona** o resto da semana. É um *deploy* que quebra **só no teto**.
6. **Em ordem:** (1) **Comunicar** no canal (impacto, hora de início, "estou conduzindo") e **abrir a ata**. (2) **Parar a causa:** **reverter o *deploy*** (o *circuit breaker* fez às 17h58; **manualmente**, `update-service` para a revisão anterior e/ou `maximum_percent` menor) — isso **devolve** as 40 conexões. (3) **Observar** `hikaricp_connections_pending` → 0, 5xx → ~0, `payflow_outbox_pending` **caindo**. (4) **Registrar** a evidência (`pg_stat_activity`, gráficos) **antes** que se perca. **Não faça:** **(a) aumentar o `maximum-pool-size`** ou **escalar para mais tarefas** (mais conexões, pior); **(b) reiniciar o RDS** ("para limpar", derruba **tudo** e **piora** o pico e o Multi-AZ faria *failover*); **(c) matar sessões no banco às cegas** (`pg_terminate_backend` em massa derruba transações **em andamento** de usuários **legítimos**); **(d)** mexer em **saldo**/lançamentos "para consertar" (nunca: **compensação**).
7. **Dinheiro:** a **conciliação** (`saldo = Σ lançamentos`, a consulta da P1.13/regra 3, **0 linhas divergentes**) **mais** a contagem de transferências **concluídas** × linhas de `ledger_entries` na janela; falhas ocorreram **antes** da transação, então **não** deve haver divergência (se houver, **é outro incidente**, e vira reparação por **compensação**). **Outbox:** `payflow_outbox_pending` voltou a ~6 (às 18h06) e `PayflowOutboxBacklog` **resolveu**; os eventos **acumulados** (212 no pior momento) são **atraso**, não perda (o relé **os publica** quando recebe conexão); os consumidores são **idempotentes**, então eventuais reenvios **não duplicam** efeito. **Dado perdido:** as **requisições** 5xx dos usuários (podem repetir com a mesma `Idempotency-Key`); **nenhum** evento nem saldo.
8. **Recorrência (5):** (1) **Corrigir a decisão 152** e registrar a fórmula da **171** no ADR (dono: arquitetura; prazo: antes do próximo *deploy*). (2) **`deployment_maximum_percent` = 150** (9 × 10 = 90 ≤ 100) **ou** `maximumPoolSize` = 6 (dono: plataforma). (3) **Teste/portão no *pipeline*** que calcula `N × excedente × P + A` contra o `max_connections` do ambiente e **reprova** (dono: plataforma; P8). (4) Avaliar **RDS Proxy** (dono: dados; custo 💸). (5) **Ensaio de *deploy* no pico** em ambiente de teste (*game day*). **Detecção (1):** alerta de **conexões do banco > 80% de `max_connections`** (**antes** de bater o teto) e de **tarefas novas sem ficar saudáveis por > 5 min**. **Processo (1):** **janela de *deploy*** fora do pico de sexta **e** *checklist* de *deploy* com "conta de conexões" (a **sexta às 17h** foi um **fator**, não o culpado). **Sem culpa:** "o sistema **permitia** um *deploy* que dobra as conexões sem avisar".

</details>

### 5.6 Modelo do postmortem (preencha **você**)
```markdown
# Postmortem: deploy no pico esgotou as conexões do banco (CK-P)
**Resumo.** (2 linhas: o que quebrou, por quanto tempo, impacto em usuários e **no dinheiro**.)
**Impacto.** (quem, quantos, de quando a quando; dinheiro: o que foi/NÃO foi afetado e a PROVA.)
**Linha do tempo.** (17h38 … 18h06: cada alerta, cada ação, cada decisão.)
**Causa e fatores.** (a conta de conexões; a decisão 152; o pico; a readiness com o banco. SEM nomes de pessoas.)
**O que funcionou.** (traceId no Problem Details; alerta de pool; circuit breaker do ECS; idempotência para repetir.)
**O que NÃO funcionou.** (nenhum alerta antes do teto; o deploy não calcula conexões; …)
**Ações.** | # | Ação | Dono | Prazo | (cinco de recorrência + uma de detecção + uma de processo)
**Lições.** (3 linhas.)
```

### 5.7 Runbook ("deploy lento ou com erros de conexão", rascunho)
> Escreva o seu primeiro, **depois** compare.

<details>
<summary>Runbook de referência</summary>

```markdown
# Runbook: erros de conexão com o banco durante/depois de um deploy
1. O deploy está em andamento? (ECS: eventos do serviço; tarefas novas HEALTHY?) Sim → PARE de pensar em "escalar".
2. Conexões por origem: SELECT application_name, count(*) FROM pg_stat_activity GROUP BY 1;  vs  SHOW max_connections;
3. A soma chegou perto/igual ao teto? As tarefas NOVAS seguram conexões sem estar prontas?
4. Mitigar: reverter o deploy (revisão anterior) OU reduzir maximum_percent; NUNCA aumentar pool, NUNCA reiniciar o banco, NUNCA matar sessões em massa.
5. Verificar: pending do Hikari → 0; 5xx → ~0; outbox_pending caindo; conciliação do ledger (0 linhas divergentes).
6. Escalar se 5xx > 2% por mais de 10 min OU a conciliação divergir (dinheiro). Depois: postmortem sem culpa.
```
</details>

**Rubrica (100 pontos):**

| Critério | Pontos | O que o avaliador procura |
|---|---|---|
| **Método** (métrica → escopo → traço → log, nessa ordem) | 20 | não começou pelo log; escreveu o escopo **antes** da causa |
| **Hipóteses** (inclusive as **descartadas**, com o dado que descarta) | 15 | Keycloak e código novo **descartados com evidência** |
| **Causa** (a conta e a decisão 152) | 20 | `N × excedente × P`, só no teto, só no pico |
| **Mitigação segura** (e o que **não** fazer) | 20 | reverter, **não** escalar/reiniciar/matar; **dinheiro intocado** |
| **Verificação** (conciliação, Outbox, atraso × perda) | 10 | provou, não afirmou |
| **Postmortem e ações** (dono, prazo, sem culpa, detecção, processo) | 15 | ações **no sistema**, não em pessoas |

**Resultado:** **≥ 80** ✅ · **65–79** ⚠️ (repita o incidente **real** da P6.07 e reescreva o postmortem) · **< 65** ❌ releia P6, P2.06 e P9.08.

---

## Parte 6: Autoavaliação pela matriz de competências

A matriz (`00-guia/matriz-de-competencias.md`) lista o comportamento observável de um **pleno**. A coluna "Aulas" abaixo é a **tabela "competência → aulas que cobrem"** gerada nesta revisão (a partir dos títulos das aulas e dos registros por módulo). Marque ✅ (faço sozinho) / ⚠️ (faço com ajuda) / ❌ (não faço).

| Competência (comportamento do pleno) | Aulas que cobrem | Cobertura | ✅ ⚠️ ❌ |
|---|---|---|---|
| **Linguagem Java:** explica heap, GC, JIT; usa `CompletableFuture` e `java.util.concurrent`; reproduz e corrige condição de corrida | P1.01–P1.04 (memória, JIT, GCs, logs de GC), P1.05–P1.07 (threads, `CompletableFuture`, `java.util.concurrent`), P1.08–P1.09 (JMM, corrida e *deadlock* que falham de verdade), P1.10 (virtuais), P1.11–P1.12 (Java moderno, módulos), P1.13 | ✅ completa | ☐ |
| **Lógica e estruturas de dados:** mede custo com JMH e explica `HashMap` por dentro | P1.02 (por que `nanoTime()` solto engana; o **JMH é apresentado e deixado como desafio opcional**, e a **S2.08 — Benchmarks com JMH** o ensina), P1.07 (`ConcurrentHashMap`, `HashMap` × CHM), J1.11 (o `HashMap` por dentro) | ⚠️ **parcial** (**medir com JMH** só vem na **S2.08**) | ☐ |
| **Git e ferramentas:** define fluxo de branches e revisa PRs com critério | J7.09 (revisão de PR), J10.01, P8.01 (pipeline, `ci-passed` e proteção da `main`), P8.08 (GitOps) | ⚠️ **parcial** (**revisar PRs** ✅; **definir o fluxo de branches** — *trunk-based* × *Git Flow* — **não tem aula**: a varredura não achou o assunto; **nenhum módulo Sênior previsto** o cobre: decisão sua) | ☐ |
| **Web e HTTP:** depura com `curl`, trace e inspeção de TLS; trata timeouts | J2.01–J2.06, P5.07 (timeouts e retry), P5.05 (gateway), P6.05 (traço), P10.01 (fluxos OAuth no HTTP) | ✅ completa | ☐ |
| **Banco de dados:** lê planos, escolhe isolamento, resolve deadlocks, dimensiona o pool | P2.01 (isolamento), P2.02–P2.03 (locking, deadlocks), P2.04–P2.05 (planos e índices), P2.06 (pool), P2.11 (estudo de caso), P9.08 (RDS e réplicas) | ✅ completa | ☐ |
| **Spring e persistência (JPA):** evita N+1, entende proxies e `@Transactional`, otimiza consultas | P2.07 (*fetch joins*, *entity graphs*, projeções), P2.08 (*batch*, 2º nível, `JdbcClient`, jOOQ), P2.09 (cache), P4.01–P4.03 | ✅ completa | ☐ |
| **APIs REST:** garante idempotência, paginação eficiente e contratos testados | P2.05 e P2.11 (cursor), P3.07 e P3.10 (idempotência, webhooks), P5.03–P5.04 (REST × gRPC, compatibilidade), P5.09 (contrato) | ✅ completa | ☐ |
| **Testes e qualidade:** equilibra a pirâmide; testes de contrato; TDD quando útil | J7.01–J7.06 (pirâmide, fatias, Testcontainers, **TDD na J7.06**), P5.09 (Pact), P4.10 (ArchUnit), e os testes que **provam** a regra (mutantes em P8–P10) | ✅ completa | ☐ |
| **Segurança:** OAuth2/OIDC, rotação de segredos, STRIDE | P10.01–P10.07 | ✅ completa | ☐ |
| **Front-end:** colabora com o front com contratos claros | P5.04 (compatibilidade de contratos), P3.11 (WebSockets/SSE), P5.09 (contratos), J4 | ✅ completa (no nível Pleno só **colaboração**) | ☐ |
| **Mensageria e assincronia:** Kafka/RabbitMQ com idempotência, DLQ e Outbox | P3.01–P3.13, P9.04 (SQS/SNS/EventBridge) | ✅ completa | ☐ |
| **Arquitetura:** hexagonal, SOLID, DDD tático, monólito modular | P4.01–P4.11, P5.01–P5.02 | ✅ completa | ☐ |
| **Sistemas distribuídos:** resiliência (timeout, retry, circuit breaker) | P5.07–P5.08, P5.10, P3.06 (garantias) | ✅ completa | ☐ |
| **Observabilidade e operação:** instrumenta métricas, logs e traces; investiga incidentes | P6.01–P6.07; Parte 5 deste checkpoint | ✅ completa | ☐ |
| **Containers e orquestração:** opera Kubernetes (probes, limites, HPA); ajusta a JVM no container | P7.01–P7.09, P1.03–P1.04 (GC em contêiner) | ✅ completa por **leitura**; ⚠️ **Kubernetes não executado** | ☐ |
| **CI/CD e IaC:** pipeline completo, blue/green, canary, Terraform | P8.01–P8.08 | ✅ completa; ⚠️ **GitHub e AWS reais não executados** | ☐ |
| **Nuvem e custo:** ECS/EKS, SQS/SNS, Lambda, VPC | P9.01–P9.08 (💸 em cada um) | ✅ completa por leitura + moto; ⚠️ **AWS real não executada** | ☐ |
| **Performance:** usa profiling básico e lê métricas | P1.02–P1.04 (JIT, GC), P2.04 (`EXPLAIN`), P2.11, P6.02–P6.03 (RED/USE), P7.02 (JVM em contêiner) | ✅ completa | ☐ |
| **Liderança e comunicação:** revisa código dos colegas e estima tarefas | J7.09 (revisão), P6.07 (postmortem sem culpa), P10.07 (decidir aceitar × corrigir); ⚠️ **estimar tarefas** só entra na **S7.03** | ⚠️ **parcial** (estimativa → S7.03) | ☐ |

**19 competências** exigidas do Pleno na matriz: **16 com cobertura completa** (3 delas com a ressalva de **execução**: Kubernetes, GitHub/AWS) e **3 parciais** (**JMH**, **fluxo de branches**, **estimativa de tarefas**); **nenhuma sem aula**. As três lacunas **não bloqueiam** nada do que vem depois e estão em **Correções pendentes** do `progresso.md` para você **decidir** (JMH → **S2.08** e estimativa → **S7.03** já estão **previstas**; o **fluxo de branches** não está em nenhum módulo: **decisão sua**, acrescentar uma aula ou aceitar). **Não** foram corrigidas por conta própria.

> **Nota sobre a contagem:** "completa" = tem aula **e** laboratório ou exercício; o ⚠️ **de execução** (Kubernetes, GitHub, AWS) indica que a aula existe e foi revisada **por leitura**, e a **evidência** vem do teste prático final. Se você prefere uma regra mais dura ("só vale o executado"), essas três linhas também viram ⚠️.

### Decisão de avançar
| Resultado | Decisão |
|---|---|
| Projeto ≥ 80% dos itens executáveis **e** quizzes ≥ 14/20 **e** simulado técnico ≥ 34 **e** design ≥ 80 **e** incidente ≥ 80, sem ❌ na matriz | ✅ **Avance para S1** |
| Alguma parte em ⚠️, sem ❌ | ⚠️ **Avance** com um **plano de reforço** de 1 semana: 3 itens, a aula e a data |
| Qualquer parte em ❌ | ❌ **Repita os módulos indicados** (e o design e o incidente, com outro enunciado) antes de seguir |

> **Consolide no `progresso.md`**: nota de cada parte, itens em ⚠️/❌ e o plano de reforço. Os **testes práticos finais** (compilar o PayFlow, rodar a suíte inteira, **Kubernetes no kind**, GitHub Actions, AWS real, Keycloak em produção) entram na lista "a validar no fim do treinamento" e **seguem a ordem** do `progresso.md` (P7 primeiro).

## ➡️ Próximo
[S1 — Sistemas distribuídos e system design](../3-senior/S1-sistemas-distribuidos/README.md) (o próximo módulo, a ser gerado quando você pedir)
