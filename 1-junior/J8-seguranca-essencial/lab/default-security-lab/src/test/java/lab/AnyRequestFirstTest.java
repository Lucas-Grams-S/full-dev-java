package lab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** J8.04: já o anyRequest() fora do fim é barrado na subida da aplicação. */
class AnyRequestFirstTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableWebSecurity
    static class App {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated()
                    .requestMatchers("/admin/**").hasRole("ADMIN"));
            return http.build();
        }
    }

    @Test
    void startupFails() {
        Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() ->
                new SpringApplicationBuilder(App.class).web(WebApplicationType.SERVLET).properties("server.port=0").run().close());
        Throwable root = failure;
        while (root != null && root.getCause() != null) {
            root = root.getCause();
        }
        System.out.println("[ordem] anyRequest() antes de requestMatchers -> " + root.getClass().getSimpleName() + ": " + root.getMessage());
        assertThat(failure).isNotNull();
    }
}
