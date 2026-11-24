package com.payflow.lab;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/** P3.08: retentativas com backoff exponencial e envio para o tópico .DLT. */
@Configuration
class KafkaErrorConfig {
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        // mesma partição do original, tópico "<origem>.DLT"
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));
        var backoff = new ExponentialBackOff(200, 2.0);
        backoff.setMaxElapsedTime(1_500);  // 200 + 400 + 800 ms ≈ 3 retentativas
        var handler = new DefaultErrorHandler(recoverer, backoff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class); // erro de dado: não adianta repetir
        return handler;
    }
}
