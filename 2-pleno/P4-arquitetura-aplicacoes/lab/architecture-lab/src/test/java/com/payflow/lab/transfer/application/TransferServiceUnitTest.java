package com.payflow.lab.transfer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.TransferCompleted;
import com.payflow.lab.transfer.application.port.out.Accounts;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** O caso de uso sem Spring, sem banco, sem Docker: só as portas, trocadas por fakes. */
class TransferServiceUnitTest {

    static class FakeAccounts implements Accounts {
        final List<String> calls = new ArrayList<>();
        public void debit(UUID account, Money amount) { calls.add("debit " + amount); }
        public void credit(UUID account, Money amount) { calls.add("credit " + amount); }
    }

    @Test
    void debitsCreditsAndPublishesInThatOrder() {
        var accounts = new FakeAccounts();
        var events = new ArrayList<TransferCompleted>();
        var service = new TransferService(accounts, events::add);

        var id = service.transfer(UUID.randomUUID(), UUID.randomUUID(), Money.of("10.00"));

        assertThat(accounts.calls).containsExactly("debit 10.00", "credit 10.00");
        assertThat(events).singleElement().satisfies(e -> assertThat(e.transferId()).isEqualTo(id));
    }

    @Test
    void doesNotPublishWhenTheDebitFails() {
        var events = new ArrayList<TransferCompleted>();
        Accounts failing = new Accounts() {
            public void debit(UUID account, Money amount) { throw new IllegalStateException("insufficient"); }
            public void credit(UUID account, Money amount) { }
        };
        var service = new TransferService(failing, events::add);

        assertThatThrownBy(() -> service.transfer(UUID.randomUUID(), UUID.randomUUID(), Money.of("10.00")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(events).isEmpty();
    }
}
