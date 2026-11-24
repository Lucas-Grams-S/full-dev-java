package com.payflow.transfer.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Transfer(UUID id, UUID from, UUID to, BigDecimal amount) {}
