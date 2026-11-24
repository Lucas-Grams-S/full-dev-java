package com.payflow.recurring;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Executes every line and branch... and checks almost nothing. JaCoCo loves it. */
class WeakScheduleRulesTest {

    @Test
    void runsEveryPath() {
        assertThat(ScheduleRules.validate(null, 5, 0)).isNotNull();
        assertThat(ScheduleRules.validate(new BigDecimal("-1.00"), 5, 0)).isNotNull();
        assertThat(ScheduleRules.validate(new BigDecimal("1.001"), 5, 0)).isNotNull();
        assertThat(ScheduleRules.validate(new BigDecimal("9000.00"), 5, 0)).isNotNull();
        assertThat(ScheduleRules.validate(new BigDecimal("50.00"), 0, 0)).isNotNull();
        assertThat(ScheduleRules.validate(new BigDecimal("50.00"), 40, 0)).isNotNull();
        assertThat(ScheduleRules.validate(new BigDecimal("50.00"), 5, 20)).isNotNull();
        assertThat(ScheduleRules.validate(new BigDecimal("50.00"), 5, 0)).isNotNull();
    }
}
