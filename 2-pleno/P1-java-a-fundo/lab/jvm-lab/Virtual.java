import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** P1.10 — virtual threads. Uso: java Virtual.java <basics|scale|cpu|threadlocal|scoped|semaphore> */
public class Virtual {

    static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();       // ScopedValue: definitivo no Java 25 (JEP 506)
    static final ThreadLocal<String> TL = new ThreadLocal<>();

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "basics" : args[0]) {
            case "basics" -> basics();
            case "scale" -> scale();
            case "cpu" -> cpu();
            case "threadlocal" -> threadLocal();
            case "scoped" -> scoped();
            case "semaphore" -> semaphore();
            default -> System.out.println("?");
        }
    }

    static void basics() throws Exception {
        Thread v = Thread.ofVirtual().name("meu-vt").unstarted(() -> {
            Thread t = Thread.currentThread();
            System.out.println("isVirtual=" + t.isVirtual() + " daemon=" + t.isDaemon() + " nome=" + t.getName() + " grupo=" + t.getThreadGroup().getName());
            System.out.println("toString: " + t);
        });
        v.start(); v.join();
        Thread anon = Thread.startVirtualThread(() -> System.out.println("nome vazio por padrão: '" + Thread.currentThread().getName() + "'"));
        anon.join();
        Thread p = Thread.ofPlatform().unstarted(() -> {}); 
        System.out.println("plataforma: isVirtual=" + p.isVirtual());
    }

    /** N tarefas que "esperam I/O" (sleep) por 500 ms. */
    static double run(ExecutorService ex, int n, long ioMillis) throws Exception {
        long t0 = System.nanoTime();
        List<Future<Integer>> fs = new ArrayList<>();
        for (int i = 0; i < n; i++) { int k = i; fs.add(ex.submit(() -> { Thread.sleep(ioMillis); return k; })); }
        for (Future<Integer> f : fs) f.get();
        return (System.nanoTime() - t0) / 1e6;
    }

    static void scale() throws Exception {
        int n = 2000;
        try (ExecutorService pool = Executors.newFixedThreadPool(100)) { System.out.printf("pool de 100 threads de plataforma, %d tarefas de 500 ms: %.0f ms%n", n, run(pool, n, 500)); }
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) { System.out.printf("uma thread virtual por tarefa,     %d tarefas de 500 ms: %.0f ms%n", n, run(vt, n, 500)); }
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) { System.out.printf("uma thread virtual por tarefa,   %d tarefas de 500 ms: %.0f ms%n", 100_000, run(vt, 100_000, 500)); }
    }

    static long spin(long n) { long x = 0; for (long i = 0; i < n; i++) x += i ^ (x >>> 3); return x; }

    static void cpu() throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();
        int tasks = cores * 4; long work = 400_000_000L;
        long[] sink = new long[tasks];
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            long t0 = System.nanoTime(); List<Future<?>> fs = new ArrayList<>();
            for (int i = 0; i < tasks; i++) { int k = i; fs.add(vt.submit(() -> { sink[k] = spin(work); })); }
            for (var f : fs) f.get();
            System.out.printf("virtuais, %d tarefas de CPU em %d núcleos: %.0f ms%n", tasks, cores, (System.nanoTime() - t0) / 1e6);
        }
        try (ExecutorService pool = Executors.newFixedThreadPool(cores)) {
            long t0 = System.nanoTime(); List<Future<?>> fs = new ArrayList<>();
            for (int i = 0; i < tasks; i++) { int k = i; fs.add(pool.submit(() -> { sink[k] = spin(work); })); }
            for (var f : fs) f.get();
            System.out.printf("pool de %d threads de plataforma, as mesmas tarefas: %.0f ms  (virtual não acelera CPU)%n", cores, (System.nanoTime() - t0) / 1e6);
        }
    }

    static void threadLocal() throws Exception {
        TL.set("main");
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            vt.submit(() -> { System.out.println("ThreadLocal na virtual: " + TL.get()); TL.set("x"); return null; }).get();
        }
        // herança: cada thread virtual criada pelo executor começa com o ThreadLocal vazio (não herdável aqui); InheritableThreadLocal copia
        InheritableThreadLocal<String> itl = new InheritableThreadLocal<>(); itl.set("herdado");
        Thread.startVirtualThread(() -> System.out.println("InheritableThreadLocal copiado para a virtual: " + itl.get())).join();
    }

    static void scoped() throws Exception {
        ScopedValue.where(REQUEST_ID, "req-42").run(() -> {
            System.out.println("dentro do escopo: " + REQUEST_ID.get() + "  isBound=" + REQUEST_ID.isBound());
            ScopedValue.where(REQUEST_ID, "req-interno").run(() -> System.out.println("rebind aninhado:   " + REQUEST_ID.get()));
            System.out.println("voltou do aninhado: " + REQUEST_ID.get());
        });
        System.out.println("fora do escopo:    isBound=" + REQUEST_ID.isBound());
        try { REQUEST_ID.get(); } catch (java.util.NoSuchElementException e) { System.out.println("get() fora do escopo -> " + e.getClass().getSimpleName()); }
        String r = ScopedValue.where(REQUEST_ID, "req-7").call(() -> REQUEST_ID.get().toUpperCase());
        System.out.println("call() devolve valor: " + r);
    }

    static void semaphore() throws Exception {
        // com threads virtuais NÃO se limita concorrência com o tamanho do pool: use Semaphore no recurso escasso
        Semaphore db = new Semaphore(10); AtomicInteger inside = new AtomicInteger(), peak = new AtomicInteger();
        long t0 = System.nanoTime();
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1000; i++) vt.submit(() -> { db.acquire(); try { peak.accumulateAndGet(inside.incrementAndGet(), Math::max); Thread.sleep(20); inside.decrementAndGet(); } finally { db.release(); } return null; });
        }
        System.out.printf("1000 tarefas virtuais, no máximo 10 no \"banco\": pico = %d, tempo = %.0f ms (1000/10 × 20 ms = 2000 ms)%n", peak.get(), (System.nanoTime() - t0) / 1e6);
    }
}
