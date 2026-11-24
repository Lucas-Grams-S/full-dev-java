package com.payflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.payflow.domain.Accounts;
import com.payflow.support.OfflineResourceServerTest;
import com.payflow.support.TestTokens;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** O Resource Server do PayFlow contra tokens de verdade (assinados, com claims no formato do Keycloak). */
class ResourceServerTest extends OfflineResourceServerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    com.payflow.domain.Accounts accounts;

    @org.junit.jupiter.api.BeforeEach
    void resetAccounts() {
        accounts.reset();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- o caminho feliz ----------

    @Test
    void validToken_listsOnlyMyAccounts() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "openid transfers:read", "USER").signed();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ownerId").value(Accounts.ALICE.toString()));
    }

    // ---------- tokens que NÃO podem passar (cada um é um ataque ou um erro real) ----------

    @Test
    void missingToken_is401_withWwwAuthenticate() throws Exception {
        mvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")));
    }

    @Test
    void expiredToken_is401() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").expiredSecondsAgo(600).signed();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("invalid_token")));
    }

    @Test
    void clockSkew_tokenExpired30sAgoStillPasses_butNot90sAgo() throws Exception {
        String recent = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").expiredSecondsAgo(30).signed();
        String old = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").expiredSecondsAgo(90).signed();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(recent))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(old))).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenNotYetValid_is401() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").notBeforeInSeconds(600).signed();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedPayload_is401() throws Exception {
        // Bob pega o próprio token e troca o "sub" pelo da Alice, mantendo a assinatura antiga
        String forged = TOKENS.user(Accounts.BOB, "transfers:read", "USER").signedThenTampered("sub", Accounts.ALICE.toString());
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(forged))).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuer_is401() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").claim("iss", "http://evil.example/realms/payflow").signed();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongAudience_is401() throws Exception {
        // um token válido, do mesmo Keycloak, mas emitido para OUTRA API
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").claim("aud", java.util.List.of("billing-api")).signed();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void idTokenIsNotAnAccessToken_is401() throws Exception {
        // assinado pela mesma chave e até com a audiência certa, mas typ = ID: é para o CLIENTE, não para a API
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").claim("typ", "ID").signed();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void algNone_is401() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").unsigned();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void hs256SignedWithThePublicKey_is401() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").hs256WithPublicKeyAsSecret();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void signedByAnUnknownKey_is401() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").signedByOtherKey("attacker-key");
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void keyRotation_aNewKeyInTheJwksIsAcceptedWithoutRestart() throws Exception {
        TestTokens rotated = new TestTokens("kid-2026-12");
        String token = rotated.user(Accounts.ALICE, "transfers:read", "USER").signed();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());

        JWKS.publish(TOKENS.publicJwk(), rotated.publicJwk());           // o Keycloak publica a chave nova ao lado da antiga
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(token))).andExpect(status().isOk());
        String oldKeyToken = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").signed();
        mvc.perform(get("/api/v1/accounts").header("Authorization", bearer(oldKeyToken))).andExpect(status().isOk());
    }

    // ---------- escopo (o app) × papel (a pessoa) × dono (o objeto) ----------

    @Test
    void readScopeIsNotEnoughToTransfer_403_insufficientScope() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").signed();
        mvc.perform(post("/api/v1/transfers").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(transfer(Accounts.ALICE_ACCOUNT)))
                .andExpect(status().isForbidden())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("insufficient_scope")));
    }

    @Test
    void writeScope_transfersFromMyOwnAccount_butNotFromSomeoneElses() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:write", "USER").signed();
        mvc.perform(post("/api/v1/transfers").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(transfer(Accounts.ALICE_ACCOUNT)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.by").value(Accounts.ALICE.toString()));
        mvc.perform(post("/api/v1/transfers").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(transfer(Accounts.BOB_ACCOUNT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void idor_bobCannotReadAlicesAccount_404() throws Exception {
        String token = TOKENS.user(Accounts.BOB, "transfers:read", "USER").signed();
        mvc.perform(get("/api/v1/accounts/" + Accounts.ALICE_ACCOUNT).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/accounts/" + Accounts.BOB_ACCOUNT).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void adminEndpoint_needsTheRole_notTheScope() throws Exception {
        String user = TOKENS.user(Accounts.ALICE, "transfers:read transfers:write", "USER").signed();
        String admin = TOKENS.user(UUID.fromString("99999999-9999-4999-8999-999999999999"), "openid", "ADMIN", "USER").signed();
        mvc.perform(post("/api/v1/admin/accounts/" + Accounts.BOB_ACCOUNT + "/block").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/accounts/" + Accounts.BOB_ACCOUNT + "/block").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void unknownRoutesAreDenied_evenWithAValidToken() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:read transfers:write", "USER", "ADMIN").signed();
        mvc.perform(get("/api/v1/secret").header("Authorization", bearer(token))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/env").header("Authorization", bearer(token))).andExpect(status().isForbidden());
        mvc.perform(get("/")).andExpect(status().isForbidden());
    }

    // ---------- entre serviços: /internal ----------

    @Test
    void internal_notificationsClientWithTheScope_is202() throws Exception {
        String token = TOKENS.service("payflow-notifications", "internal:notify").signed();
        mvc.perform(post("/internal/notifications/transfers").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"transferId\":\"t-1\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.caller").value("payflow-notifications"));
    }

    @Test
    void internal_anotherServiceWithoutTheScope_is403() throws Exception {
        String token = TOKENS.service("payflow-reporting", "").signed();
        mvc.perform(post("/internal/notifications/transfers").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internal_aKnownClientWithoutTheScope_is403_andAnUnknownClientWithTheScope_is403() throws Exception {
        String noScope = TOKENS.service("payflow-notifications", "openid").signed();
        String unknownClient = TOKENS.service("payflow-reporting", "internal:notify").signed();
        for (String token : new String[] {noScope, unknownClient}) {
            mvc.perform(post("/internal/notifications/transfers").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void internal_aUserTokenIsNotAServiceToken_403() throws Exception {
        String token = TOKENS.user(Accounts.ALICE, "transfers:read", "USER").signed();
        mvc.perform(post("/internal/notifications/transfers").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internal_withoutToken_is401() throws Exception {
        mvc.perform(post("/internal/notifications/transfers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private static String transfer(UUID source) {
        return "{\"sourceAccountId\":\"" + source + "\",\"targetAccountId\":\"" + UUID.randomUUID() + "\",\"amount\":10.00}";
    }

    @SuppressWarnings("unused")
    private static void keep() {
        assertThat(TestTokens.ISSUER).isNotBlank();
    }
}
