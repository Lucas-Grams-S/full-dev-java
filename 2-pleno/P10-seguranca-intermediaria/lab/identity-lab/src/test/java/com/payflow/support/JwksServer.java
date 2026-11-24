package com.payflow.support;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Um "Keycloak de bolso" só para as chaves: serve o JWKS (as chaves PÚBLICAS) e conta quantas vezes foi consultado. */
public final class JwksServer implements AutoCloseable {

    private final HttpServer server;
    private final AtomicReference<List<RSAKey>> keys = new AtomicReference<>(List.of());
    private final AtomicInteger fetches = new AtomicInteger();

    public JwksServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/jwks", exchange -> {
            fetches.incrementAndGet();
            byte[] body = new JWKSet(List.copyOf(keys.get())).toString(true).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    public void publish(RSAKey... newKeys) {
        keys.set(List.of(newKeys));
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
    }

    public int fetches() {
        return fetches.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
