import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * S4.04 — a chaos experiment in miniature (MODEL: two local HTTP servers, not PayFlow).
 * "payflow" has 16 request threads and calls "notifications" synchronously inside the request.
 * We inject latency into "notifications" and test the hypothesis:
 *   "transfers keep p99 < 400 ms and >= 99% success while notifications is slow".
 * Run: java ChaosLab.java <variant> where variant = no-timeout | timeout | timeout-async
 */
public class ChaosLab {
    static final AtomicInteger injectedMs = new AtomicInteger(0);
    static final int REQUEST_THREADS = 16;
    static int RATE = 40;                 // requests per second (open loop; 2nd argument overrides)
    static final int PHASE_S = 6;
    static final int SLOW_MS = 3000;      // the injected fault
    static final int ABORT_ERR_PCT = 50;  // abort condition ("stop condition")

    public static void main(String[] args) throws Exception {
        String variant = args.length > 0 ? args[0] : "timeout";
        if (args.length > 1) RATE = Integer.parseInt(args[1]);
        HttpServer notif = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        notif.setExecutor(Executors.newCachedThreadPool());
        notif.createContext("/notify", ex -> {
            try { Thread.sleep(5 + injectedMs.get()); } catch (InterruptedException ignored) {}
            byte[] b = "ok".getBytes();
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b); ex.close();
        });
        notif.start();
        int notifPort = notif.getAddress().getPort();

        HttpClient client = HttpClient.newBuilder().executor(Executors.newCachedThreadPool()).build();
        ExecutorService asyncPool = Executors.newFixedThreadPool(4);
        Semaphore asyncQueue = new Semaphore(50); // bulkhead for the fire-and-forget calls
        AtomicInteger asyncDropped = new AtomicInteger();

        HttpServer payflow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 200);
        payflow.setExecutor(Executors.newFixedThreadPool(REQUEST_THREADS));
        payflow.createContext("/transfer", ex -> {
            int status = 200;
            try {
                Thread.sleep(8); // "business work"
                HttpRequest r = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + notifPort + "/notify"))
                        .timeout(variant.equals("no-timeout") ? Duration.ofSeconds(30) : Duration.ofMillis(200)).build();
                switch (variant) {
                    case "no-timeout", "timeout" -> {
                        try { client.send(r, HttpResponse.BodyHandlers.discarding()); }
                        catch (Exception e) { if (variant.equals("no-timeout")) status = 500; /* with timeout: degrade, transfer still ok */ }
                    }
                    case "timeout-async" -> {
                        if (asyncQueue.tryAcquire()) {
                            asyncPool.submit(() -> {
                                try { client.send(r, HttpResponse.BodyHandlers.discarding()); } catch (Exception ignored) {}
                                finally { asyncQueue.release(); }
                            });
                        } else asyncDropped.incrementAndGet();
                    }
                    default -> throw new IllegalArgumentException(variant);
                }
            } catch (Exception e) { status = 500; }
            byte[] b = "done".getBytes();
            ex.sendResponseHeaders(status, b.length);
            ex.getResponseBody().write(b); ex.close();
        });
        payflow.start();
        String url = "http://127.0.0.1:" + payflow.getAddress().getPort() + "/transfer";

        System.out.println("variant=" + variant + "  load=" + RATE + " req/s  request threads=" + REQUEST_THREADS
                + "  fault=" + SLOW_MS + " ms injected into notifications  (MODEL)");
        phase("1 steady state (no fault)", url, 0, variant);
        injectedMs.set(SLOW_MS);
        boolean aborted = phase("2 fault injected", url, SLOW_MS, variant);
        injectedMs.set(0);
        if (aborted) System.out.println("   -> ABORT CONDITION hit: fault removed early (error ratio >= " + ABORT_ERR_PCT + "%)");
        Thread.sleep(SLOW_MS + 500); // let the stuck threads drain
        phase("3 after the fault is removed", url, 0, variant);
        if (variant.equals("timeout-async")) System.out.println("   notifications dropped by the bulkhead: " + asyncDropped.get());
        System.exit(0);
    }

    /** open-loop load: a new request every 1/RATE s, regardless of how long the previous ones take. */
    static boolean phase(String name, String url, int fault, String variant) throws Exception {
        HttpClient c = HttpClient.newBuilder().build();
        List<Future<long[]>> futs = new ArrayList<>();
        List<Long> starts = new ArrayList<>();
        ExecutorService callers = Executors.newCachedThreadPool();
        long t0 = System.nanoTime();
        int total = RATE * PHASE_S;
        boolean aborted = false;
        for (int i = 0; i < total; i++) {
            long due = t0 + (long) i * 1_000_000_000L / RATE;
            long wait = due - System.nanoTime();
            if (wait > 0) Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
            starts.add(System.nanoTime());
            futs.add(callers.submit(() -> {
                long s = System.nanoTime();
                int code;
                try {
                    code = c.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).build(),
                            HttpResponse.BodyHandlers.discarding()).statusCode();
                } catch (Exception e) { code = 599; }
                return new long[]{(System.nanoTime() - s) / 1_000_000, code};
            }));
            if (fault > 0 && i > 0 && i % RATE == 0) { // check the stop condition once per second
                // a request that is still running after 1 s counts as bad: failures that HANG never show up as "done"
                int done = 0, bad = 0;
                for (int k = 0; k < futs.size(); k++) {
                    Future<long[]> f = futs.get(k);
                    if (f.isDone()) { done++; try { if (f.get()[1] >= 500) bad++; } catch (Exception ignored) {} }
                    else if ((System.nanoTime() - starts.get(k)) > 1_000_000_000L) { done++; bad++; }
                }
                if (done >= 20 && bad * 100 / done >= ABORT_ERR_PCT) { aborted = true; break; }
            }
        }
        List<Long> lat = new ArrayList<>(); int ok = 0, err = 0;
        for (Future<long[]> f : futs) {
            long[] r = f.get();
            lat.add(r[0]);
            if (r[1] < 500) ok++; else err++;
        }
        callers.shutdownNow();
        Collections.sort(lat);
        long p50 = lat.get(lat.size() / 2), p99 = lat.get((int) Math.min(lat.size() - 1, Math.ceil(lat.size() * 0.99) - 1));
        double succ = 100.0 * ok / (ok + err);
        boolean hyp = p99 < 400 && succ >= 99.0;
        System.out.printf(Locale.ROOT, "%-34s n=%3d  p50=%5d ms  p99=%5d ms  success=%6.1f%%  hypothesis(p99<400ms & >=99%%): %s%n",
                name, ok + err, p50, p99, succ, hyp ? "HOLDS" : "REFUTED");
        return aborted;
    }
}
