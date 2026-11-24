package com.payflow.lab.sec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** A regra da decisão 56 (J8): health/info públicos, o resto do Actuator só ADMIN, o resto da API autenticado. */
@SpringBootApplication
public class SecApp {
    public static void main(String[] args) {
        SpringApplication.run(SecApp.class, args);
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").hasAnyRole("METRICS", "ADMIN")   // o scrape tem papel PRÓPRIO, só de leitura de métricas
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .csrf(c -> c.disable());
        return http.build();
    }

    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(User.withUsername("ops").password("{noop}lab-only").roles("ADMIN").build(),
                User.withUsername("ana").password("{noop}lab-only").roles("USER").build(),
                User.withUsername("scraper").password("{noop}lab-only").roles("METRICS").build());
    }
}

@RestController
class Hello {
    @GetMapping("/api/v1/hello")
    String hello() {
        return "hi";
    }
}
