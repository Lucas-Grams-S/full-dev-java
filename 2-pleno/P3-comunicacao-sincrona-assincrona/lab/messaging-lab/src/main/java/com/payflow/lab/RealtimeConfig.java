package com.payflow.lab;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** P3.11: SSE (servidor → cliente, sobre HTTP) e WebSocket (bidirecional). */
@Configuration
@EnableWebSocket
class RealtimeConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new EchoHandler(), "/ws/echo").setAllowedOrigins("http://localhost");
    }

    static class EchoHandler extends TextWebSocketHandler {
        @Override protected void handleTextMessage(WebSocketSession s, TextMessage m) throws IOException {
            s.sendMessage(new TextMessage("eco:" + m.getPayload()));
        }
    }
}

@RestController
class SseController {
    record Event(long id, String data) {}
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<String, List<Event>> history = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    @GetMapping(value = "/sse/wallets/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter subscribe(@PathVariable String id, @RequestHeader(value = "Last-Event-ID", required = false) String lastId) throws IOException {
        var emitter = new SseEmitter(60_000L);
        emitters.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> emitters.get(id).remove(emitter);
        emitter.onCompletion(cleanup); emitter.onTimeout(cleanup); emitter.onError(e -> cleanup.run());
        if (lastId != null) {                                   // reconexão: reenvia o que o cliente perdeu
            long last = Long.parseLong(lastId);
            for (Event e : history.getOrDefault(id, List.of())) if (e.id() > last) send(emitter, e);
        }
        emitter.send(SseEmitter.event().comment("conectado"));
        return emitter;
    }

    @PostMapping("/sse/wallets/{id}/publish")
    void publish(@PathVariable String id, @RequestBody String data) {
        var e = new Event(seq.incrementAndGet(), data);
        history.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(e);
        for (var em : emitters.getOrDefault(id, List.of())) {
            try { send(em, e); } catch (IOException ex) { em.completeWithError(ex); }
        }
    }

    private void send(SseEmitter em, Event e) throws IOException {
        em.send(SseEmitter.event().id(String.valueOf(e.id())).name("balance").data(e.data()));
    }
}
