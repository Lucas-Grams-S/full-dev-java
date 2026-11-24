package com.payflow.lab.transfer;

import static org.mockito.Mockito.verify;

import com.payflow.lab.account.AccountApi;
import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.application.port.in.TransferUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Sobe SÓ o módulo transfer; o account vira um mock (a fronteira é a interface AccountApi). */
@ApplicationModuleTest
class TransferModuleTest {

    @Autowired TransferUseCase useCase;
    @MockitoBean AccountApi accounts;

    @Test
    void publishesEventAndMovesMoneyThroughThePort(Scenario scenario) {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        scenario.stimulate(() -> useCase.transfer(from, to, Money.of("25.00")))
                .andWaitForEventOfType(TransferCompleted.class)
                .matching(e -> e.sourceAccountId().equals(from))
                .toArrive();
        verify(accounts).withdraw(from, Money.of("25.00"));
        verify(accounts).deposit(to, Money.of("25.00"));
    }
}
