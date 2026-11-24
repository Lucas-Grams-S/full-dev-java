package lab;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** J8.01: um teste de fatia (@WebMvcTest) que NÃO conhecia segurança passa a levar 401/403 depois que o starter entra. */
@WebMvcTest(SliceDefaultsTest.HelloController.class)
@ContextConfiguration(classes = {SliceDefaultsTest.App.class, SliceDefaultsTest.HelloController.class})
class SliceDefaultsTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class App { }

    @RestController
    static class HelloController {
        @GetMapping("/hello") String hello() { return "hello"; }
        @PostMapping("/hello") String post() { return "posted"; }
    }

    @Autowired MockMvc mvc;

    @Test
    void sliceWithoutSecurityConfigGets401And403() throws Exception {
        System.out.println("[fatia] GET  /hello sem config de segurança -> " + mvc.perform(get("/hello")).andReturn().getResponse().getStatus());
        System.out.println("[fatia] POST /hello sem config de segurança -> " + mvc.perform(post("/hello")).andReturn().getResponse().getStatus());
    }
}
