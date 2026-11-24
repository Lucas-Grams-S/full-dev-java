package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** P2.06: pool de 2 conexões, 4 transações que seguram a conexão por 2,5 s, timeout de 1 s. */
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.minimum-idle=2",
        "spring.datasource.hikari.connection-timeout=1000",
        "spring.datasource.hikari.leak-detection-threshold=2000",
        "spring.datasource.hikari.pool-name=payflow-pool"})
class PoolIT {

    @Autowired HikariDataSource ds;
    @Autowired PlatformTransactionManager tm;
    @Autowired JdbcClient jdbc;

    @Test
    void poolExhaustion_producesTimeouts_andTheStateIsVisibleInTheMxBean() throws Exception {
        HikariPoolMXBean pool = ds.getHikariPoolMXBean();
        ExecutorService ex = Executors.newFixedThreadPool(4);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> fs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            fs.add(ex.submit(() -> {
                go.await();
                try {
                    new TransactionTemplate(tm).executeWithoutResult(s -> {
                        jdbc.sql("SELECT 1").query(Integer.class).single();     // a conexão é emprestada AQUI...
                        sleep(2500);                                              // ...e segurada durante o "trabalho lento" (HTTP, e-mail, ...)
                    });
                    return "ok";
                } catch (Throwable t) {
                    Throwable root = t;
                    StringBuilder chain = new StringBuilder(t.getClass().getSimpleName());
                    while (root.getCause() != null) {
                        root = root.getCause();
                        chain.append(" <- ").append(root.getClass().getSimpleName());
                    }
                    return chain + " | " + root.getMessage();
                }
            }));
        }
        go.countDown();
        Thread.sleep(700);
        System.out.printf("MXBean no meio da disputa: active=%d idle=%d total=%d threadsAwaiting=%d%n",
                pool.getActiveConnections(), pool.getIdleConnections(), pool.getTotalConnections(), pool.getThreadsAwaitingConnection());
        List<String> out = new ArrayList<>();
        for (Future<String> f : fs) {
            out.add(f.get());
        }
        out.forEach(o -> System.out.println("resultado: " + o));
        ex.shutdown();
        assertThat(out.stream().filter(o -> o.equals("ok"))).hasSize(2);
        assertThat(out.stream().filter(o -> o.contains("request timed out"))).hasSize(2);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
