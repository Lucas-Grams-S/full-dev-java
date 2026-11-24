package com.payflow;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.payflow.domain.Role;
import com.payflow.security.PayflowUserDetails;
import com.payflow.web.CurrentUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebPanelTest {

    @Autowired MockMvc mvc;

    SecurityMockMvcRequestBuilders.FormLoginRequestBuilder login(String email, String password) {
        return formLogin("/login").user("email", email).password("password", password);       // acrescenta o CSRF sozinho
    }

    @Test
    void protectedPageRedirectsAnonymousToLogin() throws Exception {
        mvc.perform(get("/accounts")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));
    }

    @Test
    void theLoginFormCarriesACsrfTokenAndNothingElseIsNeededToRenderIt() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void postingTheLoginWithoutCsrfIs403AndForwardsToThePayflowErrorPage() throws Exception {
        mvc.perform(post("/login").param("email", "ana@payflow.dev").param("password", "Payflow#Dev2026"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/forbidden"));                    // o MockMvc não executa o forward; o servidor real, sim (SessionCookieServerTest)
    }

    @Test
    void rightPasswordRedirectsWith303AndAuthenticates() throws Exception {
        mvc.perform(login("ana@payflow.dev", "Payflow#Dev2026"))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/accounts")).andExpect(authenticated().withUsername("ana@payflow.dev"));
    }

    @Test
    void afterLoginTheUserGoesBackToThePageHeAskedFor() throws Exception {
        var first = mvc.perform(get("/accounts?x=1")).andExpect(status().is3xxRedirection()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) first.getRequest().getSession();
        mvc.perform(post("/login").session(session).with(csrf()).param("email", "ana@payflow.dev").param("password", "Payflow#Dev2026"))
                .andExpect(status().isSeeOther())
                // o Spring Security 7 devolve a URL absoluta da página pedida, com o marcador "continue" no fim
                .andExpect(redirectedUrl("http://localhost/accounts?x=1&continue"));
    }

    @Test
    void wrongPasswordRedirectsBackWithAnErrorFlagAndNoSession() throws Exception {
        mvc.perform(login("ana@payflow.dev", "wrong-wrong-wrong"))
                .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }

    @Test
    void anAuthenticatedUserSeesOnlyThePageForHisOwnAccounts() throws Exception {
        var ana = new PayflowUserDetails(new CurrentUser(UUID.fromString("10000000-0000-0000-0000-000000000001"), "ana@payflow.dev", "Ana Silva"),
                Role.USER, null);
        mvc.perform(get("/accounts").with(user(ana))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("20000000-0000-0000-0000-000000000001")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("20000000-0000-0000-0000-000000000002"))));
    }

    @Test
    void logoutNeedsCsrfAndRedirectsWith303() throws Exception {
        var ana = new PayflowUserDetails(new CurrentUser(UUID.randomUUID(), "ana@payflow.dev", "Ana"), Role.USER, null);
        mvc.perform(post("/logout").with(user(ana))).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(user(ana)).with(csrf())).andExpect(status().isSeeOther()).andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void securityHeadersAreOnEveryPanelResponse() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "same-origin"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.startsWith("default-src 'self'")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void healthIsPublicButOtherActuatorPathsAreNot() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/env")).andExpect(status().is3xxRedirection());
    }

    @Test
    void fetchToTheLegacyApiGets401NotTheLoginPage() throws Exception {
        mvc.perform(get("/api/accounts/x/statement")).andExpect(status().isUnauthorized());
    }
}
