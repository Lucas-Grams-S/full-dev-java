package com.payflow.transfer.application;

import com.payflow.transfer.application.port.out.EventPublisher;
import com.payflow.transfer.domain.Transfer;

public class TransferService {
    private final EventPublisher events;

    public TransferService(EventPublisher events) { this.events = events; }

    public void completed(Transfer t) {
        events.publish("TransferCompleted", t.id().toString(), "{\"transferId\":\"" + t.id() + "\"}");
    }
}
