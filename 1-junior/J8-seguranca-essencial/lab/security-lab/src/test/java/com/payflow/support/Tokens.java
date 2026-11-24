package com.payflow.support;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Fabrica tokens "ruins" de propósito: expirado, de outro emissor, de outra chave, sem assinatura. */
public final class Tokens {

    private Tokens() { }

    public static String signed(JwtEncoder encoder, String issuer, String subject, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(subject)
                .issuedAt(expiresAt.minusSeconds(900)).expiresAt(expiresAt)
                .claim("email", "ana@payflow.dev").claim("name", "Ana").claim("roles", List.of("ADMIN")).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public static JwtEncoder encoderWithKey(String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(secret.getBytes(), "HmacSHA256")));
    }

    /** Cabeçalho {"alg":"none"}, claims com papel ADMIN e NENHUMA assinatura. */
    public static String unsigned(String subject) {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = b64.encodeToString(("{\"sub\":\"" + subject + "\",\"iss\":\"payflow\",\"roles\":[\"ADMIN\"],\"exp\":"
                + Instant.now().plusSeconds(600).getEpochSecond() + "}").getBytes());
        return header + "." + payload + ".";
    }
}
