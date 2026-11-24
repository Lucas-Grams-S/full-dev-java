package com.payflow.lab.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class TimeoutsTest {

    @Test
    void defaultClientHasNoReadTimeoutAndConfiguredOneFailsFast() throws Exception {
        try (FakeService svc = new FakeService()) {
            svc.delayMs = 2_000;

            // 1) SEM timeout de leitura (padrão): espera o servidor inteiro
            NotificationClient naive = new NotificationClient(svc.url(), null, null);
            long t0 = System.nanoTime();
            naive.preferences("u1");
            long naiveMs = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("T1 sem timeout de leitura: respondeu apos " + naiveMs + " ms (servidor demora 2000 ms)");
            assertThat(naiveMs).isGreaterThanOrEqualTo(2_000);

            // 2) COM timeout de leitura de 300 ms
            NotificationClient safe = new NotificationClient(svc.url(), Duration.ofMillis(200), Duration.ofMillis(300));
            long t1 = System.nanoTime();
            assertThatThrownBy(() -> safe.preferences("u1"))
                    .isInstanceOf(ResourceAccessException.class)
                    .satisfies(e -> System.out.println("T1 com timeout 300 ms: " + e.getClass().getSimpleName() + " <- "
                            + e.getCause().getClass().getName() + ": " + e.getCause().getMessage()));
            long safeMs = (System.nanoTime() - t1) / 1_000_000;
            System.out.println("T1 com timeout 300 ms: falhou apos " + safeMs + " ms");
            assertThat(safeMs).isBetween(250L, 900L);
        }
    }

    @Test
    void connectTimeoutAgainstUnroutableAddress() {
        // 10.255.255.1 não responde (nenhum SYN-ACK): serve para provar o connect timeout
        NotificationClient c = new NotificationClient("http://10.255.255.1:81", Duration.ofMillis(300), Duration.ofSeconds(5));
        long t0 = System.nanoTime();
        try {
            c.preferences("u1");
        } catch (Exception e) {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("T1 connect timeout 300 ms: " + e.getClass().getSimpleName() + " <- "
                    + e.getCause().getClass().getName() + " apos " + ms + " ms");
        }
    }
}
