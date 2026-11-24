# P6 — Observabilidade

> 🔵 Pleno · ⏱️ 12h (8 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **laboratório executado (Spring Boot 4.1.2, Micrometer 1.17.2, OpenTelemetry 1.62.0, Prometheus 3.16.0, Grafana 13.3.0, Jaeger 2.23.0, JDK 25.0.5; 31 testes sem Docker + `promtool` + pilha real + incidente simulado + `SIGTERM`); o PayFlow do aluno foi revisado apenas por leitura; testes práticos no fim do treinamento, por decisão do aluno**

## 🎯 Objetivo do módulo
Enxergar o que o sistema faz em produção e **investigar incidentes**: **logs estruturados** com *correlation ID* (`requestId` e `traceId`), **métricas** (Micrometer + Prometheus) lidas pelos métodos **RED/USE**, **painéis** versionados no Git (Grafana), **alertas** por sintoma (testados com `promtool`), **tracing distribuído** com OpenTelemetry (Jaeger), **health checks** (*liveness* × *readiness*, desligamento gracioso) e o ensaio de um **incidente** do alerta ao *postmortem* sem culpa.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P6.01](P6.01-os-tres-pilares-e-logs-estruturados-com-correlation-id.md) | Os três pilares e logs estruturados com correlation ID | 1h30 |
| [P6.02](P6.02-actuator-e-micrometer.md) | Actuator e Micrometer | 1h30 |
| [P6.03](P6.03-prometheus-e-grafana-metricas-red-use-e-dashboards.md) | Prometheus e Grafana: métricas RED/USE e dashboards | 1h30 |
| [P6.04](P6.04-alertas.md) | Alertas | 1h30 |
| [P6.05](P6.05-tracing-distribuido-com-opentelemetry.md) | Tracing distribuído com OpenTelemetry | 1h30 |
| [P6.06](P6.06-health-checks-liveness-vs-readiness-e-monitoramento-de-erros.md) | Health checks: liveness vs readiness e monitoramento de erros | 1h30 |
| [P6.07](P6.07-investigando-um-incidente-simulado.md) | 🏗️ Investigando um incidente simulado | 1h30 |
| [P6.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): `lab/observability-lab/` (Spring Boot **4.1.2**; **31 testes** sem Docker; `prometheus/` com **12 regras** e **8 grupos de teste**; pilha Docker com Prometheus, Grafana e Jaeger; `incident.sh`, `shutdown-check.sh`) e `lab/mgmt-security-lab/` (Spring Security 7 na porta de gestão).

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório** (logs ECS, `requestId`/`traceId`, `@Async`, métricas e **cardinalidade**, porta de gestão com Spring Security, saúde, traços com `InMemorySpanExporter`, amostragem 0, *exemplars*, regras de alerta com `promtool`, a **pilha real** com o *dashboard* provisionado, o **incidente** (fases A a D) e o **`SIGTERM`**) **foi executado** nas versões acima; **as saídas mostradas nas aulas são as reais**. **Não foi executado:** o **PayFlow do aluno** (revisado por leitura), **Alertmanager** e canais de notificação, **Kubernetes** (sondas, P7), OpenTelemetry **Collector**/amostragem por **cauda**, *agent* Java, propagação de traço por **Kafka/RabbitMQ** (as propriedades `observation-enabled` foram lidas nos metadados), Prometheus/Grafana lendo ***exemplars***, rastreadores de exceção (Sentry/GlitchTip) e o *dashboard* **no navegador** (as **14 consultas** rodaram pela API do Grafana).

> **Avisos de ordem:** a P6.01 retoma o log da J5.14 (decisão 38), o Problem Details da J6.04 e o `TaskDecorator` da P3.02; a P6.02 retoma o Actuator da J5.15 e a cadeia de segurança da J8 (e **revisa** as decisões 39 e 56); a P6.03 usa o *pool* da P2.06 e a JVM da P1; a P6.04 cumpre as promessas de alerta da P5.08 (*breaker*), P3.08 (DLQ) e P3.09 (Outbox); a P6.06 antecipa o Kubernetes (P7) só como **conceito** das sondas; o **SLO e o *burn rate*** ficam para o **S4**.

## 🏗️ Entrega no PayFlow
**Logs estruturados** (ECS em produção, `requestId` + `traceId`, `ERROR` uma vez), **métricas** (`payflow.transfer` por `Observation`, *gauge* e *counter* do Outbox, etiquetas de baixa cardinalidade), Actuator na **porta de gestão** (8081) com o papel `METRICS`, **traços** OTLP (amostragem 0,1), **painel** RED + negócio + JVM provisionado do Git, **9 alertas** com *runbook* e testes `promtool`, *health checks* (*readiness* = banco; *liveness* = só o processo), `compose.observability.yaml`, *postmortem* do incidente simulado e **ADR 0018**. Tag `payflow-P6`. **Nenhuma migração nova** (a próxima livre continua V12).

## ➡️ Próximo
[P7 — Containers e Kubernetes](../P7-containers-kubernetes/README.md)
