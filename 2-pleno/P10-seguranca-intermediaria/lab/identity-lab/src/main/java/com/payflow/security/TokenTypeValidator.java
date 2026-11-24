package com.payflow.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Só aceita token de ACESSO. No Keycloak a claim "typ" é "Bearer" no access token, "ID" no ID token e
 * "Refresh" no refresh token: os três são JWTs assinados pela mesma chave.
 */
public class TokenTypeValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error WRONG_TYPE = new OAuth2Error("invalid_token", "The token is not an access token", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        return "Bearer".equals(jwt.getClaimAsString("typ"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(WRONG_TYPE);
    }
}
