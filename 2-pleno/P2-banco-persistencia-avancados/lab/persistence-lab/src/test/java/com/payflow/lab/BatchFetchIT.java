package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.lab.repo.TransferRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** P2.07: hibernate.default_batch_fetch_size = 16 transforma o N+1 em poucas consultas "IN (…)" sem mudar o código. */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.default_batch_fetch_size=16")
class BatchFetchIT {

    @Autowired JdbcClient jdbc;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager tm;
    @Autowired TransferRepository transfers;

    @BeforeEach
    void seed() {
        LabSupport.bigData(jdbc);
    }

    @Test
    void naiveLoopWithBatchFetching() {
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        st.clear();
        new TransactionTemplate(tm).executeWithoutResult(s -> transfers.findAllByOrderById(PageRequest.of(0, 20)).forEach(
                t -> { t.getSource().getOwner().getFullName(); t.getTarget().getOwner().getFullName(); }));
        System.out.printf("default_batch_fetch_size=16: o MESMO laço ingênuo (20 transferências) -> %d comandos SQL%n", st.getPrepareStatementCount());
        assertThat(st.getPrepareStatementCount()).isLessThan(8);
    }
}
