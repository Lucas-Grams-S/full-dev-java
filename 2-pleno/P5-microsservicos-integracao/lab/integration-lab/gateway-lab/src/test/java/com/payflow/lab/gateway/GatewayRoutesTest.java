package com.payflow.lab.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTest {

    static EchoService notifications;
    static EchoService monolith;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        notifications = new EchoService("notifications");
        monolith = new EchoService("monolith");
        r.add("lab.notifications.url", notifications::url);
        r.add("lab.monolith.url", monolith::url);
    }

    @AfterAll
    static void stop() {
        notifications.close();
        monolith.close();
    }

    @LocalServerPort int port;
    final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void reset() {
        notifications.calls.set(0);
        monolith.calls.set(0);
        notifications.delayMs = 0;
        notifications.status = 200;
    }

    HttpResponse<String> send(String method, String path, String... headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
        for (int i = 0; i < headers.length; i += 2) b.header(headers[i], headers[i + 1]);
        b.method(method, method.equals("POST") ? HttpRequest.BodyPublishers.ofString("{}") : HttpRequest.BodyPublishers.noBody());
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void routesByPathStripsPrefixAndAddsHeaders() throws Exception {
        var r1 = send("GET", "/notifications/internal/v1/users/u1/preferences", "Authorization", "Bearer abc");
        var r2 = send("GET", "/api/v1/accounts");
        System.out.println("T5 /notifications/internal/... -> " + r1.statusCode() + " " + r1.body());
        System.out.println("T5 /api/v1/accounts -> " + r2.statusCode() + " " + r2.body());
        assertThat(r1.body()).contains("\"service\":\"notifications\"").contains("\"path\":\"/internal/v1/users/u1/preferences\"").contains("\"xGateway\":\"payflow\"");
        assertThat(r2.body()).contains("\"service\":\"monolith\"").contains("\"path\":\"/api/v1/accounts\"");
        assertThat(r1.body()).contains("Bearer abc"); // o Authorization atravessa o gateway
    }

    @Test
    void unknownRouteIs404AtTheGateway() throws Exception {
        var r = send("GET", "/nada/aqui");
        System.out.println("T5 rota inexistente -> " + r.statusCode());
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(notifications.calls.get() + monolith.calls.get()).isZero();
    }

    @Test
    void slowUpstreamIsCutByTheClientReadTimeout() throws Exception {
        notifications.delayMs = 3_000;
        long t0 = System.nanoTime();
        var r = send("GET", "/retry/internal/v1/users/u1/preferences"); // rota sem circuit breaker
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("T5 destino lento (3 s), spring.http.clients.read-timeout=600ms, rota SEM breaker: HTTP " + r.statusCode() + " em " + ms + " ms; chamadas ao destino=" + notifications.calls.get());
    }

    @Test
    void retryFilterRetriesPostToo_whenYouAskForIt() throws Exception {
        notifications.status = 503;
        var r = send("POST", "/retry/internal/v1/users/u1/messages");
        System.out.println("T5 Retry(methods=GET,POST), destino 503: resposta " + r.statusCode() + "; o destino recebeu " + notifications.calls.get() + " chamadas (1 original + 3 retentativas)");
    }

    @Test
    void gatewayCircuitBreakerOpensAndForwardsToFallback() throws Exception {
        notifications.status = 503;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            var r = send("GET", "/notifications/internal/v1/users/u1/preferences");
            sb.append(r.statusCode()).append("(destino=").append(notifications.calls.get()).append(")").append(i < 5 ? ", " : "");
        }
        System.out.println("T5 CircuitBreaker no gateway (janela 4, destino 503): status das 6 chamadas = [" + sb + "]; chamadas que chegaram ao destino=" + notifications.calls.get());
        var last = send("GET", "/notifications/internal/v1/users/u1/preferences");
        System.out.println("T5 com o breaker aberto: " + last.statusCode() + " " + last.body());
    }

    @Test
    void upstreamDownGivesGatewayError() throws Exception {
        var r = send("GET", "/dead/x"); // a rota aponta para 127.0.0.1:9 (nada escuta lá)
        System.out.println("T5 destino fora do ar (conexao recusada): HTTP " + r.statusCode() + " " + r.body());
    }
}
