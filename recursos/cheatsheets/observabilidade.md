# Cheatsheet — Observabilidade (P6)

> Valores **executados** em `2-pleno/P6-observabilidade/lab/observability-lab/` (Spring Boot 4.1.2, Micrometer 1.17.2, OpenTelemetry 1.62.0, Prometheus 3.16.0, Grafana 13.3.0, Jaeger 2.23.0, JDK 25). Os números dependem da máquina: use como **ordem de grandeza**.

## Os três pilares (P6.01)
| | Pergunta | Custo | Cardinalidade |
|---|---|---|---|
| **Métricas** | há problema? quanto? | baixo (cresce com **séries**) | **baixa** obrigatória |
| **Traces** | **onde** o tempo foi? | médio (amostrar) | alta nos atributos |
| **Logs** | **o que** aconteceu? | alto (cresce com **tráfego**) | alta é normal |
Ordem: **métrica → escopo → traço → log**.

## Spring Boot 4.1: propriedades úteis (lidas nos metadados e testadas no laboratório)
| Propriedade | Efeito |
|---|---|
| `logging.structured.format.console: ecs` (ou `logstash`, `gelf`) | log em JSON; MDC entra como campos; **vazio** = volta ao texto |
| `logging.structured.json.rename[traceId]: trace.id` | **colchetes** (sem eles perde a caixa e não renomeia) |
| `spring.task.execution.propagate-context: true` | `traceId` atravessa o `@Async` (**o `requestId` não**) |
| `management.server.port: 8081` | Actuator fora da porta pública; **a cadeia de segurança vale lá** |
| `management.endpoints.web.exposure.include: health,info,prometheus` | **liste**, nunca `*` |
| `management.endpoint.health.group.readiness.include: readinessState,db` | prontidão = só o que **impede** de atender |
| `management.metrics.distribution.percentiles-histogram.<meter>: true` | histograma é **opt-in por medidor** (`http.server.requests`, `http.client.requests`) |
| `management.metrics.distribution.slo.http.server.requests: 50ms,100ms,...` | baldes em volta do SLO |
| `management.otlp.metrics.export.enabled: false` | o starter OpenTelemetry liga o push OTLP de métricas |
| `management.tracing.sampling.probability` | **0.1** (padrão); 1.0 no laboratório |
| `management.opentelemetry.tracing.export.otlp.endpoint` | `http://host:4318/v1/traces` (Boot 4.1: **prefixo novo**) |
| `spring.kafka.template.observation-enabled` / `...listener...` (e RabbitMQ) | **padrão `false`**: sem isso o traço morre no *broker* |

## Micrometer (P6.02)
| Tipo | Uso | Leitura no Prometheus |
|---|---|---|
| Counter | só sobe | `rate(x_total[5m])` |
| Gauge | valor agora | direto (**segure a referência**) |
| Timer | tempo + contagem | `_count`, `_sum`, `_bucket` |
- Etiqueta: **poucos valores conhecidos** (resultado, método, rota-template). **Nunca** id, e-mail, `requestId`.
- **1.000 contas como etiqueta = 1.000 séries** (medido); `MeterFilter.maximumAllowableTags` limita.
- `RestClient`: use o **`RestClient.Builder` injetado** (`spring-boot-starter-restclient`); `RestClient.create()` **não é instrumentado** (sem `traceparent`, sem métrica, **traço quebra**).

## PromQL
```
sum by (uri) (rate(http_server_requests_seconds_count[5m]))                              # taxa por rota
sum(rate(..._count{status=~"5.."}[5m])) / sum(rate(..._count[5m]))                       # razão de 5xx
histogram_quantile(0.95, sum by (le) (rate(..._bucket{uri="/api/v1/transfers"}[5m])))     # p95 (estimativa interpolada)
1 - sum(rate(..._bucket{le="1.0"}[5m])) / sum(rate(..._bucket{le="+Inf"}[5m]))            # fração acima de 1 s
up{job="payflow"} == 0                                                                    # alvo fora
absent(http_server_requests_seconds_count{job="payflow"})                                 # silêncio (copia o job para o alerta)
```
`rate()` precisa de **≥ 2 amostras** na janela; **não** use em gauge; `histogram_quantile` **sem** `sum by (le)` devolve lixo.

## RED, USE, alertas (P6.03, P6.04)
- **RED** (serviço): Rate, Errors (**só 5xx**), Duration (p95/p99). **USE** (recurso): Utilization, Saturation, Errors.
- Alerta bom: **sintoma**, `for`, razão **com tráfego mínimo**, `severity` (page/ticket), `runbook`, **testado** (`promtool test rules`, com o caso **negativo**).
- Dispara em `for` **depois** da 1ª avaliação verdadeira (`rate` exige 2 amostras): `for: 5m` com avaliação de 1 min → **6 min** (medido).
- Alertar em **p95/p99 ou fração lenta**, **nunca** na média (medido: p50 igual, média ×24, p99 ×300 com ~6% lentas).
```bash
docker run --rm -v "$PWD":/p --entrypoint promtool prom/prometheus:latest check rules /p/alerts.yml
docker run --rm -v "$PWD":/p -w /p --entrypoint promtool prom/prometheus:latest test rules alerts.test.yml
```

## Tracing (P6.05)
- `traceparent: 00-<traceId 32 hex>-<spanId pai 16 hex>-<flags>` (`01` = amostrado).
- Amostragem **0** = nenhum *span*, **mas o `traceId` continua** no log e na resposta.
- Raiz de um 500 **tratado** por `@RestControllerAdvice` **não** é `ERROR`: filtre `status=500` (atributos do Micrometer: `status`, `outcome`, `uri`).
- Jaeger v2: busca em **`/api/v3/traces?query.service_name=...&query.duration_min=1s`**; `GET /api/traces/<traceId>` por id; OTLP/HTTP em **:4318**; os spans chegam em lotes (~5 s).
- Erro raro com 10% de amostra: 29 falhas para 95% de chance de ter amostrado uma.

## Saúde (P6.06)
| | Verifica | Falhou → | Nunca verifica |
|---|---|---|---|
| **Liveness** | só o processo (`livenessState`) | **reinicia** | banco, rede, filas |
| **Readiness** | `readinessState` + o que **impede** de atender (`db`) | **sai da rotação** | dependência opcional |
- `/actuator/health` **agregado não é sonda** (medido: 503 por dependência opcional com a readiness em 200).
- `SIGTERM`: readiness **503 na hora**, liveness 200, requisição em voo termina, saída **143**.

## Docker/Compose (por leitura)
`healthcheck: wget -qO- http://localhost:8081/actuator/health/readiness`, `start_period: 40s`, `stop_grace_period: 30s`. Portas de Prometheus/Grafana/Jaeger **só em `127.0.0.1`**; senha do Grafana por `GF_SECURITY_ADMIN_PASSWORD__FILE` (medido).
