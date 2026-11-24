package com.payflow.lab.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

class CircuitBreakerTest {

    @Test
    void closedOpenHalfOpenClosed() throws Exception {
        try (FakeService svc = new FakeService()) {
            NotificationClient client = new NotificationClient(svc.url(), Duration.ofMillis(200), Duration.ofMillis(500));
            CircuitBreakerConfig cfg = CircuitBreakerConfig.custom()
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                    .slidingWindowSize(10)
                    .minimumNumberOfCalls(5)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofMillis(300))
                    .permittedNumberOfCallsInHalfOpenState(2)
                    .automaticTransitionFromOpenToHalfOpenEnabled(false)
                    .recordExceptions(HttpServerErrorException.class, org.springframework.web.client.ResourceAccessException.class)
                    .ignoreExceptions(HttpClientErrorException.class)
                    .build();
            CircuitBreaker cb = CircuitBreaker.of("notif", cfg);
            List<String> transitions = new ArrayList<>();
            cb.getEventPublisher().onStateTransition(e -> transitions.add(e.getStateTransition().getFromState() + "->" + e.getStateTransition().getToState()));

            svc.status = 503;
            int calls = 0;
            for (int i = 0; i < 5; i++) { // 5 falhas: abre
                try { cb.executeSupplier(() -> client.preferences("u")); } catch (Exception ignored) { }
                calls++;
            }
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            int before = svc.calls.get();
            long t0 = System.nanoTime();
            assertThatThrownBy(() -> cb.executeSupplier(() -> client.preferences("u"))).isInstanceOf(CallNotPermittedException.class);
            long us = (System.nanoTime() - t0) / 1_000;
            System.out.println("T3 aberto: chamada recusada em " + us + " us, sem tocar o servidor (chamadas antes=" + before + ", depois=" + svc.calls.get() + ")");
            assertThat(svc.calls.get()).isEqualTo(before);

            Thread.sleep(350);
            svc.status = 200; // o serviço se recuperou
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN); // sem chamada, continua OPEN (transição é preguiçosa)
            cb.executeSupplier(() -> client.preferences("u")); // 1ª sonda: vira HALF_OPEN
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
            cb.executeSupplier(() -> client.preferences("u")); // 2ª sonda: fecha
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            System.out.println("T3 transicoes: " + transitions + " | metricas: " + cb.getMetrics().getNumberOfSuccessfulCalls() + " ok, "
                    + cb.getMetrics().getNumberOfFailedCalls() + " falhas, " + cb.getMetrics().getNumberOfNotPermittedCalls() + " recusadas");
        }
    }

    @Test
    void clientErrorsDoNotOpenTheBreaker() throws Exception {
        try (FakeService svc = new FakeService()) {
            svc.status = 404;
            NotificationClient client = new NotificationClient(svc.url(), Duration.ofMillis(200), Duration.ofMillis(500));
            CircuitBreakerConfig cfg = CircuitBreakerConfig.custom().slidingWindowSize(10).minimumNumberOfCalls(5)
                    .recordExceptions(HttpServerErrorException.class).ignoreExceptions(HttpClientErrorException.class).build();
            CircuitBreaker cb = CircuitBreaker.of("n", cfg);
            for (int i = 0; i < 20; i++) {
                try { cb.executeSupplier(() -> client.preferences("u")); } catch (Exception ignored) { }
            }
            System.out.println("T3 404 x20 com ignoreExceptions: estado=" + cb.getState() + " (4xx = erro do chamador, nao da saude do servico)");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

            CircuitBreaker cb2 = CircuitBreaker.of("n2", CircuitBreakerConfig.custom().slidingWindowSize(10).minimumNumberOfCalls(5).build());
            for (int i = 0; i < 20; i++) {
                try { cb2.executeSupplier(() -> client.preferences("u")); } catch (Exception ignored) { }
            }
            System.out.println("T3 404 x20 com a configuracao PADRAO (toda excecao conta): estado=" + cb2.getState());
        }
    }

    @Test
    void slowCallsOpenTheBreakerEvenWithHttp200() throws Exception {
        try (FakeService svc = new FakeService()) {
            svc.delayMs = 150;
            NotificationClient client = new NotificationClient(svc.url(), Duration.ofMillis(200), Duration.ofSeconds(2));
            CircuitBreaker cb = CircuitBreaker.of("slow", CircuitBreakerConfig.custom()
                    .slidingWindowSize(10).minimumNumberOfCalls(5)
                    .slowCallDurationThreshold(Duration.ofMillis(100)).slowCallRateThreshold(50).build());
            for (int i = 0; i < 5; i++) {
                cb.executeSupplier(() -> client.preferences("u")); // todas 200 OK, porém lentas
            }
            System.out.println("T3 5 respostas 200 OK de 150 ms (limite de lentidao 100 ms): estado=" + cb.getState());
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }
    }
}
