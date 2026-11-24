package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.postgresql.ds.PGSimpleDataSource;

/** P2.05/P2.11: CREATE INDEX CONCURRENTLY dentro de uma migração do Flyway com PostgreSQL. */
class FlywayConcurrentIT {

    private static final String ADMIN = "jdbc:postgresql://127.0.0.1:55432/postgres";

    private PGSimpleDataSource freshDb(String name) throws Exception {
        try (Connection c = DriverManager.getConnection(ADMIN, "postgres", "lab"); Statement st = c.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS " + name + " WITH (FORCE)");
            st.execute("CREATE DATABASE " + name);
        }
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl("jdbc:postgresql://127.0.0.1:55432/" + name);
        ds.setUser("postgres");
        ds.setPassword("lab");
        return ds;
    }

    private String indexState(PGSimpleDataSource ds) throws Exception {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT i.indisvalid FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid "
                     + "WHERE c.relname = 'idx_ledger_entries_account_created_cov'")) {
            return rs.next() ? "existe, valid=" + rs.getBoolean(1) : "não existe";
        }
    }

    @Test
    @Timeout(120)
    void defaultConfig_hangsForever_becauseFlywayHoldsAnOpenTransaction() throws Exception {
        var ds = freshDb("payflow_fw_a");
        ExecutorService ex = Executors.newSingleThreadExecutor();
        Future<?> f = ex.submit(() -> Flyway.configure().dataSource(ds).locations("classpath:db/fw").load().migrate());
        boolean hung = false;
        try {
            f.get(15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            hung = true;
        }
        System.out.println("PADRÃO: migrate() ainda rodando após 15 s? " + hung);
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:55432/postgres", "postgres", "lab"); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT state, wait_event, left(regexp_replace(query, '\\s+', ' ', 'g'), 60) FROM pg_stat_activity "
                     + "WHERE datname = 'payflow_fw_a' ORDER BY state")) {
            while (rs.next()) {
                System.out.println("  sessão: " + rs.getString(1) + " | espera: " + rs.getString(2) + " | " + rs.getString(3));
            }
        }
        try (Connection c = DriverManager.getConnection(ADMIN, "postgres", "lab"); Statement st = c.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS payflow_fw_a WITH (FORCE)");        // destrava o experimento
        }
        ex.shutdownNow();
        assertThat(hung).isTrue();
    }

    @Test
    @Timeout(120)
    void transactionalLockFalse_succeeds() throws Exception {
        var ds = freshDb("payflow_fw_b");
        var result = Flyway.configure().dataSource(ds).locations("classpath:db/fw")
                .configuration(Map.of("flyway.postgresql.transactional.lock", "false")).load().migrate();
        System.out.println("transactional.lock=false: migrações aplicadas = " + result.migrationsExecuted + "; índice " + indexState(ds));
        assertThat(result.migrationsExecuted).isEqualTo(2);
        assertThat(indexState(ds)).isEqualTo("existe, valid=true");
    }
}
