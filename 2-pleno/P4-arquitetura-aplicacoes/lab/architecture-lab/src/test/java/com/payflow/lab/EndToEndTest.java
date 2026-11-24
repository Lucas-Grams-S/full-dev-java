package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.payflow.lab.account.AccountApi;
import com.payflow.lab.antifraud.AntifraudApi;
import com.payflow.lab.notification.adapter.out.InMemorySender;
import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.application.port.in.TransferUseCase;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EndToEndTest {

    @Autowired AccountApi accounts;
    @Autowired TransferUseCase transfers;
    @Autowired InMemorySender sender;
    @Autowired AntifraudApi antifraud;

    @Test
    void transferMovesMoneyAndTheOtherModulesReact() {
        var a = accounts.open("ana", Money.of("5000.00"));
        var b = accounts.open("bia", Money.of("0.00"));

        var id = transfers.transfer(a, b, Money.of("1500.00"));

        assertThat(accounts.balanceOf(a)).isEqualTo(Money.of("3500.00"));   // síncrono, na transação
        assertThat(accounts.balanceOf(b)).isEqualTo(Money.of("1500.00"));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {        // assíncrono, depois do commit
            assertThat(sender.sent()).anyMatch(s -> s.account().equals(b));
            assertThat(antifraud.flaggedTransfers()).contains(id);
        });
    }

    @Test
    void failedTransferPublishesNothing() {
        var a = accounts.open("carl", Money.of("10.00"));
        var b = accounts.open("dani", Money.of("0.00"));
        int before = sender.sent().size();
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> transfers.transfer(a, b, Money.of("99.00")));
        assertThat(accounts.balanceOf(a)).isEqualTo(Money.of("10.00"));
        // o ouvinte é assíncrono: "nada chegou" só prova algo se esperarmos um pouco ("during")
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(sender.sent()).hasSize(before));
    }
}
