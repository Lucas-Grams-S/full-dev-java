# P6.CP — Checkpoint do módulo P6

> ⏱️ ~1h30 · 🔵 Pleno · Pré-requisitos: P6.01 a P6.07

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5, Maven 3.9.12; Docker para a pilha): `lab/observability-lab/` (Spring Boot **4.1.2**, Micrometer **1.17.2**, Micrometer Tracing **1.7.2**, OpenTelemetry Java **1.62.0**): **[INFO] Tests run: 31, Failures: 0, Errors: 0, Skipped: 0** (sem Docker) mais `promtool` (Prometheus **3.16.0**: 12 regras, 8 grupos de teste, regra quebrada **reprova**), a pilha real (**Prometheus 3.16.0, Grafana 13.3.0, Jaeger 2.23.0**) com o **incidente simulado** (`incident.sh`), `shutdown-check.sh` (**SIGTERM** com requisição em voo) e `lab/mgmt-security-lab/` (Spring Security 7 na porta de gestão, 1 teste). **Por leitura:** o `payflow` **do aluno** (logs, métricas, traces, *dashboards*, alertas, `compose.observability.yaml`, runbooks). **Não executado:** **Alertmanager** e canais de notificação, **Kubernetes** (sondas do `kubelet`, P7), OpenTelemetry **Collector**/amostragem por cauda, *agent* Java, propagação de traço por **Kafka/RabbitMQ**, Prometheus/Grafana lendo ***exemplars***, rastreadores de exceção, o incidente **no PayFlow real**.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Quais são os **três pilares**, que **pergunta** cada um responde e em que **ordem** você os usa num incidente? Qual a diferença entre **monitorar** e **observar**?
2. Qual a diferença entre `requestId` e `traceId`? Por que o PayFlow **valida** o `X-Request-Id` e o que **mede** o laboratório sobre o `traceId` quando a amostragem é **0**?
3. O que é `WARN` e o que é `ERROR` no PayFlow? Qual **armadilha** o laboratório achou em um `@ExceptionHandler(Exception.class)` e como se corrige? Por que `rename.traceId` "não fazia nada"?
4. O que acontece com `requestId` e `traceId` numa *thread* de `@Async`? Compare as **três** configurações medidas.
5. *Counter*, *gauge* e *timer*: um exemplo do PayFlow de cada. O que é **cardinalidade** e o que o laboratório mediu com **1.000 contas** como etiqueta? Por que `uri` **não** explode?
6. Por que o Actuator fica na **porta de gestão**? O que o `mgmt-security-lab` mediu e qual a solução de **menor privilégio** para o Prometheus?
7. O que são **RED** e **USE**? Como o Prometheus calcula o **p95** de um histograma e qual a **limitação**? O que o laboratório mediu sobre **média × percentis**?
8. Cite **5** regras de um bom alerta (do PayFlow). Quando exatamente dispara um alerta com `for: 5m` e por que o `promtool` **reprovou** a regra quebrada?
9. O que o `traceparent` carrega? Cite **3** formas de o traço **quebrar** e o que a amostragem **perde** e **não perde**. Por que o **raiz** de um 500 tratado não aparece em `error=true`?
10. *Liveness* × *readiness*: o que cada uma verifica e o que acontece com cada uma no `SIGTERM`? Resuma o **incidente simulado**: como se **delimitou o escopo** e qual **ação** do postmortem você priorizaria.

<details><summary>Gabarito</summary>

1. **Métricas** ("**há** problema? quanto? desde quando?": agregado, barato, alerta); **traces** ("**onde** o tempo foi gasto?": árvore de *spans* de uma requisição); **logs** ("**o que** aconteceu neste caso?": detalhe). Ordem: **métrica → escopo → traço → log**. *Monitorar* é vigiar o que você **já sabe** que pode falhar; *observabilidade* é **investigar o que você não previu** olhando os sinais.
2. `requestId`: **do cliente/suporte**, repetível, citável ("ticket 4711"), validado por `[A-Za-z0-9._-]{8,64}` (senão gera UUID: **medido**: `nao seguro {"forjado":1}` virou UUID; evita *log injection*, decisão 65); `traceId`: **do sistema**, 32 *hex* (W3C), atravessa serviços. Com `management.tracing.sampling.probability=0.0`: **0 *spans* exportados**, **mas o `traceId` continua** no log do `ERROR` **e** no Problem Details (`traceId` na resposta): **a correlação não depende de amostrar**.
3. `WARN` = negócio esperado (saldo insuficiente, 422), **sem** *stack*; `ERROR` = **inesperado**, **uma vez**, na borda, **com** *stack* (medido: 1 linha, 4.824 caracteres de `error.stack_trace`). **Armadilha:** o *catch-all* pegou o `NoResourceFoundException` e transformou **404 em 500 + `ERROR`**; correção: estender `ResponseEntityExceptionHandler` (404/405/400 mantêm o status) e tratar só o inesperado. `rename.traceId=...` perde a caixa na ligação relaxada: use **`rename[traceId]`** (resultado: chave **plana** `"trace.id"`).
4. `@Async` padrão: `traceId` **e** `requestId` **vazios**; `spring.task.execution.propagate-context=true`: `traceId` **volta**, `requestId` **não**; `MdcTaskDecorator` próprio (copia o MDC): **ambos**. O `requestId` é um MDC **seu**; só o seu `TaskDecorator` o leva. Teste, porque sem ele o log do relé "funciona" e só falha na investigação.
5. *Counter*: `payflow.outbox.published` (só sobe; `rate`); *gauge*: `payflow.outbox.pending` (valor agora, pode descer); *timer*: `payflow.transfer` / `http.server.requests` (contagem, soma, histograma). **Cardinalidade** = nº de séries = produto dos valores das etiquetas × (baldes + 2). **1.000 contas** = **1.000 medidores** e **1.000 linhas** no *scrape*; `MeterFilter.maximumAllowableTags(…, 10, deny())` deixou **10**. `uri` é o **template** do Spring MVC: 150 ids em `/api/v1/accounts/{id}` = **1 série** (contagem 150); rota inexistente = `uri="/**"`. Cuidado com URL concatenada à mão no `RestClient`.
6. Para o Actuator **não** estar na porta pública (medido: 404 na porta da aplicação; `env`/`metrics` não expostos: 404). A cadeia de segurança da J8 **vale também na porta de gestão**: sem credencial o `/actuator/prometheus` dá **401**, `USER` **403**, `ADMIN` **200**; dar `ADMIN` ao Prometheus seria excesso; **solução:** papel **`METRICS`** só para esse caminho (`scraper` = **200**), `basic_auth` com `password_file` (segredo por arquivo). **Surpresa:** o `spring-boot-starter-opentelemetry` liga o **registro OTLP de métricas** por padrão: desligar (`management.otlp.metrics.export.enabled: false`).
7. **RED** (Rate, Errors, Duration) = **serviços**, o que o cliente sente; **USE** (Utilization, Saturation, Errors) = **recursos**. O p95 vem de `histogram_quantile(0.95, sum by (le)(rate(..._bucket[5m])))`: **interpolação linear** no balde onde o percentil cai (medido: 100 requisições, 90/97/99/100 nos baldes 0,1/0,5/1/+Inf → p95 = `0,1 + 0,4 × 5/7 = 0,3857 s`); só tão preciso quanto os **baldes**. **A média mente:** com ~7% de requisições lentas, o **p50 não mudou**, a **média** foi de ~4 ms a ~106 ms, o **p99** de ~6 ms a ~1,7 s.
8. Sintoma > causa; `for` (evita pico); **só 5xx** conta (422 não); razão **com tráfego mínimo**; **`absent()`** para silêncio; `severity` (page/ticket) + `runbook`; **teste** com `promtool` incluindo o **negativo**. Com `for: 5m` e avaliação de 1 min, a condição passa a valer em **1 min** (o `rate` precisa de 2 amostras) e o alerta dispara aos **6 min**, **não** aos 5 (`promtool` confirmou). A regra **quebrada** (troquei `5..` por `4..`) fez **2 testes falharem**: "um teste que nunca falhou não prova nada".
9. Versão, **`traceId`** (32 hex), **`spanId` do pai** (16 hex) e flags (`01` = amostrado). Quebras: (1) `RestClient.create()` (medido: **0 *spans* `CLIENT`** e outro `traceId` no serviço chamado), (2) troca de *thread* sem propagar contexto, (3) mensageria com `observation-enabled=false` (padrão). Amostragem **perde os *spans***, **não** o `traceId`. O raiz do 500 tem `status=500`/`outcome=SERVER_ERROR` mas **não** `otel.status_code`: a exceção foi **convertida** pelo `@RestControllerAdvice` e não propagou; filtre `status=500`.
10. *Liveness*: **só o processo** (falhou → reinicia; **nunca** banco/rede); *readiness*: pode atender (banco, aquecimento, desligando; falhou → sai da rotação). No `SIGTERM` (medido): *readiness* **503 na hora**, *liveness* **200**, a requisição em voo termina **201** (1,6 s), nova conexão **sem resposta**, saída **143**. **Incidente:** p50 igual, p95/p99 sobem e **5xx quase igual** (nenhum alerta de erro) → o **escopo** veio do **traço**: filtro `duração ≥ 1 s` mostrou **todos** os lentos com `payflow.source.account=acc-7`; o `CLIENT` ao risco concentra o tempo; o `traceId` da resposta levou ao log do `ERROR`. Ação prioritária (a defender): **alerta de p99/fração lenta** (a degradação de segmento **não** gerou página) e **reduzir o *timeout*/tirar o risco do caminho crítico** (decisão de produto).
</details>

## Parte 2: Entrega prática (tag `payflow-P6`)
O `payflow` ganha **logs estruturados**, **métricas**, **traços**, **painéis** e **alertas**, mais **runbooks** e **postmortem**, **sem** mudar o contrato HTTP `/api/v1`, o esquema (**nenhuma migração**) nem o dinheiro (uma transação).
```
payflow/
├── pom.xml                       + actuator, micrometer-registry-prometheus, spring-boot-starter-restclient, spring-boot-starter-opentelemetry   (P6.02, P6.05)
├── compose.yaml                  healthcheck na porta 8081 (/readiness), stop_grace_period > 20s                                                  (P6.06)
├── compose.observability.yaml    perfil "observability": Prometheus, Grafana, Jaeger; portas só em 127.0.0.1; segredos por arquivo                (P6.03)
├── observability/
│   ├── prometheus/{prometheus.yml (basic_auth + password_file), alerts.yml, alerts.test.yml}                                                       (P6.03, P6.04)
│   └── grafana/{provisioning/{datasources,dashboards}, dashboards/payflow-red.json}                                                                (P6.03)
├── docs/
│   ├── observability.md          os 3 pilares no PayFlow, o que é página × ticket, mapa de métricas/etiquetas permitidas                          (P6.01–P6.04)
│   ├── runbooks/*.md             um por alerta (9)                                                                                                 (P6.04)
│   └── postmortem-p6-transferencias-lentas.md                                                                                                      (P6.07)
├── src/main/resources/application.yml       logging.structured.format.console: ecs; management (porta 8081, exposição, probes, grupos);
│                                            métricas (tags, histogramas, SLO, otlp.metrics off); tracing (0.1 em produção)
├── src/main/resources/application-dev.yml   logging.structured.format.console: (vazio = texto); show-details: always; sampling 1.0
├── src/main/java/com/payflow/…/MdcTaskDecorator, ApiErrors (+traceId), PayflowMetrics (Observation payflow.transfer, gauge/counter do Outbox), ManagementSecurity (papel METRICS)
└── src/test/java/com/payflow/…/LoggingTest, AsyncContextTest, MetricsCardinalityTest, HealthProbesTest, TracingPropagationTest, ManagementSecurityTest, ArchitectureRulesTest (+ sem RestClient.create)
projeto/adrs/0018-observabilidade-logs-metricas-traces-e-alertas.md   (já redigido nesta geração)
```
Critérios de pronto:
- [ ] **Contrato HTTP `/api/v1`, esquema e dinheiro inalterados**; a suíte de J5–P5 **continua verde**.
- [ ] Log de produção em **JSON (ECS)** com `requestId`, `traceId` e `spanId`; **`ERROR` uma única vez**, 404/405/400 **não** viram `ERROR` (teste).
- [ ] O `traceId` volta no **Problem Details** e o `X-Request-Id` na resposta; o log da *thread* assíncrona **tem** os dois (teste que **falha** sem o `MdcTaskDecorator`).
- [ ] `/actuator/*` **só** na porta 8081 (não publicada): `health`, `info`, `prometheus`; `prometheus` exige o papel **`METRICS`** (anônimo 401, `USER` 403, `METRICS` 200: **um teste por barreira**).
- [ ] Teste de **cardinalidade**: 150 contas distintas = **1** série de `uri`; **reprova** se um id aparecer em etiqueta.
- [ ] Painel **PayFlow — RED + negócio + JVM** provisionado do Git, **todas** as consultas rodam sem erro (script como o `check-stack.sh`).
- [ ] Os **9 alertas** com `severity`, `summary`, `runbook`; `promtool check rules` e `promtool test rules` **no CI**, com o caso **negativo** e a prova de que a regra **quebrada reprova**.
- [ ] *Readiness* = `readinessState` + `db`; *liveness* = só `livenessState` (teste que afirma os `include`); nenhuma sonda no `health` **agregado**.
- [ ] Traço: o `traceparent` atravessa o `RestClient` (teste com `InMemorySpanExporter`); **nenhum** `RestClient.create` fora de teste (ArchUnit); `observation-enabled` do Kafka/RabbitMQ **ligado e validado** (⚠️ não executado aqui).
- [ ] `docs/` com `observability.md`, os **runbooks** e o **postmortem** preenchido com os números da **sua** execução; **ADR 0018** revisado; tag `payflow-P6`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Pleno: **≥ 2** em todos e **3** nos marcados com ⭐.

| Competência | Aula(s) | 0–3 |
|---|---|---|
| Distinguir os **três pilares** e a **ordem** de investigação | P6.01 | ⭐ |
| **Logs estruturados** e **correlation ID** (`requestId` × `traceId`, `@Async`) | P6.01 | ⭐ |
| **Micrometer**: tipos de medidor, `Observation`, **cardinalidade** | P6.02 | ⭐ |
| **Actuator** na porta de gestão, exposição e segurança (`METRICS`) | P6.02 | |
| **Prometheus/PromQL**: `rate`, `sum by`, `histogram_quantile` e a interpolação | P6.03 | ⭐ |
| **RED e USE**, *dashboards* como código | P6.03 | |
| **Alertas**: sintoma, `for`, tráfego mínimo, `absent`, runbook, **`promtool test`** | P6.04 | ⭐ |
| **Tracing com OpenTelemetry**: propagação, amostragem, atributos, *exemplars* | P6.05 | ⭐ |
| **Health checks**: *liveness* × *readiness*, desligamento gracioso | P6.06 | ⭐ |
| **Monitoramento de erros** (métrica, log, traço) e a decisão sobre rastreadores | P6.06 | |
| **Investigar** um incidente (escopo → traço → log) e escrever o **postmortem** | P6.07 | ⭐ |

**Se algum ⭐ ficou abaixo de 2:** refaça os experimentos (`lab/run-all.sh`, ou apenas `mvn test` na aplicação e `./incident.sh`) e o respectivo exercício de aplicação antes de seguir para a P7.

## Parte 4: Perguntas de entrevista (simulado rápido)
Responda em voz alta, 2 minutos cada: (1) "Os três pilares da observabilidade: quando você usa cada um?" (2) "Como você correlaciona uma queixa do usuário com o que aconteceu?" (3) "O que é cardinalidade e por que derruba o Prometheus?" (4) "Como calcular o p95 no Prometheus? Qual a limitação?" (5) "Por que não alertar na média?" (6) "O que torna um bom alerta e como você o testa?" (7) "Como funciona a propagação de contexto no tracing e como ela quebra?" (8) "*Liveness* × *readiness*: o que cada uma verifica?" (9) "Investigue: as transferências ficaram lentas só para alguns usuários." (10) "O que é um postmortem sem culpa?" As respostas-modelo estão em cada aula (seção "Na entrevista") e consolidadas em `recursos/perguntas-entrevista.md`.

## ➡️ Próximo
[P7 — Containers e Kubernetes](../P7-containers-kubernetes/README.md)
