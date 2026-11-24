package com.payflow.lab.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AnnotationsOrderTest {

    static FakeService svc;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        svc = new FakeService();
        r.add("lab.notifications.url", svc::url);
    }

    @AfterAll
    static void stop() {
        svc.close();
    }

    @Autowired PreferencesGateway gateway;
    @Autowired CircuitBreakerRegistry cbRegistry;
    @Autowired RetryRegistry retryRegistry;

    @org.junit.jupiter.api.BeforeEach
    void resetBreaker() {
        cbRegistry.circuitBreaker("notif").reset(); // o breaker é um singleton do contexto: um teste vaza para o outro
        svc.calls.set(0);
    }

    @Test
    void annotationOrderRetryWrapsCircuitBreakerAndFallbackAnswers() {
        svc.status = 503;
        svc.calls.set(0);
        String answer = gateway.preferences("u1");
        var cb = cbRegistry.circuitBreaker("notif");
        System.out.println("T7 CORRETO (fallback no @Retry), 1 chamada logica com @Retry(3)+@CircuitBreaker: servidor recebeu " + svc.calls.get()
                + " chamadas; o breaker registrou " + cb.getMetrics().getNumberOfFailedCalls() + " falhas (estado " + cb.getState() + ")");
        System.out.println("T7 resposta do fallback: " + answer);
        assertThat(svc.calls.get()).isEqualTo(3);
        assertThat(cb.getMetrics().getNumberOfFailedCalls()).isEqualTo(3); // o Retry está POR FORA do CircuitBreaker
    }

    @Test
    void fallbackOnTheInnerAnnotationSilencesTheRetry() {
        svc.status = 503;
        String answer = gateway.preferencesFallbackOnTheInside("u1");
        var cb = cbRegistry.circuitBreaker("notif");
        System.out.println("T7 ARMADILHA (fallback no @CircuitBreaker): servidor recebeu " + svc.calls.get() + " chamada(s) apesar de @Retry(3); resposta: " + answer);
        assertThat(svc.calls.get()).isEqualTo(1);
    }

    @Test
    void openBreakerShortCircuitsAndFallbackStillAnswers() {
        svc.status = 503;
        for (int i = 0; i < 5; i++) gateway.preferences("u1");
        int before = svc.calls.get();
        String answer = gateway.preferences("u1");
        System.out.println("T7 breaker " + cbRegistry.circuitBreaker("notif").getState() + ": chamadas ao servidor nao cresceram ("
                + before + " -> " + svc.calls.get() + "); fallback: " + answer);
        assertThat(svc.calls.get()).isEqualTo(before);
        assertThat(answer).contains("CallNotPermittedException");
    }
}
