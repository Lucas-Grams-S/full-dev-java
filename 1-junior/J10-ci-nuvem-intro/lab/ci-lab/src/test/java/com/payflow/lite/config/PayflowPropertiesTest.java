package com.payflow.lite.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PayflowPropertiesTest {

    @Test
    void rejectsAJwtSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new PayflowProperties.Security.Jwt("curto"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void rejectsAMissingJwtSecret() {
        assertThatThrownBy(() -> new PayflowProperties.Security.Jwt(null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
