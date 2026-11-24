package com.payflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.payflow.repository.InMemoryData;
import com.payflow.support.Tokens;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtApiTest {

    static final String PASSWORD = "Payflow#Dev2026";

    @Autowired MockMvc mvc;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;
    @Autowired JsonMapper json;

    ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password))));
    }

    String tokenOf(String email) throws Exception {
        String body = login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asString();
    }

    @Test
    void rightCredentialsGiveABearerTokenWithTheExpectedClaims() throws Exception {
        String body = login("ANA@payflow.dev ", PASSWORD)              // maiúsculas e espaço: normalizados
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        Jwt jwt = decoder.decode(json.readTree(body).get("accessToken").asString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("payflow");       // getIssuer() exige uma URL; o nosso emissor é só um nome
        assertThat(jwt.getSubject()).isEqualTo(InMemoryData.ANA.toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ana@payflow.dev");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaims()).doesNotContainKeys("password", "passwordHash");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).hasMinutes(15);
    }

    @Test
    void wrongPasswordAndUnknownEmailLookExactlyTheSame() throws Exception {
        String wrongPassword = login("bruno@payflow.dev", "nope-nope-nope").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:payflow:problem:invalid-credentials"))
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = login("nobody@payflow.dev", "nope-nope-nope").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(unknownEmail).isEqualTo(wrongPassword);                    // quem pergunta não descobre quais e-mails existem
    }

    @Test
    void aPasswordLongerThanBcryptLimitIsA401NotA500() throws Exception {
        login("bruno@payflow.dev", "x".repeat(100)).andExpect(status().isUnauthorized());
    }

    @Test
    void anOversizedPasswordIsRejectedBeforeHashing() throws Exception {
        login("bruno@payflow.dev", "x".repeat(129)).andExpect(status().isBadRequest());
    }

    @Test
    void afterFiveFailuresEvenTheRightPasswordIsRefusedWith429() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("ghost@payflow.dev", "wrong-wrong-wrong").andExpect(status().isUnauthorized());
        }
        login("ghost@payflow.dev", "wrong-wrong-wrong").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("urn:payflow:problem:too-many-attempts"));
    }

    @Test
    void withoutATokenTheApiAnswers401WithProblemDetailsAndWwwAuthenticate() throws Exception {
        mvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(jsonPath("$.type").value("urn:payflow:problem:not-authenticated"))
                .andDo(result -> System.out.println("WWW-Authenticate: " + result.getResponse().getHeader("WWW-Authenticate")
                        + " | corpo: " + result.getResponse().getContentAsString()));
    }

    @Test
    void aValidTokenListsOnlyMyAccounts() throws Exception {
        mvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + tokenOf("ana@payflow.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(InMemoryData.ANA_ACCOUNT.toString()))
                .andExpect(jsonPath("$[0].balance").value("830.00"));
    }

    @Test
    void anotherUsersAccountIsIndistinguishableFromANonexistentOne() throws Exception {
        String ana = "Bearer " + tokenOf("ana@payflow.dev");
        String foreign = mvc.perform(get("/api/v1/accounts/" + InMemoryData.BRUNO_ACCOUNT).header("Authorization", ana))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(get("/api/v1/accounts/20000000-0000-0000-0000-0000000000ff").header("Authorization", ana))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        // o "instance" (o caminho pedido) difere; type, title e detail são idênticos: nada revela se a conta existe
        assertThat(json.readTree(foreign).get("type")).isEqualTo(json.readTree(missing).get("type"));
        assertThat(json.readTree(foreign).get("detail")).isEqualTo(json.readTree(missing).get("detail"));
        assertThat(json.readTree(foreign).get("title")).isEqualTo(json.readTree(missing).get("title"));
    }

    @Test
    void adminEndpointsNeedTheAdminRole() throws Exception {
        String url = "/api/v1/admin/accounts/" + InMemoryData.ANA_ACCOUNT + "/block";
        mvc.perform(post(url)).andExpect(status().isUnauthorized());
        mvc.perform(post(url).header("Authorization", "Bearer " + tokenOf("ana@payflow.dev")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:payflow:problem:forbidden"));
        mvc.perform(post(url).header("Authorization", "Bearer " + tokenOf("admin@payflow.dev")))   // sem CSRF: API sem cookie
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
        mvc.perform(post(url.replace("block", "unblock")).header("Authorization", "Bearer " + tokenOf("admin@payflow.dev")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void preAuthorizeAloneAlsoAnswers403NotTheCatchAll500() throws Exception {
        mvc.perform(get("/api/v1/reports/summary").header("Authorization", "Bearer " + tokenOf("ana@payflow.dev")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:payflow:problem:forbidden"));
        mvc.perform(get("/api/v1/reports/summary").header("Authorization", "Bearer " + tokenOf("admin@payflow.dev")))
                .andExpect(status().isOk());
    }

    @Test
    void aTamperedTokenIsRefused() throws Exception {
        String token = tokenOf("ana@payflow.dev");
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        mvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("invalid_token")));
    }

    @Test
    void anExpiredTokenIsRefused() throws Exception {
        String expired = Tokens.signed(encoder, "payflow", InMemoryData.ANA.toString(), Instant.now().minusSeconds(300));
        mvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenFromAnotherIssuerIsRefusedEvenIfSignedWithOurKey() throws Exception {
        String foreign = Tokens.signed(encoder, "evil-corp", InMemoryData.ANA.toString(), Instant.now().plusSeconds(600));
        mvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + foreign)).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRefused() throws Exception {
        var other = Tokens.encoderWithKey("another-secret-0123456789abcdef0123456789abcdef");
        String forged = Tokens.signed(other, "payflow", InMemoryData.ANA.toString(), Instant.now().plusSeconds(600));
        mvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + forged)).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenWithAlgNoneIsRefused() throws Exception {
        mvc.perform(get("/api/v1/admin/accounts/" + InMemoryData.ANA_ACCOUNT + "/block").header("Authorization", "Bearer " + Tokens.unsigned(InMemoryData.ADMIN.toString())))
                .andExpect(status().isUnauthorized());
    }
}
