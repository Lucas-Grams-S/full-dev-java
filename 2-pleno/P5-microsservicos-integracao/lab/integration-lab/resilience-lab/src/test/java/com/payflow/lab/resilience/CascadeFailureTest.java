package com.payflow.lab.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** P5.10: o serviço de notificações degrada (2 s por chamada) e o checkout... */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CascadeFailureTest {

    static FakeService notifications;

    @BeforeAll
    static void start() throws Exception {
        notifications = new FakeService();
    }

    @AfterAll
    static void stop() {
        notifications.close();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        if (notifications == null) {
            notifications = new FakeService();
        }
        r.add("lab.notifications.url", notifications::url);
    }

    @LocalServerPort
    int port;

    record Result(int status, long ms) { }

    Result call(HttpClient http, String method, String path, Duration timeout) {
        long t0 = System.nanoTime();
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(timeout);
            HttpRequest req = method.equals("POST") ? b.POST(HttpRequest.BodyPublishers.noBody()).build() : b.GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return new Result(resp.statusCode(), (System.nanoTime() - t0) / 1_000_000);
        } catch (Exception e) {
            return new Result(-1, (System.nanoTime() - t0) / 1_000_000); // -1 = timeout do chamador
        }
    }

    List<Result> blast(String path, int n) throws Exception {
        HttpClient http = HttpClient.newBuilder().executor(Executors.newVirtualThreadPerTaskExecutor()).build();
        List<CompletableFuture<Result>> fs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            fs.add(CompletableFuture.supplyAsync(() -> call(http, "POST", path, Duration.ofSeconds(8)), Executors.newVirtualThreadPerTaskExecutor()));
        }
        List<Result> out = new ArrayList<>();
        for (CompletableFuture<Result> f : fs) {
            out.add(f.get(30, TimeUnit.SECONDS));
        }
        return out;
    }

    static String summary(List<Result> rs) {
        long ok = rs.stream().filter(r -> r.status() == 200).count();
        List<Long> ms = new ArrayList<>(rs.stream().map(Result::ms).toList());
        Collections.sort(ms);
        String others = rs.stream().filter(r -> r.status() != 200).map(r -> r.status() + "@" + r.ms() + "ms").toList().toString();
        return ok + "/" + rs.size() + " com 200" + (ok == rs.size() ? "" : " (outros: " + others + ")") + "; mediana " + ms.get(ms.size() / 2) + " ms; maior " + ms.getLast() + " ms";
    }

    @Test
    void naiveCheckoutExhaustsThePoolAndTakesTheWholeAppDown() throws Exception {
        notifications.delayMs = 2_000;
        notifications.calls.set(0);
        HttpClient probe = HttpClient.newHttpClient();
        System.out.println("T10 saudavel: /ping = " + call(probe, "GET", "/ping", Duration.ofSeconds(3)));

        CompletableFuture<List<Result>> load = CompletableFuture.supplyAsync(() -> {
            try { return blast("/checkout-naive", 30); } catch (Exception e) { throw new RuntimeException(e); }
        });
        Thread.sleep(500); // as 10 threads já estão presas esperando o serviço lento
        Result ping = call(probe, "GET", "/ping", Duration.ofSeconds(1));
        System.out.println("T10 SEM protecao, 0,5 s depois: /ping (nao tem nada a ver com notificacoes) = " + ping
                + (ping.status() == -1 ? "  <- NAO respondeu em 1 s" : ""));
        List<Result> rs = load.get();
        System.out.println("T10 SEM protecao: 30 checkouts -> " + summary(rs) + "; chamadas ao servico lento=" + notifications.calls.get());
        assertThat(ping.status()).isEqualTo(-1);
    }

    @Test
    void protectedCheckoutKeepsServingAndDegradesGracefully() throws Exception {
        notifications.delayMs = 2_000;
        notifications.calls.set(0);
        HttpClient probe = HttpClient.newHttpClient();
        CompletableFuture<List<Result>> load = CompletableFuture.supplyAsync(() -> {
            try { return blast("/checkout-protected", 30); } catch (Exception e) { throw new RuntimeException(e); }
        });
        Thread.sleep(500);
        Result ping = call(probe, "GET", "/ping", Duration.ofSeconds(1));
        System.out.println("T10 COM protecao, 0,5 s depois: /ping = " + ping);
        List<Result> rs = load.get();
        System.out.println("T10 COM protecao: 30 checkouts -> " + summary(rs) + "; chamadas ao servico lento=" + notifications.calls.get());
        assertThat(ping.status()).isEqualTo(200);
        assertThat(rs).allMatch(r -> r.status() == 200); // com o fallback PÚBLICO: 30/30 (com visibilidade de pacote, 1 a 2 em 30 viravam 500)
    }
}
