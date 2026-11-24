package com.payflow.labs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** J8.04/J8.05: o preflight de CORS passa pela cadeia SEM token (o .cors() vem antes da autenticação). */
@SpringBootTest(properties = "payflow.cors.allowed-origins=http://localhost:5173")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsPreflightTest {

    @Autowired MockMvc mvc;

    @Test
    void allowedOriginPassesWithoutAToken() throws Exception {
        mvc.perform(options("/api/v1/accounts").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void otherOriginIsRefused() throws Exception {
        mvc.perform(options("/api/v1/accounts").header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
