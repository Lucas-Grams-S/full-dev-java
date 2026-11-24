package com.payflow.lab;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/** Ouvintes de demonstração de retentativa/DLT. Os resultados ficam em memória para os testes lerem. */
@Component
public class RetryDemoListeners {
    public final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    public final List<String> processed = new CopyOnWriteArrayList<>();
    public final List<ConsumerRecord<String, String>> dead = new CopyOnWriteArrayList<>();
    public final List<String> rtProcessed = new CopyOnWriteArrayList<>();
    public final List<String> rtDead = new CopyOnWriteArrayList<>();
    public final List<String> rabbitProcessed = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "lab.retry", groupId = "lab-retry")
    void onMessage(ConsumerRecord<String, String> rec) {
        int n = attempts.computeIfAbsent(rec.value(), k -> new AtomicInteger()).incrementAndGet();
        System.out.printf("[kafka] %s tentativa %d%n", rec.value(), n);
        if (rec.value().startsWith("bad-data")) throw new IllegalArgumentException("dado inválido: " + rec.value());
        if (rec.value().startsWith("poison")) throw new IllegalStateException("sempre falha: " + rec.value());
        if (rec.value().startsWith("flaky") && n < 3) throw new IllegalStateException("falha transitória " + n);
        processed.add(rec.value());
    }

    @KafkaListener(topics = "lab.retry.DLT", groupId = "lab-retry-dlt")
    void onDead(ConsumerRecord<String, String> rec) {
        dead.add(rec);
    }

    // Tópicos de retry separados (não bloqueiam a partição principal)
    @RetryableTopic(attempts = "3", backOff = @org.springframework.kafka.annotation.BackOff(delay = 500, multiplier = 2.0),
            include = IllegalStateException.class)
    @KafkaListener(topics = "lab.rt", groupId = "lab-rt")
    void onRt(String value) {
        int n = attempts.computeIfAbsent("rt:" + value, k -> new AtomicInteger()).incrementAndGet();
        System.out.printf("[kafka-rt] %s tentativa %d%n", value, n);
        if (value.startsWith("poison")) throw new IllegalStateException("sempre falha");
        rtProcessed.add(value);
    }

    @DltHandler
    void onRtDlt(String value, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        rtDead.add(topic + ":" + value);
    }

    // RabbitMQ: o retry está no application.yml; esgotado, vai para a DLQ (fila declarada no teste)
    @RabbitListener(queues = "lab.retry.work")
    void onRabbit(String body) {
        int n = attempts.computeIfAbsent("rb:" + body, k -> new AtomicInteger()).incrementAndGet();
        System.out.printf("[rabbit] %s tentativa %d%n", body, n);
        if (body.startsWith("poison")) throw new IllegalStateException("sempre falha");
        rabbitProcessed.add(body);
    }
}
