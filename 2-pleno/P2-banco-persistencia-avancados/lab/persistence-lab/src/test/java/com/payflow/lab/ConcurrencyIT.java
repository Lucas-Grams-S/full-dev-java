package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.payflow.lab.repo.AccountRepository;
import com.payflow.lab.service.RetryingTransferFacade;
import com.payflow.lab.service.TransferService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** P2.02 e P2.03: locking otimista, retry, pessimista ordenado, deadlock, UPDATE atômico. Banco REAL (PostgreSQL 18). */
@SpringBootTest
class ConcurrencyIT {

    @Autowired TransferService service;
    @Autowired RetryingTransferFacade retrying;
    @Autowired JdbcClient jdbc;
    @Autowired AccountRepository accounts;
    @Autowired PlatformTransactionManager tm;

    @BeforeEach
    void seed() {
        LabSupport.reset(jdbc, "1000.00");
    }

    /** Dispara as tarefas ao mesmo tempo (largada sincronizada) e devolve os resultados/erros. */
    private static List<Object> race(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        for (Callable<Object> t : tasks) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    return t.call();
                } catch (Throwable e) {
                    return e;
                }
            }));
        }
        ready.await();
        go.countDown();
        List<Object> out = new ArrayList<>();
        for (Future<Object> f : futures) {
            out.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return out;
    }

    @Test
    @Timeout(120)
    void optimisticWithoutRetry_losersGetOptimisticLockFailure_andMoneyIsConserved() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String key = "opt-" + i;
            tasks.add(() -> service.transferOptimistic(LabSupport.A1, LabSupport.A2, new BigDecimal("10.00"), key, 50));
        }
        List<Object> results = race(tasks);
        long ok = results.stream().filter(r -> !(r instanceof Throwable)).count();
        List<String> errors = results.stream().filter(r -> r instanceof Throwable).map(r -> r.getClass().getSimpleName()).distinct().toList();
        System.out.printf("OTIMISTA sem retry: %d de 8 passaram; exceções: %s%n", ok, errors);
        BigDecimal a1 = jdbc.sql("SELECT balance FROM accounts WHERE id = :id").param("id", LabSupport.A1).query(BigDecimal.class).single();
        System.out.printf("saldo A1 = %s (esperado %s)%n", a1, new BigDecimal("1000.00").subtract(new BigDecimal("10.00").multiply(BigDecimal.valueOf(ok))));
        assertThat(LabSupport.total(jdbc)).isEqualByComparingTo("4000.00");
        assertThat(a1).isEqualByComparingTo(new BigDecimal("1000.00").subtract(new BigDecimal("10.00").multiply(BigDecimal.valueOf(ok))));
        assertThat(ok).isEqualTo(1);                                            // todas leram a versão 0: só uma vence
        assertThat(errors).containsExactly("ObjectOptimisticLockingFailureException");
        assertThat(LabSupport.ledgerEntries(jdbc)).isEqualTo(2 * ok);
    }

    @Test
    @Timeout(180)
    void optimisticWithRetry_allSucceed_andInvariantsHold() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String key = "retry-" + i;
            tasks.add(() -> retrying.transfer(LabSupport.A1, LabSupport.A2, new BigDecimal("10.00"), key, 50));
        }
        List<Object> results = race(tasks);
        long failed = results.stream().filter(r -> r instanceof Throwable).count();
        results.stream().filter(r -> r instanceof Throwable).forEach(r -> System.out.println("falhou: " + r));
        System.out.printf("OTIMISTA com @Retryable: %d falhas; tentativas totais = %d (para 8 transferências)%n", failed, retrying.attempts());
        assertThat(failed).isZero();
        assertThat(LabSupport.total(jdbc)).isEqualByComparingTo("4000.00");
        assertThat(jdbc.sql("SELECT balance FROM accounts WHERE id = :id").param("id", LabSupport.A1).query(BigDecimal.class).single())
                .isEqualByComparingTo("920.00");
        assertThat(LabSupport.ledgerEntries(jdbc)).isEqualTo(16);
    }

    @Test
    @Timeout(120)
    void pessimisticOrdered_noFailures_noDeadlock_evenWithCrossingTransfers() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String key = "pess-" + i;
            boolean forward = i % 2 == 0;
            tasks.add(() -> service.transferPessimistic(forward ? LabSupport.A1 : LabSupport.A2, forward ? LabSupport.A2 : LabSupport.A1,
                    new BigDecimal("10.00"), key, 30));
        }
        List<Object> results = race(tasks);
        long failed = results.stream().filter(r -> r instanceof Throwable).count();
        results.stream().filter(r -> r instanceof Throwable).forEach(r -> System.out.println("falhou: " + r));
        System.out.printf("PESSIMISTA ordenado, 4 ida + 4 volta cruzadas: %d falhas%n", failed);
        assertThat(failed).isZero();
        assertThat(LabSupport.total(jdbc)).isEqualByComparingTo("4000.00");
        assertThat(jdbc.sql("SELECT balance FROM accounts WHERE id = :id").param("id", LabSupport.A1).query(BigDecimal.class).single())
                .isEqualByComparingTo("1000.00");        // 4 ida e 4 volta de 10.00: volta ao inicial
    }

    @Test
    @Timeout(120)
    void pessimisticUnordered_crossingTransfers_deadlock() throws Exception {
        List<Callable<Object>> tasks = List.of(
                () -> service.transferPessimisticUnordered(LabSupport.A1, LabSupport.A2, new BigDecimal("10.00"), "dl-1", 1500),
                () -> service.transferPessimisticUnordered(LabSupport.A2, LabSupport.A1, new BigDecimal("20.00"), "dl-2", 1500));
        List<Object> results = race(tasks);
        results.forEach(r -> {
            if (r instanceof Throwable t) {
                System.out.println("DEADLOCK -> " + t.getClass().getName());
                Throwable root = t;
                while (root.getCause() != null) {
                    root = root.getCause();
                }
                System.out.println("  causa raiz: " + root.getClass().getSimpleName() + ": " + root.getMessage().lines().findFirst().orElse(""));
            }
        });
        assertThat(results.stream().filter(r -> r instanceof Throwable)).hasSize(1);        // o PostgreSQL derruba UMA delas
        assertThat(LabSupport.total(jdbc)).isEqualByComparingTo("4000.00");
    }

    @Test
    @Timeout(120)
    void atomicSql_ordered_conservesMoney_andRejectsOverdraft() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        // 16 saques de 100.00 de uma conta com 1000.00: só 10 podem passar (regra 1), sem 409 e sem retry
        for (int i = 0; i < 16; i++) {
            String key = "sql-" + i;
            tasks.add(() -> service.transferAtomicSql(LabSupport.A1, LabSupport.A2, new BigDecimal("100.00"), key, 10));
        }
        List<Object> results = race(tasks);
        long ok = results.stream().filter(r -> !(r instanceof Throwable)).count();
        System.out.printf("UPDATE atômico: %d de 16 saques de 100.00 passaram (saldo 1000.00)%n", ok);
        assertThat(ok).isEqualTo(10);
        assertThat(jdbc.sql("SELECT balance FROM accounts WHERE id = :id").param("id", LabSupport.A1).query(BigDecimal.class).single())
                .isEqualByComparingTo("0.00");
        assertThat(LabSupport.total(jdbc)).isEqualByComparingTo("4000.00");
        assertThat(LabSupport.ledgerEntries(jdbc)).isEqualTo(20);
    }

    @Test
    @Timeout(120)
    void noWait_failsFastInsteadOfQueueing() throws Exception {
        var holder = Executors.newSingleThreadExecutor();
        Future<?> f = holder.submit(() -> service.transferPessimistic(LabSupport.A1, LabSupport.A2, new BigDecimal("1.00"), "nw", 3000));
        Thread.sleep(1000);                                  // o holder já segura a linha da A1 (FOR UPDATE)
        var tx = new TransactionTemplate(tm);
        long t0 = System.nanoTime();
        Throwable err = catchThrowable(() -> tx.executeWithoutResult(s -> accounts.lockByIdNowait(LabSupport.A1)));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("NOWAIT: falhou em %d ms com %s%n", ms, err == null ? "nada" : err.getClass().getName());
        f.get();
        holder.shutdown();
        assertThat(err).isNotNull();
        assertThat(ms).isLessThan(1500);
    }

    @Test
    @Timeout(120)
    void unorderedPessimistic_withRetryOnConcurrencyFailure_healsTheDeadlock() throws Exception {
        List<Callable<Object>> tasks = List.of(
                () -> retrying.transferUnorderedWithRetry(LabSupport.A1, LabSupport.A2, new BigDecimal("10.00"), "rd-1", 1500),
                () -> retrying.transferUnorderedWithRetry(LabSupport.A2, LabSupport.A1, new BigDecimal("20.00"), "rd-2", 1500));
        List<Object> results = race(tasks);
        long failed = results.stream().filter(r -> r instanceof Throwable).count();
        System.out.printf("SEM ordem + @Retryable(ConcurrencyFailureException): %d falhas; tentativas = %d (2 transferências)%n", failed, retrying.deadlockAttempts());
        assertThat(failed).isZero();
        assertThat(retrying.deadlockAttempts()).isGreaterThan(2);                   // houve ao menos um deadlock e uma repetição
        assertThat(LabSupport.total(jdbc)).isEqualByComparingTo("4000.00");
        assertThat(LabSupport.ledgerEntries(jdbc)).isEqualTo(4);
    }
}
