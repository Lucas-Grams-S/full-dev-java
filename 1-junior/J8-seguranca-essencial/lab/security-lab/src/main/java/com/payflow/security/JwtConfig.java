package com.payflow.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.payflow.config.PayflowProperties;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Chave HS256 (um segredo compartilhado entre quem assina e quem confere: aqui, a mesma aplicação). */
@Configuration
public class JwtConfig {

    @Bean
    SecretKey jwtKey(PayflowProperties properties) {
        byte[] secret = properties.security().jwt().secret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey, PayflowProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        // expiração (com tolerância de 60 s de relógio) + emissor: um token de outro sistema, mesmo assinado, é recusado
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(properties.security().jwt().issuer());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validator));
        return decoder;
    }
}
