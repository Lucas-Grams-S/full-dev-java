package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payflow.lab.account.domain.Account;
import com.payflow.lab.account.domain.InsufficientFundsException;
import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.domain.Transfer;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Sem Spring, sem banco: o domínio roda em milissegundos. */
class DomainTests {

    @Test
    void accountNeverGoesNegative() {
        var account = new Account(UUID.randomUUID(), "ana", Money.of("10.00"));
        assertThatThrownBy(() -> account.withdraw(Money.of("10.01"))).isInstanceOf(InsufficientFundsException.class);
        assertThat(account.balance()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void moneyIsComparedByValueAndRoundedHalfEven() {
        assertThat(Money.of("10")).isEqualTo(Money.of("10.00"));
        assertThatThrownBy(() -> Money.of("10.005")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferRejectsSameAccountAndNonPositiveAmount() {
        var id = UUID.randomUUID();
        assertThatThrownBy(() -> new Transfer(id, id, Money.of("1.00"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Transfer(id, UUID.randomUUID(), Money.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }
}
