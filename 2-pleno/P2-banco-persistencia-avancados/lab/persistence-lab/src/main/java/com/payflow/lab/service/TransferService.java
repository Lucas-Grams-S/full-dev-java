package com.payflow.lab.service;

import com.payflow.lab.domain.Account;
import com.payflow.lab.domain.InsufficientFundsException;
import com.payflow.lab.domain.LedgerEntry;
import com.payflow.lab.domain.Transfer;
import com.payflow.lab.repo.AccountRepository;
import com.payflow.lab.repo.LedgerEntryRepository;
import com.payflow.lab.repo.TransferRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quatro maneiras de proteger a mesma transferência (P2.02 e P2.03). Todas escrevem os 2 lançamentos (regra 5). */
@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerEntryRepository ledger;
    private final JdbcClient jdbc;

    public TransferService(AccountRepository accounts, TransferRepository transfers, LedgerEntryRepository ledger, JdbcClient jdbc) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.ledger = ledger;
        this.jdbc = jdbc;
    }

    /** 1. Otimista: findById + @Version. Colisão = ObjectOptimisticLockingFailureException no commit. */
    @Transactional
    public Transfer transferOptimistic(UUID from, UUID to, BigDecimal amount, String key, long pauseMs) {
        Account source = accounts.findById(from).orElseThrow();
        Account target = accounts.findById(to).orElseThrow();
        pause(pauseMs);                                       // alarga a janela (num sistema real: o resto do processamento)
        return move(source, target, amount, key);
    }

    /** 2. Pessimista ORDENADO: SELECT ... FOR NO KEY UPDATE ... ORDER BY id. */
    @Transactional
    public Transfer transferPessimistic(UUID from, UUID to, BigDecimal amount, String key, long pauseMs) {
        Map<UUID, Account> locked = accounts.lockAllOrdered(List.of(from, to)).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        pause(pauseMs);
        return move(locked.get(from), locked.get(to), amount, key);
    }

    /** 3. Pessimista NA ORDEM DOS ARGUMENTOS: provoca deadlock quando duas transferências cruzam. */
    @Transactional
    public Transfer transferPessimisticUnordered(UUID from, UUID to, BigDecimal amount, String key, long pauseMs) {
        Account source = accounts.lockById(from).orElseThrow();
        pause(pauseMs);
        Account target = accounts.lockById(to).orElseThrow();
        return move(source, target, amount, key);
    }

    /** 4. UPDATE condicional atômico (sem carregar entidade): balance = balance - ? WHERE balance >= ?, na ordem do id. */
    @Transactional
    public UUID transferAtomicSql(UUID from, UUID to, BigDecimal amount, String key, long pauseMs) {
        UUID first = from.compareTo(to) < 0 ? from : to;
        UUID second = first.equals(from) ? to : from;
        for (UUID id : List.of(first, second)) {
            boolean debit = id.equals(from);
            int rows = jdbc.sql(debit
                            ? "UPDATE accounts SET balance = balance - :amt, version = version + 1 WHERE id = :id AND balance >= :amt"
                            : "UPDATE accounts SET balance = balance + :amt, version = version + 1 WHERE id = :id")
                    .param("amt", amount).param("id", id).update();
            if (rows == 0) {
                throw debit ? new InsufficientFundsException(id) : new IllegalStateException("account not found: " + id);
            }
            if (id.equals(first)) {
                pause(pauseMs);
            }
        }
        UUID transferId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.sql("INSERT INTO transfers (id, source_account_id, target_account_id, amount, status, idempotency_key, created_at) "
                        + "VALUES (:id, :s, :t, :a, 'COMPLETED', :k, :now)")
                .param("id", transferId).param("s", from).param("t", to).param("a", amount).param("k", key)
                .param("now", java.sql.Timestamp.from(now)).update();
        for (Object[] e : new Object[][] {{from, "DEBIT"}, {to, "CREDIT"}}) {
            jdbc.sql("INSERT INTO ledger_entries (account_id, transfer_id, type, amount, created_at) VALUES (:acc, :tr, :ty, :am, :now)")
                    .param("acc", e[0]).param("tr", transferId).param("ty", e[1]).param("am", amount)
                    .param("now", java.sql.Timestamp.from(now)).update();
        }
        return transferId;
    }

    private Transfer move(Account source, Account target, BigDecimal amount, String key) {
        source.withdraw(amount);
        target.deposit(amount);
        Instant now = Instant.now();
        Transfer t = transfers.save(new Transfer(source, target, amount, key, now));
        ledger.save(new LedgerEntry(source, t.getId(), "DEBIT", amount, now));
        ledger.save(new LedgerEntry(target, t.getId(), "CREDIT", amount, now));
        return t;
    }

    private static void pause(long ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
