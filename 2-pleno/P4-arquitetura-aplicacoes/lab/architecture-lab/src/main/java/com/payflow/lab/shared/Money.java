package com.payflow.lab.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Value object: imutável, sem identidade, igualdade por valor, valida ao nascer. */
public record Money(BigDecimal amount) {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("scale greater than 2: " + amount);
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isLessThan(Money other) {
        return amount.compareTo(other.amount) < 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
