package com.payflow.lab.gateway;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Serviço de destino falso: devolve o caminho e cabeçalhos que RECEBEU, com latência e status configuráveis. */
public class EchoService implements AutoCloseable {

    public final AtomicInteger calls = new AtomicInteger();
    public volatile long delayMs = 0;
    public volatile int status = 200;
    private final HttpServer server;

    public EchoService(String name) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", ex -> {
            calls.incrementAndGet();
            try {
                ex.getRequestBody().readAllBytes();
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
                var h = ex.getRequestHeaders();
                String json = "{\"service\":\"" + name + "\",\"method\":\"" + ex.getRequestMethod() + "\",\"path\":\"" + ex.getRequestURI().getPath()
                        + "\",\"xGateway\":\"" + h.getFirst("X-Gateway") + "\",\"xForwardedFor\":\"" + h.getFirst("X-Forwarded-For")
                        + "\",\"xForwardedHost\":\"" + h.getFirst("X-Forwarded-Host") + "\",\"xForwardedProto\":\"" + h.getFirst("X-Forwarded-Proto")
                        + "\",\"xForwardedPrefix\":\"" + h.getFirst("X-Forwarded-Prefix") + "\",\"authorization\":\"" + h.getFirst("Authorization") + "\",\"headerNames\":\"" + h.keySet() + "\"}";
                byte[] body = json.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(status, body.length);
                ex.getResponseBody().write(body);
            } catch (IOException | InterruptedException e) {
                // cliente (o gateway) desistiu
            } finally {
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
