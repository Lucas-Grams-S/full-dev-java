package com.payflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payflow.security.PayflowUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

/** J8.02: o AuthenticationManager sozinho, sem HTTP: o que entra e o que sai. */
@SpringBootTest
@ActiveProfiles("test")
class AuthenticationManagerTest {

    @Autowired AuthenticationManager manager;

    Authentication attempt(String email, String password) {
        return manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
    }

    @Test
    void rightPasswordGivesAnAuthenticatedPrincipalWithoutTheHash() {
        Authentication result = attempt("admin@payflow.dev", "Payflow#Dev2026");
        PayflowUserDetails principal = (PayflowUserDetails) result.getPrincipal();
        System.out.println("[auth] authenticated=" + result.isAuthenticated() + " name=" + result.getName()
                + " authorities=" + result.getAuthorities() + " hashDepoisDoLogin=" + principal.getPassword());
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN", "FACTOR_PASSWORD");     // o Security 7 acrescenta a autoridade do fator usado no login
        assertThat(principal.getPassword()).isNull();                       // eraseCredentials
    }

    @Test
    void wrongPasswordAndUnknownEmailThrowTheSameException() {
        Throwable wrong = org.assertj.core.api.Assertions.catchThrowable(() -> attempt("ana@payflow.dev", "wrong-wrong-wrong"));
        Throwable unknown = org.assertj.core.api.Assertions.catchThrowable(() -> attempt("nobody@payflow.dev", "wrong-wrong-wrong"));
        System.out.println("[auth] senha errada  -> " + wrong.getClass().getSimpleName() + ": " + wrong.getMessage());
        System.out.println("[auth] e-mail ausente -> " + unknown.getClass().getSimpleName() + ": " + unknown.getMessage());
        assertThat(wrong).isInstanceOf(BadCredentialsException.class);
        assertThat(unknown).isInstanceOf(BadCredentialsException.class).hasMessage(wrong.getMessage());
    }

    @Test
    void anEmailWithDifferentCaseIsTheSameUser() {
        assertThat(attempt("  ANA@payflow.dev ", "Payflow#Dev2026").isAuthenticated()).isTrue();
    }

    @Test
    void blankCredentialsAreRefused() {
        assertThatThrownBy(() -> attempt("ana@payflow.dev", "")).isInstanceOf(BadCredentialsException.class);
    }
}
