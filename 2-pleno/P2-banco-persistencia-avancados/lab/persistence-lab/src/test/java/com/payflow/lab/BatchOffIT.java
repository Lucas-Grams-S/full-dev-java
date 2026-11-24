package com.payflow.lab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BatchOffIT extends BatchSupport {
    @Test
    void run() {
        runAll("padrão (sem batch_size)");
    }
}
