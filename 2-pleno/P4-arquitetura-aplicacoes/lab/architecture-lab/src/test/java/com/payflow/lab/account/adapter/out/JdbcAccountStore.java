package com.payflow.lab.account.adapter.out;

import com.payflow.lab.account.application.port.out.AccountStore;
import com.payflow.lab.account.domain.Account;
import com.payflow.lab.shared.Money;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** SEGUNDO adaptador da mesma porta (só nos testes): prova que a troca não toca o domínio nem o caso de uso. */
public class JdbcAccountStore implements AccountStore {

    private final JdbcClient jdbc;

    public JdbcAccountStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Account save(Account account) {
        int updated = jdbc.sql("update accounts set owner = ?, balance = ? where id = ?")
                .params(account.owner(), account.balance().amount(), account.id()).update();
        if (updated == 0) {
            jdbc.sql("insert into accounts (id, owner, balance) values (?, ?, ?)")
                    .params(account.id(), account.owner(), account.balance().amount()).update();
        }
        return account;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jdbc.sql("select id, owner, balance from accounts where id = ?").param(id)
                .query((rs, n) -> new Account(rs.getObject("id", UUID.class), rs.getString("owner"),
                        new Money(rs.getBigDecimal("balance"))))
                .optional();
    }
}
