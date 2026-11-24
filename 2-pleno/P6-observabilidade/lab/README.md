# Laboratório do P6

Tudo foi **executado** em Linux, JDK **25.0.5** (`/usr/lib/jvm/java-25-openjdk-amd64`), Maven **3.9.12**, Docker **29.9.2**, 12 núcleos **compartilhados** (a *load average* varia: tempos oscilam). As saídas reais estão em `out-*.txt` e são **o que as aulas mostram** (as aulas as **injetam** por script a partir desses arquivos).

## `observability-lab/` (Spring Boot **4.1.2**; P6.01 a P6.07)
"PayFlow Lite": uma transferência fake com `Observation`, um serviço de **risco** interno (chamada HTTP real, para o traço atravessar a rede), um relé de Outbox **assíncrono** e **interruptores de falha** (`/lab/**`, que **não existem** em produção). Versões efetivas: Micrometer **1.17.2**, Micrometer Tracing **1.7.2**, OpenTelemetry Java **1.62.0** (BOM do Boot); pilha de apoio: **Prometheus 3.16.0**, **Grafana 13.3.0**, **Jaeger 2.23.0** (imagens `latest` na data da execução).

```bash
cd lab
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./run-all.sh   # ~7 min: testes + promtool + pilha + incidente + SIGTERM + segurança; regrava os out-*.txt
```

| Parte | Aulas | O que prova |
|---|---|---|
| `mvn test` (31 testes, **sem Docker**) | P6.01 a P6.06 | `LoggingTest`/`LoggingTextTest`/`LoggingRenameTest`/`LoggingEmptyFormatTest` (ECS, `requestId`, `traceId`, WARN × ERROR, 404 sem `ERROR`, `rename[...]`), `Async*Test` (contexto na *thread*: 3 configurações), `MetricsTest` (7: timers por resultado, `uri` template, **cardinalidade**, histogramas, porta de gestão, `logback_events_total`), `HealthTest` (6: liveness × readiness), `TracingTest`/`TracingSamplingTest`/`ExemplarTest` (árvore de *spans*, `traceparent`, amostragem 0, `RestClient.create()` que **quebra** o traço, *exemplars*) |
| `prometheus/` + `promtool` (Docker) | P6.03, P6.04 | `alerts.yml` (12 regras), `alerts.test.yml` (**8** grupos de teste, com casos **negativos**), a **regra quebrada de propósito reprova**, `histogram-pause.test.yml` (a conta da pausa de lógica), `prometheus.payflow.yml` (`basic_auth` + `password_file`) validado |
| `run-stack.sh` + `incident.sh` + `check-stack.sh` (Docker) | P6.03, P6.05, P6.07 | Prometheus *scrapeando* a porta de gestão, Grafana com o *dashboard* **provisionado** (14 consultas rodam), Jaeger recebendo **OTLP**, e o **incidente** (fases A a D) |
| `shutdown-check.sh` | P6.06 | `SIGTERM` com requisição em voo: *readiness* 503 na hora, requisição termina 201, saída 143 |
| `mgmt-security-lab/` (1 teste) | P6.02 | Spring Security 7 **vale na porta de gestão**; papel `METRICS` para o *scrape* |

### Saídas gravadas
`observability-lab/out-suite-resumo.txt`, `out-T1.txt` (logs), `out-T2.txt` (métricas), `out-T3.txt` (saúde), `out-T4.txt` (traços e *exemplars*), `out-T5.txt` (incidente), `out-T6.txt` (pilha), `out-T8.txt` (`promtool`), `out-T9.txt` (`SIGTERM`), `mgmt-security-lab/out-T7.txt`.

### Surpresas que o laboratório achou
Ver o registro do P6 em `00-guia/progresso.md` (lista numerada).

### Limpeza
`run-all.sh` remove contêineres (`lab-p6-*`), `target/`, `out-suite.raw` e arquivos temporários. Sem volumes nem imagens criados (as imagens `prom/prometheus`, `grafana/grafana` e `jaegertracing/jaeger` já existiam no Docker local).

> **Atenção (segredos):** a senha `lab-only-admin` do Grafana e `lab-only` do `mgmt-security-lab` são **só de laboratório**. No PayFlow real: segredo por arquivo (`GF_SECURITY_ADMIN_PASSWORD__FILE` **medido** funcionando; decisão 71).
