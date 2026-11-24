package com.payflow.lab.resilience;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ResilienceLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResilienceLabApplication.class, args);
    }

    /** Cliente SEM timeout de leitura: o padrão do JDK. */
    @Bean("naiveClient")
    NotificationClient naiveClient(@Value("${lab.notifications.url}") String url) {
        return new NotificationClient(url, null, null);
    }

    /** Cliente com timeout de conexão (200 ms) e de leitura (300 ms). */
    @Bean("safeClient")
    NotificationClient safeClient(@Value("${lab.notifications.url}") String url) {
        return new NotificationClient(url, Duration.ofMillis(200), Duration.ofMillis(300));
    }
}
