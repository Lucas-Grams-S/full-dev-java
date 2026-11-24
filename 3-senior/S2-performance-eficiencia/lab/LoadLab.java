import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * S2.06 - teste de carga: curva de saturacao e "omissao coordenada". Servidor HTTP real
 * (com.sun.net.httpserver, POOL FIXO de 16 threads, ~20 ms de servico modelado com Thread.sleep)
 * e geradores de carga em Java puro.
 *   java LoadLab.java saturation        -> latencia x taxa de chegada (carga ABERTA)
 *   java LoadLab.java omission          -> mesma falha vista por carga FECHADA e por carga ABERTA
 *   java LoadLab.java server <porta>    -> so o servidor (para o k6)
 * O servico e um MODELO: nao representa o PayFlow.
 */
public class LoadLab {
    static final int POOL = 16;
    static final AtomicLong stallUntil = new AtomicLong();       // nanoTime ate quando o servidor "congela"
    static HttpServer server;

    static HttpServer startServer(int port) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 1024);
        s.setExecutor(Executors.newFixedThreadPool(POOL));
        s.createContext("/api", ex -> {
            try {
                long wait;
                while ((wait = stallUntil.get() - System.nanoTime()) > 0) Thread.sleep(Math.min(10, wait / 1_000_000 + 1));
                ThreadLocalRandom r = ThreadLocalRandom.current();
                Thread.sleep(r.nextInt(100) == 0 ? 200 : 15 + r.nextInt(11));   // 15-25 ms; 1% com 200 ms
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            byte[] body = "{\"ok\":true}".getBytes();
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        s.start();
        return s;
    }

    static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5)).build();

    static boolean call(URI uri) {
        try {
            HttpResponse<Void> r = CLIENT.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).build(),
                    HttpResponse.BodyHandlers.discarding());
            return r.statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    static double pct(long[] s, double p) { return s[Math.max(0, (int) Math.ceil(p / 100 * s.length) - 1)] / 1e6; }

    static String summary(List<Long> lat) {
        long[] v = lat.stream().mapToLong(Long::longValue).sorted().toArray();
        if (v.length == 0) return "sem amostras";
        return String.format(Locale.ROOT, "n=%d  p50=%.1f  p95=%.1f  p99=%.1f  p99.9=%.1f  max=%.1f ms",
                v.length, pct(v, 50), pct(v, 95), pct(v, 99), pct(v, 99.9), v[v.length - 1] / 1e6);
    }

    /** Carga ABERTA: chegadas no ritmo fixo; latencia = fim - instante em que DEVERIA ter comecado. */
    static List<Long> openLoop(URI uri, int rate, int secs) throws Exception {
        List<Long> lat = Collections.synchronizedList(new ArrayList<>());
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            long start = System.nanoTime();
            for (long i = 0; i < (long) rate * secs; i++) {
                long due = start + i * 1_000_000_000L / rate;
                long wait = due - System.nanoTime();
                if (wait > 0) java.util.concurrent.locks.LockSupport.parkNanos(wait);
                ex.submit(() -> { if (call(uri)) lat.add(System.nanoTime() - due); });
            }
        }
        return lat;
    }

    /** Carga FECHADA: N usuarios, cada um so manda a proxima depois da resposta. */
    static List<Long> closedLoop(URI uri, int users, int secs) throws Exception {
        List<Long> lat = Collections.synchronizedList(new ArrayList<>());
        long end = System.nanoTime() + secs * 1_000_000_000L;
        List<Thread> ts = new ArrayList<>();
        for (int u = 0; u < users; u++) ts.add(Thread.ofVirtual().start(() -> {
            while (System.nanoTime() < end) { long t0 = System.nanoTime(); if (call(uri)) lat.add(System.nanoTime() - t0); }
        }));
        for (Thread t : ts) t.join();
        return lat;
    }

    public static void main(String[] a) throws Exception {
        if (a[0].equals("server")) { startServer(Integer.parseInt(a[1])); System.out.println("servidor em :" + a[1]); Thread.currentThread().join(); }
        server = startServer(0);
        URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api");
        for (int i = 0; i < 300; i++) call(uri);                                  // aquece
        if (a[0].equals("saturation")) saturation(uri); else omission(uri);
        server.stop(0);
        System.exit(0);
    }

    static void saturation(URI uri) throws Exception {
        double svc = 0.02 * 0.99 + 0.2 * 0.01;                                    // tempo medio de servico ~ 21,8 ms
        System.out.printf(Locale.ROOT, "pool=%d  tempo medio de servico ~%.1f ms  =>  capacidade teorica ~%.0f req/s%n", POOL, svc * 1000, POOL / svc);
        System.out.println("taxa (req/s)  resultado (carga aberta, 8 s por linha, latencia desde o instante pretendido)");
        for (int rate : new int[]{200, 400, 600, 700, 800}) {
            System.out.printf(Locale.ROOT, "%12d  %s%n", rate, summary(openLoop(uri, rate, 8)));
        }
    }

    static void omission(URI uri) throws Exception {
        // o servidor "congela" por 3 s, 4 s depois do inicio, em cada teste
        System.out.println("Falha injetada: servidor congelado por 3 s (a partir de t = 4 s); teste de 10 s; ~21,8 ms de servico");
        Thread.ofPlatform().daemon().start(() -> { try { Thread.sleep(4000); } catch (InterruptedException e) { } stallUntil.set(System.nanoTime() + 3_000_000_000L); });
        List<Long> closed = closedLoop(uri, 12, 10);
        System.out.println("FECHADA (12 usuarios):          " + summary(closed));
        stallUntil.set(0);
        Thread.ofPlatform().daemon().start(() -> { try { Thread.sleep(4000); } catch (InterruptedException e) { } stallUntil.set(System.nanoTime() + 3_000_000_000L); });
        List<Long> open = openLoop(uri, 400, 10);
        System.out.println("ABERTA (400 req/s):             " + summary(open));
    }
}
