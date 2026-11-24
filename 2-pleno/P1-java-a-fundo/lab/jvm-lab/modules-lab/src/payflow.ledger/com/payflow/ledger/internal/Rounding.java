package com.payflow.ledger.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Rounding {
    public static BigDecimal money(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_EVEN); }
}
