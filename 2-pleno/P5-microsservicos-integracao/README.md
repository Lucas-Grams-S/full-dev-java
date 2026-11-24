# P5 — Microsserviços e integração

> 🔵 Pleno · ⏱️ 16.5h (11 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **laboratório executado (Spring Boot 4.1.2, Resilience4j 2.4.0, Spring Cloud 2025.1.4/Gateway 5.0.4, Spring gRPC 1.1.2, Pact JVM 4.7.7, JDK 25.0.5; 41 testes, sem Docker); o PayFlow do aluno foi revisado apenas por leitura; testes práticos no fim do treinamento, por decisão do aluno**

## 🎯 Objetivo do módulo
Decidir **quando dividir** um sistema (e quando **não**) e **como integrar** serviços com **resiliência**: extrair **um** serviço (`payflow-notifications`) pelo método do **estrangulamento**, escolher **REST ou gRPC**, evoluir APIs e eventos **sem quebrar** consumidores, pôr um **gateway** na frente, proteger as chamadas remotas (**timeouts, retry, circuit breaker, bulkhead, rate limiter**) e provar o contrato com **testes de contrato**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P5.01](P5.01-monolito-vs-microsservicos-quando-dividir-e-quando-nao.md) | Monólito vs microsserviços: quando dividir e quando NÃO | 1h30 |
| [P5.02](P5.02-decomposicao-por-dominio.md) | Decomposição por domínio | 1h30 |
| [P5.03](P5.03-rest-vs-grpc.md) | REST vs gRPC | 1h30 |
| [P5.04](P5.04-eventos-e-compatibilidade-de-apis-e-de-schemas.md) | Eventos e compatibilidade de APIs e de schemas | 1h30 |
| [P5.05](P5.05-api-gateway-spring-cloud-gateway.md) | API Gateway (Spring Cloud Gateway) | 1h30 |
| [P5.06](P5.06-configuracao-e-descoberta-de-servicos-vs-kubernetes.md) | Configuração e descoberta de serviços vs Kubernetes | 1h30 |
| [P5.07](P5.07-resilience4j-i-timeouts-e-retry.md) | Resilience4j I: timeouts e retry | 1h30 |
| [P5.08](P5.08-resilience4j-ii-circuit-breaker-bulkhead-e-rate-limiter.md) | Resilience4j II: circuit breaker, bulkhead e rate limiter | 1h30 |
| [P5.09](P5.09-testes-de-contrato-spring-cloud-contract-ou-pact.md) | Testes de contrato (Spring Cloud Contract ou Pact) | 1h30 |
| [P5.10](P5.10-estudo-de-caso-o-servico-de-notificacoes-que-derrubou-o-checkout.md) | 🏗️ Estudo de caso: o serviço de notificações que derrubou o checkout | 1h30 |
| [P5.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): `lab/integration-lab/` (Maven multimódulo: `resilience-lab`, `gateway-lab`, `grpc-lab`, `contract-lab`; Spring Boot **4.1.2**; **41 testes**; **sem Docker**).

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório** (timeouts, retry, circuit breaker, bulkhead, rate limiter, ordem das anotações, **cascata** com Tomcat de 10 *threads*, gateway, gRPC, evolução de `.proto` e de JSON, **Pact** consumidor/provedor com provedor quebrado) **foi executado** nas versões acima; **as saídas mostradas nas aulas são as reais**. **Não foi executado:** o **PayFlow do aluno** (tudo o que é "por leitura": `payflow-notifications`, `payflow-gateway`, remoção da chamada síncrona, modo sombra), **Spring Cloud Contract**, **Pact Broker**/`can-i-deploy`, Eureka/Config Server/Kubernetes, *rate limit* Bucket4j, TLS/mTLS, *streaming* gRPC, *schema registry*. ⚠️ **Compatibilidade:** **nenhum trem do Spring Cloud declara o Boot 4.1** (2025.1 → Boot 4.0.x; 2026.0 ainda em RC → Boot 4.2): o Gateway 5.0.4 funcionou no 4.1.2 no laboratório, mas é combinação **não declarada** (P5.05).

> **Avisos de ordem:** a P5.01 retoma o **monólito modular** (P4.09) e a disponibilidade que se **multiplica** (P3.01); a P5.02 usa o mapa de contextos (P4.08), "um dono por tabela" (P4.11), idempotência (P3.07) e Outbox (P3.09); a P5.03 retoma `RestClient` e timeouts (J6.08); a P5.04 usa Kafka/Outbox (P3) e o versionamento da J6.06; a P5.06 antecipa o **Kubernetes** (P7) só como **comparação** (o cluster **não** é pré-requisito); a P5.10 usa `L = λ × W` (P3.01) e o formato de postmortem (P3.13, P4.11). **Observabilidade (P6)** é citada para **alertas e métricas** do *breaker*, sem ser pré-requisito.

## 🏗️ Entrega no PayFlow
**Um serviço extraído** (`payflow-notifications`, com banco e migrações próprios, consumindo `payflow.transfers.v1` e as tarefas do RabbitMQ), **um gateway** (`payflow-gateway`, Spring Cloud Gateway WebMVC, sem rotas `/internal/**`), **resiliência** nas chamadas remotas (timeouts obrigatórios, retry só idempotente, circuit breaker com `record`/`ignore`, bulkhead por `L = λ × W`, fallback documentado e com alerta), **testes de contrato** (Pact) e **testes de compatibilidade de eventos**, e **o checkout sem chamada síncrona** ao serviço de notificações. **Contrato HTTP `/api/v1` inalterado; o dinheiro continua numa transação do monólito.** Tag `payflow-P5`. Decisões: ADR [0017](../../projeto/adrs/0017-extracao-do-servico-de-notificacoes-gateway-e-resiliencia.md).

## ➡️ Próximo
[P6 — Observabilidade](../P6-observabilidade/README.md)
