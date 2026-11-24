package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.payflow.lab.account.AccountApi;
import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.TransferCompleted;
import com.payflow.lab.transfer.application.port.in.TransferUseCase;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.ApplicationModuleListener;

/** Ouvinte ASSÍNCRONO (@ApplicationModuleListener) que falha: o publicador NUNCA fica sabendo. */
@SpringBootTest
@Import(AsyncListenerFailureTest.Boom.class)
class AsyncListenerFailureTest {

    static final AtomicInteger CALLS = new AtomicInteger();

    @TestConfiguration
    static class Boom {
        @Bean
        Object asyncBoom() {
            return new Object() {
                @ApplicationModuleListener
                void on(TransferCompleted event) {
                    CALLS.incrementAndGet();
                    throw new IllegalStateException("async listener failed");
                }
            };
        }
    }

    @Autowired AccountApi accounts;
    @Autowired TransferUseCase transfers;

    @Test
    void failureInAnAsyncListenerIsInvisibleToThePublisher() {
        var a = accounts.open("gus", Money.of("100.00"));
        var b = accounts.open("hana", Money.of("0.00"));

        transfers.transfer(a, b, Money.of("40.00"));                 // NÃO lança

        assertThat(accounts.balanceOf(b)).isEqualTo(Money.of("40.00"));
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> assertThat(CALLS.get()).isEqualTo(1));
        System.out.println("ASYNC LISTENER FAILED -> transfer ok, saldo b=" + accounts.balanceOf(b) + ", ouvinte chamado " + CALLS.get() + "x (erro só no log)");
    }
}
