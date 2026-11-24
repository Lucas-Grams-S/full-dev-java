package com.payflow.lab.obs;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private final ObservationRegistry registry;
    private final RiskClient risk;
    private final OutboxPublisher outbox;
    private final LabState state;

    TransferService(ObservationRegistry registry, RiskClient risk, OutboxPublisher outbox, LabState state) {
        this.registry = registry;
        this.risk = risk;
        this.outbox = outbox;
        this.state = state;
    }

    public String transfer(TransferRequest r) {
        // 1 observação = 1 métrica (timer payflow.transfer, baixa cardinalidade) + 1 span (com atributos de alta cardinalidade)
        Observation obs = Observation.createNotStarted("payflow.transfer", registry)
                .highCardinalityKeyValue("payflow.source.account", String.valueOf(r.from()))   // SÓ no span, nunca como tag de métrica
                .start();
        try (Observation.Scope ignored = obs.openScope()) {
            if ("slow".equals(r.simulate())) {
                sleep(300);
            }
            int score = risk.score(r.from());
            if ("insufficient".equals(r.simulate())) {
                throw new Rejection("insufficient_funds");
            }
            if ("error".equals(r.simulate())) {
                throw new IllegalStateException("simulated failure talking to ledger");
            }
            String id = UUID.randomUUID().toString();
            state.outboxPending.incrementAndGet();
            outbox.publishAsync(id);
            log.info("transfer completed transferId={} riskScore={}", id, score);
            obs.lowCardinalityKeyValue("outcome", "completed");
            return id;
        } catch (Rejection e) {
            obs.lowCardinalityKeyValue("outcome", "rejected");
            log.warn("transfer rejected reason={}", e.getMessage());   // negócio esperado: WARN, sem stack trace
            throw e;
        } catch (RuntimeException e) {
            obs.lowCardinalityKeyValue("outcome", "failed");
            obs.error(e);
            throw e;                                                  // quem loga ERROR é a borda (uma vez)
        } finally {
            obs.stop();
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
