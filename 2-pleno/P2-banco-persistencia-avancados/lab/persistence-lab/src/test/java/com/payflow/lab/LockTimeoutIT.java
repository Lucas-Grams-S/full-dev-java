package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.payflow.lab.repo.AccountRepository;
import com.payflow.lab.service.TransferService;
import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** P2.02: como limitar a ESPERA por uma trava: NOWAIT, a dica lock.timeout do JPA e SET LOCAL lock_timeout do PostgreSQL. */
@SpringBootTest
class LockTimeoutIT {

    @Autowired TransferService service;
    @Autowired AccountRepository accounts;
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager tm;

    @BeforeEach
    void seed() {
        LabSupport.reset(jdbc, "1000.00");
    }

    /** Segura a linha da A1 por 3 s; em seguida roda `attempt` e devolve (exceção, milissegundos gastos). */
    private String whileHeld(Runnable attempt) throws Exception {
        var holder = Executors.newSingleThreadExecutor();
        Future<?> f = holder.submit(() -> service.transferPessimistic(LabSupport.A1, LabSupport.A2, new BigDecimal("1.00"), "held-" + System.nanoTime(), 3000));
        Thread.sleep(1000);
        long t0 = System.nanoTime();
        Throwable t = catchThrowable(attempt::run);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        f.get();
        holder.shutdown();
        return (t == null ? "NENHUMA exceção" : t.getClass().getSimpleName()) + " após " + ms + " ms";
    }

    @Test
    @Timeout(60)
    void threeWaysToBoundTheWait() throws Exception {
        var tx = new TransactionTemplate(tm);
        String nowait = whileHeld(() -> tx.executeWithoutResult(s -> accounts.lockByIdNowait(LabSupport.A1)));
        System.out.println("NOWAIT (hint lock.timeout=0):          " + nowait);
        String hint = whileHeld(() -> tx.executeWithoutResult(s -> accounts.lockByIdHint500(LabSupport.A1)));
        System.out.println("hint lock.timeout=500 (PostgreSQL):    " + hint);
        String local = whileHeld(() -> tx.executeWithoutResult(s -> {
            jdbc.sql("SET LOCAL lock_timeout = '500ms'").update();
            accounts.lockById(LabSupport.A1);
        }));
        System.out.println("SET LOCAL lock_timeout = '500ms':       " + local);
        assertThat(nowait).contains("CannotAcquireLockException");
        assertThat(local).contains("CannotAcquireLockException");
    }
}
