package com.payflow.ledger;

import com.payflow.ledger.internal.Rounding;
import java.math.BigDecimal;

public final class Ledger {
    public static BigDecimal fee(BigDecimal amount) { return Rounding.money(amount.multiply(new BigDecimal("0.015"))); }
}
