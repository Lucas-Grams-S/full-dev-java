package com.payflow.lab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.jdbc.batch_size=50",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true"})
class BatchOnIT extends BatchSupport {
    @Test
    void run() {
        runAll("batch_size=50, order_inserts");
    }
}
