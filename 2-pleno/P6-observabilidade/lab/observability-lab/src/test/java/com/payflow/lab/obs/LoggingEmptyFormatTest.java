package com.payflow.lab.obs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Como o perfil dev volta ao texto se a base (produção) liga o JSON? Propriedade vazia. */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false", "logging.structured.format.console="})
class LoggingEmptyFormatTest {
    @LocalServerPort
    int port;

    @Test
    void emptyStructuredFormatFallsBackToText(CapturedOutput out) {
        Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of("X-Request-Id", "req-empty-0001"));
        String line = LogLines.text(out, "transfer completed").stream().findFirst().orElse("(nenhuma linha em texto)");
        LogLines.print("T1", "formato vazio", line.length() > 160 ? line.substring(0, 160) + "..." : line);
        assertThat(line).contains("requestId=req-empty-0001");
        assertThat(LogLines.json(out, "transfer completed")).isEmpty();
    }
}
