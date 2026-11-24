package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** P3.12: Reactor (não bloqueante) x threads virtuais (bloqueante barato) x pool de plataforma, em I/O-bound e CPU-bound. */
class ReactiveVsVirtualIT {
    static final int CALLS = 1000;

    static long ms(Runnable r) { long t = System.nanoTime(); r.run(); return (System.nanoTime() - t) / 1_000_000; }

    static void blockingCall() { try { Thread.sleep(100); } catch (InterruptedException e) { throw new RuntimeException(e); } }
    static Mono<Integer> reactiveCall(int i) { return Mono.delay(Duration.ofMillis(100)).thenReturn(i); }

    @Test
    void ioBoundThousandCalls() {
        long platform = ms(() -> {
            try (var pool = Executors.newFixedThreadPool(50)) {
                IntStream.range(0, CALLS).forEach(i -> pool.submit(ReactiveVsVirtualIT::blockingCall));
            }
        });
        long virtual = ms(() -> {
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                IntStream.range(0, CALLS).forEach(i -> pool.submit(ReactiveVsVirtualIT::blockingCall));
            }
        });
        long reactor = ms(() -> Flux.range(0, CALLS).flatMap(ReactiveVsVirtualIT::reactiveCall, CALLS).blockLast());
        // ERRO CLÁSSICO: chamada bloqueante dentro do pipeline reativo, sem trocar de scheduler
        long blockedInReactor = ms(() -> Flux.range(0, 100).flatMap(i -> Mono.fromCallable(() -> { blockingCall(); return i; }), 100)
                .blockLast());
        long boundedElastic = ms(() -> Flux.range(0, 100).flatMap(i -> Mono.fromCallable(() -> { blockingCall(); return i; })
                .subscribeOn(Schedulers.boundedElastic()), 100).blockLast());
        System.out.println("IO 1000 chamadas de 100 ms: pool_plataforma_50=" + platform + " ms | threads_virtuais=" + virtual + " ms | reactor_flatMap=" + reactor + " ms");
        System.out.println("IO 100 chamadas BLOQUEANTES dentro do Reactor: sem_subscribeOn=" + blockedInReactor + " ms (≈100×100) | boundedElastic=" + boundedElastic + " ms");
        assertThat(virtual).isLessThan(platform);
        assertThat(reactor).isLessThan(platform);
        assertThat(blockedInReactor).isGreaterThan(5_000);
    }

    static long cpu(int seed) {
        long h = seed;
        for (int i = 0; i < 40_000_000; i++) h = h * 31 + i;
        return h;
    }

    @Test
    void cpuBoundGainsNothingFromEitherModel() {
        int n = Runtime.getRuntime().availableProcessors() * 2;
        cpu(1);
        long fixed = ms(() -> { try (var p = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) { IntStream.range(0, n).forEach(i -> p.submit(() -> cpu(i))); } });
        long virtual = ms(() -> { try (var p = Executors.newVirtualThreadPerTaskExecutor()) { IntStream.range(0, n).forEach(i -> p.submit(() -> cpu(i))); } });
        long reactor = ms(() -> Flux.range(0, n).flatMap(i -> Mono.fromCallable(() -> cpu(i)).subscribeOn(Schedulers.parallel())).blockLast());
        System.out.println("CPU " + n + " tarefas: pool_de_N_nucleos=" + fixed + " ms | threads_virtuais=" + virtual + " ms | reactor_parallel=" + reactor + " ms");
    }

    @Test
    void stackTracesAreEasierToReadWithBlockingCode() throws Exception {
        Throwable blocking;
        try (var p = Executors.newVirtualThreadPerTaskExecutor()) {
            try { p.submit(() -> level1()).get(); throw new AssertionError(); } catch (ExecutionException e) { blocking = e.getCause(); }
        }
        Throwable reactive = null;
        try { Mono.just(1).map(x -> level1b(x)).flatMap(x -> Mono.just(x)).block(); } catch (Throwable t) { reactive = t; }
        for (var pair : List.of(Map.entry("threads_virtuais", blocking), Map.entry("reactor", reactive))) {
            Throwable t = pair.getValue();
            long own = Arrays.stream(t.getStackTrace()).filter(f -> f.getClassName().startsWith("com.payflow")).count();
            System.out.println("STACK " + pair.getKey() + ": frames_totais=" + t.getStackTrace().length + " frames_do_nosso_codigo=" + own
                    + " suprimidas=" + t.getSuppressed().length);
            Arrays.stream(t.getStackTrace()).limit(4).forEach(f -> System.out.println("   " + f));
        }
    }

    static int level1() { return level2(); }
    static int level2() { throw new IllegalStateException("falha no nível 2"); }
    static int level1b(int x) { return level2(); }
}
