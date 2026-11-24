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
import tools.jackson.databind.json.JsonMapper;

/** P6.01: logs estruturados (ECS) com requestId (negócio) e traceId (técnico). */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false",
                "logging.structured.format.console=ecs"})
class LoggingTest {
    @LocalServerPort
    int port;

    @Test
    void successLogIsOneJsonLineWithRequestIdAndTraceId(CapturedOutput out) {
        var r = Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of("X-Request-Id", "req-12345678"));
        assertThat(r.status()).isEqualTo(201);
        await(out, "transfer completed");
        JsonNode line = LogLines.json(out, "transfer completed").getFirst();
        LogLines.print("T1", "linha ECS", line);
        assertThat(line.path("log").path("level").asString()).isEqualTo("INFO");
        assertThat(line.path("requestId").asString()).isEqualTo("req-12345678");
        assertThat(line.path("traceId").asString()).hasSize(32).matches("[0-9a-f]{32}");
        assertThat(line.path("spanId").asString()).hasSize(16);
        assertThat(line.path("service").path("name").asString()).isEqualTo("payflow-obs-lab");
        assertThat(line.has("error")).isFalse();
        // o X-Request-Id volta na resposta (o usuário pode citá-lo ao suporte)
        assertThat(r.headers().firstValue("X-Request-Id")).hasValue("req-12345678");
    }

    @Test
    void businessRejectionIsWarnWithoutStackTraceAndNotAnError(CapturedOutput out) {
        var r = Http.post(port, "/api/v1/transfers", Http.transfer("insufficient"), Map.of("X-Request-Id", "req-reject-001"));
        assertThat(r.status()).isEqualTo(422);
        await(out, "transfer rejected");
        JsonNode line = LogLines.json(out, "transfer rejected").getFirst();
        LogLines.print("T1", "rejeição de negócio", line);
        assertThat(line.path("log").path("level").asString()).isEqualTo("WARN");
        assertThat(line.has("error")).as("sem stack trace").isFalse();
        assertThat(LogLines.json(out, "\"level\":\"ERROR\"")).isEmpty();
    }

    @Test
    void unexpectedFailureLogsErrorOnceWithStackAndTheProblemCarriesTheSameTraceId(CapturedOutput out) {
        var r = Http.post(port, "/api/v1/transfers", Http.transfer("error"), Map.of("X-Request-Id", "req-error-001"));
        assertThat(r.status()).isEqualTo(500);
        await(out, "unexpected failure");
        var errors = LogLines.json(out, "unexpected failure");
        assertThat(errors).as("ERROR uma única vez (na borda)").hasSize(1);
        JsonNode line = errors.getFirst();
        JsonNode problem = JsonMapper.builder().build().readTree(r.body());
        LogLines.print("T1", "ERROR (sem a stack)", line.path("log").path("level").asString() + " error.type=" + line.path("error").path("type").asString()
                + " stack_trace.length=" + line.path("error").path("stack_trace").asString().length());
        LogLines.print("T1", "corpo da resposta (Problem Details)", r.body());
        assertThat(line.path("log").path("level").asString()).isEqualTo("ERROR");
        assertThat(line.path("error").path("type").asString()).isEqualTo("java.lang.IllegalStateException");
        assertThat(line.path("error").path("stack_trace").asString()).contains("TransferService.transfer");
        assertThat(problem.path("traceId").asString()).isEqualTo(line.path("traceId").asString());
        assertThat(problem.path("detail").asString()).doesNotContain("ledger");   // a resposta não vaza o interno
    }

    @Test
    void unknownRouteIsA404WithoutAnErrorLine(CapturedOutput out) throws Exception {
        var r = Http.get(port, "/nao/existe", Map.of());
        Thread.sleep(200);
        LogLines.print("T1", "rota inexistente", r.status() + " " + r.body());
        assertThat(r.status()).isEqualTo(404);
        assertThat(LogLines.json(out, "unexpected failure")).as("scanner batendo em rota inexistente não pode virar ERROR").isEmpty();
    }

    @Test
    void unsafeRequestIdIsReplacedNotEchoed(CapturedOutput out) {
        var r = Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of("X-Request-Id", "nao seguro {\"forjado\":1}"));
        String echoed = r.headers().firstValue("X-Request-Id").orElseThrow();
        LogLines.print("T1", "X-Request-Id inseguro virou", echoed);
        assertThat(echoed).matches("[0-9a-f-]{36}").doesNotContain("forjado");
    }

    private static void await(CapturedOutput out, String text) {
        long end = System.currentTimeMillis() + 3000;
        while (!out.getAll().contains(text) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
