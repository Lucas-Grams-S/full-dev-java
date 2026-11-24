package com.payflow.lab.account.application;

import com.payflow.lab.account.AccountApi;
import com.payflow.lab.account.application.port.out.AccountStore;
import com.payflow.lab.account.domain.Account;
import com.payflow.lab.shared.Money;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class AccountService implements AccountApi {

    private final AccountStore store;

    AccountService(AccountStore store) {
        this.store = store;
    }

    @Override
    public UUID open(String owner, Money initialBalance) {
        var account = new Account(UUID.randomUUID(), owner, initialBalance);
        return store.save(account).id();
    }

    @Override
    @Transactional(readOnly = true)
    public Money balanceOf(UUID accountId) {
        return load(accountId).balance();
    }

    @Override
    public void withdraw(UUID accountId, Money amount) {
        var account = load(accountId);
        account.withdraw(amount);
        store.save(account);
    }

    @Override
    public void deposit(UUID accountId, Money amount) {
        var account = load(accountId);
        account.deposit(amount);
        store.save(account);
    }

    private Account load(UUID id) {
        return store.findById(id).orElseThrow(() -> new IllegalArgumentException("account not found: " + id));
    }
}
