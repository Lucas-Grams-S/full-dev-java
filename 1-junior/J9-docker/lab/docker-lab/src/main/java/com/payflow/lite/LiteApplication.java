package com.payflow.lite;

import com.payflow.lite.config.PayflowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** PayFlow Lite: o minimo para praticar Docker (banco, Flyway, Actuator e um segredo obrigatorio). */
@SpringBootApplication
@EnableConfigurationProperties(PayflowProperties.class)
public class LiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteApplication.class, args);
    }
}
