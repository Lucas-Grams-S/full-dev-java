package com.payflow.lab.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** A transação é do CHAMADOR (como o TransferService do PayFlow): o evict acontece dentro dela, antes do commit dela. */
@Service
public class OuterTxService {

    private final BalanceService balances;

    public OuterTxService(BalanceService balances) {
        this.balances = balances;
    }

    @Transactional
    public void deposit(UUID id, BigDecimal amount, long commitDelayMs, boolean txAware) {
        if (txAware) {
            balances.depositEvictTxAware(id, amount);
        } else {
            balances.deposit(id, amount);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                try {
                    Thread.sleep(commitDelayMs);                         // o resto do trabalho/commit demora
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
}
