package com.payflow.lab.transfer.application;

import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.TransferCompleted;
import com.payflow.lab.transfer.application.port.in.TransferUseCase;
import com.payflow.lab.transfer.application.port.out.Accounts;
import com.payflow.lab.transfer.application.port.out.DomainEvents;
import com.payflow.lab.transfer.domain.Transfer;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TransferService implements TransferUseCase {

    private final Accounts accounts;
    private final DomainEvents events;

    TransferService(Accounts accounts, DomainEvents events) {
        this.accounts = accounts;
        this.events = events;
    }

    @Override
    @Transactional
    public UUID transfer(UUID source, UUID target, Money amount) {
        var transfer = new Transfer(source, target, amount);
        accounts.debit(source, amount);
        accounts.credit(target, amount);
        transfer.complete();
        events.publish(new TransferCompleted(transfer.id(), source, target, amount));
        return transfer.id();
    }
}
