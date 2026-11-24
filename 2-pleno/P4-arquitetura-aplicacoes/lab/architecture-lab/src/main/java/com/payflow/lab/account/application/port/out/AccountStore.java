package com.payflow.lab.account.application.port.out;

import com.payflow.lab.account.domain.Account;
import java.util.Optional;
import java.util.UUID;

/** Porta de SAÍDA: o domínio diz o que precisa; quem implementa é um adaptador. */
public interface AccountStore {
    Account save(Account account);
    Optional<Account> findById(UUID id);
}
