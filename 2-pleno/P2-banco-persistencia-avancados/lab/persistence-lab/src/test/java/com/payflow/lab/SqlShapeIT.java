package com.payflow.lab;

import com.payflow.lab.repo.AccountRepository;
import com.payflow.lab.repo.LedgerEntryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Mostra o SQL que o Hibernate GERA (logging.level.org.hibernate.SQL=debug) para locks, projeções e paginação com coleção. */
@SpringBootTest(properties = "logging.level.org.hibernate.SQL=debug")
class SqlShapeIT {

    @Autowired JdbcClient jdbc;
    @Autowired AccountRepository accounts;
    @Autowired LedgerEntryRepository ledger;
    @Autowired PlatformTransactionManager tm;

    @BeforeEach
    void seed() {
        LabSupport.bigData(jdbc);
    }

    private void mark(String s) {
        System.out.println(">>>>> " + s);
    }

    @Test
    void showSql() {
        UUID acc = jdbc.sql("SELECT id FROM accounts ORDER BY id LIMIT 1").query(UUID.class).single();
        UUID acc2 = jdbc.sql("SELECT id FROM accounts ORDER BY id OFFSET 1 LIMIT 1").query(UUID.class).single();
        var tx = new TransactionTemplate(tm);
        mark("PESSIMISTIC_WRITE ordenado");
        tx.executeWithoutResult(s -> accounts.lockAllOrdered(List.of(acc, acc2)));
        mark("PESSIMISTIC_WRITE + lock.timeout=0 (NOWAIT)");
        tx.executeWithoutResult(s -> accounts.lockByIdNowait(acc));
        mark("entidade completa");
        tx.executeWithoutResult(s -> ledger.findByAccount_IdOrderByCreatedAtDescIdDesc(acc, Limit.of(5)));
        mark("projeção por interface");
        tx.executeWithoutResult(s -> ledger.findByAccount_IdOrderByCreatedAtDesc(acc, Limit.of(5)).forEach(v -> v.getAmount()));
        mark("record (select new)");
        tx.executeWithoutResult(s -> ledger.lines(acc, Limit.of(5)));
        mark("Page + join fetch de coleção");
        tx.executeWithoutResult(s -> accounts.pageWithEntries(PageRequest.of(0, 3)));
        mark("fim");
    }
}
