package com.payflow.lab.resilience;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Chamada protegida ao serviço de notificações, com as quatro anotações do Resilience4j. */
@Component
public class PreferencesGateway {

    private final NotificationClient client;

    public PreferencesGateway(@Qualifier("safeClient") NotificationClient client) {
        this.client = client;
    }

    /** Correto: o fallback fica na anotação MAIS EXTERNA (@Retry), depois que todas as tentativas falharam. */
    @Retry(name = "notif", fallbackMethod = "fallback")
    @CircuitBreaker(name = "notif")
    @RateLimiter(name = "notif")
    @Bulkhead(name = "notif")
    public String preferences(String userId) {
        return client.preferences(userId);
    }

    /** ARMADILHA: o fallback na anotação INTERNA (@CircuitBreaker) transforma a falha em sucesso e o @Retry nunca age. */
    @Retry(name = "notif")
    @CircuitBreaker(name = "notif", fallbackMethod = "fallback")
    @RateLimiter(name = "notif")
    @Bulkhead(name = "notif")
    public String preferencesFallbackOnTheInside(String userId) {
        return client.preferences(userId);
    }

    /** PÚBLICO de propósito: com visibilidade de pacote o Resilience4j falhou em chamadas concorrentes (IllegalAccessException). */
    public String fallback(String userId, Throwable t) {
        return "{\"channel\":\"EMAIL\",\"source\":\"fallback\",\"cause\":\"" + t.getClass().getSimpleName() + "\"}";
    }
}
