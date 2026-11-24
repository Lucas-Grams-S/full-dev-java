package com.payflow;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.BDDMockito.given;

import com.payflow.config.LabConfig;
import com.payflow.config.PayflowProperties;
import com.payflow.domain.Account;
import com.payflow.repository.AccountRepository;
import com.payflow.repository.InMemoryData;
import com.payflow.repository.UserRepository;
import com.payflow.security.ApiSecurityErrors;
import com.payflow.security.JwtConfig;
import com.payflow.security.PayflowUserDetailsService;
import com.payflow.security.SecurityConfig;
import com.payflow.web.WebConfig;
import com.payflow.web.api.AccountsController;
import com.payflow.web.api.ApiErrors;
import com.payflow.web.api.ApiProblems;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** A fatia web da J7.03, agora com a segurança REAL importada e o usuário "logado" por jwt(). */
@WebMvcTest(AccountsController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(PayflowProperties.class)      // a fatia NÃO registra o PayflowProperties sozinha (ver J8.04)
@Import({SecurityConfig.class, JwtConfig.class, ApiSecurityErrors.class, ApiProblems.class, ApiErrors.class,
        PayflowUserDetailsService.class, LabConfig.class})
class AccountsControllerWebTest {

    @Autowired MockMvc mvc;
    @MockitoBean AccountRepository accounts;
    @MockitoBean UserRepository users;

    @Test
    void withoutTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void listsOnlyTheAccountsOfTheTokenSubject() throws Exception {
        given(accounts.findByOwnerId(InMemoryData.ANA)).willReturn(List.of(new Account(InMemoryData.ANA_ACCOUNT, InMemoryData.ANA, new BigDecimal("830.00"))));
        mvc.perform(get("/api/v1/accounts").with(jwt().jwt(j -> j.subject(InMemoryData.ANA.toString()).claim("email", "ana@payflow.dev").claim("name", "Ana"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].balance").value("830.00"));
    }

    @Test
    void aForeignAccountIs404() throws Exception {
        given(accounts.findById(InMemoryData.BRUNO_ACCOUNT)).willReturn(Optional.of(new Account(InMemoryData.BRUNO_ACCOUNT, InMemoryData.BRUNO, BigDecimal.TEN)));
        mvc.perform(get("/api/v1/accounts/" + InMemoryData.BRUNO_ACCOUNT)
                        .with(jwt().jwt(j -> j.subject(InMemoryData.ANA.toString()).claim("email", "ana@payflow.dev").claim("name", "Ana"))))
                .andExpect(status().isNotFound());
    }
}
