package com.payflow.lab.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** O que vai para o Redis: um DTO pequeno e imutável, nunca a entidade JPA. */
public record BalanceView(UUID accountId, BigDecimal balance, Instant asOf) {
}
