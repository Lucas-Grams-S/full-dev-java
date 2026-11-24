package com.payflow.web;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Quem chama, extraído do token: o "sub" é o id do usuário no Keycloak (e no PayFlow). Nunca vem do corpo da requisição. */
public record CurrentUser(UUID id, String email) {
    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("email"));
    }
}
