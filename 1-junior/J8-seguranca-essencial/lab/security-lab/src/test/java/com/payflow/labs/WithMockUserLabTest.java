package com.payflow.labs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** J8.04: @WithMockUser serve para regra de papel, mas NÃO entrega o CurrentUser (que espera o PayflowUserDetails ou o Jwt). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WithMockUserLabTest {

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void rolesWork() throws Exception {
        int code = mvc.perform(get("/api/v1/reports/summary")).andReturn().getResponse().getStatus();
        System.out.println("[withmockuser] papel ADMIN em /reports/summary (não usa CurrentUser) -> " + code);
        mvc.perform(get("/api/v1/reports/summary")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void currentUserIsNotAvailable() throws Exception {
        int code = mvc.perform(get("/api/v1/accounts")).andReturn().getResponse().getStatus();
        System.out.println("[withmockuser] @WithMockUser em /accounts (usa CurrentUser)         -> " + code);
        mvc.perform(get("/api/v1/accounts")).andExpect(status().isInternalServerError());
    }
}
