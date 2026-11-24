package com.payflow.lab.obs;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

/** P6.02/P6.03: o que o Micrometer mede sozinho, o que o PayFlow acrescenta e como não estragar o Prometheus. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false"})
class MetricsTest {
    @LocalServerPort
    int port;
    @LocalManagementPort
    int mgmt;
    @Autowired
    MeterRegistry registry;

    @Test
    void transferObservationBecomesATimerByOutcomeAndHttpMetricsAppearForFree() {
        Scrape b = new Scrape(mgmt);
        double before = b.value("payflow_transfer_seconds_count", "outcome=\"completed\"");
        double beforeRejected = b.value("payflow_transfer_seconds_count", "outcome=\"rejected\"");
        double beforeFailed = b.value("payflow_transfer_seconds_count", "outcome=\"failed\"");
        double h201 = b.value("http_server_requests_seconds_count", "uri=\"/api/v1/transfers\"", "status=\"201\"");
        double h422 = b.value("http_server_requests_seconds_count", "uri=\"/api/v1/transfers\"", "status=\"422\"");
        double h500 = b.value("http_server_requests_seconds_count", "uri=\"/api/v1/transfers\"", "status=\"500\"");
        for (int i = 0; i < 3; i++) {
            Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of());
        }
        Http.post(port, "/api/v1/transfers", Http.transfer("insufficient"), Map.of());
        Http.post(port, "/api/v1/transfers", Http.transfer("error"), Map.of());
        Scrape s = new Scrape(mgmt);
        s.lines("payflow_transfer_seconds_count").forEach(l -> LogLines.print("T2", "timer de negócio", l));
        s.lines("http_server_requests_seconds_count", "uri=\"/api/v1/transfers\"").forEach(l -> LogLines.print("T2", "RED (taxa e erros)", l));
        assertThat(s.value("payflow_transfer_seconds_count", "outcome=\"completed\"") - before).isEqualTo(3);
        assertThat(s.value("payflow_transfer_seconds_count", "outcome=\"rejected\"") - beforeRejected).isEqualTo(1);
        assertThat(s.value("payflow_transfer_seconds_count", "outcome=\"failed\"") - beforeFailed).isEqualTo(1);
        assertThat(s.value("http_server_requests_seconds_count", "uri=\"/api/v1/transfers\"", "status=\"201\"") - h201).isEqualTo(3);
        assertThat(s.value("http_server_requests_seconds_count", "uri=\"/api/v1/transfers\"", "status=\"422\"") - h422).isEqualTo(1);
        assertThat(s.value("http_server_requests_seconds_count", "uri=\"/api/v1/transfers\"", "status=\"500\"") - h500).isEqualTo(1);
        // o Logback também vira métrica: erros inesperados são contáveis (e alertáveis) sem ler o log
        s.lines("logback_events_total").forEach(l -> LogLines.print("T2", "log vira métrica", l));
        assertThat(s.value("logback_events_total", "level=\"error\"")).isGreaterThanOrEqualTo(1);
        assertThat(s.value("logback_events_total", "level=\"warn\"")).isGreaterThanOrEqualTo(1);   // a rejeição de negócio é WARN
        // o cliente HTTP (chamada ao serviço de risco) também é medido, sem código
        s.lines("http_client_requests_seconds_count").forEach(l -> LogLines.print("T2", "cliente HTTP", l));
        assertThat(s.value("http_client_requests_seconds_count", "uri=\"/internal/v1/risk?account={a}\"")).as("uri é o TEMPLATE (com a query); concatenar a URL à mão gera uma série por valor").isGreaterThanOrEqualTo(3);
        // etiqueta comum em TODA métrica
        assertThat(s.all().stream().filter(l -> l.contains("{")).filter(l -> !l.startsWith("target_info")))
                .as("a etiqueta comum application está em toda série").allMatch(l -> l.contains("application=\"payflow-obs-lab\""));
        assertThat(s.lines("jvm_memory_used_bytes", "application=\"payflow-obs-lab\"")).isNotEmpty();
    }

    @Test
    void familiesTheBootMeasuresWithoutAnyCode() {
        Http.get(port, "/api/v1/accounts/acc-1", Map.of());
        Scrape all = new Scrape(mgmt);
        var s = new Scrape(String.join("\n", all.all().stream().filter(l -> !l.startsWith("bad_")).toList()));   // sem o experimento "errado de propósito"
        var names = s.all().stream().map(l -> l.substring(0, Math.min(l.indexOf('{') < 0 ? l.indexOf(' ') : l.indexOf('{'), 200)))
                .map(n -> n.replaceAll("_(count|sum|bucket|max|total)$", "")).distinct().sorted().toList();
        var prefixes = names.stream().map(n -> n.split("_")[0]).distinct().sorted().toList();
        LogLines.print("T2", "séries no scrape / famílias de medidores", s.all().size() + " / " + names.size());
        LogLines.print("T2", "prefixos das famílias", prefixes);
        assertThat(prefixes).contains("jvm", "http", "process", "tomcat", "logback", "executor", "system");
    }

    @Test
    void gaugeShowsTheOutboxBacklogAndCounterTheThroughput() throws Exception {
        Http.post(port, "/lab/switch?what=outbox-stall&on=true", "", Map.of());
        for (int i = 0; i < 4; i++) {
            Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of());
        }
        Thread.sleep(300);
        Scrape s = new Scrape(mgmt);
        LogLines.print("T2", "outbox parado", s.lines("payflow_outbox_pending") + " " + s.lines("payflow_outbox_published_total"));
        assertThat(s.value("payflow_outbox_pending")).isGreaterThanOrEqualTo(4);
        Http.post(port, "/lab/switch?what=outbox-stall&on=false", "", Map.of());
    }

    @Test
    void uriTagUsesTheTemplateSoManyIdsAreOneSeries() {
        for (int i = 0; i < 150; i++) {
            Http.get(port, "/api/v1/accounts/acc-" + i, Map.of());
        }
        Http.get(port, "/nao/existe/" + 1, Map.of());
        Scrape s = new Scrape(mgmt);
        var accountSeries = s.lines("http_server_requests_seconds_count", "uri=\"/api/v1/accounts/{id}\"");
        accountSeries.forEach(l -> LogLines.print("T2", "150 ids, 1 série", l));
        assertThat(accountSeries).hasSize(1);
        assertThat(s.value("http_server_requests_seconds_count", "uri=\"/api/v1/accounts/{id}\"")).isEqualTo(150);
        assertThat(s.all()).noneMatch(l -> l.contains("acc-1"));
        s.lines("http_server_requests_seconds_count", "status=\"404\"").forEach(l -> LogLines.print("T2", "404", l));
        assertThat(s.lines("http_server_requests_seconds_count", "status=\"404\"")).isNotEmpty();
        assertThat(s.all()).noneMatch(l -> l.contains("uri=\"/nao/existe"));   // o caminho cru não vira tag
    }

    @Test
    void highCardinalityTagMultipliesSeries_theAccountIdMustStayInTheSpanNotInTheMetric() {
        int before = registry.getMeters().size();
        for (int i = 0; i < 1000; i++) {
            Counter.builder("bad.transfers").tag("account", "acc-" + i).register(registry).increment();   // ERRADO de propósito
        }
        int after = registry.getMeters().size();
        long lines = new Scrape(mgmt).lines("bad_transfers_total").size();
        LogLines.print("T2", "medidores antes -> depois de 1000 contas como tag", before + " -> " + after + " (linhas no scrape: " + lines + ")");
        assertThat(after - before).isEqualTo(1000);
        assertThat(lines).isEqualTo(1000);
        // o filtro de meters (MeterFilter) é a rede de segurança: negar o que passar do limite
        registry.config().meterFilter(io.micrometer.core.instrument.config.MeterFilter.maximumAllowableTags("bad.guarded", "account", 10,
                io.micrometer.core.instrument.config.MeterFilter.deny()));
        for (int i = 0; i < 1000; i++) {
            Counter.builder("bad.guarded").tag("account", "acc-" + i).register(registry).increment();
        }
        long guarded = new Scrape(mgmt).lines("bad_guarded_total").size();
        LogLines.print("T2", "com maximumAllowableTags(10)", guarded + " linhas");
        assertThat(guarded).isEqualTo(10);
    }

    @Test
    void histogramBucketsAndSloBoundariesAreExposedForTheRequestTimer() {
        Http.post(port, "/api/v1/transfers", Http.transfer("slow"), Map.of());
        Scrape s = new Scrape(mgmt);
        var buckets = s.lines("http_server_requests_seconds_bucket", "uri=\"/api/v1/transfers\"", "status=\"201\"");
        buckets.stream().limit(40).filter(l -> l.contains("le=\"0.05\"") || l.contains("le=\"0.25\"") || l.contains("le=\"0.5\"") || l.contains("le=\"+Inf\""))
                .forEach(l -> LogLines.print("T2", "bucket", l));
        LogLines.print("T2", "total de buckets desse timer (uma série)", buckets.size());
        assertThat(buckets.stream().anyMatch(l -> l.contains("le=\"0.05\""))).isTrue();   // SLO 50ms declarado
        assertThat(buckets.stream().anyMatch(l -> l.contains("le=\"+Inf\""))).isTrue();
        assertThat(s.value("http_server_requests_seconds_bucket", "uri=\"/api/v1/transfers\"", "status=\"201\"", "le=\"0.25\""))
                .as("a requisição lenta (300 ms) NÃO cabe em 250 ms").isLessThan(s.value("http_server_requests_seconds_bucket", "uri=\"/api/v1/transfers\"", "status=\"201\"", "le=\"+Inf\""));
    }

    @Test
    void actuatorLivesOnlyOnTheManagementPortAndOnlyWhatWasExposed() {
        assertThat(Http.get(port, "/actuator/prometheus", Map.of()).status()).isEqualTo(404);
        assertThat(Http.get(port, "/actuator/health", Map.of()).status()).isEqualTo(404);
        assertThat(Http.get(mgmt, "/actuator/prometheus", Map.of()).status()).isEqualTo(200);
        assertThat(Http.get(mgmt, "/actuator/env", Map.of()).status()).as("não exposto").isEqualTo(404);
        assertThat(Http.get(mgmt, "/actuator/metrics", Map.of()).status()).as("não exposto").isEqualTo(404);
        var r = Http.get(mgmt, "/actuator/prometheus", Map.of());
        LogLines.print("T2", "Content-Type do scrape", r.headers().firstValue("Content-Type").orElse("?"));
        LogLines.print("T2", "/actuator/prometheus na porta da aplicação", Http.get(port, "/actuator/prometheus", Map.of()).status());
    }
}
