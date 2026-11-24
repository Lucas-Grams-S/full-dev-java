package com.payflow.lab.resilience;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** "Serviço de notificações" falso: latência, falhas e contadores configuráveis. */
public class FakeService implements AutoCloseable {

    public final AtomicInteger calls = new AtomicInteger();
    public final AtomicInteger inFlight = new AtomicInteger();
    public final AtomicInteger maxInFlight = new AtomicInteger();
    public volatile long delayMs = 0;
    public volatile int status = 200;
    public volatile int failFirst = 0;
    private final HttpServer server;

    public FakeService() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", ex -> {
            int n = calls.incrementAndGet();
            int now = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(now, Math::max);
            try {
                ex.getRequestBody().readAllBytes();
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
                int code = n <= failFirst ? 503 : status;
                byte[] body = ("{\"ok\":" + (code < 400) + "}").getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(code, body.length);
                ex.getResponseBody().write(body);
            } catch (IOException | InterruptedException e) {
                // cliente desistiu (timeout): normal neste laboratório
            } finally {
                inFlight.decrementAndGet();
                ex.close();
            }
        });
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
