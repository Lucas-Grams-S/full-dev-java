package com.payflow.lab;

import java.math.BigDecimal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** P3.07: consumidor idempotente. A marca "já processei" e o efeito de negócio andam na MESMA transação. */
@Service
public class PaymentConsumer {
    private final JdbcClient jdbc;

    public PaymentConsumer(JdbcClient jdbc) { this.jdbc = jdbc; }

    /** Versão correta: INSERT ... ON CONFLICT DO NOTHING + efeito, atômicos. */
    @Transactional
    public boolean handleIdempotent(String messageId, String walletId, BigDecimal amount) {
        int inserted = jdbc.sql("insert into processed_messages (consumer, message_id) values ('payment', :id) on conflict do nothing")
                .param("id", messageId).update();
        if (inserted == 0) return false;               // duplicata: não faz nada
        debit(walletId, amount, messageId);
        return true;
    }

    /** Versão INGÊNUA (check-then-act): lê, processa, grava a marca depois. Tem corrida. */
    public boolean handleNaive(String messageId, String walletId, BigDecimal amount) {
        Integer seen = jdbc.sql("select count(*) from processed_messages where consumer='naive' and message_id=:id")
                .param("id", messageId).query(Integer.class).single();
        if (seen > 0) return false;
        debit(walletId, amount, messageId);
        jdbc.sql("insert into processed_messages (consumer, message_id) values ('naive', :id) on conflict do nothing")
                .param("id", messageId).update();
        return true;
    }

    /** Versão sem nenhuma proteção. */
    public void handleUnprotected(String walletId, BigDecimal amount) { debit(walletId, amount, null); }

    private void debit(String walletId, BigDecimal amount, String requestId) {
        jdbc.sql("update wallets set balance = balance - :a where id = :w").param("a", amount).param("w", walletId).update();
        jdbc.sql("insert into payments (request_id, wallet_id, amount) values (:r, :w, :a)")
                .param("r", requestId).param("w", walletId).param("a", amount).update();
    }
}
