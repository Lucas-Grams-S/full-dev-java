package com.payflow.lab;

import java.math.BigDecimal;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** P3.09: escrever o fato de negócio e o evento na MESMA transação. */
@Service
public class OutboxService {
    private final JdbcClient jdbc;

    public OutboxService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void transferWithOutbox(String from, String to, BigDecimal amount, String transferId) {
        jdbc.sql("update wallets set balance = balance - :a where id = :w").param("a", amount).param("w", from).update();
        jdbc.sql("update wallets set balance = balance + :a where id = :w").param("a", amount).param("w", to).update();
        jdbc.sql("insert into outbox_events (aggregate_id, type, payload) values (:agg, 'TransferCompleted', :p)")
                .param("agg", from).param("p", "{\"transferId\":\"" + transferId + "\",\"amount\":\"" + amount + "\"}").update();
    }
}
