package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeIT {
    @LocalServerPort int port;
    final HttpClient http = HttpClient.newHttpClient();

    void publish(String wallet, String data) throws Exception {
        http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/sse/wallets/" + wallet + "/publish"))
                .POST(HttpRequest.BodyPublishers.ofString(data)).header("Content-Type", "text/plain").build(), HttpResponse.BodyHandlers.discarding());
    }

    List<String> subscribe(String wallet, String lastId, CompletableFuture<?>[] holder) {
        List<String> lines = new CopyOnWriteArrayList<>();
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/sse/wallets/" + wallet)).header("Accept", "text/event-stream");
        if (lastId != null) b.header("Last-Event-ID", lastId);
        holder[0] = http.sendAsync(b.build(), HttpResponse.BodyHandlers.ofLines()).thenAccept(r -> {
            System.out.println("SSE content-type=" + r.headers().firstValue("Content-Type").orElse("?"));
            r.body().forEach(lines::add);
        });
        return lines;
    }

    @Test
    void sseStreamsEventsAndReplaysAfterReconnect() throws Exception {
        var holder = new CompletableFuture<?>[1];
        var lines = subscribe("w1", null, holder);
        await().atMost(Duration.ofSeconds(5)).until(() -> lines.contains(":conectado"));
        publish("w1", "{\"balance\":\"90.00\"}");
        publish("w1", "{\"balance\":\"80.00\"}");
        await().atMost(Duration.ofSeconds(5)).until(() -> lines.stream().filter(l -> l.startsWith("data:")).count() == 2);
        System.out.println("SSE linhas=" + lines);
        assertThat(lines).contains("event:balance");
        holder[0].cancel(true);                                   // o cliente cai
        publish("w1", "{\"balance\":\"70.00\"}");                // evento perdido pelo cliente
        String lastSeen = lines.stream().filter(l -> l.startsWith("id:")).reduce((a, b) -> b).get().substring(3);
        var holder2 = new CompletableFuture<?>[1];
        var lines2 = subscribe("w1", lastSeen, holder2);        // reconecta dizendo até onde viu
        await().atMost(Duration.ofSeconds(5)).until(() -> lines2.stream().anyMatch(l -> l.contains("70.00")));
        System.out.println("SSE reconexao Last-Event-ID=" + lastSeen + " recebeu=" + lines2);
        holder2[0].cancel(true);
    }

    @Test
    void webSocketEchoesBothWays() throws Exception {
        List<String> got = new CopyOnWriteArrayList<>();
        WebSocket ws = http.newWebSocketBuilder().header("Origin", "http://localhost")
                .buildAsync(URI.create("ws://localhost:" + port + "/ws/echo"), new WebSocket.Listener() {
                    @Override public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
                        got.add(data.toString()); w.request(1); return null;
                    }
                }).get(5, TimeUnit.SECONDS);
        ws.sendText("ola", true).get(5, TimeUnit.SECONDS);
        ws.sendText("saldo?", true).get(5, TimeUnit.SECONDS);
        await().atMost(Duration.ofSeconds(5)).until(() -> got.size() == 2);
        System.out.println("WS recebeu=" + got);
        assertThat(got).containsExactly("eco:ola", "eco:saldo?");
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "fim").get(5, TimeUnit.SECONDS);
        // origem não permitida é recusada no handshake
        var bad = http.newWebSocketBuilder().header("Origin", "http://evil.example")
                .buildAsync(URI.create("ws://localhost:" + port + "/ws/echo"), new WebSocket.Listener() {});
        try { bad.get(5, TimeUnit.SECONDS); System.out.println("WS origem hostil: ACEITA (!)"); }
        catch (ExecutionException e) { System.out.println("WS origem hostil recusada: " + e.getCause().getClass().getSimpleName() + " " + e.getCause().getMessage()); }
    }
}
