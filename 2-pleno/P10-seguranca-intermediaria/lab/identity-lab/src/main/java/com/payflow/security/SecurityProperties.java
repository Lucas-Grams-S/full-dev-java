package com.payflow.security;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuração própria do PayFlow sobre o token (o resto vem de spring.security.oauth2.resourceserver.jwt.*). */
@ConfigurationProperties("payflow.security")
public record SecurityProperties(@DefaultValue Internal internal) {

    /** Quem pode chamar /internal/**: o escopo exigido e os clientes (claim azp) autorizados. */
    public record Internal(@DefaultValue("internal:notify") String requiredScope,
                           @DefaultValue("payflow-notifications") Set<String> allowedClients) { }
}
