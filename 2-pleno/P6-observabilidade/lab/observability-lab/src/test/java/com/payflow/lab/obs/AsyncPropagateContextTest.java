package com.payflow.lab.obs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false", "logging.structured.format.console=ecs", "spring.task.execution.propagate-context=true"})
class AsyncPropagateContextTest extends AsyncContextBase {
    @LocalServerPort
    int port;

    @Test
    void propagateContextRestoresTheTrace(CapturedOutput out) {
        Seen s = run(port, out, "propagate-context=true");
        assertThat(s.asyncTrace()).isEqualTo(s.httpTrace());
    }
}
