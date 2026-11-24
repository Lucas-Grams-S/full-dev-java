package com.payflow.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.support.KeycloakFixture;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** As políticas que o PayFlow antes implementava à mão (LoginThrottle, política de senha, J8) agora são do realm. */
class RealmPolicyIT {

    private static final String CAROL = "33333333-3333-4333-8333-333333333333";

    @BeforeAll
    static void up() {
        KeycloakFixture.requireRunning();
    }

    @BeforeEach
    void unlockCarol() {
        KeycloakFixture.admin("DELETE", "/attack-detection/brute-force/users/" + CAROL, null);
    }

    private static KeycloakFixture.Result login(String password) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", "payflow-lab-direct");
        form.put("username", "carol@example.com");
        form.put("password", password);
        return KeycloakFixture.token(form);
    }

    @Test
    void bruteForceDetection_locksTheAccountAfterFiveFailures_evenForTheRightPassword() {
        for (int i = 1; i <= 5; i++) {
            KeycloakFixture.Result r = login("wrong-password-" + i);
            System.out.println("T10.06 failure #" + i + " -> " + r.status() + " " + r.body().path("error_description").asText());
            assertThat(r.status()).isEqualTo(400);
        }
        KeycloakFixture.Result locked = login("carol-lab-pass-1");              // a senha CERTA, depois do bloqueio
        System.out.println("T10.06 right password after 5 failures -> " + locked.status() + " " + locked.body());
        assertThat(locked.status()).isNotEqualTo(200);
    }

    @Test
    void theRightPasswordWorksBeforeTheLimit() {
        assertThat(login("wrong-1").status()).isEqualTo(400);
        assertThat(login("carol-lab-pass-1").status()).isEqualTo(200);
    }

    @Test
    void passwordPolicy_rejectsAShortPassword_andOneEqualToTheUsername() {
        int shortPassword = resetPassword("short");
        int sameAsUser = resetPassword("carol@example.com");
        int good = resetPassword("carol-lab-pass-1");
        System.out.println("T10.06 reset 'short' -> " + shortPassword + "; reset equal to username -> " + sameAsUser + "; reset ok -> " + good);
        assertThat(shortPassword).isEqualTo(400);
        assertThat(sameAsUser).isEqualTo(400);
        assertThat(good).isEqualTo(204);
    }

    private static int resetPassword(String value) {
        return KeycloakFixture.admin("PUT", "/users/" + CAROL + "/reset-password",
                "{\"type\":\"password\",\"value\":\"" + value + "\",\"temporary\":false}");
    }
}
