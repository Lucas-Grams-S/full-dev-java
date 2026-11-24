package com.payflow.support;

import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base dos testes SEM Keycloak: a configuração do Boot é a REAL (issuer, audiences, jws-algorithms, validadores);
 * só a origem das chaves muda: o JWKS vem de um servidor local, e o token, de {@link TestTokens}.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class OfflineResourceServerTest {

    protected static final JwksServer JWKS = new JwksServer();
    protected static final TestTokens TOKENS = new TestTokens("kid-2026-11");

    static {
        JWKS.publish(TOKENS.publicJwk());
    }

    @DynamicPropertySource
    static void jwks(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", JWKS::url);
    }

    @AfterAll
    static void restoreKeys() {
        JWKS.publish(TOKENS.publicJwk());
    }
}
