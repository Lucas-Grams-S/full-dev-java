package com.payflow.labs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.StreamReadConstraints;

/** J8.07: o que o Jackson já limita sozinho, e o que sobra para a borda (@Size, proxy). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JacksonLimitsLabTest {

    @Autowired MockMvc mvc;

    @Test
    void print() throws Exception {
        StreamReadConstraints c = StreamReadConstraints.defaults();
        System.out.println("[limites] Jackson: maxStringLength=" + c.getMaxStringLength() + " maxNestingDepth=" + c.getMaxNestingDepth()
                + " maxNumberLength=" + c.getMaxNumberLength() + " maxDocumentLength=" + c.getMaxDocumentLength());
        String bigPassword = "x".repeat(3_000_000);
        long t0 = System.nanoTime();
        int code = mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@payflow.dev\",\"password\":\"" + bigPassword + "\"}")).andReturn().getResponse().getStatus();
        System.out.printf("[limites] senha de 3 milhões de caracteres -> %d em %.0f ms (o corpo foi lido inteiro; o @Size barrou depois)%n", code, (System.nanoTime() - t0) / 1e6);
        String deep = "[".repeat(2000) + "]".repeat(2000);
        int codeDeep = mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON).content(deep)).andReturn().getResponse().getStatus();
        System.out.println("[limites] JSON com 2000 níveis de aninhamento -> " + codeDeep);
    }
}
