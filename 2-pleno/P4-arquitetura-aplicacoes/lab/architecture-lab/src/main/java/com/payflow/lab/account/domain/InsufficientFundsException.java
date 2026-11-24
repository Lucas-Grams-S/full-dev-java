package com.payflow.lab.account.domain;

import com.payflow.lab.shared.Money;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID id, Money balance, Money wanted) {
        super("account %s has %s, wanted %s".formatted(id, balance, wanted));
    }
}
