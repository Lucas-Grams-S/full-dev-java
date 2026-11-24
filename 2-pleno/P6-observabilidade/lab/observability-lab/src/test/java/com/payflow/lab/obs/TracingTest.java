package com.payflow.lab.obs;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** P6.05: um traço atravessa servidor, observação de negócio, cliente HTTP e outro servidor. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false"})
@Import(TracingTest.Memory.class)
class TracingTest {
    @TestConfiguration
    static class Memory {
        @Bean
        InMemorySpanExporter memoryExporter() {
            return InMemorySpanExporter.create();
        }

        @Bean
        SpanProcessor memoryProcessor(InMemorySpanExporter exporter) {
            return SimpleSpanProcessor.create(exporter);
        }
    }

    @LocalServerPort
    int port;
    @Autowired
    InMemorySpanExporter exporter;

    @BeforeEach
    void clear() {
        exporter.reset();
    }

    private static String shortId(String id) {
        return id == null || id.equals("0000000000000000") ? "-" : id.substring(0, 6);
    }

    private List<SpanData> spans() {
        long end = System.currentTimeMillis() + 3000;
        while (exporter.getFinishedSpanItems().size() < 4 && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return exporter.getFinishedSpanItems().stream().sorted(Comparator.comparingLong(SpanData::getStartEpochNanos)).toList();
    }

    private static void print(List<SpanData> spans) {
        for (SpanData s : spans) {
            LogLines.print("T4", "span", String.format("trace=%s id=%s parent=%s kind=%-8s name=%s", shortId(s.getTraceId()), shortId(s.getSpanId()),
                    shortId(s.getParentSpanId()), s.getKind(), s.getName()));
        }
    }

    @Test
    void oneRequestIsOneTraceWithFourSpansLinkedByParent() {
        Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of());
        List<SpanData> spans = spans();
        print(spans);
        assertThat(spans).extracting(SpanData::getTraceId).containsOnly(spans.getFirst().getTraceId());
        SpanData server = spans.stream().filter(s -> s.getName().equals("http post /api/v1/transfers")).findFirst().orElseThrow();
        SpanData business = spans.stream().filter(s -> s.getName().equals("payflow.transfer")).findFirst().orElseThrow();
        SpanData client = spans.stream().filter(s -> s.getName().startsWith("http get") && s.getKind().name().equals("CLIENT")).findFirst().orElseThrow();
        SpanData risk = spans.stream().filter(s -> s.getName().equals("http get /internal/v1/risk") && s.getKind().name().equals("SERVER")).findFirst().orElseThrow();
        assertThat(business.getParentSpanId()).isEqualTo(server.getSpanId());
        assertThat(client.getParentSpanId()).isEqualTo(business.getSpanId());
        assertThat(risk.getParentSpanId()).as("o traceparent atravessou a rede").isEqualTo(client.getSpanId());
        // alta cardinalidade vai no span (nunca na métrica)
        assertThat(business.getAttributes().asMap().toString()).contains("payflow.source.account=acc-1").contains("outcome=completed");
        LogLines.print("T4", "atributos do span de negócio", business.getAttributes());
    }

    @Test
    void anIncomingTraceparentIsJoinedNotReplaced() {
        String trace = "0af7651916cd43dd8448eb211c80319c";
        Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of("traceparent", "00-" + trace + "-b7ad6b7169203331-01"));
        List<SpanData> spans = spans();
        print(spans);
        SpanData server = spans.stream().filter(s -> s.getName().equals("http post /api/v1/transfers")).findFirst().orElseThrow();
        assertThat(server.getTraceId()).isEqualTo(trace);
        assertThat(server.getParentSpanId()).isEqualTo("b7ad6b7169203331");
    }

    @Test
    void aClientBuiltWithRestClientCreateIsNotInstrumentedAndBreaksTheTrace() {
        Http.get(port, "/lab/plain-risk?account=acc-1", Map.of());
        List<SpanData> spans = spans();
        print(spans);
        SpanData entry = spans.stream().filter(s -> s.getName().equals("http get /lab/plain-risk")).findFirst().orElseThrow();
        SpanData risk = spans.stream().filter(s -> s.getName().equals("http get /internal/v1/risk")).findFirst().orElseThrow();
        LogLines.print("T4", "RestClient.create(): spans CLIENT", spans.stream().filter(s -> s.getKind().name().equals("CLIENT")).count());
        LogLines.print("T4", "RestClient.create(): mesmo traceId nos dois servidores?", entry.getTraceId().equals(risk.getTraceId()));
        assertThat(spans.stream().filter(s -> s.getKind().name().equals("CLIENT"))).isEmpty();
        assertThat(risk.getTraceId()).as("o traço QUEBRA: o serviço de risco abriu outro traço").isNotEqualTo(entry.getTraceId());
        assertThat(risk.getParentSpanId()).isEqualTo("0000000000000000");
    }

    @Test
    void failureMarksTheSpanAsErrorWithTheException() {
        Http.post(port, "/api/v1/transfers", Http.transfer("error"), Map.of());
        List<SpanData> spans = spans();
        SpanData business = spans.stream().filter(s -> s.getName().equals("payflow.transfer")).findFirst().orElseThrow();
        LogLines.print("T4", "span com falha", business.getStatus().getStatusCode() + " eventos=" + business.getEvents().stream().map(e -> e.getName()).toList()
                + " outcome=" + business.getAttributes().asMap().entrySet().stream().filter(e -> e.getKey().getKey().equals("outcome")).findFirst().map(Map.Entry::getValue).orElse(null));
        assertThat(business.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(business.getEvents()).extracting("name").contains("exception");
    }

    @Test
    void rejectionIsABusinessOutcomeAndStillAnErrorSpanUnlessWeSayOtherwise() {
        Http.post(port, "/api/v1/transfers", Http.transfer("insufficient"), Map.of());
        SpanData business = spans().stream().filter(s -> s.getName().equals("payflow.transfer")).findFirst().orElseThrow();
        LogLines.print("T4", "span de rejeição de negócio (422)", business.getStatus().getStatusCode());
        assertThat(business.getAttributes().asMap().toString()).contains("outcome=rejected");
    }
}
