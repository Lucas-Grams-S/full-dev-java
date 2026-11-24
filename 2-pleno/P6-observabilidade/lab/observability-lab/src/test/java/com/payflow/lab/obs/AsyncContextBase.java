package com.payflow.lab.obs;

import java.util.Map;
import org.springframework.boot.test.system.CapturedOutput;
import tools.jackson.databind.JsonNode;

/** Mede o que o log da thread do @Async enxerga (traceId/requestId) em cada configuração. */
abstract class AsyncContextBase {
    record Seen(String httpThread, String asyncThread, String httpTrace, String asyncTrace, String asyncRequestId) {
    }

    static Seen run(int port, CapturedOutput out, String label) {
        Http.post(port, "/api/v1/transfers", Http.transfer(null), Map.of("X-Request-Id", "req-async-0001"));
        long end = System.currentTimeMillis() + 3000;
        while (!out.getAll().contains("outbox event published") && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        JsonNode http = LogLines.json(out, "transfer completed").getLast();
        JsonNode async = LogLines.json(out, "outbox event published").getLast();
        Seen s = new Seen(http.path("process").path("thread").path("name").asString(), async.path("process").path("thread").path("name").asString(),
                http.path("traceId").asString(), async.path("traceId").asString(), async.path("requestId").asString());
        LogLines.print("T1", label, "thread " + s.httpThread() + " -> " + s.asyncThread() + " | traceId http='" + s.httpTrace().substring(0, 8)
                + "...' async='" + (s.asyncTrace().isEmpty() ? "" : s.asyncTrace().substring(0, 8) + "...") + "' | requestId async='" + s.asyncRequestId() + "'");
        return s;
    }
}
