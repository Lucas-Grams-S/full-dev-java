package com.payflow.recurring;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.recurring.ScheduleRules.Result;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Same rule, tests on the BOUNDARIES with the exact expected result. */
class StrongScheduleRulesTest {

    @ParameterizedTest(name = "{0} on day {1} with {2} active -> {3}")
    @CsvSource(nullValues = "NULL", value = {
            "NULL,    5,  0, INVALID_AMOUNT",
            "0.00,    5,  0, INVALID_AMOUNT",
            "-0.01,   5,  0, INVALID_AMOUNT",
            "0.01,    5,  0, OK",
            "1.001,   5,  0, INVALID_AMOUNT",
            "5000.00, 5,  0, OK",
            "5000.01, 5,  0, ABOVE_LIMIT",
            "50.00,   0,  0, INVALID_DAY",
            "50.00,   1,  0, OK",
            "50.00,  31,  0, OK",
            "50.00,  32,  0, INVALID_DAY",
            "50.00,   5,  9, OK",
            "50.00,   5, 10, TOO_MANY"
    })
    void validates(BigDecimal amount, int day, int active, Result expected) {
        assertThat(ScheduleRules.validate(amount, day, active)).isEqualTo(expected);
    }
}
