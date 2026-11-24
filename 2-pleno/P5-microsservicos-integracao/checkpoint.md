# P5.CP — Checkpoint do módulo P5

> ⏱️ ~1h30 · 🔵 Pleno · Pré-requisitos: P5.01 a P5.10

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5, Maven 3.9.12, **sem Docker**): `lab/integration-lab/` (Spring Boot **4.1.2**; **Resilience4j 2.4.0**; **Spring Cloud 2025.1.4** → Gateway WebMVC **5.0.4**; **Spring gRPC 1.1.2** / grpc-java **1.84.1** / protobuf-java **4.36.2**; **Pact JVM 4.7.7**; Jackson **3.1.7**): **41 testes verdes** (`resilience-lab` 17, `gateway-lab` 6, `grpc-lab` 10, `contract-lab` 8). **Por leitura:** o `payflow-notifications` e o `payflow-gateway` **do aluno**, o monólito sem a chamada síncrona, os eventos `NotificationPreferencesChanged`, a migração em modo sombra. **Não executado:** Spring Cloud Contract, Pact Broker/`can-i-deploy`, Eureka/Config Server/Kubernetes, *rate limit* com Bucket4j no gateway, TLS/mTLS, *streaming* gRPC, *schema registry*, `ThreadPoolBulkhead`, *threads* virtuais no Tomcat sob carga.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Liste **4 custos** de microsserviços e **2 benefícios** que **justificam** pagá-los. O que é um **monólito distribuído** e qual o **teste** em uma pergunta para reconhecê-lo?
2. Cinco serviços com **99,9%** em **cadeia síncrona** dão qual disponibilidade? Por que o PayFlow extraiu **só** o `notification` (cite **3** critérios) e manteve `account`/`transfer` no monólito?
3. O que é **"banco por serviço"**, o que se **perde** e como se **compensa**? Cite **2 anti-padrões** de decomposição e **1 risco** do passo "virada" do estrangulamento.
4. Compare **REST** e **gRPC** em **4 critérios**. O que o laboratório mostrou sobre **tamanho**, **latência** e **prazo** (*deadline*)? Por que **não** se conclui "gRPC é 3× mais rápido"?
5. Dê **3** regras para evoluir um `.proto` e **1** evidência do laboratório de que **reutilizar um número de campo** é perigoso.
6. O que são compatibilidades **para trás** e **para frente**? O que o Jackson 3 do Boot faz com **campo desconhecido**, **campo renomeado** e **enum novo**? Como se muda um evento de forma **incompatível**?
7. O que um *gateway* **deve** e **não deve** fazer? Cite **3** comportamentos medidos no laboratório (X-Forwarded, timeout, `CircuitBreaker` e 503, `Retry` de `POST`) e o **risco de versão** do Spring Cloud com o Boot 4.1.
8. Por que o PayFlow **não** usa Eureka nem Config Server? Qual o **problema** de um servidor de configuração **sem cache local**? Por que DNS/`Service` **não** dispensa *circuit breaker*?
9. Explique **timeout** (conexão × leitura), **retry com backoff + jitter** e a **amplificação** (**27**). Por que `POST` repetido **duplicou** (3 mensagens) e o que o `@Retry` + `fallbackMethod` mostrou sobre **onde** pôr o *fallback* (e sobre `public`)?
10. Descreva o **estudo de caso**: por que o `/ping` parou, o que mudou com a proteção (**30 → 8** chamadas; mediana **4 s → ~40 ms**), qual é a **correção de raiz** e qual **teste** impede a volta. Por que um teste de contrato (Pact) **falha** o build do **provedor** num **renome** e **passa** numa mudança **aditiva**?

<details><summary>Gabarito</summary>

1. **Custos:** rede/falha parcial, disponibilidade em cadeia, dados (sem JOIN/ACID entre serviços), contratos públicos, operação (N *pipelines*), testes e **depuração** distribuídos. **Benefícios:** *deploy* independente, escala independente, **isolamento de falha**, autonomia de times, isolamento técnico/regulatório. **Monólito distribuído:** serviços que **não** se implantam nem funcionam **independentemente** (banco compartilhado, cadeia síncrona, *deploys* coordenados). **Teste:** "posso **implantar este serviço hoje, sozinho**, e ele continua **correto** se os vizinhos caírem?"
2. 0,999⁵ ≈ **99,50%** (10 serviços: 99,00%; 20: 98,02%). **`notification` sai** porque: (1) **baixo acoplamento de dados** (só reage a eventos); (2) **falha tolerável** (a notificação pode atrasar sem corromper nada); (3) **escala/ritmo** diferentes (picos, provedor externo instável). `account`/`transfer` **ficam**: dinheiro exige **transação ACID** (decisão 104), alto acoplamento entre si e **nenhum** benefício de isolamento a ganhar.
3. Cada serviço é **dono exclusivo** dos seus dados. **Perde:** `JOIN`, ACID entre serviços, relatório cruzado. **Compensa:** API em lote, evento com o dado, cópia própria, Outbox, sagas (S1), modelo de leitura (CDC/réplica, S3). **Anti-padrões:** serviço-entidade, nanosserviço, banco compartilhado, serviço "utilitário" central, corte por camada. **Risco da virada:** **duplicidade** (falta de idempotência/`processed_messages`), **divergência** sem modo sombra e **duas fontes de verdade**.
4. **REST:** depuração fácil, alcance (navegador/parceiros), ferramentas de HTTP maduras, equipe conhece; **gRPC:** contrato `.proto` com geração de código, binário compacto, HTTP/2, *streaming*, *deadline* embutido. **Medido:** corpo **129 B** (proto) × **209 B** (JSON), resposta **4 B** × **30 B**; latência **76–127 µs** (gRPC) × **241–482 µs** (REST) em *loopback*, 1 cliente, máquina compartilhada, 3 rodadas; `DEADLINE_EXCEEDED` em **215 ms** com prazo de 200 ms, **1.504 ms de espera sem prazo**. **Não se conclui 3×** porque foi *loopback*, sem TLS, cliente REST sem *pool*, valores variaram e a **rede real** domina a diferença.
5. **Regras:** (1) **nunca** reutilizar número de campo (`reserved`); (2) **não** mudar o tipo; (3) enum com valor **0 = não especificado**; (4) dinheiro como *string*; (5) pacote versionado (`…v1`). **Evidência:** `ReplyV1 { int32 score = 2 }` lido como `ReplyV2Broken { string reason = 2 }` → `parseFrom` **não lança**, `reason=''` e o campo 2 vira **desconhecido**: **o score sumiu sem erro**.
6. **Para trás:** leitor **novo** lê dado **antigo**. **Para frente:** leitor **antigo** lê dado **novo**. **Jackson 3 (Boot):** campo **desconhecido** → **ignorado** (`FAIL_ON_UNKNOWN_PROPERTIES=false` também de fábrica no Jackson 3); campo **renomeado** → `null` **silencioso** (valide os obrigatórios); **enum novo** → `InvalidFormatException` (use `UNKNOWN` ou `READ_UNKNOWN_ENUM_VALUES_AS_NULL` em `EnumFeature`). **Mudança incompatível:** **expandir** (publicar as duas formas/tópico `v2`), **migrar** consumidores (medir *lag* no `v1`), **contrair**.
7. **Deve:** roteamento, TLS, CORS, limite grosseiro, observabilidade, *timeouts*/CB de borda. **Não deve:** regra de negócio, orquestração de domínio, substituir a autenticação dos serviços. **Medido:** sem `trusted-proxies` **não** há `X-Forwarded-*` (e `.*` em produção é perigoso); destino **lento/fora do ar → HTTP 500** (não 502/504) e `spring.http.clients.read-timeout` corta em ~609 ms; `CircuitBreaker` **sem `statusCodes`** **não** conta 503 como falha; `Retry` com `methods: GET,POST` **repetiu o POST** (4 chamadas). **Versão:** o trem **2025.1** declara o **Boot 4.0.x**; o **2026.0** (RC2) o **4.2**; **nenhum** declara o **4.1**: funcionou no laboratório, mas é **combinação não declarada** (mitigar com teste no CI).
8. Em **Kubernetes** o `Service` + DNS **já é** a descoberta, e `ConfigMap`/`Secret`/`configtree` **já** são a configuração: Eureka/Config Server seriam **dois serviços críticos a mais** sem problema que justifique. Servidor de configuração **sem cache** está no caminho de **partida**: se cair, **ninguém reinicia**. DNS/`Service` só resolve **quem está pronto**; **lentidão** e falhas parciais continuam pedindo **timeout e circuit breaker**.
9. **Conexão:** abrir o TCP/TLS; **leitura:** esperar a resposta; o padrão do JDK **não tem** *timeout* de leitura (medido: **2.003 ms** contra servidor de 2.000 ms). **Retry:** só falha **transitória** em operação **idempotente**, com *backoff* exponencial + ***jitter*** (desincroniza clientes) e limite de tentativas; **amplificação:** 3 camadas × 3 tentativas = **27** chamadas por requisição. **POST repetido:** o servidor **processou** mas respondeu **depois** do *timeout*: **3 mensagens** sem chave de idempotência. **Fallback:** na anotação **mais externa** (`@Retry`); no `@CircuitBreaker` (interna) o *fallback* vira **sucesso** e o *retry* **não age** (1 chamada em vez de 3); e o método de *fallback* deve ser **`public`** (visibilidade de pacote → `IllegalAccessException` **intermitente**, 1–2 em 30 viravam 500).
10. As **10 *threads*** ficaram presas na chamada lenta; **qualquer** requisição precisa de uma *thread*: `/ping` **sem resposta em 1 s**. Com *timeout* + *retry* + *breaker* + *bulkhead* + *fallback*: `/ping` em **3 ms**, **30/30** com 200, mediana **~37–42 ms** (sem proteção: **4.024 ms**, máx. 6.034 ms), **8** chamadas ao serviço doente (em vez de **30**). **Raiz:** **tirar a chamada do caminho crítico** (evento + cópia); o que sobrar síncrono leva as defesas. **Teste:** servidor lento no CI que **falha** se o chamador esperar além do prazo. **Pact:** o provedor com `channels → notificationChannels` **falha** (`missing the following keys: channels`); `type` falha (`Type mismatch`); **acrescentar** `quietHours` **passa** (campo extra é ignorado: tolerância).
</details>

## Parte 2: Entrega prática (tag `payflow-P5`)
O `payflow` ganha **um serviço extraído** (`payflow-notifications`), um **gateway**, **resiliência** nas chamadas remotas e **contratos testados**, **sem** mudar o contrato HTTP público `/api/v1` nem mover o **dinheiro** para fora da transação do monólito.
```
payflow/                                  (monólito; repositório existente)
├── docs/
│   ├── services.md              mapa de serviços, donos, o que é síncrono × assíncrono        (P5.01, P5.02)
│   ├── events-contract.md       envelope, versões, regras de compatibilidade                  (P5.04)
│   ├── resilience.md            tabela de timeouts/retry/breaker/bulkhead por chamada         (P5.07, P5.08)
│   └── postmortem-notificacoes-derrubaram-checkout.md                                         (P5.10)
├── src/main/java/com/payflow/
│   ├── notification/            módulo vira adaptador fino durante a virada; removido ao fim  (P5.02)
│   ├── transfer/…               SEM chamada síncrona ao serviço de notificações               (P5.10)
│   └── …/NotificationsProperties  baseUrl + connect/read timeouts (obrigatórios)              (P5.06, P5.07)
├── src/test/java/com/payflow/
│   ├── pact/ConsumerPactTest.java            gera o pacto com o serviço de notificações        (P5.09)
│   ├── events/TransferCompletedCompatibilityTest.java   amostras v1 + campo extra + enum novo  (P5.04)
│   └── resilience/NotificationClientSlowTest.java       servidor lento: falha se esperar > prazo (P5.10)
payflow-notifications/                    (repositório NOVO; mesmos princípios da P4)
├── pom.xml                      Spring Boot 4.1.2, Java 25; banco/schema PRÓPRIO, migrações V1… próprias
├── src/main/java/com/payflow/notifications/{domain,application,adapter/in,adapter/out}
├── src/test/java/…/ProviderPactVerificationTest.java  @State das interações do pacto           (P5.09)
└── src/test/java/…/ArchitectureRulesTest.java         as regras da P4.10, levadas junto         (P4.10)
payflow-gateway/                          (repositório NOVO)
├── pom.xml                      spring-cloud-starter-gateway-server-webmvc + BOM 2025.1.4
├── src/main/resources/application.yml     rotas por caminho; NENHUMA rota /internal/** ou /actuator/**   (P5.05)
└── src/test/java/…/GatewayRoutesTest.java  roteamento, destino lento, X-Forwarded forjado não passa   (P5.05)
projeto/adrs/0017-extracao-do-servico-de-notificacoes-gateway-e-resiliencia.md   (já redigido nesta geração)
```
Critérios de pronto:
- [ ] **Contrato HTTP `/api/v1` e esquema do monólito inalterados**; a suíte de J5–P4 **continua verde**; o **dinheiro** continua **uma transação** (decisão 104).
- [ ] O *checkout* **não chama** o serviço de notificações de forma **síncrona** (teste: com o serviço **parado**, a transferência **funciona**).
- [ ] **Toda** chamada remota tem **timeout de conexão e de leitura** por propriedade **obrigatória**, e **teste com servidor lento** que falha se o chamador esperar além do prazo.
- [ ] *Retry* só de operação **idempotente** (ou com `Idempotency-Key`), com *backoff* + *jitter*, **uma camada**; `fallbackMethod` **na anotação mais externa** e **`public`**.
- [ ] *Circuit breaker* com `record`/`ignore` **explícitos** (4xx não abre) e **alerta** quando abre; *bulkhead* dimensionado por `L = λ × W`.
- [ ] **Pacto** do consumidor gravado e **verificado no CI do provedor**; **prova** de que renomear um campo **falha** e acrescentar **passa**.
- [ ] Eventos com **envelope** e **teste de compatibilidade** com amostras (mínima, completa, campo extra, enum novo); consumidor valida os obrigatórios.
- [ ] **Gateway:** sem rota `/internal/**`; `trusted-proxies` restrito; *timeouts* configurados; teste de **roteamento** no CI (cobre o **risco de versão**).
- [ ] **Modo sombra** executado e **critério de desligar** o módulo antigo registrado; **flag** de reversão; `docs/` com as 4 notas; ADR 0017 revisada; tag `payflow-P5`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Pleno: **≥ 2** em todos e **3** nos marcados com ⭐.

| Competência | Aula(s) | 0–3 |
|---|---|---|
| Decidir **dividir × não dividir** (custos, benefícios, monólito distribuído) | P5.01 | ⭐ |
| **Decompor por domínio** (contextos, banco por serviço, estrangulamento) | P5.02 | ⭐ |
| **REST × gRPC**: contrato `.proto`, *deadlines*, evolução, escolha com critério | P5.03 | |
| **Compatibilidade** de APIs e eventos (para trás/frente, leitor tolerante, expandir→migrar→contrair) | P5.04 | ⭐ |
| **API Gateway**: o que faz e **não** faz; rotas, filtros, erros, risco de versão | P5.05 | |
| **Configuração e descoberta** × Kubernetes (e por que não Eureka/Config Server aqui) | P5.06 | |
| **Timeouts e retry** (backoff + jitter, idempotência, amplificação, fallback) | P5.07 | ⭐ |
| **Circuit breaker, bulkhead, rate limiter** (o que conta como falha, Lei de Little) | P5.08 | ⭐ |
| **Testes de contrato** (consumidor/provedor, Pact × SCC, o que entra) | P5.09 | ⭐ |
| Diagnosticar e corrigir uma **falha em cascata** (+ postmortem sem culpa) | P5.10 | ⭐ |

**Se algum ⭐ ficou abaixo de 2:** refaça os experimentos (`lab/integration-lab`: `./run-all.sh`) e o respectivo exercício de aplicação antes de seguir para a P6.

## Parte 4: Perguntas de entrevista (simulado rápido)
Responda em voz alta, 2 minutos cada: (1) "Monólito ou microsserviços? Como você decide?" (2) "O que é um monólito distribuído?" (3) "Como cortar um serviço de um monólito sem reescrever?" (4) "REST ou gRPC?" (5) "Como evoluir um evento sem quebrar consumidores?" (6) "O que um API gateway **não** deve fazer?" (7) "Descreva retry com backoff e jitter e quando **não** usar." (8) "Como funciona um circuit breaker e o que deve contar como falha?" (9) "Teste de contrato: como funciona e por que não basta teste de integração?" (10) "Um serviço lento derrubou o seu: diagnostique e corrija." As respostas-modelo estão em cada aula (seção "Na entrevista") e consolidadas em `recursos/perguntas-entrevista.md`.

## ➡️ Próximo
[P6 — Observabilidade](../P6-observabilidade/README.md)
