package com.payflow.lab.notification.application;

import com.payflow.lab.notification.application.port.out.Sender;
import com.payflow.lab.transfer.TransferCompleted;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Reage a um evento de OUTRO módulo sem que o transfer saiba que ele existe. */
@Component
class NotifyOnTransfer {

    private final Sender sender;

    NotifyOnTransfer(Sender sender) {
        this.sender = sender;
    }

    @ApplicationModuleListener
    void on(TransferCompleted event) {
        sender.send(event.targetAccountId(), "You received " + event.amount());
    }
}
