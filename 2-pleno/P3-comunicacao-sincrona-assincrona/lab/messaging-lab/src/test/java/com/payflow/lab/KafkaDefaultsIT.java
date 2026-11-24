package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.errors.InvalidProducerEpochException;
import org.junit.jupiter.api.Test;

/** P3.04–P3.06: padrões do cliente, chave nula, e o que acontece quando o processamento passa de max.poll.interval.ms. */
class KafkaDefaultsIT {
    static String def(ConfigDef d, String k) { return String.valueOf(d.configKeys().get(k).defaultValue); }

    @Test
    void clientDefaults() {
        var c = ConsumerConfig.configDef(); var p = ProducerConfig.configDef();
        for (String k : List.of("acks", "enable.idempotence", "retries", "delivery.timeout.ms", "linger.ms", "batch.size", "max.in.flight.requests.per.connection", "compression.type"))
            System.out.println("PRODUCER_DEFAULT " + k + "=" + def(p, k));
        for (String k : List.of("enable.auto.commit", "auto.offset.reset", "isolation.level", "max.poll.records", "max.poll.interval.ms", "session.timeout.ms", "heartbeat.interval.ms", "group.protocol", "partition.assignment.strategy"))
            System.out.println("CONSUMER_DEFAULT " + k + "=" + def(c, k));
        assertThat(def(p, "acks")).isEqualTo("all");
        assertThat(def(p, "enable.idempotence")).isEqualTo("true");
    }

    @Test
    void nullKeySpreadsAcrossPartitionsAndLosesOrdering() throws Exception {
        String topic = KafkaBasicsIT.newTopic(3);
        Map<Integer, Integer> perPartition = new TreeMap<>();
        try (var p = KafkaBasicsIT.producer()) {
            for (int i = 0; i < 300; i++) {
                var md = p.send(new ProducerRecord<String, String>(topic, null, "v" + i)).get();
                perPartition.merge(md.partition(), 1, Integer::sum);
            }
        }
        System.out.println("CHAVE_NULA 300 mensagens por partição=" + perPartition);
        assertThat(perPartition.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(300);
    }

    @Test
    void slowProcessingKicksTheConsumerOutAndMessagesComeBackDuplicated() throws Exception {
        String topic = KafkaBasicsIT.newTopic(1);
        try (var p = KafkaBasicsIT.producer()) { for (int i = 0; i < 5; i++) p.send(new ProducerRecord<>(topic, "k", "m" + i)).get(); }
        var cfg = new HashMap<String, Object>(Map.of("bootstrap.servers", KafkaBasicsIT.BOOT, "group.id", "g-slow-" + topic,
                "auto.offset.reset", "earliest", "enable.auto.commit", "false", "max.poll.interval.ms", "3000",
                "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                "value.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName()));
        try (var c = new KafkaConsumer<String, String>(cfg)) {
            c.subscribe(List.of(topic));
            ConsumerRecords<String, String> recs = ConsumerRecords.empty();
            while (recs.isEmpty()) recs = c.poll(Duration.ofMillis(500));
            System.out.println("LENTO recebeu " + recs.count() + " mensagens; vai 'processar' por 6 s (max.poll.interval.ms=3000)");
            Thread.sleep(6000);                          // o processamento passou do limite; nenhum poll() nesse tempo
            try {
                c.commitSync();
                System.out.println("LENTO commit funcionou (inesperado)");
            } catch (CommitFailedException e) {
                System.out.println("LENTO commitSync falhou: " + e.getClass().getSimpleName() + ": " + e.getMessage().substring(0, 110) + "…");
            }
        }
        // outro consumidor do mesmo grupo recebe tudo de novo
        try (var c = new KafkaConsumer<String, String>(new HashMap<>(cfg) {{ put("max.poll.interval.ms", "300000"); }})) {
            c.subscribe(List.of(topic));
            int again = 0; long end = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < end && again < 5) again += c.poll(Duration.ofMillis(300)).count();
            System.out.println("LENTO depois da expulsão, o grupo reentregou " + again + " mensagens (processamento duplicado)");
            assertThat(again).isEqualTo(5);
        }
    }
}
