package com.payflow.lab.account.domain;

import com.payflow.lab.shared.Money;
import java.util.UUID;

/** Entidade (raiz do agregado). A invariante "saldo nunca negativo" vive AQUI, não no serviço. */
public class Account {

    private final UUID id;
    private final String owner;
    private Money balance;

    public Account(UUID id, String owner, Money balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
    }

    public void withdraw(Money amount) {
        requirePositive(amount);
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        balance = balance.minus(amount);
    }

    public void deposit(Money amount) {
        requirePositive(amount);
        balance = balance.plus(amount);
    }

    private static void requirePositive(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive: " + amount);
        }
    }

    public UUID id() { return id; }
    public String owner() { return owner; }
    public Money balance() { return balance; }
}
