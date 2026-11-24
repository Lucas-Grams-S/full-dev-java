package com.payflow.lab.transfer.adapter.out;

import com.payflow.lab.transfer.TransferCompleted;
import com.payflow.lab.transfer.application.port.out.DomainEvents;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringDomainEvents implements DomainEvents {

    private final ApplicationEventPublisher publisher;

    SpringDomainEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(TransferCompleted event) {
        publisher.publishEvent(event);
    }
}
