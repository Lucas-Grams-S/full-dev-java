package com.payflow.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.support.KeycloakFixture;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** Refresh token com rotação: cada refresh devolve um token novo; reusar o antigo é sinal de roubo. */
class RefreshTokenRotationIT {

    @BeforeAll
    static void up() {
        KeycloakFixture.requireRunning();
    }

    private static KeycloakFixture.Result refresh(String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", "payflow-lab-direct");
        form.put("refresh_token", refreshToken);
        return KeycloakFixture.token(form);
    }

    @Test
    void everyRefreshReturnsANewRefreshToken_andTheOldOneStopsWorking() {
        JsonNode first = KeycloakFixture.userTokens("bob@example.com", "bob-lab-pass-12", "openid");
        String r1 = first.get("refresh_token").asText();

        KeycloakFixture.Result second = refresh(r1);
        assertThat(second.status()).isEqualTo(200);
        String r2 = second.body().get("refresh_token").asText();
        assertThat(r2).isNotEqualTo(r1);
        System.out.println("T10.05 refresh #1 ok; new refresh token differs=" + !r1.equals(r2)
                + " access token expires_in=" + second.body().get("expires_in"));

        KeycloakFixture.Result replay = refresh(r1);                       // o atacante usa o refresh token ROUBADO (já gasto)
        System.out.println("T10.05 replay of the spent refresh token -> " + replay.status() + " " + replay.body());
        assertThat(replay.status()).isEqualTo(400);
        assertThat(replay.body().get("error").asText()).isEqualTo("invalid_grant");

        KeycloakFixture.Result legit = refresh(r2);                        // e o usuário legítimo, com o token novo?
        System.out.println("T10.05 the legitimate newest refresh token after the replay -> " + legit.status() + " " + legit.body());
    }
}
