package com.payflow.lab;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RabbitRetryTopology {
    @Bean DirectExchange retryDlx() { return new DirectExchange("lab.retry.dlx", true, false); }
    @Bean Queue retryDlq() { return QueueBuilder.durable("lab.retry.dlq").build(); }
    @Bean Queue retryWork() {
        return QueueBuilder.durable("lab.retry.work").deadLetterExchange("lab.retry.dlx").deadLetterRoutingKey("dead").build();
    }
    @Bean Binding retryDlqBinding() { return BindingBuilder.bind(retryDlq()).to(retryDlx()).with("dead"); }
}
