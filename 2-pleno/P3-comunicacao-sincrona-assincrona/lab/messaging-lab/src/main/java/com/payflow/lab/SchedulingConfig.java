package com.payflow.lab;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** P3.02: @Scheduled em várias instâncias; com e sem ShedLock. Só liga com lab.scheduling=true. */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
@ConditionalOnProperty("lab.scheduling")
class SchedulingConfig {
    @Bean
    LockProvider lockProvider(DataSource ds) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(ds)).usingDbTime().build());
    }

    @Component
    static class UnlockedJob {
        private final JdbcClient jdbc; private final String instance;
        UnlockedJob(JdbcClient jdbc, @Value("${lab.instance:?}") String instance) { this.jdbc = jdbc; this.instance = instance; }
        @Scheduled(fixedRate = 1000)
        void run() { jdbc.sql("insert into job_runs (job, instance) values ('unlocked', :i)").param("i", instance).update(); }
    }

    @Component
    static class LockedJob {
        private final JdbcClient jdbc; private final String instance;
        LockedJob(JdbcClient jdbc, @Value("${lab.instance:?}") String instance) { this.jdbc = jdbc; this.instance = instance; }
        @Scheduled(fixedRate = 1000)
        @SchedulerLock(name = "lab-locked-job", lockAtMostFor = "PT10S", lockAtLeastFor = "PT2S")
        void run() { jdbc.sql("insert into job_runs (job, instance) values ('locked', :i)").param("i", instance).update(); }
    }
}
