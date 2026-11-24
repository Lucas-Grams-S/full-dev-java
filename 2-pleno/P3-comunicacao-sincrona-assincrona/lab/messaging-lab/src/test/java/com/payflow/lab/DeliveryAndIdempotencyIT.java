package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** P3.06 (semânticas de entrega) e P3.07 (consumidor idempotente). */
@SpringBootTest
class DeliveryAndIdempotencyIT {
    @Autowired JdbcClient jdbc;
    @Autowired PaymentConsumer consumer;

    @BeforeEach
    void reset() {
        jdbc.sql("delete from payments").update();
        jdbc.sql("delete from processed_messages").update();
        jdbc.sql("delete from wallets").update();
        jdbc.sql("insert into wallets values ('ana', 1000.00)").update();
    }

    BigDecimal balance() { return jdbc.sql("select balance from wallets where id='ana'").query(BigDecimal.class).single(); }
    int payments() { return jdbc.sql("select count(*) from payments").query(Integer.class).single(); }

    // ---------- P3.06 ----------

    @Test
    void atMostOnceLosesAndAtLeastOnceDuplicates() throws Exception {
        String topic = KafkaBasicsIT.newTopic(1);
        try (var p = KafkaBasicsIT.producer()) {
            for (int i = 0; i < 10; i++) p.send(new ProducerRecord<>(topic, "k", "m" + i)).get();
        }
        // at-most-once: confirma o offset ANTES de processar e cai no meio
        List<String> done = new ArrayList<>();
        try (var c = KafkaBasicsIT.consumer("g-amo-" + topic)) {
            c.subscribe(List.of(topic));
            ConsumerRecords<String, String> recs = ConsumerRecords.empty();
            while (recs.isEmpty()) recs = c.poll(Duration.ofMillis(500));
            c.commitSync();                          // 1) confirma tudo que veio
            int k = 0;
            for (var r : recs) { if (k++ == 3) break; done.add(r.value()); } // 2) processa 3 e "cai"
        }
        List<String> after = new ArrayList<>();
        try (var c = KafkaBasicsIT.consumer("g-amo-" + topic)) {
            c.subscribe(List.of(topic));
            long end = System.currentTimeMillis() + 4000;
            while (System.currentTimeMillis() < end) c.poll(Duration.ofMillis(300)).forEach(r -> after.add(r.value()));
        }
        System.out.println("AT_MOST_ONCE processou=" + done + " depois_da_queda_recebeu=" + after + " (perdidas=7)");
        assertThat(after).isEmpty();

        // at-least-once: processa ANTES de confirmar e cai no meio
        String topic2 = KafkaBasicsIT.newTopic(1);
        try (var p = KafkaBasicsIT.producer()) {
            for (int i = 0; i < 10; i++) p.send(new ProducerRecord<>(topic2, "k", "m" + i)).get();
        }
        List<String> first = new ArrayList<>();
        try (var c = KafkaBasicsIT.consumer("g-alo-" + topic2)) {
            c.subscribe(List.of(topic2));
            ConsumerRecords<String, String> recs = ConsumerRecords.empty();
            while (recs.isEmpty()) recs = c.poll(Duration.ofMillis(500));
            int k = 0;
            for (var r : recs) { if (k++ == 6) break; first.add(r.value()); } // processa 6 e cai SEM confirmar
        }
        List<String> second = new ArrayList<>();
        try (var c = KafkaBasicsIT.consumer("g-alo-" + topic2)) {
            c.subscribe(List.of(topic2));
            long end = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < end && second.size() < 10) c.poll(Duration.ofMillis(300)).forEach(r -> second.add(r.value()));
        }
        System.out.println("AT_LEAST_ONCE antes_da_queda=" + first + " depois_recebeu=" + second);
        assertThat(second).hasSize(10);
    }

    @Test
    void transactionsHideAbortedRecordsFromReadCommitted() throws Exception {
        String topic = KafkaBasicsIT.newTopic(1);
        var props = new HashMap<String, Object>(Map.of("bootstrap.servers", KafkaBasicsIT.BOOT,
                "key.serializer", StringSerializer.class.getName(), "value.serializer", StringSerializer.class.getName(),
                "transactional.id", "tx-" + topic));
        try (var p = new KafkaProducer<String, String>(props)) {
            p.initTransactions();
            p.beginTransaction();
            p.send(new ProducerRecord<>(topic, "k", "aborted-1"));
            p.send(new ProducerRecord<>(topic, "k", "aborted-2"));
            p.flush();
            // lê com read_uncommitted e read_committed ENQUANTO a transação está aberta
            System.out.println("TX_ABERTA read_uncommitted=" + readAll(topic, "read_uncommitted") + " read_committed=" + readAll(topic, "read_committed"));
            p.abortTransaction();
            p.beginTransaction();
            p.send(new ProducerRecord<>(topic, "k", "committed-1"));
            p.commitTransaction();
        }
        var rc = readAll(topic, "read_committed");
        var ru = readAll(topic, "read_uncommitted");
        System.out.println("TX_FIM read_committed=" + rc + " read_uncommitted=" + ru);
        assertThat(rc).containsExactly("committed-1");
        assertThat(ru).contains("aborted-1", "aborted-2", "committed-1");
    }

    static List<String> readAll(String topic, String isolation) {
        var cfg = new HashMap<String, Object>(Map.of("bootstrap.servers", KafkaBasicsIT.BOOT, "group.id", "g-" + UUID.randomUUID(),
                "auto.offset.reset", "earliest", "isolation.level", isolation,
                "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                "value.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName()));
        List<String> out = new ArrayList<>();
        try (var c = new KafkaConsumer<String, String>(cfg)) {
            c.subscribe(List.of(topic));
            long end = System.currentTimeMillis() + 4000;
            while (System.currentTimeMillis() < end) c.poll(Duration.ofMillis(300)).forEach(r -> out.add(r.value()));
        }
        return out;
    }

    // ---------- P3.07 ----------

    @Test
    void withoutProtectionDuplicatesDebitTwice() {
        for (int i = 0; i < 3; i++) consumer.handleUnprotected("ana", new BigDecimal("100.00")); // a mesma mensagem, 3 entregas
        System.out.println("SEM_PROTECAO saldo=" + balance() + " pagamentos=" + payments());
        assertThat(balance()).isEqualByComparingTo("700.00");
    }

    @Test
    void idempotentConsumerDebitsOnce() {
        boolean[] r = new boolean[5];
        for (int i = 0; i < 5; i++) r[i] = consumer.handleIdempotent("msg-1", "ana", new BigDecimal("100.00"));
        System.out.println("IDEMPOTENTE_SEQUENCIAL resultados=" + Arrays.toString(r) + " saldo=" + balance() + " pagamentos=" + payments());
        assertThat(balance()).isEqualByComparingTo("900.00");
        assertThat(payments()).isEqualTo(1);
    }

    @Test
    void idempotentConsumerSurvivesConcurrentDuplicates() throws Exception {
        runConcurrently(8, () -> consumer.handleIdempotent("msg-2", "ana", new BigDecimal("100.00")));
        System.out.println("IDEMPOTENTE_CONCORRENTE(8 threads) saldo=" + balance() + " pagamentos=" + payments());
        assertThat(balance()).isEqualByComparingTo("900.00");
    }

    @Test
    void naiveCheckThenActHasARace() throws Exception {
        runConcurrently(8, () -> consumer.handleNaive("msg-3", "ana", new BigDecimal("100.00")));
        System.out.println("INGENUO_CONCORRENTE(8 threads) saldo=" + balance() + " pagamentos=" + payments());
        assertThat(payments()).isGreaterThan(1);   // a corrida acontece (não é determinístico, mas com 8 threads e a barreira é quase certo)
    }

    @Test
    void failureRollsBackTheMarkSoRetryWorks() {
        // saldo insuficiente: o CHECK do banco falha DENTRO da transação -> a marca também volta atrás
        try { consumer.handleIdempotent("msg-4", "ana", new BigDecimal("5000.00")); } catch (Exception e) {
            System.out.println("FALHA " + e.getClass().getSimpleName());
        }
        int marks = jdbc.sql("select count(*) from processed_messages where message_id='msg-4'").query(Integer.class).single();
        assertThat(marks).isZero();                // a marca NÃO ficou: uma retentativa processa de verdade
        jdbc.sql("update wallets set balance = 6000 where id='ana'").update();
        assertThat(consumer.handleIdempotent("msg-4", "ana", new BigDecimal("5000.00"))).isTrue();
    }

    static void runConcurrently(int n, Callable<Object> task) throws Exception {
        var pool = Executors.newFixedThreadPool(n);
        var barrier = new CyclicBarrier(n);
        List<Future<Object>> fs = new ArrayList<>();
        for (int i = 0; i < n; i++) fs.add(pool.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return task.call(); }));
        for (var f : fs) f.get(30, TimeUnit.SECONDS);
        pool.shutdownNow();
    }
}
