package com.payflow.lite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "payflow.security.jwt.secret=test-only-secret-with-at-least-32-bytes!!")
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class AccountsApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createsAnAccountAndListsIt() throws Exception {
        mvc.perform(post("/api/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"owner":"ci@payflow.dev","balance":"10"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.balance").value("10.00"));

        mvc.perform(get("/api/v1/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.owner == 'ci@payflow.dev')].balance").value("10.00"));
    }

    @Test
    void flywayAppliedTheSchemaOnARealPostgres(@Autowired org.springframework.jdbc.core.simple.JdbcClient jdbc) {
        String version = jdbc.sql("select version()").query(String.class).single();
        assertThat(version).startsWith("PostgreSQL 18");
    }
}
