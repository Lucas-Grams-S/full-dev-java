package com.payflow.lab.notification.application.port.out;

import java.util.UUID;

public interface Sender {
    void send(UUID account, String message);
}
