package com.payflow.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.Ordered;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableCaching
// O retry precisa ficar FORA da transação: a ordem do proxy de retry vem antes (menor número) do proxy @Transactional.
@EnableResilientMethods(order = Ordered.HIGHEST_PRECEDENCE)
public class LabApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabApplication.class, args);
    }
}
