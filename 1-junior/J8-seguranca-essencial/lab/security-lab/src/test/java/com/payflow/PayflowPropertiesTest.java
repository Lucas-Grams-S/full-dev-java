package com.payflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payflow.config.PayflowProperties.Security.Jwt;
import com.payflow.config.PayflowProperties.Security.Login;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PayflowPropertiesTest {

    @Test
    void aShortJwtSecretIsRefused() {
        assertThatThrownBy(() -> new Jwt("too-short", "payflow", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least 32 bytes");
    }

    @Test
    void aMissingJwtSecretIsRefused() {
        assertThatThrownBy(() -> new Jwt(null, "payflow", Duration.ofMinutes(15))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTokenLongerThanOneHourIsRefused() {
        assertThatThrownBy(() -> new Jwt("x".repeat(32), "payflow", Duration.ofHours(2))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validValuesAreAccepted() {
        assertThat(new Jwt("x".repeat(32), "payflow", Duration.ofMinutes(15)).ttl()).hasMinutes(15);
        assertThat(new Login(5, Duration.ofMinutes(15)).maxFailures()).isEqualTo(5);
    }
}
