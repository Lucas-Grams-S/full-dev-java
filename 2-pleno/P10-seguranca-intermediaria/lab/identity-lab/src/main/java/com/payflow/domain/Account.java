package com.payflow.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Account(UUID id, UUID ownerId, BigDecimal balance, String status) {
    public Account withStatus(String newStatus) {
        return new Account(id, ownerId, balance, newStatus);
    }
}
