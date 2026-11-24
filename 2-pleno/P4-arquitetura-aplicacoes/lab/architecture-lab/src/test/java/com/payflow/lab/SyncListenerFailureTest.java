package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payflow.lab.account.AccountApi;
import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.TransferCompleted;
import com.payflow.lab.transfer.application.port.in.TransferUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

/** Ouvinte SÍNCRONO (@EventListener) que falha: a exceção sobe e DESFAZ a transferência. */
@SpringBootTest
@Import({SyncListenerFailureTest.Boom.class, SyncListenerFailureTest.UseJdbc.class})
class SyncListenerFailureTest {

    /** Troca o adaptador em memória pelo JDBC (H2) para que a TRANSAÇÃO tenha o que desfazer. */
    @TestConfiguration
    static class UseJdbc {
        @Bean
        @org.springframework.context.annotation.Primary
        com.payflow.lab.account.application.port.out.AccountStore jdbcStore(org.springframework.jdbc.core.simple.JdbcClient jdbc) {
            return new com.payflow.lab.account.adapter.out.JdbcAccountStore(jdbc);
        }
    }

    @TestConfiguration
    static class Boom {
        @Bean
        Object syncBoom() {
            return new Object() {
                @EventListener
                void on(TransferCompleted event) {
                    throw new IllegalStateException("sync listener failed");
                }
            };
        }
    }

    @Autowired AccountApi accounts;
    @Autowired TransferUseCase transfers;

    @Test
    void failureInASyncListenerRollsBackTheMoney() {
        var a = accounts.open("eva", Money.of("100.00"));
        var b = accounts.open("fab", Money.of("0.00"));

        assertThatThrownBy(() -> transfers.transfer(a, b, Money.of("40.00")))
                .hasMessage("sync listener failed");

        // o banco desfez TUDO: nem o débito nem o crédito ficaram
        System.out.println("SYNC LISTENER FAILED -> saldo a=" + accounts.balanceOf(a) + " b=" + accounts.balanceOf(b));
        assertThat(accounts.balanceOf(a)).isEqualTo(Money.of("100.00"));
        assertThat(accounts.balanceOf(b)).isEqualTo(Money.of("0.00"));
    }
}
