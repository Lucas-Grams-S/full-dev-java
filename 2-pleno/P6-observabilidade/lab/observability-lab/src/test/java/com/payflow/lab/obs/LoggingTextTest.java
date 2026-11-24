package com.payflow.lab.obs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;

/** O formato de TEXTO (desenvolvimento): mesmo conteúdo, legível para humanos. */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false"})
class LoggingTextTest {
    @LocalServerPort
    int port;

    @Test
    void textFormatShowsTraceIdAndRequestId(CapturedOutput out) {
        Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of("X-Request-Id", "req-text-0001"));
        String line = LogLines.text(out, "transfer completed").getFirst();
        LogLines.print("T1", "linha em texto", line);
        assertThat(line).contains("requestId=req-text-0001").containsPattern("\\[payflow-obs-lab] \\[.+?] \\[[0-9a-f]{32}-[0-9a-f]{16}]");
        String outside = LogLines.text(out, "Started LoggingTextTest").stream().findFirst().orElse("(nao capturada)");
        LogLines.print("T1", "linha fora de requisição", outside);
    }
}
