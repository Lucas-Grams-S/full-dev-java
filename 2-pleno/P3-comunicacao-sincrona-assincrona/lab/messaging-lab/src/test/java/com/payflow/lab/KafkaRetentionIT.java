package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

/** P3.05: retenção por tempo e compactação por chave. Requer o broker com log.retention.check.interval.ms e log.cleaner.backoff.ms curtos (ver lab/README.md). */
class KafkaRetentionIT {
    @Test
    void retentionDeletesOldSegmentsButOffsetsKeepGrowing() throws Exception {
        String t = "lab.ret." + UUID.randomUUID().toString().substring(0, 8);
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KafkaBasicsIT.BOOT))) {
            admin.createTopics(List.of(new NewTopic(t, 1, (short) 1).configs(Map.of("retention.ms", "3000", "segment.ms", "1000", "segment.bytes", "1048576")))).all().get();
            try (var p = KafkaBasicsIT.producer()) {
                for (int i = 0; i < 20; i++) p.send(new ProducerRecord<>(t, "k", "old-" + i)).get();
                Thread.sleep(2500);
                for (int i = 0; i < 5; i++) p.send(new ProducerRecord<>(t, "k", "new-" + i)).get();   // força rolar o segmento
            }
            var tp = new TopicPartition(t, 0);
            long start = 0, end = 0;
            for (int i = 0; i < 40; i++) {
                start = admin.listOffsets(Map.of(tp, OffsetSpec.earliest())).all().get().get(tp).offset();
                end = admin.listOffsets(Map.of(tp, OffsetSpec.latest())).all().get().get(tp).offset();
                if (start > 0) break;
                Thread.sleep(1000);
            }
            System.out.println("RETENCAO log_start_offset=" + start + " log_end_offset=" + end + " (20 antigas escritas, retention.ms=3000)");
            assertThat(start).isGreaterThan(0);
            // quem pede um offset que já foi apagado:
            var cfg = new HashMap<String, Object>(Map.of("bootstrap.servers", KafkaBasicsIT.BOOT, "group.id", "g-ret-" + t, "enable.auto.commit", "false",
                    "auto.offset.reset", "none",
                    "key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName(),
                    "value.deserializer", org.apache.kafka.common.serialization.StringDeserializer.class.getName()));
            try (var c = new org.apache.kafka.clients.consumer.KafkaConsumer<String, String>(cfg)) {
                c.assign(List.of(tp)); c.seek(tp, 0);
                try { c.poll(Duration.ofSeconds(3)); System.out.println("RETENCAO seek(0) não falhou?"); }
                catch (org.apache.kafka.clients.consumer.OffsetOutOfRangeException e) { System.out.println("RETENCAO seek(0) -> OffsetOutOfRangeException: " + e.offsetOutOfRangePartitions()); }
            }
        }
    }

    @Test
    void compactionKeepsTheLatestValuePerKey() throws Exception {
        String t = "lab.compact." + UUID.randomUUID().toString().substring(0, 8);
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KafkaBasicsIT.BOOT))) {
            admin.createTopics(List.of(new NewTopic(t, 1, (short) 1).configs(Map.of("cleanup.policy", "compact", "segment.ms", "1000",
                    "min.cleanable.dirty.ratio", "0.01", "min.compaction.lag.ms", "0", "delete.retention.ms", "100")))).all().get();
            try (var p = KafkaBasicsIT.producer()) {
                for (int round = 1; round <= 5; round++) for (String k : List.of("ana", "bruno", "carla")) p.send(new ProducerRecord<>(t, k, "saldo-v" + round)).get();
                Thread.sleep(2500);
                for (String k : List.of("ana", "bruno", "carla")) p.send(new ProducerRecord<>(t, k, "saldo-v6")).get();   // rola o segmento
            }
            List<String> seen = List.of();
            for (int i = 0; i < 40; i++) {
                seen = DeliveryAndIdempotencyIT.readAll(t, "read_uncommitted");
                if (seen.size() <= 6) break;
                Thread.sleep(1000);
            }
            System.out.println("COMPACTACAO 18 mensagens escritas (3 chaves × 6 versões); depois da compactação restam " + seen.size() + ": " + seen);
            assertThat(seen.size()).isLessThan(18);
        }
    }
}
