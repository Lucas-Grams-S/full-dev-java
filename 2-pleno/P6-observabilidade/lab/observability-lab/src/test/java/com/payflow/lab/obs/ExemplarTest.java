package com.payflow.lab.obs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Exemplar: a ponte da métrica (um balde lento no gráfico) para o traço (a requisição que caiu nele). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false"})
class ExemplarTest {
    @LocalServerPort
    int port;
    @LocalManagementPort
    int mgmt;

    @Test
    void openMetricsScrapeCarriesTraceIdExemplars() {
        Http.post(port, "/api/v1/transfers", Http.transfer("slow"), Map.of());
        var plain = Http.get(mgmt, "/actuator/prometheus", Map.of());
        var om = Http.get(mgmt, "/actuator/prometheus", Map.of("Accept", "application/openmetrics-text; version=1.0.0"));
        LogLines.print("T4", "exemplar: Content-Type OpenMetrics", om.headers().firstValue("Content-Type").orElse("?"));
        String withExemplar = om.body().lines().filter(l -> l.startsWith("http_server_requests_seconds_bucket") && l.contains("/api/v1/transfers") && l.contains("trace_id")).findFirst().orElse("(nenhuma)");
        LogLines.print("T4", "exemplar: linha", withExemplar);
        assertThat(plain.body()).as("o formato texto clássico não carrega exemplars").doesNotContain("trace_id");
        assertThat(withExemplar).containsPattern("# \\{[^}]*trace_id=\"[0-9a-f]{32}\"");
    }
}
