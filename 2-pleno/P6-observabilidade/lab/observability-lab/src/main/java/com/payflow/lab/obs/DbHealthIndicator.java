package com.payflow.lab.obs;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/** Bean "dbHealthIndicator" => componente "db" (como o do DataSource). */
@Component("dbHealthIndicator")
class DbHealthIndicator implements HealthIndicator {
    private final LabState state;

    DbHealthIndicator(LabState state) {
        this.state = state;
    }

    @Override
    public Health health() {
        return state.dbUp.get() ? Health.up().build() : Health.down().withDetail("reason", "connection refused").build();
    }
}
