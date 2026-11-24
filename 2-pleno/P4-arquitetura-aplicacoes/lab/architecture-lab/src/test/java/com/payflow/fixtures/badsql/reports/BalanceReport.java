package com.payflow.fixtures.badsql.reports;

import org.springframework.jdbc.core.simple.JdbcClient;

/** Nenhum import de outro módulo: o acoplamento está na STRING do SQL, que nenhuma ferramenta de tipos enxerga. */
public class BalanceReport {
    private final JdbcClient jdbc;

    public BalanceReport(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public String balance(String id) {
        return jdbc.sql("select balance from accounts where id = ?").param(id).query(String.class).single();
    }
}
