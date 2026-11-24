package com.payflow.security;

import com.payflow.config.PayflowProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite o access token da API: curto (padrão 15 min), com o mínimo de claims e nenhum dado sensível. */
@Service
public class TokenService {

    public record IssuedToken(String value, Instant expiresAt) { }

    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder, Clock clock, PayflowProperties properties) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = properties.security().jwt().issuer();
        this.ttl = properties.security().jwt().ttl();
    }

    public Duration ttl() {
        return ttl;
    }

    public IssuedToken issue(PayflowUserDetails user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.currentUser().id().toString())          // sub = id do usuário (não o e-mail)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())                      // jti: identifica este token (auditoria, revogação futura)
                .claim("email", user.currentUser().email())
                .claim("name", user.currentUser().fullName())
                .claim("roles", List.of(user.role().name()))
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }
}
