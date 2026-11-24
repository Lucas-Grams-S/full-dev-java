package com.payflow.lab.transfer.domain;

import com.payflow.lab.shared.Money;
import java.util.UUID;

public class Transfer {

    public enum Status { PENDING, COMPLETED }

    private final UUID id = UUID.randomUUID();
    private final UUID source;
    private final UUID target;
    private final Money amount;
    private Status status = Status.PENDING;

    public Transfer(UUID source, UUID target, Money amount) {
        if (source.equals(target)) {
            throw new IllegalArgumentException("source and target must differ");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.source = source;
        this.target = target;
        this.amount = amount;
    }

    public void complete() {
        this.status = Status.COMPLETED;
    }

    public UUID id() { return id; }
    public UUID source() { return source; }
    public UUID target() { return target; }
    public Money amount() { return amount; }
    public Status status() { return status; }
}
