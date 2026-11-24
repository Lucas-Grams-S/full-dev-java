package com.payflow.lab.repo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Linha do extrato: o DTO de leitura (record). */
public record EntryLine(UUID id, String type, BigDecimal amount, Instant createdAt) {
}
