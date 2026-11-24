package com.payflow.lab.obs;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Faz o papel do relé do Outbox (P3.09): outro thread; o contexto (traceId, requestId) precisa atravessá-lo. */
@Component
class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final LabState state;
    private final Counter published;

    OutboxPublisher(LabState state, MeterRegistry registry) {
        this.state = state;
        this.published = Counter.builder("payflow.outbox.published").description("Eventos do outbox publicados").register(registry);
        Gauge.builder("payflow.outbox.pending", state.outboxPending, java.util.concurrent.atomic.AtomicInteger::get)
                .description("Eventos do outbox ainda não publicados").register(registry);
    }

    @Async
    public void publishAsync(String transferId) {
        if (state.outboxStalled.get()) {
            log.warn("outbox relay stalled, event stays pending transferId={}", transferId);
            return;
        }
        state.outboxPending.decrementAndGet();
        published.increment();
        log.info("outbox event published transferId={}", transferId);
    }
}
