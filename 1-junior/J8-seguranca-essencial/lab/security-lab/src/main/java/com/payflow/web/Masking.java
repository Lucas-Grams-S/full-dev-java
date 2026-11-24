package com.payflow.web;

/** Mascara dados pessoais para log e mensagens (decisão 38: nenhum dado pessoal em log). */
public final class Masking {

    private static final String MASK = "*";

    private Masking() {
    }

    /**
     * "ana@payflow.dev" vira "a**@payflow.dev". Uma parte local de um caractere é mascarada por inteiro
     * ("a@x.com" vira "*@x.com"), porque mostrar a única letra revelaria o dado inteiro.
     * As mensagens de erro NÃO repetem o valor recebido: elas podem acabar em um log.
     */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        int at = email.indexOf('@');
        if (at < 1) {
            throw new IllegalArgumentException("not an email address");
        }
        String domain = email.substring(at);
        if (at == 1) {
            return MASK + domain;
        }
        return email.charAt(0) + MASK.repeat(at - 1) + domain;
    }
}
