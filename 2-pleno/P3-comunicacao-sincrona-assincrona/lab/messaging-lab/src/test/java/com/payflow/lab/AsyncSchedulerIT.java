package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** P3.02: @Async (thread, exceção, rejeição, MDC, auto-chamada) e @Scheduled com ShedLock em duas instâncias. */
@SpringBootTest
class AsyncSchedulerIT {
    @Autowired AsyncDemo demo;
    @Autowired JdbcClient jdbc;
    @Autowired org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor labExecutor;

    @org.junit.jupiter.api.AfterEach
    void drain() {   // o executor é compartilhado: espera esvaziar para um teste não contaminar o outro
        await().atMost(Duration.ofSeconds(10)).until(() -> labExecutor.getActiveCount() == 0 && labExecutor.getThreadPoolExecutor().getQueue().isEmpty());
    }

    @Test
    void asyncRunsInAnotherThreadAndKeepsMdcViaTaskDecorator() throws Exception {
        MDC.put("requestId", "req-123");
        String r = demo.where().get(5, TimeUnit.SECONDS);
        MDC.clear();
        System.out.println("ASYNC_THREAD " + r + " (chamador: " + Thread.currentThread().getName() + ")");
        assertThat(r).startsWith("lab-async-").endsWith("requestId=req-123");
    }

    @Test
    void selfInvocationIsNotAsync() {
        String thread = demo.callsAsyncOnSelf();
        System.out.println("AUTO_CHAMADA thread=" + thread);
        assertThat(thread).isEqualTo(Thread.currentThread().getName());
    }

    @Test
    void exceptionsInVoidGoToTheHandlerAndFuturesCarryThem() {
        AsyncConfig.uncaught.clear();
        demo.fireAndForgetThatFails();
        await().atMost(Duration.ofSeconds(5)).until(() -> !AsyncConfig.uncaught.isEmpty());
        System.out.println("VOID_EXCECAO handler=" + AsyncConfig.uncaught);
        assertThatThrownBy(() -> demo.failsWithFuture().get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void boundedExecutorRejectsWhenSaturated() {
        int accepted = 0, rejected = 0;
        for (int i = 0; i < 10; i++) {
            try { demo.slow(800); accepted++; } catch (TaskRejectedException e) { rejected++; }
        }
        System.out.println("EXECUTOR_LIMITADO aceitas=" + accepted + " rejeitadas=" + rejected + " (2 threads + fila 2)");
        assertThat(accepted).isEqualTo(4);
        assertThat(rejected).isEqualTo(6);
    }

    @Test
    void shedLockRunsTheJobOncePerTickAcrossTwoInstances() throws Exception {
        jdbc.sql("delete from job_runs").update();
        jdbc.sql("delete from shedlock").update();
        var props = new String[] {"lab.scheduling=true", "server.port=0", "spring.kafka.listener.auto-startup=false",
                "spring.rabbitmq.listener.simple.auto-startup=false"};
        try (var a = new SpringApplicationBuilder(LabApp.class).web(WebApplicationType.NONE).properties(props).properties("lab.instance=A").run();
             var b = new SpringApplicationBuilder(LabApp.class).web(WebApplicationType.NONE).properties(props).properties("lab.instance=B").run()) {
            Thread.sleep(9_000);
        }
        var rows = jdbc.sql("select job, instance, count(*) c from job_runs group by job, instance order by 1,2").query().listOfRows();
        rows.forEach(r -> System.out.println("SHEDLOCK " + r));
        int locked = jdbc.sql("select count(*) from job_runs where job='locked'").query(Integer.class).single();
        int unlocked = jdbc.sql("select count(*) from job_runs where job='unlocked'").query(Integer.class).single();
        System.out.println("SHEDLOCK total_com_lock=" + locked + " total_sem_lock=" + unlocked + " (9 s, 2 instâncias, fixedRate 1 s, lockAtLeastFor 2 s)");
        assertThat(unlocked).isGreaterThan(locked);
        assertThat(locked).isBetween(3, 6);
    }
}
