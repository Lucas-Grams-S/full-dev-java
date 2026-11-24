package lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;

/** J8.04: COM @EnableMethodSecurity, a chamada de fora é barrada, mas a chamada interna (self-invocation) passa. */
@SpringBootTest(classes = SelfInvocationTest.Config.class)
class SelfInvocationTest {

    static class Reports {
        @PreAuthorize("hasRole('ADMIN')")
        public String adminOnly() { return "segredo"; }

        public String viaSelf() { return adminOnly(); }                  // this.adminOnly(): NÃO passa pelo proxy
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableMethodSecurity
    static class Config {
        @Bean Reports reports() { return new Reports(); }
    }

    @Autowired Reports reports;

    @Test
    @WithMockUser(roles = "USER")
    void directCallIsDeniedButSelfInvocationIsNot() {
        Throwable direct = catchThrowable(() -> reports.adminOnly());
        System.out.println("[método] COM @EnableMethodSecurity, USER chama adminOnly() direto -> " + direct.getClass().getSimpleName() + ": " + direct.getMessage());
        System.out.println("[método] COM @EnableMethodSecurity, USER chama viaSelf() (que chama adminOnly() por dentro) -> " + reports.viaSelf());
        assertThat(direct).isNotNull();
    }
}
