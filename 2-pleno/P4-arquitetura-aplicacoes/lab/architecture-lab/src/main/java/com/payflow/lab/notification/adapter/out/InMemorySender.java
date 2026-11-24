package com.payflow.lab.notification.adapter.out;

import com.payflow.lab.notification.application.port.out.Sender;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class InMemorySender implements Sender {

    public record Sent(UUID account, String message) { }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(UUID account, String message) {
        sent.add(new Sent(account, message));
    }

    public List<Sent> sent() {
        return List.copyOf(sent);
    }
}
