package com.payflow.lab.obs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ObsLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(ObsLabApplication.class, args);
    }
}
