package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.payflow.lab.repo.EntryLine;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** P2.08: JdbcClient mapeando linhas para records (o caso do extrato) e o que o pgjdbc faz com timestamptz → Instant. */
@SpringBootTest
class JdbcClientIT {

    record EntryLineOdt(UUID id, String type, java.math.BigDecimal amount, OffsetDateTime createdAt) { }

    @Autowired JdbcClient jdbc;

    @BeforeEach
    void seed() {
        LabSupport.bigData(jdbc);
    }

    @Test
    void recordWithInstant_andWithOffsetDateTime() {
        UUID acc = jdbc.sql("SELECT id FROM accounts ORDER BY id LIMIT 1").query(UUID.class).single();
        String sql = "SELECT id, type, amount, created_at FROM ledger_entries WHERE account_id = :a ORDER BY created_at DESC, id DESC LIMIT 3";
        Throwable t = catchThrowable(() -> jdbc.sql(sql).param("a", acc).query(EntryLine.class).list());
        System.out.println("JdbcClient -> record com Instant: " + (t == null ? "OK" : t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()).lines().findFirst().orElse("")));
        List<EntryLineOdt> odt = jdbc.sql(sql).param("a", acc).query(EntryLineOdt.class).list();
        System.out.println("JdbcClient -> record com OffsetDateTime: " + odt.size() + " linhas; 1ª = " + odt.get(0));
        assertThat(odt).hasSize(3);
    }
}
