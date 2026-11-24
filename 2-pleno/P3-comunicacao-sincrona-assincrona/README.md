# P3 — Comunicação síncrona e assíncrona

> 🔵 Pleno · ⏱️ 21h (14 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **laboratórios executados (Kafka 4.3.1, RabbitMQ 4.4.0, PostgreSQL 18, Spring Boot 4.1.2, Spring Kafka/AMQP 4.1.2, ShedLock 7.11.0, JDK 25.0.5); o PayFlow do aluno foi revisado apenas por leitura; testes práticos no fim do treinamento, por decisão do aluno**

## 🎯 Objetivo do módulo
Desacoplar o PayFlow com **mensageria confiável**: decidir **o que sai do caminho da requisição**, escolher entre **fila e log** (RabbitMQ × Kafka), entender as **garantias de entrega** e tornar tudo **seguro contra duplicatas** (idempotência, retry com *backoff*, DLQ, Outbox), receber **webhooks** com segurança e empurrar eventos em **tempo real**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P3.01](P3.01-bloqueante-vs-nao-bloqueante-io-bound-vs-cpu-bound.md) | Bloqueante × não bloqueante; I/O-bound × CPU-bound | 1h30 |
| [P3.02](P3.02-async-scheduled-e-shedlock.md) | `@Async`, `@Scheduled` e ShedLock | 1h30 |
| [P3.03](P3.03-filas-vs-topicos-e-rabbitmq-com-spring-amqp.md) | Filas × tópicos e RabbitMQ com Spring AMQP | 1h30 |
| [P3.04](P3.04-apache-kafka-i-particoes-consumer-groups-e-offsets.md) | Apache Kafka I: partições, *consumer groups* e *offsets* | 1h30 |
| [P3.05](P3.05-apache-kafka-ii-ordenacao-retencao-e-spring-kafka.md) | Apache Kafka II: ordenação, retenção e Spring for Apache Kafka | 1h30 |
| [P3.06](P3.06-garantias-de-entrega-e-exactly-once.md) | Garantias de entrega e "*exactly-once*" | 1h30 |
| [P3.07](P3.07-idempotencia-de-consumidores.md) | Idempotência de consumidores | 1h30 |
| [P3.08](P3.08-retentativas-com-backoff-e-dead-letter-queues.md) | Retentativas com *backoff* e *dead-letter queues* | 1h30 |
| [P3.09](P3.09-padrao-outbox.md) | Padrão Outbox | 1h30 |
| [P3.10](P3.10-webhooks-e-pagamento-por-qr-code-simulado.md) | Webhooks e pagamento por QR code simulado | 1h30 |
| [P3.11](P3.11-websockets-e-sse.md) | WebSockets e SSE | 1h30 |
| [P3.12](P3.12-webflux-e-reactor-vs-virtual-threads.md) | Visão de WebFlux e Reactor × *virtual threads* | 1h30 |
| [P3.13](P3.13-estudo-de-caso-o-pagamento-debitado-duas-vezes.md) | 🏗️ Estudo de caso: o pagamento debitado duas vezes | 1h30 |
| [P3.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratórios prontos em [`lab/`](lab/README.md): `lab/messaging-lab/` (Maven, Spring Boot 4.1.2, **47 testes de integração** contra Kafka, RabbitMQ e PostgreSQL em Docker) e `lab/io-lab/` (Java puro).

> ⚠️ **Aviso de verificação (leia antes de estudar):** os **experimentos** (partições/grupos/offsets, retenção e compactação, *at-most/at-least-once*, transações e `read_committed`, idempotência, retry/DLT, Outbox, webhook, SSE/WebSocket, Reactor × *virtual threads*, ShedLock com 2 instâncias, o caso do débito duplicado) **foram executados** nas versões acima; os números são **da máquina do laboratório** (12 núcleos **compartilhados**). **Não foi executado:** o **PayFlow do aluno** (tudo o que é "por leitura"), vários *brokers*/réplicas, TLS/SASL, *publisher confirms*, filas *quorum*, CDC/Debezium, `sendOffsetsToTransaction`, Testcontainers de Kafka/RabbitMQ, WebFlux/`WebClient`, navegador e *proxies*. Cada aula diz o que foi e o que não foi executado.

> **Avisos de ordem:** a **lacuna do Júnior** (síncrono × assíncrono × fila, anotada no checkpoint Júnior) é coberta pela **P3.01 e P3.03**; a P3.02 **fecha** o `@Scheduled` em "um nó" da P2.11; a P3.03 e a P3.04 **retomam** o `SKIP LOCKED` da P2.02 (usado na P3.09); a P3.07 corrige no **consumidor** o que a J6 fez na **borda** (`Idempotency-Key`); a P3.10 estende a segurança da J8 (uma terceira cadeia); `@TransactionalEventListener`, filas *quorum*, `@RetryableTopic` com ordem e BlockHound são **citados e marcados** como não executados.

## 🏗️ Entrega no PayFlow
Notificações (RabbitMQ) e antifraude (Kafka) **assíncronos**, alimentados por **Outbox** (`outbox_events`), com **consumidores idempotentes** (`processed_messages`), **retry + DLQ**, **pagamento por QR code** (`payment_requests`) confirmado por **webhook assinado**, **SSE** para o painel e *jobs* com **ShedLock**. Tag `payflow-P3`. Decisões: ADR [0015](../../projeto/adrs/0015-mensageria-outbox-idempotencia-e-pagamento-por-qr.md).

## ➡️ Próximo
[P4 — Arquitetura de aplicações](../P4-arquitetura-aplicacoes/README.md)
