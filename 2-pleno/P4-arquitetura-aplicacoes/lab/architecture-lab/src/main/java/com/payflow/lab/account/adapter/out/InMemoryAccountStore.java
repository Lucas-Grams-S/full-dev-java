package com.payflow.lab.account.adapter.out;

import com.payflow.lab.account.application.port.out.AccountStore;
import com.payflow.lab.account.domain.Account;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
class InMemoryAccountStore implements AccountStore {

    private final Map<UUID, Account> data = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        data.put(account.id(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(data.get(id));
    }
}
