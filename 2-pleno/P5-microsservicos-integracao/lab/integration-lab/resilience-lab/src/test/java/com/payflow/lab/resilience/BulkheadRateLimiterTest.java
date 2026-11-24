package com.payflow.lab.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BulkheadRateLimiterTest {

    @Test
    void semaphoreBulkheadLimitsConcurrencyAndRejectsTheRest() throws Exception {
        try (FakeService svc = new FakeService()) {
            svc.delayMs = 300;
            NotificationClient client = new NotificationClient(svc.url(), Duration.ofMillis(200), Duration.ofSeconds(2));
            Bulkhead bh = Bulkhead.of("notif", BulkheadConfig.custom().maxConcurrentCalls(5).maxWaitDuration(Duration.ZERO).build());
            AtomicInteger ok = new AtomicInteger(), rejected = new AtomicInteger();
            CountDownLatch go = new CountDownLatch(1);
            List<Thread> ts = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                ts.add(Thread.ofVirtual().start(() -> {
                    try {
                        go.await();
                        Bulkhead.decorateSupplier(bh, () -> client.preferences("u")).get();
                        ok.incrementAndGet();
                    } catch (BulkheadFullException e) {
                        rejected.incrementAndGet();
                    } catch (InterruptedException ignored) {
                    }
                }));
            }
            go.countDown();
            for (Thread t : ts) t.join();
            System.out.println("T4 bulkhead 5: 20 chamadas simultaneas -> " + ok + " executadas, " + rejected + " rejeitadas; maximo simultaneo no servidor=" + svc.maxInFlight.get());
            assertThat(svc.maxInFlight.get()).isLessThanOrEqualTo(5);
            assertThat(ok.get() + rejected.get()).isEqualTo(20);
        }
    }

    @Test
    void rateLimiterAdmitsLimitPerPeriod() {
        RateLimiter rl = RateLimiter.of("rl", RateLimiterConfig.custom()
                .limitForPeriod(10).limitRefreshPeriod(Duration.ofSeconds(1)).timeoutDuration(Duration.ZERO).build());
        int ok = 0, denied = 0;
        for (int i = 0; i < 25; i++) {
            try {
                RateLimiter.decorateRunnable(rl, () -> { }).run();
                ok++;
            } catch (RequestNotPermitted e) {
                denied++;
            }
        }
        System.out.println("T4 rate limiter 10/s: 25 chamadas instantaneas -> " + ok + " permitidas, " + denied + " negadas (RequestNotPermitted)");
        assertThat(ok).isEqualTo(10);
    }

    @Test
    void timeLimiterGivesUpButDoesNotStopTheWork() throws Exception {
        try (FakeService svc = new FakeService()) {
            svc.delayMs = 800;
            NotificationClient client = new NotificationClient(svc.url(), Duration.ofMillis(200), null); // sem timeout de leitura!
            TimeLimiter tl = TimeLimiter.of(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(200)).cancelRunningFuture(true).build());
            var pool = Executors.newVirtualThreadPerTaskExecutor();
            long t0 = System.nanoTime();
            try {
                tl.executeCompletionStage(java.util.concurrent.Executors.newSingleThreadScheduledExecutor(),
                        () -> CompletableFuture.supplyAsync(() -> client.preferences("u"), pool)).toCompletableFuture().get();
            } catch (Exception e) {
                long ms = (System.nanoTime() - t0) / 1_000_000;
                Throwable c = e.getCause();
                System.out.println("T4 TimeLimiter 200 ms: chamador liberado apos " + ms + " ms com " + (c == null ? e : c.getClass().getSimpleName())
                        + "; mas o servidor ainda tem " + svc.inFlight.get() + " requisicao em andamento (o trabalho NAO foi interrompido)");
                assertThat(c).isInstanceOf(TimeoutException.class);
                assertThat(svc.inFlight.get()).isEqualTo(1);
            }
            Thread.sleep(900);
        }
    }
}
