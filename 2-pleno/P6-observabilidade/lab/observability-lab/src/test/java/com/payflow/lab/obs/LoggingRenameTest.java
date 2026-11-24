package com.payflow.lab.obs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;

/** Padronizar nomes de campo (ECS quer trace.id/span.id): rename + add, sem escrever código. */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false",
                "logging.structured.format.console=ecs",
                "logging.structured.json.rename[traceId]=trace.id", "logging.structured.json.rename[spanId]=span.id",
                "logging.structured.json.add.deployment=lab"})
class LoggingRenameTest {
    @LocalServerPort
    int port;

    @Test
    void renameAndAddCustomizeFieldNames(CapturedOutput out) {
        Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of());
        JsonNode line = LogLines.json(out, "transfer completed").getFirst();
        LogLines.print("T1", "com rename/add", line);
        assertThat(line.has("traceId")).isFalse();
        assertThat(line.has("trace.id")).as("chave plana, como o ECS pede").isTrue();
        assertThat(line.path("deployment").asString()).isEqualTo("lab");
    }
}
