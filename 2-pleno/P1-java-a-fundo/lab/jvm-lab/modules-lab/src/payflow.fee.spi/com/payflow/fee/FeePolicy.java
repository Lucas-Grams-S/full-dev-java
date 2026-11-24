package com.payflow.fee;

import java.math.BigDecimal;

public interface FeePolicy { String name(); BigDecimal fee(BigDecimal amount); }
