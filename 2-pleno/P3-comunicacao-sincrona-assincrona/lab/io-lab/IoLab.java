import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** P3.01: bloqueante x não bloqueante; I/O-bound x CPU-bound. Uso: java IoLab.java <io|cpu|http> */
public class IoLab {
    static final int TASKS = 200;

    public static void main(String[] args) throws Exception {
        System.out.println("JDK " + Runtime.version() + " | núcleos=" + Runtime.getRuntime().availableProcessors());
        switch (args.length == 0 ? "io" : args[0]) {
            case "io" -> ioBound();
            case "cpu" -> cpuBound();
            case "http" -> http();
            default -> System.out.println("modos: io | cpu | http");
        }
    }

    static long time(ExecutorService pool, Runnable task, int n) throws Exception {
        long t0 = System.nanoTime();
        var fs = new ArrayList<Future<?>>();
        for (int i = 0; i < n; i++) fs.add(pool.submit(task));
        for (var f : fs) f.get();
        pool.shutdown();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    /** Tarefa I/O-bound simulada: a thread fica 100 ms PARADA esperando (como esperar o banco ou uma API). */
    static void ioBound() throws Exception {
        Runnable io = () -> { try { Thread.sleep(100); } catch (InterruptedException e) { throw new RuntimeException(e); } };
        System.out.println("== I/O-bound: " + TASKS + " tarefas de 100 ms de espera ==");
        for (int n : new int[] {1, 4, 16, 50, 200}) {
            long ms = time(Executors.newFixedThreadPool(n), io, TASKS);
            System.out.printf("pool de %3d threads de plataforma: %6d ms (ideal %d ms)%n", n, ms, TASKS * 100L / n);
        }
        long ms = time(Executors.newVirtualThreadPerTaskExecutor(), io, TASKS);
        System.out.printf("threads virtuais (uma por tarefa):  %6d ms%n", ms);
        ms = time(Executors.newVirtualThreadPerTaskExecutor(), io, 10_000);
        System.out.printf("threads virtuais, 10 000 tarefas:   %6d ms%n", ms);
    }

    /** Tarefa CPU-bound: a thread trabalha o tempo todo (hash repetido). */
    static void cpuBound() throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();
        Runnable cpu = () -> {
            try {
                var md = MessageDigest.getInstance("SHA-256");
                byte[] b = new byte[64];
                for (int i = 0; i < 300_000; i++) b = md.digest(b);
            } catch (Exception e) { throw new RuntimeException(e); }
        };
        int tasks = cores * 4;
        System.out.println("== CPU-bound: " + tasks + " tarefas (300 000 SHA-256 cada) ==");
        time(Executors.newFixedThreadPool(cores), cpu, tasks); // aquecimento (JIT)
        for (int n : new int[] {1, cores / 2, cores, cores * 4, 200}) {
            if (n < 1) continue;
            long ms = time(Executors.newFixedThreadPool(n), cpu, tasks);
            System.out.printf("pool de %3d threads: %6d ms%n", n, ms);
        }
        long ms = time(Executors.newVirtualThreadPerTaskExecutor(), cpu, tasks);
        System.out.printf("threads virtuais:    %6d ms%n", ms);
    }

    /** Cliente HTTP: bloqueante (HttpClient.send) x não bloqueante (sendAsync) contra um servidor local lento (100 ms). */
    static void http() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/slow", ex -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            byte[] body = "ok".getBytes();
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/slow");
        var version = System.getProperty("http.version", "HTTP_1_1");
        java.util.function.Supplier<HttpClient> newClient = () -> HttpClient.newBuilder().version(HttpClient.Version.valueOf(version)).build();
        System.out.println("HttpClient.Version = " + version + " (um cliente NOVO por variante: o pool de conexões não pode favorecer ninguém)");
        var client = newClient.get();
        var req = HttpRequest.newBuilder(uri).build();
        client.send(req, HttpResponse.BodyHandlers.ofString()); // aquecimento
        int n = 200;

        long t0 = System.nanoTime();
        for (int i = 0; i < 20; i++) client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.printf("sequencial bloqueante, 20 chamadas:        %5d ms%n", (System.nanoTime() - t0) / 1_000_000);

        client = newClient.get();
        client.send(req, HttpResponse.BodyHandlers.ofString());
        final var pclient = client;
        t0 = System.nanoTime();
        var pool = Executors.newFixedThreadPool(10);
        var fs = new ArrayList<Future<?>>();
        for (int i = 0; i < n; i++) fs.add(pool.submit(() -> pclient.send(req, HttpResponse.BodyHandlers.ofString())));
        for (var f : fs) f.get();
        pool.shutdown();
        System.out.printf("bloqueante, pool de 10 threads, %d chamadas: %5d ms%n", n, (System.nanoTime() - t0) / 1_000_000);

        client = newClient.get();
        client.send(req, HttpResponse.BodyHandlers.ofString());
        t0 = System.nanoTime();
        var futures = new ArrayList<CompletableFuture<HttpResponse<String>>>();
        for (int i = 0; i < n; i++) futures.add(client.sendAsync(req, HttpResponse.BodyHandlers.ofString()));
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();
        System.out.printf("não bloqueante (sendAsync), %d chamadas:    %5d ms  (nenhuma thread esperando)%n", n, (System.nanoTime() - t0) / 1_000_000);

        final var vclient = newClient.get();
        vclient.send(req, HttpResponse.BodyHandlers.ofString());
        t0 = System.nanoTime();
        try (var vt = Executors.newVirtualThreadPerTaskExecutor()) {
            var vfs = new ArrayList<Future<?>>();
            for (int i = 0; i < n; i++) vfs.add(vt.submit(() -> vclient.send(req, HttpResponse.BodyHandlers.ofString())));
            for (var f : vfs) f.get();
        }
        System.out.printf("bloqueante em threads virtuais, %d chamadas: %5d ms  (código bloqueante simples)%n", n, (System.nanoTime() - t0) / 1_000_000);
        server.stop(0);
    }
}
