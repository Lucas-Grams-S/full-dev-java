package lab;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** J8.04: a PRIMEIRA regra que casa vale. Aqui a regra geral vem antes da regra do admin: ela a "sombreia", em silêncio. */
@SpringBootTest(classes = ShadowedRuleTest.App.class)
@AutoConfigureMockMvc
class ShadowedRuleTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableWebSecurity
    @RestController
    static class App {
        @GetMapping("/admin/secret") String secret() { return "admin-secret"; }

        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/**").authenticated()                    // ❌ regra geral PRIMEIRO
                            .requestMatchers("/admin/**").hasRole("ADMIN"))            // nunca é alcançada para quem passou na de cima
                    .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(roles = "USER")
    void aPlainUserReachesTheAdminPage() throws Exception {
        int code = mvc.perform(get("/admin/secret")).andReturn().getResponse().getStatus();
        System.out.println("[ordem] USER em /admin/secret com a regra geral primeiro -> " + code + "  (deveria ser 403)");
        mvc.perform(get("/admin/secret")).andExpect(status().isOk());
    }
}
