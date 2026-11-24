package lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** J8.01: a "ponte": só a dependência + uma cadeia que libera tudo, para o app antigo seguir funcionando. */
@SpringBootTest(classes = BridgeChainTest.App.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BridgeChainTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableWebSecurity
    @RestController
    static class App {
        @GetMapping("/hello") String hello() { return "hello"; }
        @PostMapping("/hello") String post() { return "posted"; }

        @Bean
        SecurityFilterChain bridge(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }

    @LocalServerPort int port;

    @Test
    void everythingStillWorksWithoutLogin() throws Exception {
        var client = HttpClient.newHttpClient();
        for (String method : new String[] {"GET", "POST"}) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/hello"))
                    .method(method, HttpRequest.BodyPublishers.noBody()).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[ponte] " + method + " /hello sem credenciais -> " + response.statusCode() + " " + response.body()
                    + "  | cabeçalhos: " + response.headers().map().keySet().stream().filter(h -> h.startsWith("x-") || h.equals("cache-control")).sorted().toList());
            assertThat(response.statusCode()).isEqualTo(200);
        }
    }
}
