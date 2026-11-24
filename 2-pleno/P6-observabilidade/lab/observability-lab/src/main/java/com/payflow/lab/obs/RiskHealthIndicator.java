package com.payflow.lab.obs;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/** Dependência OPCIONAL (o serviço de risco): não pode derrubar a prontidão. */
@Component("riskHealthIndicator")
class RiskHealthIndicator implements HealthIndicator {
    private final LabState state;

    RiskHealthIndicator(LabState state) {
        this.state = state;
    }

    @Override
    public Health health() {
        return state.riskUp.get() ? Health.up().build() : Health.down().build();
    }
}
