package com.payflow.lab.antifraud.application;

import com.payflow.lab.antifraud.AntifraudApi;
import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.TransferCompleted;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class FraudReview implements AntifraudApi {

    private static final Money LIMIT = Money.of("1000.00");
    private final List<UUID> flagged = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener
    void on(TransferCompleted event) {
        if (LIMIT.isLessThan(event.amount())) {
            flagged.add(event.transferId());
        }
    }

    @Override
    public List<UUID> flaggedTransfers() {
        return List.copyOf(flagged);
    }
}
