package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.lab.service.AccountOpeningService;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** P2.01: write skew em READ COMMITTED × SERIALIZABLE, e como o Spring traduz a falha de serialização (SQLSTATE 40001 vira CannotAcquireLockException, subclasse de PessimisticLockingFailureException). */
@SpringBootTest
class IsolationIT {

    @Autowired AccountOpeningService service;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void seed() {
        LabSupport.reset(jdbc, "0.00");                    // Ana já tem 2 contas (A1 e A3)
    }

    private List<Object> twoAtOnce(Callable<Object> task) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> fs = List.of(
                ex.submit(() -> { go.await(); try { return task.call(); } catch (Throwable t) { return t; } }),
                ex.submit(() -> { go.await(); try { return task.call(); } catch (Throwable t) { return t; } }));
        go.countDown();
        List<Object> out = List.of(fs.get(0).get(), fs.get(1).get());
        ex.shutdown();
        return out;
    }

    private long anaAccounts() {
        return jdbc.sql("SELECT count(*) FROM accounts WHERE owner_id = :o").param("o", LabSupport.ANA).query(Long.class).single();
    }

    @Test
    void readCommitted_bothPass_andTheRuleIsBroken() throws Exception {
        List<Object> r = twoAtOnce(() -> { service.openReadCommitted(LabSupport.ANA, 300); return "ok"; });
        System.out.println("READ COMMITTED: resultados " + r + " -> contas de Ana: " + anaAccounts() + " (limite 3)");
        assertThat(anaAccounts()).isEqualTo(4);
    }

    @Test
    void serializable_oneFails_withPessimisticLockingFailure() throws Exception {
        List<Object> r = twoAtOnce(() -> { service.openSerializable(LabSupport.ANA, 300); return "ok"; });
        r.stream().filter(o -> o instanceof Throwable).forEach(o -> System.out.println("SERIALIZABLE: " + o.getClass().getName()));
        System.out.println("SERIALIZABLE: contas de Ana: " + anaAccounts() + " (limite 3)");
        assertThat(anaAccounts()).isEqualTo(3);
        assertThat(r.stream().filter(o -> o instanceof org.springframework.dao.PessimisticLockingFailureException)).hasSize(1);
    }
}
