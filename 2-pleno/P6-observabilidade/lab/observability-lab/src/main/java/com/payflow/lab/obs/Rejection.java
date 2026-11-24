package com.payflow.lab.obs;

/** Erro de negócio esperado (saldo insuficiente): WARN sem stack trace, 422, NÃO é falha do sistema. */
public class Rejection extends RuntimeException {
    public Rejection(String message) {
        super(message);
    }
}
