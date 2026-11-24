package com.payflow.lab.transfer.application.port.out;

import com.payflow.lab.shared.Money;
import java.util.UUID;

/** Porta de saída com a LINGUAGEM do módulo transfer (anticorruption layer): ele não sabe o que é AccountApi. */
public interface Accounts {
    void debit(UUID account, Money amount);
    void credit(UUID account, Money amount);
}
