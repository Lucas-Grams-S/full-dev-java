package com.payflow.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Fábrica de tokens assinados com uma chave RSA de teste: do jeito do Keycloak 26.8 (claims reais, medidas na aula). */
public final class TestTokens {

    public static final String ISSUER = "http://127.0.0.1:8180/realms/payflow";

    private final RSAKey key;

    public TestTokens(String kid) {
        try {
            this.key = new RSAKeyGenerator(2048).keyID(kid).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public RSAKey publicJwk() {
        return key.toPublicJWK();
    }

    /** Um access token de usuário, válido e com o que o Keycloak coloca; cada teste muda só o que quer provar. */
    public Builder user(UUID sub, String scope, String... roles) {
        return new Builder()
                .claim("iss", ISSUER).claim("aud", List.of("payflow-api")).claim("sub", sub.toString())
                .claim("typ", "Bearer").claim("azp", "payflow-web").claim("scope", scope)
                .claim("email", "user@example.com")
                .claim("realm_access", Map.of("roles", List.of(roles)));
    }

    /** Um token de máquina (client_credentials): sem e-mail e sem papéis, com o escopo do serviço. */
    public Builder service(String azp, String scope) {
        return new Builder()
                .claim("iss", ISSUER).claim("aud", List.of("payflow-api")).claim("sub", UUID.randomUUID().toString())
                .claim("typ", "Bearer").claim("azp", azp).claim("scope", scope);
    }

    public final class Builder {
        private final JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
        private Instant issuedAt = Instant.now();
        private Duration lifetime = Duration.ofMinutes(5);

        public Builder claim(String name, Object value) {
            if (value == null) {
                claims.claim(name, null);
            } else {
                claims.claim(name, value);
            }
            return this;
        }

        public Builder expiredSecondsAgo(long seconds) {
            this.issuedAt = Instant.now().minusSeconds(seconds + 300);
            this.lifetime = Duration.ofMinutes(5);
            return this;
        }

        public Builder notBeforeInSeconds(long seconds) {
            claims.notBeforeTime(Date.from(Instant.now().plusSeconds(seconds)));
            return this;
        }

        private JWTClaimsSet build() {
            return claims.jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(issuedAt)).expirationTime(Date.from(issuedAt.plus(lifetime))).build();
        }

        /** Assinado com a chave de teste (RS256), o caso normal. */
        public String signed() {
            return signWith(key, JWSAlgorithm.RS256);
        }

        public String signedByOtherKey(String kid) {
            return signWith(new TestTokens(kid).key, JWSAlgorithm.RS256);
        }

        /** Troca o corpo depois de assinar: a assinatura deixa de bater. */
        public String signedThenTampered(String claim, Object newValue) {
            try {
                SignedJWT jwt = SignedJWT.parse(signed());
                JWTClaimsSet changed = new JWTClaimsSet.Builder(jwt.getJWTClaimsSet()).claim(claim, newValue).build();
                String payload = com.nimbusds.jose.util.Base64URL.encode(changed.toString()).toString();
                String[] parts = jwt.serialize().split("\\.");
                return parts[0] + "." + payload + "." + parts[2];
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /** alg = none: "sem assinatura", o ataque clássico. */
        public String unsigned() {
            return new PlainJWT(build()).serialize();
        }

        /** Confusão de algoritmo: HS256 usando a chave PÚBLICA (que todos conhecem) como se fosse o segredo. */
        public String hs256WithPublicKeyAsSecret() {
            try {
                byte[] secret = key.toPublicKey().getEncoded();
                SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(key.getKeyID()).build(), build());
                jwt.sign(new MACSigner(padded(secret)));
                return jwt.serialize();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        private byte[] padded(byte[] secret) {
            return secret.length >= 32 ? secret : java.util.Arrays.copyOf(secret, 32);
        }

        private String signWith(RSAKey signingKey, JWSAlgorithm algorithm) {
            try {
                SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(signingKey.getKeyID()).type(com.nimbusds.jose.JOSEObjectType.JWT).build(), build());
                jwt.sign(new RSASSASigner(signingKey));
                return jwt.serialize();
            } catch (JOSEException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
