package com.payflow.lab.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class GatewayLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayLabApplication.class, args);
    }

    /** Resposta de contingência do próprio gateway (rota {@code forward:/fallback/notifications}). */
    @RestController
    static class FallbackController {
        @GetMapping("/fallback/notifications")
        String fallback() {
            return "{\"source\":\"gateway-fallback\"}";
        }
    }
}
