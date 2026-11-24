package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payflow.fixtures.badsql.reports.BalanceReport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * O incidente do P4.11, reproduzido: o módulo "reports" lê a tabela do módulo "accounts" por SQL.
 * Uma migração legítima do dono da tabela quebra o "reports" EM PRODUÇÃO, e nada no build avisou.
 */
class SqlCouplingIncidentTest {

    @Test
    void renamingAColumnBreaksTheOtherModuleAtRuntime() {
        var db = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                .setName("incident-" + UUID.randomUUID()).addScript("classpath:schema.sql").build();
        var jdbc = JdbcClient.create(db);
        var id = UUID.randomUUID();
        jdbc.sql("insert into accounts (id, owner, balance) values (?, 'ana', 10.00)").param(id).update();
        var report = new BalanceReport(jdbc);

        assertThat(report.balance(id.toString())).isEqualTo("10.00");          // hoje funciona

        // o dono da tabela faz uma migração legítima (e os testes DELE continuam verdes)
        jdbc.sql("alter table accounts alter column balance rename to current_balance").update();

        assertThatThrownBy(() -> report.balance(id.toString())).satisfies(e ->
                System.out.println("INCIDENT -> " + e.getClass().getSimpleName() + ": " + e.getMessage().lines().findFirst().orElse("")));
    }
}
