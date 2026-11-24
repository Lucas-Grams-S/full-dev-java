package com.payflow.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Só o que a segurança usa (no PayFlow, estes records se somam a Statement, Exchange e Cors das J5/J6). */
@ConfigurationProperties(prefix = "payflow")
public record PayflowProperties(@DefaultValue Cors cors, @DefaultValue Security security) {

    public record Cors(@DefaultValue List<String> allowedOrigins) { }

    /** Segurança (J8): JWT da API e proteção contra tentativas de senha. */
    public record Security(@DefaultValue Jwt jwt, @DefaultValue Login login) {

        /** O segredo NÃO tem valor padrão: sem ele a aplicação não sobe (decisão 31). */
        public record Jwt(String secret, @DefaultValue("payflow") String issuer, @DefaultValue("PT15M") Duration ttl) {
            public Jwt {
                if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
                    throw new IllegalArgumentException("payflow.security.jwt.secret must have at least 32 bytes (HS256 needs a 256-bit key)");
                }
                if (issuer == null || issuer.isBlank()) {
                    throw new IllegalArgumentException("payflow.security.jwt.issuer must not be blank");
                }
                if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofHours(1)) > 0) {
                    throw new IllegalArgumentException("payflow.security.jwt.ttl must be between 1 second and 1 hour");
                }
            }
        }

        /** Tentativas de login: depois de maxFailures erros seguidos, o e-mail fica bloqueado por lockDuration. */
        public record Login(@DefaultValue("5") int maxFailures, @DefaultValue("PT15M") Duration lockDuration) {
            public Login {
                if (maxFailures < 1) {
                    throw new IllegalArgumentException("payflow.security.login.max-failures must be >= 1");
                }
                if (lockDuration.isNegative() || lockDuration.isZero()) {
                    throw new IllegalArgumentException("payflow.security.login.lock-duration must be positive");
                }
            }
        }
    }
}
