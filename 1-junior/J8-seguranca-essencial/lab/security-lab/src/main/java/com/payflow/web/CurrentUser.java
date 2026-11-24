package com.payflow.web;

import java.io.Serializable;
import java.util.UUID;

/** Quem está logado (id e dados simples, nunca a entidade). Serializable: a sessão pode ser gravada (P2, Redis). */
public record CurrentUser(UUID id, String email, String fullName) implements Serializable { }
