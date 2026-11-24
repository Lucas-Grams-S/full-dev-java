package lab;

import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorityAuthorizationManager;

/** J8.04: hasRole('ROLE_ADMIN') é um erro: o prefixo ROLE_ é acrescentado sozinho. */
class RolePrefixTest {

    @Test
    void print() {
        Throwable t = org.assertj.core.api.Assertions.catchThrowable(() -> AuthorityAuthorizationManager.hasRole("ROLE_ADMIN"));
        System.out.println("[prefixo] hasRole(\"ROLE_ADMIN\") -> " + (t == null ? "aceito" : t.getClass().getSimpleName() + ": " + t.getMessage()));
        System.out.println("[prefixo] hasRole(\"ADMIN\")      -> " + AuthorityAuthorizationManager.hasRole("ADMIN").getClass().getSimpleName());
    }
}
