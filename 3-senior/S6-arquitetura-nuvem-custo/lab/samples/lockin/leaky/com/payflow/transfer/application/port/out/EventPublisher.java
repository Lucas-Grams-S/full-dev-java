package com.payflow.transfer.application.port.out;

/** Porta: o domínio publica eventos sem saber que existe SNS, Kafka ou Pub/Sub. */
public interface EventPublisher {
    void publish(String eventType, String key, String jsonPayload);
}
