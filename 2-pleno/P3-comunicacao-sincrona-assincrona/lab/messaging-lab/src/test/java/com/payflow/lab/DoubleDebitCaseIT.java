package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** P3.13: o pagamento debitado duas vezes. Evento no Kafka + consumidor lento (expulso do grupo) + debito sem idempotencia. */
@SpringBootTest
class DoubleDebitCaseIT {
    @Autowired JdbcClient jdbc;
    @Autowired PaymentConsumer consumer;

    @BeforeEach
    void reset() {
        jdbc.sql("delete from payments").update();
        jdbc.sql("delete from processed_messages").update();
        jdbc.sql("delete from wallets").update();
        jdbc.sql("insert into wallets values ('cliente', 1000.00)").update();
    }

    /** Um consumidor com max.poll.interval.ms=3000 cujo processamento da 1a entrega leva 5 s. */
    void runScenario(Consumer<String> handler) throws Exception {
        String topic = KafkaBasicsIT.newTopic(1);
        try (var p = KafkaBasicsIT.producer()) { p.send(new ProducerRecord<>(topic, "cliente", "pay-777")).get(); }
        var cfg = new HashMap<String, Object>(Map.of("bootstrap.servers", KafkaBasicsIT.BOOT, "group.id", "g-case-" + topic,
                "auto.offset.reset", "earliest", "enable.auto.commit", "false", "max.poll.interval.ms", "3000",
                "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                "value.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName()));
        try (var c = new KafkaConsumer<String, String>(cfg)) {
            c.subscribe(List.of(topic));
            boolean first = true;
            long end = System.currentTimeMillis() + 25_000;
            int committed = 0;
            while (committed == 0 && System.currentTimeMillis() < end) {
                for (var r : c.poll(Duration.ofMillis(500))) {
                    handler.accept(r.value());                              // 1) efeito (debito)
                    if (first) { Thread.sleep(5000); first = false; }       // 2) lento: passa do max.poll.interval.ms
                    try { c.commitSync(); committed++; }                    // 3) commit
                    catch (CommitFailedException e) { System.out.println("CASO commit falhou (consumidor expulso do grupo); a mensagem sera reentregue"); }
                }
            }
        }
    }

    @Test
    void withoutIdempotencyTheCustomerIsDebitedTwice() throws Exception {
        runScenario(id -> consumer.handleUnprotected("cliente", new BigDecimal("100.00")));
        var saldo = jdbc.sql("select balance from wallets where id='cliente'").query(BigDecimal.class).single();
        int n = jdbc.sql("select count(*) from payments").query(Integer.class).single();
        System.out.println("CASO_SEM_IDEMPOTENCIA pagamento unico pay-777 de 100.00 -> debitos=" + n + " saldo=" + saldo);
        assertThat(n).isEqualTo(2);
    }

    @Test
    void withIdempotencyTheCustomerIsDebitedOnce() throws Exception {
        runScenario(id -> consumer.handleIdempotent(id, "cliente", new BigDecimal("100.00")));
        var saldo = jdbc.sql("select balance from wallets where id='cliente'").query(BigDecimal.class).single();
        int n = jdbc.sql("select count(*) from payments").query(Integer.class).single();
        System.out.println("CASO_COM_IDEMPOTENCIA pagamento unico pay-777 de 100.00 -> debitos=" + n + " saldo=" + saldo);
        assertThat(n).isEqualTo(1);
    }

    @Test
    void reconciliationQueryFindsDuplicates() throws Exception {
        runScenario(id -> {
            consumer.handleUnprotected("cliente", new BigDecimal("100.00"));
            jdbc.sql("update payments set request_id = :r where request_id is null").param("r", id).update();
        });
        var dups = jdbc.sql("select request_id, count(*) c, sum(amount) total from payments where request_id is not null group by request_id having count(*) > 1")
                .query().listOfRows();
        System.out.println("CASO_CONCILIACAO duplicados=" + dups);
        assertThat(dups).hasSize(1);
    }
}
