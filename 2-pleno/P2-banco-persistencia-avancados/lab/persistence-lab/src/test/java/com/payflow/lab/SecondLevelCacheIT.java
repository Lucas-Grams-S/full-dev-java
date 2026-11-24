package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.lab.domain.User;
import com.payflow.lab.repo.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** P2.08: cache de segundo nível do Hibernate (JCache + Caffeine) em uma entidade de leitura (User). */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=true",
        "spring.jpa.properties.hibernate.cache.region.factory_class=jcache",
        "spring.jpa.properties.hibernate.javax.cache.provider=com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider",
        "spring.jpa.properties.hibernate.javax.cache.missing_cache_strategy=create"})
class SecondLevelCacheIT {

    @Autowired UserRepository users;
    @Autowired JdbcClient jdbc;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager tm;

    @BeforeEach
    void seed() {
        LabSupport.reset(jdbc, "1000.00");
    }

    @Test
    void secondReadsInNewSessionsComeFromTheSharedCache() {
        Statistics st = emf.unwrap(SessionFactory.class).getStatistics();
        emf.getCache().evictAll();
        st.clear();
        for (int i = 0; i < 3; i++) {
            new TransactionTemplate(tm).executeWithoutResult(s -> users.findById(LabSupport.ANA).orElseThrow().getFullName());
        }
        System.out.printf("L2 ligado: 3 leituras de User em 3 transações -> %d SELECT, L2 hits=%d, misses=%d, puts=%d%n",
                st.getPrepareStatementCount(), st.getSecondLevelCacheHitCount(), st.getSecondLevelCacheMissCount(), st.getSecondLevelCachePutCount());
        assertThat(st.getPrepareStatementCount()).isEqualTo(1);
        assertThat(st.getSecondLevelCacheHitCount()).isEqualTo(2);

        // alteração POR FORA do Hibernate (outra aplicação, script, JdbcClient): o L2 não sabe
        jdbc.sql("UPDATE users SET full_name = 'Ana (alterada por fora)' WHERE id = :id").param("id", LabSupport.ANA).update();
        String seen = new TransactionTemplate(tm).execute(s -> users.findById(LabSupport.ANA).orElseThrow().getFullName());
        System.out.println("depois do UPDATE por fora, o L2 devolve: " + seen);
        assertThat(seen).isEqualTo("Ana");                      // dado velho até a expiração/invalidação
    }
}
