package com.payflow.lab.obs;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;

/** Amostragem 0: nenhum span é exportado, mas o traceId continua nos logs (correlação não depende de amostrar). */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false", "management.tracing.sampling.probability=0.0",
                "logging.structured.format.console=ecs"})
@Import(TracingSamplingTest.Memory.class)
class TracingSamplingTest {
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

    @Test
    void notSampledMeansNoSpansButStillATraceIdInTheLogAndInTheProblemResponse(CapturedOutput out) throws Exception {
        var r = Http.post(port, "/api/v1/transfers", Http.transfer("error"), Map.of());
        Thread.sleep(300);
        JsonNode line = LogLines.json(out, "unexpected failure").getFirst();
        LogLines.print("T4", "amostragem 0.0: spans exportados", exporter.getFinishedSpanItems().size());
        LogLines.print("T4", "amostragem 0.0: traceId no log do ERROR", line.path("traceId").asString());
        assertThat(exporter.getFinishedSpanItems()).isEmpty();
        assertThat(line.path("traceId").asString()).matches("[0-9a-f]{32}");
        assertThat(r.body()).contains(line.path("traceId").asString());
    }
}
