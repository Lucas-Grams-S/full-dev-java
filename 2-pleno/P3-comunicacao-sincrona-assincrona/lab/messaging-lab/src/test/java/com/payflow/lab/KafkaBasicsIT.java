package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

/** P3.04: partições, chaves, grupos e offsets com o cliente puro (sem Spring). */
class KafkaBasicsIT {
    static final String BOOT = "localhost:59092";

    static String newTopic(int partitions) throws Exception {
        String t = "lab.basics." + UUID.randomUUID().toString().substring(0, 8);
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", BOOT))) {
            admin.createTopics(List.of(new NewTopic(t, partitions, (short) 1))).all().get();
        }
        return t;
    }

    static KafkaConsumer<String, String> consumer(String group) {
        return new KafkaConsumer<>(Map.of("bootstrap.servers", BOOT, "group.id", group,
                "auto.offset.reset", "earliest", "enable.auto.commit", "false",
                "key.deserializer", StringDeserializer.class.getName(),
                "value.deserializer", StringDeserializer.class.getName()));
    }

    static KafkaProducer<String, String> producer() {
        return new KafkaProducer<>(Map.of("bootstrap.servers", BOOT,
                "key.serializer", StringSerializer.class.getName(),
                "value.serializer", StringSerializer.class.getName()));
    }

    @Test
    void sameKeyGoesToSamePartitionAndKeepsOrder() throws Exception {
        String topic = newTopic(3);
        Map<String, Set<Integer>> partitionsByKey = new TreeMap<>();
        try (var p = producer()) {
            for (int i = 0; i < 30; i++) {
                String key = "acc-" + (i % 3);
                var md = p.send(new ProducerRecord<>(topic, key, "seq-" + i)).get();
                partitionsByKey.computeIfAbsent(key, k -> new TreeSet<>()).add(md.partition());
            }
        }
        System.out.println("PARTICAO_POR_CHAVE " + partitionsByKey);
        partitionsByKey.values().forEach(s -> assertThat(s).hasSize(1));

        Map<String, List<Integer>> seqByKey = new TreeMap<>();
        try (var c = consumer("g-order-" + topic)) {
            c.subscribe(List.of(topic));
            int read = 0;
            long end = System.currentTimeMillis() + 15_000;
            while (read < 30 && System.currentTimeMillis() < end) {
                for (var r : c.poll(Duration.ofMillis(500))) {
                    seqByKey.computeIfAbsent(r.key(), k -> new ArrayList<>()).add(Integer.parseInt(r.value().substring(4)));
                    read++;
                }
            }
        }
        System.out.println("ORDEM_POR_CHAVE " + seqByKey);
        seqByKey.values().forEach(l -> assertThat(l).isSorted());
        assertThat(seqByKey.values().stream().mapToInt(List::size).sum()).isEqualTo(30);
    }

    @Test
    void consumerGroupSplitsPartitions() throws Exception {
        String topic = newTopic(3);
        String group = "g-split-" + topic;
        List<KafkaConsumer<String, String>> cs = new ArrayList<>();
        for (int n = 1; n <= 4; n++) {
            var c = consumer(group);
            c.subscribe(List.of(topic));
            cs.add(c);
            final int count = n;
            long end = System.currentTimeMillis() + 20_000;
            // todos os consumidores precisam fazer poll para o rebalance terminar
            while (System.currentTimeMillis() < end) {
                cs.forEach(x -> x.poll(Duration.ofMillis(200)));
                if (cs.stream().mapToInt(x -> x.assignment().size()).sum() == 3
                        && cs.stream().allMatch(x -> count > 3 || !x.assignment().isEmpty())) break;
            }
            System.out.println("CONSUMIDORES=" + n + " ATRIBUICAO=" + cs.stream().map(x -> x.assignment().stream().map(TopicPartition::partition).sorted().toList()).toList());
        }
        assertThat(cs.stream().filter(x -> x.assignment().isEmpty()).count()).isEqualTo(1);
        cs.forEach(KafkaConsumer::close);
    }

    @Test
    void offsetsAreCommittedPerGroup() throws Exception {
        String topic = newTopic(1);
        try (var p = producer()) {
            for (int i = 0; i < 10; i++) p.send(new ProducerRecord<>(topic, "k", "m" + i)).get();
        }
        String group = "g-off-" + topic;
        try (var c = consumer(group)) {
            c.subscribe(List.of(topic));
            var got = new ArrayList<ConsumerRecord<String, String>>();
            while (got.size() < 5) c.poll(Duration.ofMillis(500)).forEach(got::add);
            // devolve o excedente: confirma só até a 5ª
            var tp = new TopicPartition(topic, 0);
            c.commitSync(Map.of(tp, new OffsetAndMetadata(5)));
        }
        try (var c = consumer(group)) {
            c.subscribe(List.of(topic));
            ConsumerRecords<String, String> r = ConsumerRecords.empty();
            long end = System.currentTimeMillis() + 15_000;
            while (r.isEmpty() && System.currentTimeMillis() < end) r = c.poll(Duration.ofMillis(500));
            String first = r.iterator().next().value();
            System.out.println("MESMO_GRUPO_RETOMA_EM=" + first);
            assertThat(first).isEqualTo("m5");
        }
        try (var c = consumer("g-other-" + topic)) {
            c.subscribe(List.of(topic));
            ConsumerRecords<String, String> r = ConsumerRecords.empty();
            long end = System.currentTimeMillis() + 15_000;
            while (r.isEmpty() && System.currentTimeMillis() < end) r = c.poll(Duration.ofMillis(500));
            String first = r.iterator().next().value();
            System.out.println("OUTRO_GRUPO_COMECA_EM=" + first);
            assertThat(first).isEqualTo("m0");
        }
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", BOOT))) {
            var offs = admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get();
            var end = admin.listOffsets(Map.of(new TopicPartition(topic, 0), OffsetSpec.latest())).all().get();
            System.out.println("COMMITADO=" + offs.values().iterator().next().offset() + " FIM=" + end.values().iterator().next().offset());
        }
    }

    @Test
    void describeRetentionDefaults() throws Exception {
        String topic = newTopic(1);
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", BOOT))) {
            var cr = new ConfigResource(ConfigResource.Type.TOPIC, topic);
            var cfg = admin.describeConfigs(List.of(cr)).all().get().get(cr);
            for (String k : List.of("retention.ms", "retention.bytes", "cleanup.policy", "segment.bytes", "min.insync.replicas", "max.message.bytes"))
                System.out.println("CFG " + k + "=" + cfg.get(k).value() + " (" + cfg.get(k).source() + ")");
            assertThat(cfg.get("retention.ms").value()).isEqualTo("604800000");
        }
    }
}
