package com.payflow.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.client.InternalApiClient;
import com.payflow.support.KeycloakFixture;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

/** O PayFlow (Resource Server) recebendo tokens REAIS do Keycloak: a configuração do application.yml, sem nenhum atalho. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResourceServerKeycloakIT {

    @LocalServerPort
    int port;

    @Autowired
    InternalApiClient internalClient;

    @Autowired
    com.payflow.domain.Accounts accounts;

    @org.junit.jupiter.api.BeforeEach
    void resetAccounts() {
        accounts.reset();
    }

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeAll
    static void up() {
        KeycloakFixture.requireRunning();
    }

    private HttpResponse<String> call(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        b.header("Content-Type", "application/json");
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String accessToken(String email, String password, String scope) {
        return KeycloakFixture.userTokens(email, password, scope).get("access_token").asText();
    }

    @Test
    void realUserToken_readsOnlyHisOwnAccounts() throws Exception {
        HttpResponse<String> alice = call("GET", "/api/v1/accounts", accessToken("alice@example.com", "alice-lab-pass-1", "openid transfers:read"), null);
        HttpResponse<String> bob = call("GET", "/api/v1/accounts", accessToken("bob@example.com", "bob-lab-pass-12", "openid transfers:read"), null);
        System.out.println("T10.03 alice -> " + alice.statusCode() + " " + alice.body());
        System.out.println("T10.03 bob   -> " + bob.statusCode() + " " + bob.body());
        assertThat(alice.statusCode()).isEqualTo(200);
        assertThat(alice.body()).contains("aaaaaaaa-0000-4000-8000-000000000001").doesNotContain("bbbbbbbb");
        assertThat(bob.body()).contains("bbbbbbbb-0000-4000-8000-000000000002").doesNotContain("aaaaaaaa");
    }

    @Test
    void withoutTheOptionalScope_theKeycloakTokenIsValidButTheApiSays403() throws Exception {
        HttpResponse<String> r = call("GET", "/api/v1/accounts", accessToken("alice@example.com", "alice-lab-pass-1", "openid"), null);
        System.out.println("T10.03 no transfers:read -> " + r.statusCode() + " WWW-Authenticate=" + r.headers().firstValue("WWW-Authenticate").orElse(""));
        assertThat(r.statusCode()).isEqualTo(403);
        assertThat(r.headers().firstValue("WWW-Authenticate").orElse("")).contains("insufficient_scope");
    }

    @Test
    void theIdTokenIsRefused_evenThoughItIsSignedByTheSameKey() throws Exception {
        String idToken = KeycloakFixture.userTokens("alice@example.com", "alice-lab-pass-1", "openid transfers:read").get("id_token").asText();
        HttpResponse<String> r = call("GET", "/api/v1/accounts", idToken, null);
        System.out.println("T10.03 id_token as bearer -> " + r.statusCode() + " WWW-Authenticate=" + r.headers().firstValue("WWW-Authenticate").orElse(""));
        assertThat(r.statusCode()).isEqualTo(401);
    }

    @Test
    void realRoles_adminCanBlock_userCannot() throws Exception {
        String path = "/api/v1/admin/accounts/bbbbbbbb-0000-4000-8000-000000000002/block";
        assertThat(call("POST", path, accessToken("alice@example.com", "alice-lab-pass-1", "openid"), null).statusCode()).isEqualTo(403);
        assertThat(call("POST", path, accessToken("ops@example.com", "ops-lab-pass-123", "openid"), null).statusCode()).isEqualTo(200);
    }

    @Test
    void logoutRevokesTheRefreshToken_butTheAccessTokenKeepsWorkingUntilItExpires() throws Exception {
        JsonNode tokens = KeycloakFixture.userTokens("alice@example.com", "alice-lab-pass-1", "openid transfers:read");
        String access = tokens.get("access_token").asText();
        String refresh = tokens.get("refresh_token").asText();
        assertThat(call("GET", "/api/v1/accounts", access, null).statusCode()).isEqualTo(200);

        Map<String, String> logout = new LinkedHashMap<>();
        logout.put("client_id", "payflow-lab-direct");
        logout.put("refresh_token", refresh);
        int logoutStatus = KeycloakFixture.post(KeycloakFixture.OIDC + "/logout", logout).status();

        Map<String, String> again = new LinkedHashMap<>();
        again.put("grant_type", "refresh_token");
        again.put("client_id", "payflow-lab-direct");
        again.put("refresh_token", refresh);
        KeycloakFixture.Result refreshAfterLogout = KeycloakFixture.token(again);

        Map<String, String> introspect = new LinkedHashMap<>();
        introspect.put("client_id", "payflow-notifications");
        introspect.put("client_secret", "lab-only-notifications-secret-change-me");
        introspect.put("token", access);
        KeycloakFixture.Result introspection = KeycloakFixture.post(KeycloakFixture.OIDC + "/token/introspect", introspect);

        int apiAfterLogout = call("GET", "/api/v1/accounts", access, null).statusCode();
        System.out.println("T10.03 logout=" + logoutStatus + " refresh-after-logout=" + refreshAfterLogout.status() + " "
                + refreshAfterLogout.body().path("error_description").asText());
        System.out.println("T10.03 introspection after logout=" + introspection.body());
        System.out.println("T10.03 API with the same access token after logout=" + apiAfterLogout);
        assertThat(logoutStatus).isEqualTo(204);
        assertThat(refreshAfterLogout.status()).isEqualTo(400);
        assertThat(introspection.body().get("active").asBoolean()).isFalse();      // o Keycloak SABE que a sessão acabou...
        assertThat(apiAfterLogout).isEqualTo(200);                                  // ...mas o Resource Server só olha assinatura e prazo
    }

    // ---------- entre serviços (client_credentials com o Spring como cliente) ----------

    @Test
    void service_notificationsClientIsAccepted_andTheTokenIsReusedUntilItExpires() {
        String base = "http://127.0.0.1:" + port;
        ResponseEntity<Map<String, Object>> first = internalClient.notifyTransfer(base, "notifications", Map.of("transferId", "t-1"));
        ResponseEntity<Map<String, Object>> second = internalClient.notifyTransfer(base, "notifications", Map.of("transferId", "t-2"));
        System.out.println("T10.04 first=" + first.getStatusCode() + " second=" + second.getStatusCode()
                + " same jti (token reused)=" + first.getBody().get("jti").equals(second.getBody().get("jti")));
        assertThat(first.getStatusCode().value()).isEqualTo(202);
        assertThat(first.getBody().get("caller")).isEqualTo("payflow-notifications");
        assertThat(second.getBody().get("jti")).isEqualTo(first.getBody().get("jti"));
    }

    @Test
    void service_reportingClientGetsAValidTokenButTheApiRefusesIt() {
        String base = "http://127.0.0.1:" + port;
        ResponseEntity<Map<String, Object>> r = internalClient.notifyTransfer(base, "reporting", Map.of("transferId", "t-1"));
        System.out.println("T10.04 reporting client -> " + r.getStatusCode() + " WWW-Authenticate=" + r.getHeaders().getFirst("WWW-Authenticate"));
        assertThat(r.getStatusCode().value()).isEqualTo(403);
    }
}
