package com.payflow.labs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** J8.06/J8.07: campo a mais no JSON ("role":"ADMIN"): o que acontece no modo Default. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MassAssignmentDefaultLabTest {

    static final String BODY = "{\"email\":\"ana@payflow.dev\",\"password\":\"Payflow#Dev2026\",\"role\":\"ADMIN\",\"id\":\"x\"}";

    @Autowired MockMvc mvc;

    @Test
    void extraFields() throws Exception {
        int code = mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON).content(BODY)).andReturn().getResponse().getStatus();
        System.out.println("[mass-assignment] modo Default: status " + code);
        mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());
    }
}
