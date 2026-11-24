package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.lab.domain.Account;
import com.payflow.lab.domain.LedgerEntry;
import com.payflow.lab.repo.AccountRepository;
import com.payflow.lab.repo.LedgerEntryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** P2.08: o mesmo trabalho (20 000 lançamentos) por JPA saveAll, JPA com flush/clear e JdbcTemplate.batchUpdate. */
abstract class BatchSupport {

    static final int N = 20_000;

    @Autowired JdbcClient jdbc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManagerFactory emf;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;
    @Autowired AccountRepository accounts;
    @Autowired LedgerEntryRepository ledger;

    private static final int REPS = 4;

    private void jpaSaveAll() {
        new TransactionTemplate(tm).executeWithoutResult(s -> {
            Account a = accounts.getReferenceById(LabSupport.A1);
            List<LedgerEntry> list = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                list.add(new LedgerEntry(a, null, "CREDIT", BigDecimal.ONE, Instant.now()));
            }
            ledger.saveAll(list);
        });
    }

    private void jpaFlushClear() {
        new TransactionTemplate(tm).executeWithoutResult(s -> {
            Account a = accounts.getReferenceById(LabSupport.A1);
            for (int i = 0; i < N; i++) {
                em.persist(new LedgerEntry(a, null, "CREDIT", BigDecimal.ONE, Instant.now()));
                if ((i + 1) % 50 == 0) {
                    em.flush();
                    em.clear();                            // não deixa 20 000 entidades na memória do contexto
                }
            }
        });
    }

    private void jdbcBatch() {
        List<Object[]> rows = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            rows.add(new Object[] {java.util.UUID.randomUUID(), LabSupport.A1, "CREDIT", BigDecimal.ONE, Timestamp.from(Instant.now())});
        }
        new TransactionTemplate(tm).executeWithoutResult(s ->
                jdbcTemplate.batchUpdate("INSERT INTO ledger_entries (id, account_id, type, amount, created_at) VALUES (?, ?, ?, ?, ?)", rows));
    }

    private String time(String name, Runnable r) {
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        List<Long> ms = new ArrayList<>();
        long prepared = 0;
        for (int i = 0; i < REPS; i++) {
            LabSupport.reset(jdbc, "1000.00");
            jdbc.sql("CHECKPOINT").update();                       // começa cada repetição "limpa" para o banco (reduz ruído)
            st.clear();
            long t0 = System.nanoTime();
            r.run();
            ms.add((System.nanoTime() - t0) / 1_000_000);
            prepared = st.getPrepareStatementCount();
            assertThat(jdbc.sql("SELECT count(*) FROM ledger_entries").query(Long.class).single()).isEqualTo(N);
        }
        List<Long> sorted = ms.stream().sorted().toList();
        return String.format("%-44s tempos(ms)=%s  mín=%d  PreparedStatements=%d", name, ms, sorted.get(0), prepared);
    }

    void runAll(String label) {
        System.out.println("[" + label + "] " + time("JPA saveAll(" + N + ")", this::jpaSaveAll));
        System.out.println("[" + label + "] " + time("JPA persist + flush/clear a cada 50", this::jpaFlushClear));
        System.out.println("[" + label + "] " + time("JdbcTemplate.batchUpdate", this::jdbcBatch));
    }
}
