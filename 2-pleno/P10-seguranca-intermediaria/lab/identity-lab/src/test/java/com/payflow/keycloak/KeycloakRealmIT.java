package com.payflow.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import com.payflow.support.KeycloakFixture;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** O que o Keycloak REAL (26.8.2) publica e emite: o contrato que o Resource Server assume. */
class KeycloakRealmIT {

    @BeforeAll
    static void up() {
        KeycloakFixture.requireRunning();
    }

    @Test
    void discoveryDocument_advertisesWhatWeRelyOn() throws Exception {
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(KeycloakFixture.ISSUER + "/.well-known/openid-configuration")).build();
        JsonNode doc = new tools.jackson.databind.ObjectMapper()
                .readTree(KeycloakFixture.client().send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).body());
        System.out.println("T10.01 issuer=" + doc.get("issuer").asText());
        System.out.println("T10.01 jwks_uri=" + doc.get("jwks_uri").asText());
        System.out.println("T10.01 pkce=" + doc.get("code_challenge_methods_supported"));
        System.out.println("T10.01 grants=" + doc.get("grant_types_supported"));
        System.out.println("T10.01 id_token_signing_alg=" + doc.get("id_token_signing_alg_values_supported"));
        assertThat(doc.get("issuer").asText()).isEqualTo(KeycloakFixture.ISSUER);
        assertThat(doc.get("code_challenge_methods_supported").toString()).contains("S256");
        assertThat(doc.get("jwks_uri").asText()).startsWith(KeycloakFixture.ISSUER);
    }

    @Test
    void userAccessToken_hasTheClaimsTheApiReads() {
        JsonNode tokens = KeycloakFixture.userTokens("alice@example.com", "alice-lab-pass-1", "openid transfers:read");
        JsonNode header = KeycloakFixture.header(tokens.get("access_token").asText());
        JsonNode claims = KeycloakFixture.claims(tokens.get("access_token").asText());
        System.out.println("T10.02 header=" + header);
        System.out.println("T10.02 claims=" + claims);
        assertThat(header.get("alg").asText()).isEqualTo("RS256");
        assertThat(claims.get("iss").asText()).isEqualTo(KeycloakFixture.ISSUER);
        assertThat(claims.get("sub").asText()).isEqualTo("11111111-1111-4111-8111-111111111111");
        assertThat(claims.get("aud").asText()).isEqualTo("payflow-api");
        assertThat(claims.get("typ").asText()).isEqualTo("Bearer");
        assertThat(claims.get("scope").asText()).contains("transfers:read").doesNotContain("transfers:write");
        assertThat(claims.get("realm_access").get("roles").toString()).contains("USER").doesNotContain("ADMIN");
        assertThat(claims.get("exp").asLong() - claims.get("iat").asLong()).isEqualTo(300);
    }

    @Test
    void theScopeRequestedIsTheScopeGranted_anOptionalScopeNotAskedForIsNotIssued() {
        JsonNode tokens = KeycloakFixture.userTokens("alice@example.com", "alice-lab-pass-1", "openid");
        String scope = KeycloakFixture.claims(tokens.get("access_token").asText()).get("scope").asText();
        System.out.println("T10.02 scope-without-request=" + scope);
        assertThat(scope).doesNotContain("transfers:read").doesNotContain("transfers:write");
    }

    @Test
    void idToken_isForTheClient_notForTheApi() {
        JsonNode tokens = KeycloakFixture.userTokens("alice@example.com", "alice-lab-pass-1", "openid");
        JsonNode id = KeycloakFixture.claims(tokens.get("id_token").asText());
        System.out.println("T10.02 id_token typ=" + id.get("typ").asText() + " aud=" + id.get("aud"));
        assertThat(id.get("typ").asText()).isEqualTo("ID");
        assertThat(id.get("aud").asText()).isEqualTo("payflow-lab-direct");
    }

    @Test
    void adminRoleOnlyForTheAdmin() {
        JsonNode ops = KeycloakFixture.claims(KeycloakFixture.userTokens("ops@example.com", "ops-lab-pass-123", "openid").get("access_token").asText());
        assertThat(ops.get("realm_access").get("roles").toString()).contains("ADMIN");
    }

    @Test
    void clientCredentials_issuesAMachineToken_withoutUserClaims() {
        JsonNode token = KeycloakFixture.serviceToken("payflow-notifications", "lab-only-notifications-secret-change-me", "internal:notify");
        JsonNode claims = KeycloakFixture.claims(token.get("access_token").asText());
        System.out.println("T10.04 service claims=" + claims);
        System.out.println("T10.04 service has refresh_token=" + token.has("refresh_token"));
        assertThat(claims.get("azp").asText()).isEqualTo("payflow-notifications");
        assertThat(claims.get("scope").asText()).isEqualTo("internal:notify");
        assertThat(claims.has("email")).isFalse();
        assertThat(token.has("refresh_token")).isFalse();
    }

    @Test
    void aClientCannotAskForAScopeItWasNotGiven() {
        // payflow-reporting não tem o internal:notify: o Keycloak ignora o pedido (ou recusa), mas não concede
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "client_credentials");
        form.put("client_id", "payflow-reporting");
        form.put("client_secret", "lab-only-reporting-secret-change-me");
        form.put("scope", "internal:notify");
        KeycloakFixture.Result r = KeycloakFixture.token(form);
        System.out.println("T10.04 reporting asks internal:notify -> " + r.status() + " " + r.body());
        if (r.status() == 200) {
            assertThat(KeycloakFixture.claims(r.body().get("access_token").asText()).path("scope").asText()).doesNotContain("internal:notify");
        } else {
            assertThat(r.body().get("error").asText()).isEqualTo("invalid_scope");
        }
    }

    @Test
    void wrongClientSecret_isRejected() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "client_credentials");
        form.put("client_id", "payflow-notifications");
        form.put("client_secret", "wrong");
        KeycloakFixture.Result r = KeycloakFixture.token(form);
        System.out.println("T10.04 wrong secret -> " + r.status() + " " + r.body());
        assertThat(r.status()).isEqualTo(401);
    }
}
