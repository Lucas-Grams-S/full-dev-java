package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** P3.03: filas, exchanges, ack/nack, TTL e dead-letter com Spring AMQP. */
@SpringBootTest
class RabbitBasicsIT {
    @Autowired RabbitTemplate rabbit;
    @Autowired RabbitAdmin admin;

    String id() { return UUID.randomUUID().toString().substring(0, 8); }

    @Test
    void directTopicAndFanout() {
        String s = id();
        var topic = new TopicExchange("lab.topic." + s, true, false);
        var fan = new FanoutExchange("lab.fan." + s, true, false);
        var qAll = new Queue("lab.q.all." + s, true, false, false);
        var qBr = new Queue("lab.q.br." + s, true, false, false);
        var qAudit = new Queue("lab.q.audit." + s, true, false, false);
        admin.declareExchange(topic); admin.declareExchange(fan);
        admin.declareQueue(qAll); admin.declareQueue(qBr); admin.declareQueue(qAudit);
        admin.declareBinding(BindingBuilder.bind(qAll).to(topic).with("transfer.#"));
        admin.declareBinding(BindingBuilder.bind(qBr).to(topic).with("transfer.*.brl"));
        admin.declareBinding(BindingBuilder.bind(qAudit).to(fan));
        rabbit.convertAndSend(topic.getName(), "transfer.completed.brl", "t1");
        rabbit.convertAndSend(topic.getName(), "transfer.failed.usd", "t2");
        rabbit.convertAndSend(topic.getName(), "deposit.completed.brl", "d1"); // ninguém recebe
        rabbit.convertAndSend(fan.getName(), "ignorada", "a1");
        var all = drain(qAll.getName()); var br = drain(qBr.getName()); var audit = drain(qAudit.getName());
        System.out.println("TOPIC transfer.# -> " + all + " | transfer.*.brl -> " + br + " | fanout -> " + audit);
        assertThat(all).containsExactlyInAnyOrder("t1", "t2");
        assertThat(br).containsExactly("t1");
        assertThat(audit).containsExactly("a1");
        // o deposit.completed.brl foi descartado silenciosamente (sem fila ligada): sem erro, sem rastro
    }

    List<String> drain(String q) {
        List<String> out = new ArrayList<>();
        Object o;
        while ((o = rabbit.receiveAndConvert(q, 500)) != null) out.add((String) o);
        return out;
    }

    @Test
    void competingConsumersShareTheQueue() throws Exception {
        String q = "lab.work." + id();
        admin.declareQueue(new Queue(q, true, false, false));
        for (int i = 0; i < 20; i++) rabbit.convertAndSend("", q, "job-" + i);
        Map<String, Integer> byWorker = new ConcurrentHashMap<>();
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch done = new CountDownLatch(20);
        for (String w : List.of("A", "B")) pool.submit(() -> {
            // cada trabalhador usa a própria conexão/canal
            rabbit.execute(ch -> {
                while (true) {
                    var r = ch.basicGet(q, true);
                    if (r == null) { if (done.getCount() == 0) return null; Thread.sleep(20); continue; }
                    byWorker.merge(w, 1, Integer::sum); done.countDown();
                    Thread.sleep(10);
                }
            });
            return null;
        });
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        System.out.println("COMPETING " + new TreeMap<>(byWorker));
        assertThat(byWorker.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(20);
    }

    @Test
    void nackWithoutRequeueSendsToDeadLetterQueue() {
        String s = id();
        var dlx = new DirectExchange("lab.dlx." + s, true, false);
        var dlq = new Queue("lab.dlq." + s, true, false, false);
        var work = QueueBuilder.durable("lab.work." + s)
                .deadLetterExchange(dlx.getName()).deadLetterRoutingKey("dead")
                .ttl(60_000).build();
        admin.declareExchange(dlx); admin.declareQueue(dlq); admin.declareQueue(work);
        admin.declareBinding(BindingBuilder.bind(dlq).to(dlx).with("dead"));
        rabbit.convertAndSend("", work.getName(), "poison");
        rabbit.execute(ch -> {
            var r = ch.basicGet(work.getName(), false);
            ch.basicNack(r.getEnvelope().getDeliveryTag(), false, false); // requeue=false
            return null;
        });
        var dead = rabbit.receive(dlq.getName(), 2000);
        System.out.println("DLQ headers x-death=" + dead.getMessageProperties().getXDeathHeader());
        assertThat(new String(dead.getBody())).isEqualTo("poison");
        assertThat(dead.getMessageProperties().getXDeathHeader()).isNotEmpty();
    }

    @Test
    void unackedMessageIsRedeliveredWhenConsumerDies() throws Exception {
        String q = "lab.redeliver." + id();
        admin.declareQueue(new Queue(q, true, false, false));
        rabbit.convertAndSend("", q, "pay-1");
        // consumidor 1 pega sem confirmar e "morre" (fecha o canal)
        rabbit.execute(ch -> { ch.basicGet(q, false); return null; });
        // o canal foi devolvido ao pool; forçamos fechar a conexão do template
        rabbit.getConnectionFactory().resetConnection();
        var r = rabbit.receive(q, 3000);
        assertThat(r).isNotNull();
        System.out.println("REDELIVERED=" + r.getMessageProperties().isRedelivered());
        assertThat(r.getMessageProperties().isRedelivered()).isTrue();
    }

    @Test
    void ttlExpiresMessages() throws Exception {
        String q = "lab.ttl." + id();
        admin.declareQueue(QueueBuilder.durable(q).ttl(300).build());
        rabbit.convertAndSend("", q, "ephemeral");
        Thread.sleep(700);
        assertThat(rabbit.receive(q, 300)).isNull();
    }
}
