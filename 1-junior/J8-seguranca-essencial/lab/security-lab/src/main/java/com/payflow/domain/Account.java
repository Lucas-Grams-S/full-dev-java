package com.payflow.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Versão reduzida da entidade da J5.07/J5.08: dono por id e o par block()/unblock(). */
public class Account {

    private final UUID id;
    private final UUID ownerId;
    private final BigDecimal balance;
    private AccountStatus status = AccountStatus.ACTIVE;

    public Account(UUID id, UUID ownerId, BigDecimal balance) {
        this.id = id;
        this.ownerId = ownerId;
        this.balance = balance;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }

    public void block() { this.status = AccountStatus.BLOCKED; }
    public void unblock() { this.status = AccountStatus.ACTIVE; }
}
