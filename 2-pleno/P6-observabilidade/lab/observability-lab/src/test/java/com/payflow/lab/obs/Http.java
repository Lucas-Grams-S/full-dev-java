package com.payflow.lab.obs;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Cliente mínimo para os testes: duas portas (aplicação e gestão), cabeçalhos opcionais. */
final class Http {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    record Res(int status, String body, java.net.http.HttpHeaders headers) {
    }

    static Res get(int port, String path, Map<String, String> headers) {
        return send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET(), headers);
    }

    static Res post(int port, String path, String json, Map<String, String> headers) {
        return send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)), headers);
    }

    private static Res send(HttpRequest.Builder b, Map<String, String> headers) {
        headers.forEach(b::header);
        try {
            HttpResponse<String> r = CLIENT.send(b.timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
            return new Res(r.statusCode(), r.body(), r.headers());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String transfer(String simulate) {
        return "{\"from\":\"acc-1\",\"to\":\"acc-2\",\"amount\":\"10.00\",\"simulate\":" + (simulate == null ? "null" : "\"" + simulate + "\"") + "}";
    }
}
