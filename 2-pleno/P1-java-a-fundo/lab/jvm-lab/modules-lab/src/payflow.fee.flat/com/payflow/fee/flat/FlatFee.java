package com.payflow.fee.flat;

import com.payflow.fee.FeePolicy;
import java.math.BigDecimal;

public class FlatFee implements FeePolicy {
    public String name() { return "flat"; }
    public BigDecimal fee(BigDecimal amount) { return new BigDecimal("2.00"); }
}
