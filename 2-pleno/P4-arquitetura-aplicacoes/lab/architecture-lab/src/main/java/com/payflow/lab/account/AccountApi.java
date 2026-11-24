package com.payflow.lab.account;

import com.payflow.lab.shared.Money;
import java.util.UUID;

/** API pública do módulo account: o ÚNICO ponto de entrada para outros módulos. */
public interface AccountApi {

    UUID open(String owner, Money initialBalance);

    Money balanceOf(UUID accountId);

    void withdraw(UUID accountId, Money amount);

    void deposit(UUID accountId, Money amount);
}
