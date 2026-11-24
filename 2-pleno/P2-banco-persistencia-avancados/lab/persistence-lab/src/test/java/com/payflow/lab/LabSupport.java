package com.payflow.lab;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Dados do laboratório: dois usuários, quatro contas com saldo conhecido. Recriados a cada teste. */
public final class LabSupport {

    public static final UUID ANA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID BRUNO = UUID.fromString("10000000-0000-0000-0000-000000000002");
    public static final UUID A1 = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID A2 = UUID.fromString("20000000-0000-0000-0000-000000000002");
    public static final UUID A3 = UUID.fromString("20000000-0000-0000-0000-000000000003");
    public static final UUID A4 = UUID.fromString("20000000-0000-0000-0000-000000000004");

    private LabSupport() {
    }

    public static void reset(JdbcClient jdbc, String balance) {
        jdbc.sql("TRUNCATE ledger_entries, transfers, accounts, users CASCADE").update();
        jdbc.sql("INSERT INTO users (id, email, full_name, password_hash) VALUES (:id, :e, :n, 'x')").param("id", ANA).param("e", "ana@example.com").param("n", "Ana").update();
        jdbc.sql("INSERT INTO users (id, email, full_name, password_hash) VALUES (:id, :e, :n, 'x')").param("id", BRUNO).param("e", "bruno@example.com").param("n", "Bruno").update();
        List<Object[]> accs = List.of(new Object[] {A1, ANA}, new Object[] {A2, BRUNO}, new Object[] {A3, ANA}, new Object[] {A4, BRUNO});
        for (Object[] a : accs) {
            jdbc.sql("INSERT INTO accounts (id, owner_id, balance) VALUES (:id, :o, :b)")
                    .param("id", a[0]).param("o", a[1]).param("b", new BigDecimal(balance)).update();
        }
    }

    public static BigDecimal total(JdbcClient jdbc) {
        return jdbc.sql("SELECT sum(balance) FROM accounts").query(BigDecimal.class).single();
    }

    /** Regra 3: saldo = soma dos lançamentos? (aqui o saldo inicial não tem lançamento, então comparamos a variação) */
    public static long ledgerEntries(JdbcClient jdbc) {
        return jdbc.sql("SELECT count(*) FROM ledger_entries").query(Long.class).single();
    }

    /** 10 usuários, 10 contas, 30 lançamentos por conta, 40 transferências entre contas diferentes (para os testes de fetch). */
    public static void bigData(JdbcClient jdbc) {
        jdbc.sql("TRUNCATE ledger_entries, transfers, accounts, users CASCADE").update();
        jdbc.sql("INSERT INTO users (email, full_name, password_hash) SELECT 'u' || g || '@example.com', 'Pessoa ' || g, 'x' FROM generate_series(1, 10) g").update();
        jdbc.sql("INSERT INTO accounts (owner_id, balance) SELECT id, 1000 FROM users").update();
        jdbc.sql("""
                INSERT INTO ledger_entries (account_id, type, amount, created_at)
                SELECT a.id, CASE WHEN g % 2 = 0 THEN 'DEBIT' ELSE 'CREDIT' END, 10 + g, now() - make_interval(mins => g)
                FROM accounts a, generate_series(1, 30) g""").update();
        jdbc.sql("""
                INSERT INTO transfers (source_account_id, target_account_id, amount, idempotency_key, created_at)
                SELECT s.id, t.id, 5, 'k-' || g, now() - make_interval(mins => g)
                FROM generate_series(1, 40) g
                JOIN (SELECT id, row_number() OVER (ORDER BY id) n FROM accounts) s ON s.n = 1 + g % 10
                JOIN (SELECT id, row_number() OVER (ORDER BY id) n FROM accounts) t ON t.n = 1 + (g + 3) % 10""").update();
    }
}
