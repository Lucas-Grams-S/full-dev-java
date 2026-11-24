package lab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;

/** J8.04: SEM @EnableMethodSecurity, o @PreAuthorize é ignorado em silêncio. */
@SpringBootTest(classes = MethodSecurityIgnoredTest.Config.class)
class MethodSecurityIgnoredTest {

    static class Reports {
        @PreAuthorize("hasRole('ADMIN')")
        public String adminOnly() { return "segredo"; }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Config {
        @Bean Reports reports() { return new Reports(); }
    }

    @Autowired Reports reports;

    @Test
    @WithMockUser(roles = "USER")
    void annotationIsIgnoredSilently() {
        System.out.println("[método] SEM @EnableMethodSecurity, USER chama adminOnly() -> " + reports.adminOnly());
    }
}
