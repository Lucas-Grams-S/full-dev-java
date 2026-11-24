package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;

/** P3.09: o problema da escrita dupla e o padrão Outbox. */
@SpringBootTest
class OutboxIT {
    @Autowired JdbcClient jdbc;
    @Autowired OutboxService service;
    @Autowired OutboxRelay relay;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired org.springframework.transaction.support.TransactionTemplate tx;

    String topic;

    @BeforeEach
    void setup() throws Exception {
        jdbc.sql("delete from outbox_events").update();
        jdbc.sql("delete from wallets").update();
        jdbc.sql("insert into wallets values ('ana', 1000.00), ('bruno', 0.00)").update();
        topic = "lab.outbox." + UUID.randomUUID().toString().substring(0, 8);
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KafkaBasicsIT.BOOT))) {
            admin.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all().get();
        }
    }

    BigDecimal bal(String w) { return jdbc.sql("select balance from wallets where id=:w").param("w", w).query(BigDecimal.class).single(); }

    @Test
    void dualWriteLosesTheEventWhenTheProcessDiesBetweenCommitAndSend() {
        // escrita dupla ingênua: commit no banco, depois envia ao Kafka
        tx.executeWithoutResult(s -> {
            jdbc.sql("update wallets set balance = balance - 100 where id='ana'").update();
            jdbc.sql("update wallets set balance = balance + 100 where id='bruno'").update();
        });
        try { throw new IllegalStateException("processo morreu antes do kafka.send"); } catch (IllegalStateException ignored) { }
        System.out.println("ESCRITA_DUPLA saldo_ana=" + bal("ana") + " eventos_no_kafka=" + KafkaReader.count(topic));
        assertThat(bal("ana")).isEqualByComparingTo("900.00");
        assertThat(KafkaReader.count(topic)).isZero();   // dinheiro mexeu, ninguém foi avisado
    }

    @Test
    void outboxKeepsTheEventAndTheRelayPublishesIt() {
        service.transferWithOutbox("ana", "bruno", new BigDecimal("100.00"), "t-1");
        int pending = jdbc.sql("select count(*) from outbox_events where published_at is null").query(Integer.class).single();
        assertThat(pending).isEqualTo(1);
        int n = relay.pollOnce(topic, 100, w -> { });
        System.out.println("OUTBOX publicados=" + n + " no_kafka=" + KafkaReader.count(topic));
        assertThat(n).isEqualTo(1);
        assertThat(KafkaReader.count(topic)).isEqualTo(1);
        assertThat(relay.pollOnce(topic, 100, w -> { })).isZero();
    }

    @Test
    void crashAfterSendBeforeMarkingCausesDuplicate_atLeastOnce() {
        service.transferWithOutbox("ana", "bruno", new BigDecimal("100.00"), "t-2");
        try {
            relay.pollOnce(topic, 100, w -> { throw new IllegalStateException("crash " + w); });
        } catch (IllegalStateException e) {
            System.out.println("CRASH_SIMULADO " + e.getMessage());
        }
        // o envio aconteceu, mas a marca "published_at" foi desfeita (rollback): na próxima rodada sai de novo
        relay.pollOnce(topic, 100, w -> { });
        int total = KafkaReader.count(topic);
        System.out.println("AT_LEAST_ONCE mensagens_no_topico=" + total + " (mesmo event-id duas vezes)");
        assertThat(total).isEqualTo(2);
        var ids = KafkaReader.eventIds(topic);
        assertThat(new HashSet<>(ids)).hasSize(1);       // mesmo event-id: o consumidor idempotente descarta a 2ª
    }

    @Test
    void twoRelaysWithSkipLockedDoNotPublishTheSameEventTwice() throws Exception {
        for (int i = 0; i < 300; i++) service.transferWithOutbox("ana", "bruno", new BigDecimal("0.01"), "t-" + i);
        var pool = Executors.newFixedThreadPool(2);
        var published = new ConcurrentHashMap<String, Integer>();
        Callable<Integer> job = () -> {
            int total = 0, n;
            while ((n = relay.pollOnce(topic, 20, w -> { })) > 0) total += n;
            return total;
        };
        var f1 = pool.submit(job); var f2 = pool.submit(job);
        int a = f1.get(60, TimeUnit.SECONDS), b = f2.get(60, TimeUnit.SECONDS);
        pool.shutdown();
        System.out.println("DOIS_RELES relay1=" + a + " relay2=" + b + " soma=" + (a + b) + " no_kafka=" + KafkaReader.count(topic));
        assertThat(a + b).isEqualTo(300);
        assertThat(KafkaReader.count(topic)).isEqualTo(300);
        assertThat(new HashSet<>(KafkaReader.eventIds(topic))).hasSize(300);
    }

    /** Lê um tópico inteiro (read_committed) e devolve contagem e event-ids. */
    static class KafkaReader {
        static int count(String topic) { return read(topic).size(); }
        static List<String> eventIds(String topic) { return read(topic).stream().map(r -> r.headers().lastHeader("event-id") == null ? "?" : new String(r.headers().lastHeader("event-id").value())).toList(); }
        static List<org.apache.kafka.clients.consumer.ConsumerRecord<String, String>> read(String topic) {
            var cfg = new HashMap<String, Object>(Map.of("bootstrap.servers", KafkaBasicsIT.BOOT, "group.id", "g-" + UUID.randomUUID(),
                    "auto.offset.reset", "earliest",
                    "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                    "value.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName()));
            List<org.apache.kafka.clients.consumer.ConsumerRecord<String, String>> out = new ArrayList<>();
            try (var c = new org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(cfg)) {
                c.subscribe(List.of(topic));
                long quiet = System.currentTimeMillis() + 3000;
                while (System.currentTimeMillis() < quiet) {
                    var recs = c.poll(Duration.ofMillis(300));
                    if (!recs.isEmpty()) { recs.forEach(out::add); quiet = System.currentTimeMillis() + 1500; }
                }
            }
            return out;
        }
    }
}
