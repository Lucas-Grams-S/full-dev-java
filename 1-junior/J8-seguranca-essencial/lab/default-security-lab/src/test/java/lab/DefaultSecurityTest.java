package lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** O que o Spring Boot faz com APENAS a dependência do Spring Security (nenhuma configuração nossa). */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = DefaultSecurityTest.App.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DefaultSecurityTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @RestController
    static class App {
        @GetMapping("/hello") String hello() { return "hello"; }
        @PostMapping("/hello") String post() { return "posted"; }
    }

    @LocalServerPort int port;
    final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    HttpResponse<String> call(String method, String... headers) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/hello")).method(method, HttpRequest.BodyPublishers.noBody());
        for (int i = 0; i < headers.length; i += 2) {
            b.header(headers[i], headers[i + 1]);
        }
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void whatTheDefaultsDo(CapturedOutput output) throws Exception {
        var anon = call("GET");
        System.out.println("[padrão] GET sem credenciais, sem Accept        -> " + anon.statusCode() + "  WWW-Authenticate: " + anon.headers().firstValue("WWW-Authenticate").orElse("-"));
        var browser = call("GET", "Accept", "text/html");
        System.out.println("[padrão] GET sem credenciais, Accept text/html  -> " + browser.statusCode() + "  Location: " + browser.headers().firstValue("Location").orElse("-").replaceAll(":\\d+", ":<porta>"));

        // a senha gerada aparece no log (o bean do usuário é criado na primeira autenticação)
        Matcher m = Pattern.compile("Using generated security password: (\\S+)").matcher(output.getAll());
        assertThat(m.find()).as("senha gerada no log").isTrue();
        String generated = m.group(1);
        System.out.println("[padrão] log do Boot: 'Using generated security password: <" + generated.length() + " caracteres, um UUID>'");
        System.out.println("[padrão] aviso do Boot: " + output.getAll().lines().filter(l -> l.contains("for development use only")).findFirst().orElse("-").trim());

        String basic = "Basic " + Base64.getEncoder().encodeToString(("user:" + generated).getBytes());
        var ok = call("GET", "Authorization", basic);
        System.out.println("[padrão] GET com Basic user:<senha do log>      -> " + ok.statusCode() + " " + ok.body());
        var csrf = call("POST", "Authorization", basic);
        System.out.println("[padrão] POST com Basic e SEM token CSRF        -> " + csrf.statusCode() + "  (esperava-se 403: o /error também exige login)");
        System.out.println("[padrão] cabeçalhos de segurança da resposta 200: "
                + ok.headers().map().keySet().stream().filter(h -> !h.equals("date") && !h.equals("content-length") && !h.equals("content-type")).sorted().toList());
        assertThat(anon.statusCode()).isEqualTo(401);
        assertThat(browser.statusCode()).isEqualTo(302);
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(csrf.statusCode()).isEqualTo(401);
    }
}
