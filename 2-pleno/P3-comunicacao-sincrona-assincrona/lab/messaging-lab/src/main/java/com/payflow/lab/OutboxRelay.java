package com.payflow.lab;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** P3.09: lê o outbox com FOR UPDATE SKIP LOCKED, publica no Kafka, espera o ack e marca como publicado. */
@Component
public class OutboxRelay {
    public interface CrashPoint { void check(String where); }

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;

    public OutboxRelay(JdbcClient jdbc, KafkaTemplate<String, String> kafka, TransactionTemplate tx) {
        this.jdbc = jdbc; this.kafka = kafka; this.tx = tx;
    }

    record Row(UUID id, String aggregateId, String type, String payload) {}

    /** Publica até {@code batch} eventos pendentes. Devolve quantos publicou. */
    public int pollOnce(String topic, int batch, CrashPoint crash) {
        Integer n = tx.execute(status -> {
            List<Row> rows = jdbc.sql("""
                    select id, aggregate_id, type, payload from outbox_events
                    where published_at is null order by created_at, id
                    limit :n for update skip locked""").param("n", batch)
                    .query((rs, i) -> new Row(rs.getObject("id", UUID.class), rs.getString("aggregate_id"),
                            rs.getString("type"), rs.getString("payload"))).list();
            for (Row r : rows) {
                try {
                    // a chave = agregado: eventos da mesma conta ficam na mesma partição, em ordem
                    var rec = new org.apache.kafka.clients.producer.ProducerRecord<>(topic, r.aggregateId(), r.payload());
                    rec.headers().add("event-id", r.id().toString().getBytes());
                    rec.headers().add("event-type", r.type().getBytes());
                    kafka.send(rec).get(10, TimeUnit.SECONDS);       // espera o ack do broker (acks=all)
                } catch (Exception e) {
                    throw new IllegalStateException("falha ao publicar " + r.id(), e);
                }
                crash.check("depois-do-envio-antes-de-marcar");
                jdbc.sql("update outbox_events set published_at = now(), attempts = attempts + 1 where id = :id")
                        .param("id", r.id()).update();
            }
            return rows.size();
        });
        return n == null ? 0 : n;
    }
}
