package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.payflow.lab.domain.Account;
import com.payflow.lab.domain.LedgerEntry;
import com.payflow.lab.domain.Transfer;
import com.payflow.lab.repo.AccountRepository;
import com.payflow.lab.repo.EntryLine;
import com.payflow.lab.repo.LedgerEntryRepository;
import com.payflow.lab.repo.TransferRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** P2.07: quantos comandos SQL cada estratégia de leitura gera (Hibernate Statistics) e o que cada uma carrega. */
@SpringBootTest
class FetchIT {

    @Autowired JdbcClient jdbc;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager tm;
    @Autowired TransferRepository transfers;
    @Autowired AccountRepository accounts;
    @Autowired LedgerEntryRepository ledger;

    @BeforeEach
    void seed() {
        LabSupport.bigData(jdbc);
    }

    /** Executa dentro de UMA transação (sessão aberta, como no @Transactional do serviço) e conta os comandos SQL preparados. */
    private <T> T counting(String label, Supplier<T> body) {
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        st.clear();
        T result = new TransactionTemplate(tm).execute(s -> body.get());
        System.out.printf("%-48s -> %2d comandos SQL%n", label, st.getPrepareStatementCount());
        return result;
    }

    @Test
    void nPlusOne_versus_entityGraph() {
        counting("transfers ingênuo + nomes dos donos (20 transf.)", () -> transfers.findAllByOrderById(PageRequest.of(0, 20)).stream()
                .map(t -> t.getSource().getOwner().getFullName() + t.getTarget().getOwner().getFullName()).toList());
        counting("transfers @EntityGraph(source.owner, target.owner)", () -> transfers.findAllWithGraphByOrderById(PageRequest.of(0, 20)).stream()
                .map(t -> t.getSource().getOwner().getFullName() + t.getTarget().getOwner().getFullName()).toList());
    }

    @Test
    void collectionFetch_joinFetch_multipleBag_andInMemoryPagination() {
        UUID owner = jdbc.sql("SELECT id FROM users ORDER BY email LIMIT 1").query(UUID.class).single();
        List<Account> list = counting("join fetch de UMA coleção (entries), 1 dono", () -> accounts.withEntriesOfOwner(owner));
        System.out.println("   conta(s): " + list.size() + ", lançamentos da 1ª: " + list.get(0).getEntries().size());

        Throwable t = catchThrowable(() -> accounts.withEntriesAndSent(accounts.findAll().get(0).getId()));
        System.out.println("   DUAS coleções (entries + sent) -> " + t.getClass().getName());
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        System.out.println("   causa raiz: " + root.getClass().getName() + ": " + root.getMessage());

        var page = counting("Page + join fetch de coleção (paginação!)", () -> accounts.pageWithEntries(PageRequest.of(0, 3)));
        System.out.println("   página com " + page.getContent().size() + " contas de " + page.getTotalElements());
        assertThat(page.getContent()).hasSize(3);
    }

    @Test
    void projections_loadOnlyWhatIsNeeded() {
        UUID acc = accounts.findAll().get(0).getId();
        List<LedgerEntry> entities = counting("entidade (LedgerEntry completa) x 20", () -> ledger.findByAccount_IdOrderByCreatedAtDescIdDesc(acc, Limit.of(20)));
        List<?> view = counting("projeção por interface x 20", () -> ledger.findByAccount_IdOrderByCreatedAtDesc(acc, Limit.of(20)).stream().map(v -> v.getAmount()).toList());
        List<EntryLine> lines = counting("record via JPQL (select new) x 20", () -> ledger.lines(acc, Limit.of(20)));
        assertThat(entities).hasSize(20);
        assertThat(view).hasSize(20);
        assertThat(lines).hasSize(20);
        EntryLine last = lines.get(19);
        List<EntryLine> next = counting("keyset: próximos 20 depois do último visto", () -> ledger.after(acc, last.createdAt(), last.id(), Limit.of(20)));
        System.out.println("   keyset devolveu " + next.size() + " (restam 10 dos 30)");
        assertThat(next).hasSize(10);
        assertThat(next.get(0).createdAt()).isBeforeOrEqualTo(last.createdAt());
    }

    @Test
    void lazyOutsideTransaction_failsWithLazyInitializationException() {
        List<Transfer> list = transfers.findAllByOrderById(PageRequest.of(0, 5));      // sem @Transactional e open-in-view=false
        Throwable t = catchThrowable(() -> list.get(0).getSource().getOwner().getFullName());
        System.out.println("fora da transação: " + t.getClass().getName() + ": " + t.getMessage());
        assertThat(t).isInstanceOf(org.hibernate.LazyInitializationException.class);
    }
}
