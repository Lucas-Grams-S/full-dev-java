package com.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** Servidor real (Tomcat, porta aleatória): o que o MockMvc não mostra bem são os cookies de verdade. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SessionCookieServerTest {

    @LocalServerPort int port;
    final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    HttpResponse<String> send(HttpRequest.Builder builder, String cookie) throws Exception {
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    HttpRequest.Builder req(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    String setCookie(HttpResponse<?> response) {
        return response.headers().allValues("Set-Cookie").stream().filter(c -> c.startsWith("PAYFLOW_SESSION=")).findFirst().orElseThrow();
    }

    String nameValue(String setCookie) {
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    String csrf(String html) {
        Matcher m = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(html);
        assertThat(m.find()).as("csrf field in html").isTrue();
        return m.group(1);
    }

    String form(String... pairs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            sb.append(sb.isEmpty() ? "" : "&").append(pairs[i]).append('=').append(URLEncoder.encode(pairs[i + 1], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Test
    void sessionLifecycleCookieFlagsFixationAndLogout() throws Exception {
        // 1. a página de login já cria a sessão (guarda o token CSRF) e o cookie sai com os atributos certos
        HttpResponse<String> loginPage = send(req("/login"), null);
        String cookie1 = setCookie(loginPage);
        System.out.println("Set-Cookie (login page): " + cookie1);
        loginPage.headers().map().entrySet().stream().filter(e -> !e.getKey().equals("date") && !e.getKey().equals("set-cookie"))
                .sorted(java.util.Map.Entry.comparingByKey()).forEach(e -> System.out.println("[cabeçalho] " + e.getKey() + ": " + String.join(", ", e.getValue())));
        assertThat(cookie1).contains("HttpOnly").contains("SameSite=Lax").contains("Secure").contains("Path=/");
        assertThat(cookie1).doesNotContain("JSESSIONID");

        // 1b. POST sem token CSRF: 403 e a página de erro do PayFlow (o forward de verdade, que o MockMvc não executa)
        HttpResponse<String> noCsrf = send(req("/login").header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form("email", "ana@payflow.dev", "password", "Payflow#Dev2026"))), nameValue(cookie1));
        assertThat(noCsrf.statusCode()).isEqualTo(403);
        assertThat(noCsrf.body()).contains("Acesso negado");

        // 2. login: 303 e um cookie NOVO (a sessão anterior foi trocada: proteção contra session fixation)
        HttpResponse<String> login = send(req("/login").header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form("email", "ana@payflow.dev", "password", "Payflow#Dev2026", "_csrf", csrf(loginPage.body())))),
                nameValue(cookie1));
        assertThat(login.statusCode()).isEqualTo(303);
        assertThat(login.headers().firstValue("Location").orElseThrow()).endsWith("/accounts");
        String cookie2 = nameValue(setCookie(login));
        System.out.println("cookie antes do login:  " + nameValue(cookie1).substring(0, 30) + "...");
        System.out.println("cookie depois do login: " + cookie2.substring(0, 30) + "...");
        assertThat(cookie2).isNotEqualTo(nameValue(cookie1));

        // 3. o cookie novo vale; o antigo não
        HttpResponse<String> accounts = send(req("/accounts"), cookie2);
        assertThat(accounts.statusCode()).isEqualTo(200);
        assertThat(send(req("/accounts"), nameValue(cookie1)).statusCode()).isEqualTo(302);

        // 4. a API não aceita sessão: só Bearer
        assertThat(send(req("/api/v1/accounts"), cookie2).statusCode()).isEqualTo(401);

        // 5. logout (POST com CSRF): 303, cookie apagado, sessão morta no servidor
        HttpResponse<String> logout = send(req("/logout").header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form("_csrf", csrf(accounts.body())))), cookie2);
        assertThat(logout.statusCode()).isEqualTo(303);
        assertThat(logout.headers().firstValue("Location").orElseThrow()).endsWith("/login?logout");
        assertThat(setCookie(logout)).contains("Expires=Thu, 01 Jan 1970");
        assertThat(send(req("/accounts"), cookie2).statusCode()).isEqualTo(302);
    }
}
