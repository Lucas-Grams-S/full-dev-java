package com.payflow.lab.transfer.application.port.out;

import com.payflow.lab.transfer.TransferCompleted;

public interface DomainEvents {
    void publish(TransferCompleted event);
}
