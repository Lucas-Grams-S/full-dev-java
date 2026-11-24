package com.payflow.lite.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Mesma ideia da J8: o segredo do JWT vem so do ambiente e a aplicacao nao sobe sem ele. */
@ConfigurationProperties("payflow")
public record PayflowProperties(@DefaultValue Security security) {

    public record Security(@DefaultValue Jwt jwt) {
        public record Jwt(String secret) {
            public Jwt {
                if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                    throw new IllegalArgumentException(
                        "payflow.security.jwt.secret must have at least 32 bytes (HS256 needs a 256-bit key)");
                }
            }
        }
    }
}
