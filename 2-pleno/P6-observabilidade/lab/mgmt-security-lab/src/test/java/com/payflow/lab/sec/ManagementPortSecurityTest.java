package com.payflow.lab.sec;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

/** O que a regra da decisão 56 faz quando o Actuator fica em OUTRA porta (porta de gestão)? */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
class ManagementPortSecurityTest {
    @LocalServerPort
    int port;
    @LocalManagementPort
    int mgmt;

    private int status(int p, String path, String user) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + p + path)).GET();
        if (user != null) {
            b.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":lab-only").getBytes()));
        }
        int s = HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
        System.out.println("T7 " + (p == mgmt ? "gestão " : "app    ") + path + (user == null ? " (sem credencial)" : " (" + user + ")") + " -> " + s);
        return s;
    }

    @Test
    void managementPortWithTheMainChain() throws Exception {
        int health = status(mgmt, "/actuator/health", null);
        int prometheusAnon = status(mgmt, "/actuator/prometheus", null);
        int prometheusUser = status(mgmt, "/actuator/prometheus", "ana");
        int prometheusAdmin = status(mgmt, "/actuator/prometheus", "ops");
        int scraper = status(mgmt, "/actuator/prometheus", "scraper");
        int api = status(port, "/api/v1/hello", null);
        int apiScraper = status(port, "/api/v1/hello", "scraper");
        int actuatorOnApp = status(port, "/actuator/health", "ops");
        assertThat(api).isEqualTo(401);
        assertThat(prometheusAnon).isEqualTo(401);
        assertThat(prometheusUser).isEqualTo(403);
        assertThat(prometheusAdmin).isEqualTo(200);
        assertThat(scraper).isEqualTo(200);
        assertThat(apiScraper).as("o papel METRICS também é autenticado na API; a regra de objeto da API (J8) é quem nega dados alheios").isEqualTo(200);
        assertThat(actuatorOnApp).as("o Actuator NÃO existe na porta da aplicação").isEqualTo(404);
        System.out.println("T7 resumo: health=" + health + " prometheus anon/USER/ADMIN=" + prometheusAnon + "/" + prometheusUser + "/" + prometheusAdmin + " scraper=" + scraper);
    }
}
