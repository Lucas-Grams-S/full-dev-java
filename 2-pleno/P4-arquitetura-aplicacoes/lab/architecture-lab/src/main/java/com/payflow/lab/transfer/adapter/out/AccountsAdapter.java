package com.payflow.lab.transfer.adapter.out;

import com.payflow.lab.account.AccountApi;
import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.application.port.out.Accounts;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Único lugar do módulo transfer que conhece o módulo account. */
@Component
class AccountsAdapter implements Accounts {

    private final AccountApi accountApi;

    AccountsAdapter(AccountApi accountApi) {
        this.accountApi = accountApi;
    }

    @Override
    public void debit(UUID account, Money amount) {
        accountApi.withdraw(account, amount);
    }

    @Override
    public void credit(UUID account, Money amount) {
        accountApi.deposit(account, amount);
    }
}
