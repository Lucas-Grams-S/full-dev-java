package com.payflow.lab.service;

import com.payflow.lab.domain.Account;
import com.payflow.lab.domain.User;
import com.payflow.lab.repo.AccountRepository;
import com.payflow.lab.repo.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Regra de negócio "no máximo 3 contas por usuário": conta e depois insere (write skew, P2.01). */
@Service
public class AccountOpeningService {

    public static final int MAX_ACCOUNTS = 3;

    private final JdbcClient jdbc;
    private final AccountRepository accounts;
    private final UserRepository users;

    public AccountOpeningService(JdbcClient jdbc, AccountRepository accounts, UserRepository users) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.users = users;
    }

    @Transactional                                                   // READ COMMITTED (padrão do PostgreSQL)
    public void openReadCommitted(UUID ownerId, long pauseMs) {
        open(ownerId, pauseMs);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void openSerializable(UUID ownerId, long pauseMs) {
        open(ownerId, pauseMs);
    }

    private void open(UUID ownerId, long pauseMs) {
        long count = jdbc.sql("SELECT count(*) FROM accounts WHERE owner_id = :o").param("o", ownerId).query(Long.class).single();
        if (count >= MAX_ACCOUNTS) {
            throw new IllegalStateException("limit of " + MAX_ACCOUNTS + " accounts reached");
        }
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        User owner = users.getReferenceById(ownerId);
        accounts.saveAndFlush(new Account(owner, BigDecimal.ZERO));
    }
}
