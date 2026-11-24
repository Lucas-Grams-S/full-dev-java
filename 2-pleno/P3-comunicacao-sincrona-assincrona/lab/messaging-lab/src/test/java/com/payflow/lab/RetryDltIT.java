package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

/** P3.08: retentativas com backoff e dead-letter, em Kafka (DefaultErrorHandler e @RetryableTopic) e RabbitMQ. */
@SpringBootTest
class RetryDltIT {
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired RabbitTemplate rabbit;
    @Autowired RetryDemoListeners l;

    @BeforeAll
    static void topics() throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", "localhost:59092"))) {
            var existing = admin.listTopics().names().get();
            var news = new java.util.ArrayList<org.apache.kafka.clients.admin.NewTopic>();
            for (String t : List.of("lab.retry", "lab.retry.DLT"))
                if (!existing.contains(t)) news.add(new org.apache.kafka.clients.admin.NewTopic(t, 1, (short) 1));
            admin.createTopics(news).all().get();
        }
    }

    @Test
    void errorHandlerRetriesThenDeadLetters() throws Exception {
        long t0 = System.currentTimeMillis();
        kafka.send("lab.retry", "k", "ok-1").get();
        kafka.send("lab.retry", "k", "flaky-1").get();
        kafka.send("lab.retry", "k", "bad-data-1").get();
        kafka.send("lab.retry", "k", "poison-1").get();
        kafka.send("lab.retry", "k", "ok-2").get();
        await().atMost(Duration.ofSeconds(30)).until(() -> l.dead.size() == 2 && l.processed.size() == 3);
        System.out.println("TEMPO_TOTAL_MS=" + (System.currentTimeMillis() - t0));
        System.out.println("PROCESSADAS=" + l.processed);
        System.out.println("TENTATIVAS=" + new java.util.TreeMap<>(l.attempts));
        for (var d : l.dead) {
            System.out.println("DLT valor=" + d.value() + " particao=" + d.partition());
            d.headers().forEach(h -> {
                if (h.key().startsWith("kafka_dlt-")) {
                    String v = new String(h.value());
                    System.out.println("   " + h.key() + "=" + (v.length() > 80 ? v.substring(0, 80) + "…" : v));
                }
            });
        }
        assertThat(l.processed).containsExactly("ok-1", "flaky-1", "ok-2");
        assertThat(l.attempts.get("flaky-1").get()).isEqualTo(3);
        assertThat(l.attempts.get("bad-data-1").get()).isEqualTo(1);   // não retentável: 1 tentativa
        assertThat(l.attempts.get("poison-1").get()).isGreaterThan(2);  // retentável: várias
    }

    @Test
    void retryableTopicUsesSeparateTopics() throws Exception {
        kafka.send("lab.rt", "k", "poison-rt").get();
        kafka.send("lab.rt", "k", "ok-rt").get();
        await().atMost(Duration.ofSeconds(30)).until(() -> !l.rtDead.isEmpty() && l.rtProcessed.contains("ok-rt"));
        System.out.println("RT_DLT=" + l.rtDead);
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", "localhost:59092"))) {
            var names = admin.listTopics().names().get().stream().filter(n -> n.startsWith("lab.rt")).sorted().toList();
            System.out.println("RT_TOPICOS=" + names);
            assertThat(names).contains("lab.rt-retry-500", "lab.rt-retry-1000", "lab.rt-dlt");
        }
    }

    @Test
    void rabbitRetriesThenDeadLetters() throws Exception {
        rabbit.convertAndSend("", "lab.retry.work", "poison-r1");
        rabbit.convertAndSend("", "lab.retry.work", "ok-r1");
        var dead = rabbit.receive("lab.retry.dlq", 15_000);
        assertThat(dead).isNotNull();
        System.out.println("RABBIT_DLQ corpo=" + new String(dead.getBody()) + " x-death=" + dead.getMessageProperties().getXDeathHeader());
        // max-retries: 2 = 1 entrega + 2 retentativas = 3 entregas
        assertThat(l.attempts.get("rb:poison-r1").get()).isEqualTo(3);
        await().atMost(Duration.ofSeconds(5)).until(() -> l.rabbitProcessed.contains("ok-r1"));
    }
}
