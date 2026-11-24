package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.payflow.lab.service.BalanceService;
import com.payflow.lab.service.BalanceView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** P2.09: o que acontece com o endpoint quando o Redis cai (Strict). */
@SpringBootTest
class CacheOutageStrictIT {

    @Autowired BalanceService service;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void seed() {
        LabSupport.reset(jdbc, "1000.00");
        service.loadDelayMs(0);
        service.resetLoads();
    }

    @Test
    void redisDown() {
        long t0 = System.nanoTime();
        Object[] result = new Object[1];
        Throwable t = catchThrowable(() -> result[0] = service.balanceWithDeadRedis(LabSupport.A1));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("STRICT: Redis fora -> %s em %d ms; consultas ao banco: %d%n",
                t == null ? "resposta " + result[0] : "EXCEÇÃO " + t.getClass().getName(), ms, service.dbLoads());
        assertThat(t).isNotNull();
    }
}
