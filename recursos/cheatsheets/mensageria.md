# Cheatsheet — Comunicação assíncrona e mensageria (P3)

> Valores **medidos** em `2-pleno/P3-comunicacao-sincrona-assincrona/lab/` (Kafka 4.3.1, RabbitMQ 4.4.0, PostgreSQL 18, Spring Boot 4.1.2, Spring Kafka/AMQP 4.1.2). Veja a aula indicada para o contexto.

## Escolhas rápidas
| Preciso de… | Use | Aula |
|---|---|---|
| Resposta agora, muda dinheiro | **síncrono**, na transação | P3.01 |
| Tarefa curta e **perdível** | `@Async` com executor **limitado** + `TaskDecorator` | P3.02 |
| *Job* periódico com N réplicas | `@Scheduled` + **ShedLock** (`usingDbTime`, `lockAtMostFor` > duração) | P3.02 |
| Fila de tarefas (e-mail), DLQ, TTL | **RabbitMQ** (filas **duráveis** no 4.x) | P3.03 |
| Fluxo de eventos, vários leitores, reprocesso | **Kafka** (chave = entidade) | P3.04, P3.05 |
| Não perder evento ao gravar no banco | **Outbox** + relé (`FOR UPDATE SKIP LOCKED`) | P3.09 |
| Não duplicar efeito | consumidor **idempotente** (`ON CONFLICT` na mesma transação) | P3.07 |
| Receber aviso de terceiro | **webhook** com HMAC dos bytes + `timestamp` + `eventId` | P3.10 |
| Empurrar do servidor para o navegador | **SSE** (`Last-Event-ID`); WebSocket só se bidirecional | P3.11 |

## Kafka
- Ordem **só dentro da partição** (⇒ por chave). Chave nula espalha desigual (`{1=230,2=70}` / `{1=300}`).
- Paralelismo máximo = nº de partições (o 4º consumidor de 3 partições fica ocioso). Aumentar partições **muda** chave→partição.
- Padrões do cliente (4.2.2): produtor `acks=all`, `enable.idempotence=true`, `linger.ms=5`; consumidor `enable.auto.commit=true` (⚠️), `auto.offset.reset=latest` (⚠️ grupo novo não recebe nada), `isolation.level=read_uncommitted`, `max.poll.records=500`, `max.poll.interval.ms=300000`, `session.timeout.ms=45000`.
- *Lag* = fim − confirmado. Retenção padrão 7 dias (`604800000`), `retention.bytes=-1`, `cleanup.policy=delete`; compactação **não** compacta o segmento ativo.
- Spring Kafka: `AckMode` padrão `BATCH`; DLT padrão `<tópico>-dlt` (mesma partição); `DefaultErrorHandler` sem DLT **descarta** depois de esgotar.
- Transações: `transactional.id` + `read_committed`; cobrem **Kafka→Kafka**, **não** o banco.
- Docker de laboratório: porta **igual dentro e fora** do contêiner (senão transações travam).

## RabbitMQ
- Produtor → *exchange* → *binding* → fila. *direct* (chave igual), *topic* (`*` uma palavra, `#` zero ou mais), *fanout*. Sem fila ligada = **descartada em silêncio**.
- `ack`/`nack`/`reject`; não confirmada volta (`redelivered=true`). `nack(requeue=false)` → DLX (`x-death`).
- Spring AMQP: *prefetch* padrão **250**; conversor JSON `JacksonJsonMessageConverter`.
- **RabbitMQ 4.4:** fila não durável e não exclusiva → `541 INTERNAL_ERROR … transient_nonexcl_queues is deprecated`.
- Boot 4: `spring.rabbitmq.listener.simple.retry.max-retries` (`max-attempts` **não tem efeito**); `default-requeue-rejected: false` para ir à DLQ.

## Garantias
| | Ordem das operações | Falha |
|---|---|---|
| *At-most-once* | confirma → processa | **perde** |
| *At-least-once* | processa → confirma | **duplica** |
| *Effectively-once* | *at-least-once* + idempotência | efeito 1× |

## Idempotência e Outbox (SQL)
```sql
insert into processed_messages (consumer, message_id) values (:c, :id) on conflict do nothing;  -- 0 linhas = duplicata
-- outbox: relé
select id, aggregate_id, type, payload from outbox_events
where published_at is null order by created_at, id limit 100 for update skip locked;
-- índice parcial dos pendentes
create index idx_outbox_pending on outbox_events (created_at) where published_at is null;
-- conciliação de pedido x débitos
select request_id, count(*), sum(amount) from payments where request_id is not null group by 1 having count(*) > 1;
```

## Webhook (HMAC-SHA256)
`X-Payflow-Signature: t=<epoch>,v1=<hex(HMAC(secret, t + "." + bytesDoCorpo))>`; janela ±300 s; `MessageDigest.isEqual`; duplicata → `200`; assinatura inválida → `401`; valor diferente → `422`; expirado → `409`.

## Números do laboratório (referência rápida)
200×100 ms: 1 thread 20 063 ms · 200 threads 110 ms · virtuais 106 ms · CPU-bound não melhora além dos núcleos · 1 000×100 ms: pool 50 ≈ 2 000 ms, virtuais ≈ 107 ms, Reactor ≈ 123 ms · bloqueio no Reactor: 100×100 ms = **10 s** · ShedLock (9 s, 2 instâncias): 19 sem *lock*, 5 com.
