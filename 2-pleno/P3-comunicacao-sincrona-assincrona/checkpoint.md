# P3.CP — Checkpoint do módulo P3

> ⏱️ ~1h30 · 🔵 Pleno · Pré-requisitos: P3.01 a P3.13

> ✅ **Validação final (2026-11-24):** módulo executado no PayFlow reconstruído; resultado e o que continua não executado no [README do módulo](README.md). Os avisos "não executado" / "por leitura" abaixo descrevem a escrita original.

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5, 12 núcleos **compartilhados**, Docker): `lab/messaging-lab/` (Maven, Spring Boot **4.1.2**, Spring Kafka **4.1.2**, Spring AMQP **4.1.2**, ShedLock **7.11.0**, `kafka-clients` **4.2.2**, Reactor 2025.0.8) contra **Kafka 4.3.1** (KRaft, 1 nó), **RabbitMQ 4.4.0** e **PostgreSQL 18**: **47 testes `*IT`** (`mvn test` inteiro; contagem por classe em `lab/README.md`) e `lab/io-lab/IoLab.java` (JDK 25). **Não executado:** o PayFlow do aluno (tudo o que está em "Entrega prática"), Testcontainers de Kafka/RabbitMQ, Kafka com vários *brokers*/réplicas, TLS/SASL, *publisher confirms*, filas *quorum*, CDC/Debezium, `@RetryableTopic` com ordem, `sendOffsetsToTransaction`, WebSocket/SSE em navegador e atrás de *proxy*, WebFlux/`WebClient`/BlockHound.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Diferencie **síncrono × assíncrono** e **bloqueante × não bloqueante**. Dê um exemplo de "síncrono não bloqueante" e outro de "assíncrono bloqueante". Faça a **Lei de Little** para 150 req/s com `W = 0,84 s` e diga o que acontece com um *pool* de 200 *threads*.
2. Para tarefas **I/O-bound** e **CPU-bound**, quantas *threads* faz sentido usar e o que o `IoLab` mediu (200 tarefas de 100 ms; 48 tarefas de CPU em 12 núcleos)? Qual foi a **armadilha de medição** do cliente HTTP?
3. Cite **três armadilhas** do `@Async` (uma sobre auto-chamada, uma sobre `void`, uma sobre o executor padrão) e o que o laboratório mostrou sobre o executor **limitado** (aceitas × rejeitadas).
4. Por que um `@Scheduled` roda **duas vezes** com 2 réplicas? Explique `lockAtMostFor` × `lockAtLeastFor` e o que o ShedLock **não** garante. Quantas execuções o laboratório mediu **com e sem** *lock*?
5. Desenhe o caminho **produtor → *exchange* → fila → consumidor** no RabbitMQ; diferencie *direct*, *topic* e *fanout*; o que acontece com uma mensagem **sem fila ligada** e com uma mensagem **não confirmada** quando o consumidor cai? Que erro o RabbitMQ 4.4 deu para uma fila **não durável**?
6. No Kafka, onde existe **ordem**? O que decide a partição? Quantos consumidores de um grupo são **úteis** para 3 partições (use a medição de 1 a 4)? Por que um grupo novo "**não recebe nada**" e o que significa `COMMITADO=5 FIM=10`?
7. Diferencie *at-most-once*, *at-least-once* e *exactly-once* pela **ordem** entre processar e confirmar. O que as **transações do Kafka** cobrem e **não** cobrem? O que o `read_committed` escondeu no laboratório?
8. Implemente (em palavras e SQL) um **consumidor idempotente**. Por que o `SELECT` seguido de `INSERT` falha sob concorrência (use `700`, `900`, `200`)? De onde vem a chave? Como dimensionar a retenção da tabela?
9. Compare as **estratégias de retentativa**: `DefaultErrorHandler` (bloqueante), `@RetryableTopic` e retry + DLX no RabbitMQ: o que cada uma **bloqueia** ou **desordena**? Que armadilha de propriedade o Boot 4 trouxe (`max-attempts` × `max-retries`)? Que cabeçalhos o DLT guarda e como se **reprocessa** sem duplicar?
10. Explique o **Outbox** (problema da escrita dupla, garantia, `SKIP LOCKED`, o que o consumidor precisa fazer) e o **webhook** do QR code (assinatura HMAC sobre os **bytes**, *timestamp*, tempo constante, idempotência). Por que `MESMO event-id duas vezes` no tópico **não é um defeito**?

<details><summary>Gabarito</summary>

1. **Síncrono/assíncrono** é o **contrato** (o chamador precisa da resposta para seguir?); **bloqueante/não bloqueante** é o **mecanismo** (a *thread* fica parada?). Síncrono não bloqueante: `var f = client.sendAsync(...); outraCoisa(); f.get();`. Assíncrono bloqueante: consumidor de fila em *thread* própria que bloqueia em `poll()`. **Little:** `L = λ × W = 150 × 0,84 = 126` simultâneas (63% do *pool* de 200); uma lentidão a mais (`W = 1,4 s` ⇒ 210) **satura** o Tomcat, e as rotas que nem usam o parceiro também sofrem; teto `200 ÷ 0,84 ≈ 238 req/s`.
2. **I/O-bound:** mais *threads* que núcleos ajudam **até o recurso esperado** (conexões, parceiro); o tempo seguiu `tarefas × 100 ms ÷ threads` (1 thread 20 063 ms; 200 threads 110 ms; **virtuais 106 ms**; 10 000 virtuais ≈ 125 ms). **CPU-bound:** ≈ nº de núcleos: 1 → 6 *threads* 776 → 177 ms; **12 → 48 → 200: 139 → 154 → 133 ms (sem ganho)**; virtuais **não** aceleram CPU. **Armadilha:** variantes no mesmo `HttpClient`: as virtuais, por último, reutilizaram 200 conexões abertas e deram **114 ms**; com **cliente novo por variante**, `sendAsync` ≈ 1,15 s e virtuais ≈ 1,1 s, **empatadas**.
3. **Auto-chamada** (`this.método()`) não passa pelo proxy: `thread=main`. **`void`**: a exceção não chega ao chamador, vai ao `AsyncUncaughtExceptionHandler` (`fireAndForgetThatFails: falhou no @Async void`); com `CompletableFuture` vem no `Future`. **Executor padrão do Boot:** núcleo **8**, fila **ilimitada** (a memória é o limite). Executor **2 + fila 2**: **4 aceitas e 6 rejeitadas** (`TaskRejectedException`). Também: o MDC **não** acompanha a *thread* sem `TaskDecorator`.
4. O agendador é **local** a cada instância. **`lockAtMostFor`** = teto (o *lock* expira se o nó morrer; deve ser **maior** que o *job*, senão duas execuções coexistem); **`lockAtLeastFor`** = piso (protege contra relógios e *jobs* rápidos). ShedLock é **exclusão mútua com tempo**, não **exatamente uma** execução garantida: o *job* precisa ser idempotente. Medido (9 s, 2 instâncias, `fixedRate` 1 s, `lockAtLeastFor` 2 s): **19 execuções sem *lock*** (10 + 9) e **5 com *lock*** (alternando entre A e B).
5. Produtor publica no ***exchange*** com *routing key*; as *bindings* ligam o *exchange* às filas; consumidores leem as filas. ***direct***: chave igual; ***topic***: padrão `*`/`#`; ***fanout***: todas as filas ligadas. **Sem fila ligada, a mensagem é descartada em silêncio** (`deposit.completed.brl`). **Não confirmada**: volta à fila (`redelivered=true`), *at-least-once*. RabbitMQ 4.4: `reply-code=541, INTERNAL_ERROR - Feature transient_nonexcl_queues is deprecated` ⇒ declarar `durable=true`.
6. **Dentro de uma partição** (logo, **por chave**). A **chave** (hash) decide a partição; sem chave, o particionador espalhou **desigualmente** (`{1=230, 2=70}`). Para 3 partições: 1 consumidor `[0,1,2]`, 2 → `[0,1][2]`, 3 → `[0][1][2]`, **4 → o quarto fica `[]`** (ocioso). Grupo novo com `auto.offset.reset=latest` (**padrão**) só vê mensagens **novas**. `COMMITADO=5 FIM=10` ⇒ **lag 5** (ainda não processadas).
7. *At-most-once:* confirma **antes** de processar (perda: processou 3, "caiu", **7 perdidas**). *At-least-once:* confirma **depois** (queda ⇒ **reentrega** das 10). *Exactly-once* ponta a ponta **não existe**: é *at-least-once* + **idempotência**. **Transações do Kafka** (`transactional.id`, `sendOffsetsToTransaction`, `read_committed`) cobrem **ler-processar-escrever no Kafka**; **não** cobrem banco/e-mail/HTTP. `read_committed` **escondeu** registros de transação **aberta** (`[]`) e **abortada** (só `committed-1`); `read_uncommitted` (padrão) viu todos.
8. `INSERT INTO processed_messages(consumer, message_id) … ON CONFLICT DO NOTHING` **e** o efeito **na mesma transação**; só executa o efeito se **1 linha** entrou; duplicata ⇒ ignora e confirma; falha ⇒ *rollback* leva a marca junto. O *check-then-act* deixa duas entregas passarem no `SELECT`: sem proteção saldo **700** (3 débitos), idempotente **900** (1), ingênuo concorrente **200** (8 débitos). A chave vem **do produtor** (`eventId`), nunca do consumidor. **Retenção da marca ≥ retenção do tópico + janela de reenvio** (limpeza em lotes com ShedLock; particionar se grande).
9. **`DefaultErrorHandler`**: repete na **mesma partição** (preserva ordem, **bloqueia** a partição; `poison-1` = 5 tentativas, `ok-2` esperou) e ao esgotar manda ao **`.DLT`**; **`@RetryableTopic`**: tópicos de retry (`lab.rt-retry-500`, `-retry-1000`, `lab.rt-dlt`), **não bloqueia** e **perde a ordem** (`ok-rt` passou na frente); **RabbitMQ**: retry no listener, esgotado ⇒ **rejeita** ⇒ DLX/DLQ (`max-retries: 2` ⇒ 3 entregas). Armadilha: no Boot 4 a propriedade é **`max-retries`**; `max-attempts` **não tem efeito** (4 entregas com o padrão 3). Cabeçalhos do DLT: `kafka_dlt-original-topic/-partition/-offset/-consumer-group`, `-exception-fqcn/-cause-fqcn/-message/-stacktrace`. Reprocessar = **republicar com o mesmo `eventId`** (a idempotência impede duplicar).
10. **Outbox:** o evento é gravado **na mesma transação** da mudança; um relé publica **depois**, com `FOR UPDATE SKIP LOCKED`, espera o *ack* e **só então** marca `published_at`. Resolve a **escrita dupla** (laboratório: saldo 900 e **0 eventos** sem Outbox). Garantia: **at-least-once** + ordem por agregado (chave); dois relés: 160 + 140 = 300, **300 distintos**. O consumidor é **idempotente** pelo `eventId`: queda entre publicar e marcar ⇒ **mesma mensagem 2×** (esperado, **não** é defeito). **Webhook:** HMAC-SHA256 de `t.corpo` sobre os **bytes recebidos**; janela de ±300 s (anti-*replay*); `MessageDigest.isEqual`; `ON CONFLICT` por `eventId` + máquina de estados `PENDING→PAID` (3 reenvios ⇒ saldo 50,00); recusas `401` (assinatura, corpo adulterado, 1 h atrás, sem cabeçalho), `422` (valor), `409` (expirado).
</details>

## Parte 2: Entrega prática (tag `payflow-P3`)
O `payflow` ganha **notificações e antifraude assíncronos** (RabbitMQ para tarefas, Kafka para eventos) com **Outbox**, **consumidores idempotentes**, **retry + DLQ**, o **pagamento por QR code com webhook** assinado, **eventos em tempo real (SSE)** e **jobs com ShedLock**. Estrutura esperada (**por leitura**; nada disso foi compilado):
```
payflow/
├── docs/
│   ├── async-boundaries.md        passos síncronos × assíncronos; conta de Little                      (P3.01)
│   ├── delivery-guarantees.md     fluxo × garantia × chave de deduplicação × teste que prova         (P3.06)
│   ├── kafka-topics.md            tópicos, chaves, partições (por conta), retenção/compactação, formato do evento   (P3.04, P3.05)
│   ├── runbook-dlq.md             alerta, triagem, reprocesso (mesmo eventId), quem decide descartar    (P3.08)
│   ├── webhooks.md                assinatura, janela, rotação do segredo, reconciliação                 (P3.10)
│   ├── concurrency-model.md       MVC + threads virtuais; o que será medido                            (P3.12)
│   └── postmortem-pagamento-duplicado.md                                                                (P3.13)
├── src/main/java/com/payflow/
│   ├── config/AsyncConfig.java, SchedulingConfig.java        executor limitado + TaskDecorator (MDC); ShedLock (JdbcTemplateLockProvider, usingDbTime)   (P3.02)
│   ├── notification/NotificationTopology.java, EmailWorker.java     exchange payflow.notifications, filas duráveis + DLQ, prefetch justificado   (P3.03)
│   ├── events/TransferEventProducer.java, TransferEventsListener.java   KafkaTemplate (resultado tratado), grupos notifications/antifraud     (P3.05)
│   ├── events/KafkaErrorConfig.java                           DefaultErrorHandler + DeadLetterPublishingRecoverer; exceções não retentáveis  (P3.08)
│   ├── idempotency/IdempotentHandler.java                     runOnce(consumer, messageId, effect) com ON CONFLICT na mesma transação        (P3.07)
│   ├── outbox/OutboxEvent.java, OutboxRelay.java, OutboxCleanupJob.java    FOR UPDATE SKIP LOCKED, ack antes de marcar, limpeza em lotes    (P3.09)
│   ├── payment/PaymentRequest.java, PaymentRequestController.java          POST /api/v1/payment-requests (JWT)                              (P3.10)
│   ├── webhook/WebhookSignature.java, PspWebhookController.java, WebhookSecurityConfig.java   /webhooks/** (3ª cadeia, sem CSRF, só HMAC)     (P3.10)
│   ├── realtime/AccountEventsController.java                  SSE autenticado, por conta, Last-Event-ID a partir do Outbox, heartbeat           (P3.11)
│   ├── fraud/FraudFlagConsumer.java                           INSERT ... ON CONFLICT (event_id) DO NOTHING                                      (P3.07)
│   └── reconciliation/PaymentReconciliationJob.java           duplicados por request_id; alerta; estorno por lançamento compensatório           (P3.13)
├── src/main/resources/db/migration/    V9__shedlock.sql · V10__processed_messages.sql · V11__outbox_events.sql · V12__payment_requests.sql
│                                       (+ UNIQUE (request_id) em payments/lançamentos; renumere se já criou V9+)
├── src/test/java/com/payflow/
│   ├── idempotency/IdempotentHandlerTest.java      reentrega 3× e 8 em paralelo; falha se tirar o ON CONFLICT         (P3.07)
│   ├── outbox/OutboxIT.java                        rollback não deixa linha; queda após o envio ⇒ duplicata com o mesmo eventId; 2 relés       (P3.09)
│   ├── webhook/WebhookIT.java                      os 6 cenários do laboratório                                         (P3.10)
│   ├── messaging/RetryDltIT.java                   Testcontainers kafka + rabbitmq: poison → DLT/DLQ; erro de dado = 1 tentativa     (P3.08)
│   ├── scheduling/ShedLockIT.java                  duas instâncias: 1 execução por janela                              (P3.02)
│   └── realtime/AccountEventsIT.java               replay por Last-Event-ID; conta alheia = 404                         (P3.11)
├── compose.yaml          + serviços rabbitmq e kafka (KRaft) SEM porta publicada; senhas por segredo; healthchecks       (P3.03, P3.04)
├── pom.xml               + spring-boot-starter-kafka, spring-boot-starter-amqp, shedlock-spring + shedlock-provider-jdbc-template (7.11.0),
│                           spring-boot-starter-websocket (se WebSocket) ; testes: org.testcontainers:testcontainers-kafka e -rabbitmq (BOM 2.0.5)
└── projeto/adrs/0015-mensageria-outbox-idempotencia-e-pagamento-por-qr.md     (já redigido nesta geração; preencher as medidas reais no fim do treinamento)
```
Critérios de pronto:
- [ ] **Nenhum** passo que altera dinheiro é assíncrono; antifraude/notificação reagem a **eventos do Outbox** (P3.01, P3.09).
- [ ] Todo consumidor é **idempotente**, com teste de reentrega (3× e concorrente) que **falha** sem a proteção (P3.07).
- [ ] Erro de dado vai **direto** ao DLT/DLQ; erro transitório tem *backoff* com limite < `max.poll.interval.ms`; há **runbook** e alerta de DLQ (P3.08).
- [ ] `/webhooks/**` verifica HMAC dos **bytes**, janela de tempo, `eventId` único e valor; **3 reenvios = 1 crédito** (P3.10).
- [ ] Os *jobs* têm ShedLock com `lockAtMostFor` **justificado**; teste com 2 instâncias (P3.02).
- [ ] O SSE respeita **autorização por conta** e retoma por `Last-Event-ID` a partir do **banco** (P3.11).
- [ ] A reconciliação de **pedido × débitos** existe e o estorno é **lançamento novo** (P3.13).
- [ ] Brokers **sem porta publicada**; segredos fora do Git (J8/J9).
- [ ] `docs/` com as 7 notas; ADR 0015 revisada; tag `payflow-P3`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Pleno: **≥ 2** em todos e **3** nos marcados com ⭐.

| Competência | Aula(s) | 0–3 |
|---|---|---|
| Distinguir síncrono/assíncrono e bloqueante/não bloqueante; Little e disponibilidade composta | P3.01 | |
| Dimensionar *pools* para I/O-bound e CPU-bound e **medir sem enganar-se** | P3.01 | |
| `@Async` (executor limitado, exceção, MDC) e `@Scheduled` com ShedLock | P3.02 | |
| Modelar RabbitMQ (exchanges, ack/nack, prefetch, DLX, TTL) | P3.03 | |
| Kafka: partições, chave, grupos, offsets, *lag*, expulsão | P3.04 | ⭐ |
| Retenção × compactação; Spring Kafka (`AckMode`, serialização) | P3.05 | |
| Garantias de entrega e o escopo do *exactly-once* | P3.06 | ⭐ |
| **Consumidor idempotente** (chave, mesma transação, retenção) | P3.07 | ⭐ |
| Retry com *backoff*, classificação de falhas, DLQ e **reprocesso** | P3.08 | ⭐ |
| **Padrão Outbox** (garantias, `SKIP LOCKED`, relé, limpeza) | P3.09 | ⭐ |
| Webhook seguro (HMAC, *replay*, idempotência) e máquina de estados do pagamento | P3.10 | ⭐ |
| SSE × WebSocket (segurança, escala, reconexão) | P3.11 | |
| WebFlux/Reactor × *virtual threads* (decisão com evidência) | P3.12 | |
| Investigar, reproduzir e corrigir duplicidade; estorno com livro-razão imutável | P3.13 | ⭐ |

**Se algum ⭐ ficou abaixo de 2:** refaça os experimentos da aula (`lab/messaging-lab`) e o respectivo exercício de aplicação antes de seguir para a P4.

## Parte 4: Perguntas de entrevista (simulado rápido)
Responda em voz alta, 2 minutos cada: (1) "Fila ou tópico? RabbitMQ ou Kafka?" (2) "O Kafka é *exactly-once*?" (3) "Como evitar cobrar duas vezes?" (4) "O que é o Outbox e por que não basta um `@TransactionalEventListener`?" (5) "Como você protege um webhook?" (6) "WebFlux ou MVC com *virtual threads*?" (7) "Por que meu `@Scheduled` rodou duas vezes?" (8) "O que é uma DLQ e quem cuida dela?"

## ➡️ Próximo
[P4 — Arquitetura de aplicações](../P4-arquitetura-aplicacoes/README.md)
