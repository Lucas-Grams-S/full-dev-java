package com.payflow.lab.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class RetryTest {

    @Test
    void retriesTransientFailureWithExponentialBackoff() throws Exception {
        try (FakeService svc = new FakeService()) {
            svc.failFirst = 2; // 503, 503, 200
            NotificationClient client = new NotificationClient(svc.url(), Duration.ofMillis(200), Duration.ofMillis(500));
            List<Long> times = new ArrayList<>();
            long t0 = System.nanoTime();
            RetryConfig cfg = RetryConfig.custom()
                    .maxAttempts(3)
                    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(100), 2.0, 0.5))
                    .retryExceptions(HttpServerErrorException.class, ResourceAccessException.class)
                    .build();
            Retry retry = Retry.of("notif", cfg);
            retry.getEventPublisher().onRetry(e -> times.add((System.nanoTime() - t0) / 1_000_000));
            String body = Retry.decorateSupplier(retry, () -> client.preferences("u1")).get();
            System.out.println("T2 retry: chamadas no servidor=" + svc.calls.get() + ", retentativas em t(ms)=" + times + ", resposta=" + body);
            assertThat(svc.calls.get()).isEqualTo(3);
        }
    }

    @Test
    void retryDoesNotHelpWhenFailureIsPermanentAndMultipliesLoad() throws Exception {
        try (FakeService svc = new FakeService()) {
            svc.status = 503;
            NotificationClient client = new NotificationClient(svc.url(), Duration.ofMillis(200), Duration.ofMillis(500));
            RetryConfig cfg = RetryConfig.custom().maxAttempts(3).waitDuration(Duration.ofMillis(10))
                    .retryExceptions(HttpServerErrorException.class).build();
            // 3 camadas empilhadas, cada uma com 3 tentativas: o que o servidor sofre?
            Retry l1 = Retry.of("l1", cfg), l2 = Retry.of("l2", cfg), l3 = Retry.of("l3", cfg);
            try {
                Retry.decorateSupplier(l1, Retry.decorateSupplier(l2, Retry.decorateSupplier(l3, () -> client.preferences("u1")))).get();
            } catch (HttpServerErrorException e) {
                // esperado
            }
            System.out.println("T2 amplificacao: 1 requisicao do usuario -> " + svc.calls.get() + " chamadas ao servico (3 camadas x 3 tentativas)");
            assertThat(svc.calls.get()).isEqualTo(27);
        }
    }

    @Test
    void clientErrorsAreNotRetriedByDefaultOnlyWhenConfigured() throws Exception {
        try (FakeService svc = new FakeService()) {
            svc.status = 404;
            NotificationClient client = new NotificationClient(svc.url(), Duration.ofMillis(200), Duration.ofMillis(500));
            RetryConfig cfg = RetryConfig.custom().maxAttempts(3).waitDuration(Duration.ofMillis(10))
                    .retryExceptions(HttpServerErrorException.class).build(); // só 5xx
            try {
                Retry.decorateSupplier(Retry.of("c", cfg), () -> client.preferences("u1")).get();
            } catch (Exception e) {
                System.out.println("T2 4xx: " + e.getClass().getSimpleName() + ", chamadas=" + svc.calls.get());
            }
            assertThat(svc.calls.get()).isEqualTo(1);
        }
    }

    @Test
    void postWithoutIdempotencyIsDuplicatedByRetryWhenResponseIsLost() throws Exception {
        try (FakeService svc = new FakeService()) {
            svc.delayMs = 400; // o servidor PROCESSA, mas responde depois do timeout do cliente
            NotificationClient client = new NotificationClient(svc.url(), Duration.ofMillis(200), Duration.ofMillis(100));
            RetryConfig cfg = RetryConfig.custom().maxAttempts(3).waitDuration(Duration.ofMillis(10))
                    .retryExceptions(ResourceAccessException.class).build();
            try {
                Retry.decorateSupplier(Retry.of("post", cfg), () -> client.send("u1", "voce recebeu R$ 10,00")).get();
            } catch (ResourceAccessException e) {
                // esgotou
            }
            Thread.sleep(700); // deixa o servidor terminar
            System.out.println("T2 POST repetido: o cliente desistiu 3x, mas o servidor recebeu " + svc.calls.get() + " mensagens (sem chave de idempotencia = 3 e-mails)");
            assertThat(svc.calls.get()).isEqualTo(3);
        }
    }
}
