package com.payflow;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.payflow.web.api.AdminAccountsController;
import com.payflow.web.api.ApiErrors;
import com.payflow.web.api.ApiProblems;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Fatia web com papéis: o jwt() cria o usuário "logado"; .authorities(...) é que lhe dá o papel. */
@WebMvcTest(AdminAccountsController.class)
@ActiveProfiles("test")
@EnableConfigurationProperties(PayflowProperties.class)
@Import({SecurityConfig.class, JwtConfig.class, ApiSecurityErrors.class, ApiProblems.class, ApiErrors.class,
        PayflowUserDetailsService.class, LabConfig.class})
class AdminAccountsControllerWebTest {

    static final String URL = "/api/v1/admin/accounts/" + InMemoryData.ANA_ACCOUNT + "/block";

    @Autowired MockMvc mvc;
    @MockitoBean AccountRepository accounts;
    @MockitoBean UserRepository users;

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(post(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenWithoutTheAdminRoleIs403() throws Exception {
        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(InMemoryData.ANA.toString())))).andExpect(status().isForbidden());
    }

    @Test
    void aTokenWithRoleUserIsStill403() throws Exception {
        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(InMemoryData.ANA.toString())).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAdminBlocksTheAccount() throws Exception {
        given(accounts.findById(InMemoryData.ANA_ACCOUNT)).willReturn(Optional.of(new Account(InMemoryData.ANA_ACCOUNT, InMemoryData.ANA, BigDecimal.TEN)));
        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(InMemoryData.ADMIN.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void anAdminBlockingAMissingAccountGets404() throws Exception {
        given(accounts.findById(InMemoryData.ANA_ACCOUNT)).willReturn(Optional.empty());
        mvc.perform(post(URL).with(jwt().jwt(j -> j.subject(InMemoryData.ADMIN.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}
