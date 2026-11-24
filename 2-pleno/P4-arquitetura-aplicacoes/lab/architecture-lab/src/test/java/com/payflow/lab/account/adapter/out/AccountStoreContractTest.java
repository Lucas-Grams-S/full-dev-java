package com.payflow.lab.account.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payflow.lab.account.application.port.out.AccountStore;
import com.payflow.lab.account.domain.Account;
import com.payflow.lab.shared.Money;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * LSP na prática: o CONTRATO da porta, escrito uma vez, roda contra TODA implementação.
 * Quem não cumpre o contrato não é substituível, mesmo compilando.
 */
abstract class AccountStoreContract {

    abstract AccountStore store();

    void saveThenFindReturnsTheSameAccount() {
        var account = new Account(UUID.randomUUID(), "ana", Money.of("10.00"));
        store().save(account);
        assertThat(store().findById(account.id())).isPresent();
    }

    void unknownIdReturnsEmptyNotNull() {
        Optional<Account> found = store().findById(UUID.randomUUID());
        assertThat(found).isNotNull().isEmpty();
    }
}

class InMemoryAccountStoreContractTest extends AccountStoreContract {
    private final AccountStore store = new InMemoryAccountStore();

    @Override AccountStore store() { return store; }

    @Test void saveThenFind() { saveThenFindReturnsTheSameAccount(); }
    @Test void unknownId() { unknownIdReturnsEmptyNotNull(); }
}

class JdbcAccountStoreContractTest extends AccountStoreContract {
    private final AccountStore store;

    JdbcAccountStoreContractTest() {
        var db = new org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder()
                .setType(org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2)
                .setName("contract-" + UUID.randomUUID()).addScript("classpath:schema.sql").build();
        this.store = new JdbcAccountStore(org.springframework.jdbc.core.simple.JdbcClient.create(db));
    }

    @Override AccountStore store() { return store; }

    @Test void saveThenFind() { saveThenFindReturnsTheSameAccount(); }
    @Test void unknownId() { unknownIdReturnsEmptyNotNull(); }
}

/** Uma "implementação" que compila e quebra o contrato: devolve null onde a porta promete Optional. */
class BrokenAccountStoreContractTest extends AccountStoreContract {
    private final AccountStore store = new AccountStore() {
        public Account save(Account account) { return account; }
        public Optional<Account> findById(UUID id) { return null; }
    };

    @Override AccountStore store() { return store; }

    @Test
    void theContractCatchesTheViolation() {
        assertThatThrownBy(this::unknownIdReturnsEmptyNotNull).isInstanceOf(AssertionError.class);
    }
}
