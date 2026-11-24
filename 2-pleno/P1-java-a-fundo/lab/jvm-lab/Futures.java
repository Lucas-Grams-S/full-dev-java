import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/** P1.06 — CompletableFuture. Cenário do PayFlow: "saldo convertido" (J6) consultando 3 provedores de câmbio. Uso: java Futures.java <chain|fanout|errors|timeout|pitfall|median> */
public class Futures {

    static final ExecutorService IO = Executors.newFixedThreadPool(8, r -> { Thread t = new Thread(r, "io-" + System.nanoTime() % 1000); t.setDaemon(true); return t; });

    /** Simula uma chamada HTTP a um provedor de câmbio (J6: RestClient com timeout). */
    static BigDecimal rate(String provider, long millis, boolean fail) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException("interrompido"); }
        if (fail) throw new IllegalStateException(provider + " fora do ar");
        return new BigDecimal("5.10").add(BigDecimal.valueOf(provider.length(), 2));
    }

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "chain" : args[0]) {
            case "chain" -> chain();
            case "fanout" -> fanout();
            case "errors" -> errors();
            case "timeout" -> timeout();
            case "pitfall" -> pitfall();
            case "median" -> medianDemo();
            default -> System.out.println("?");
        }
    }

    static void chain() {
        CompletableFuture<String> cf = CompletableFuture
                .supplyAsync(() -> rate("ecb", 100, false), IO)                       // 1) assíncrono
                .thenApply(r -> new BigDecimal("830.00").multiply(r))                   // 2) transforma (map)
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> v.setScale(2, java.math.RoundingMode.HALF_EVEN), IO))   // 3) encadeia outro futuro (flatMap)
                .thenApply(v -> "saldo convertido = " + v);
        System.out.println("(main não bloqueou: isDone=" + cf.isDone() + ")");
        System.out.println(cf.join());
    }

    static void fanout() {
        long t0 = System.nanoTime();
        var a = CompletableFuture.supplyAsync(() -> rate("ecb", 300, false), IO);
        var b = CompletableFuture.supplyAsync(() -> rate("bcb", 500, false), IO);
        var c = CompletableFuture.supplyAsync(() -> rate("xe", 400, false), IO);
        CompletableFuture.allOf(a, b, c).join();                                       // espera as três (em paralelo)
        System.out.printf("allOf: ecb=%s bcb=%s xe=%s em %.0f ms (sequencial seria ~1200 ms)%n", a.join(), b.join(), c.join(), (System.nanoTime() - t0) / 1e6);
        t0 = System.nanoTime();
        var x = CompletableFuture.supplyAsync(() -> rate("lento", 800, false), IO);
        var y = CompletableFuture.supplyAsync(() -> rate("rapido", 200, false), IO);
        Object first = CompletableFuture.anyOf(x, y).join();                           // a primeira que terminar
        System.out.printf("anyOf: %s em %.0f ms%n", first, (System.nanoTime() - t0) / 1e6);
        t0 = System.nanoTime();
        var s1 = CompletableFuture.supplyAsync(() -> rate("ecb", 300, false), IO);
        var s2 = CompletableFuture.supplyAsync(() -> rate("bcb", 500, false), IO);
        System.out.printf("thenCombine: média=%s em %.0f ms%n", s1.thenCombine(s2, (p, q) -> p.add(q).divide(BigDecimal.TWO, 4, java.math.RoundingMode.HALF_EVEN)).join(), (System.nanoTime() - t0) / 1e6);
    }

    static void errors() {
        var failing = CompletableFuture.supplyAsync(() -> rate("bcb", 50, true), IO);
        try { failing.join(); } catch (CompletionException e) { System.out.println("join():  " + e.getClass().getSimpleName() + " -> causa " + e.getCause()); }
        try { failing.get(); } catch (Exception e) { System.out.println("get():   " + e.getClass().getSimpleName() + " -> causa " + e.getCause()); }
        System.out.println("exceptionally: " + CompletableFuture.supplyAsync(() -> rate("bcb", 50, true), IO).exceptionally(ex -> new BigDecimal("5.00")).join());
        System.out.println("handle:        " + CompletableFuture.supplyAsync(() -> rate("bcb", 50, true), IO)
                .handle((v, ex) -> ex == null ? "ok " + v : "falhou: " + ex.getClass().getSimpleName() + "/" + ex.getCause().getMessage()).join());
        // o erro atravessa a cadeia: os passos intermediários são pulados
        System.out.println("cadeia:        " + CompletableFuture.supplyAsync(() -> rate("bcb", 50, true), IO)
                .thenApply(v -> { System.out.println("  (este passo NÃO roda)"); return v; })
                .handle((v, ex) -> ex != null ? "tratado no fim" : "ok").join());
        // allOf: um erro não cancela os outros e o allOf espera todos
        var ok = CompletableFuture.supplyAsync(() -> rate("ecb", 200, false), IO);
        var bad = CompletableFuture.supplyAsync(() -> rate("bcb", 50, true), IO);
        try { CompletableFuture.allOf(ok, bad).join(); } catch (CompletionException e) { System.out.println("allOf falhou: " + e.getCause().getMessage() + "; o outro já terminou? " + ok.isDone() + " (o allOf só completa quando TODOS completam)"); }
    }

    static void timeout() throws Exception {
        long t0 = System.nanoTime();
        var slow = CompletableFuture.supplyAsync(() -> rate("lento", 2000, false), IO).orTimeout(300, TimeUnit.MILLISECONDS);
        try { slow.join(); } catch (CompletionException e) { System.out.printf("orTimeout: %s após %.0f ms%n", e.getCause().getClass().getSimpleName(), (System.nanoTime() - t0) / 1e6); }
        var withFallback = CompletableFuture.supplyAsync(() -> rate("lento", 2000, false), IO).completeOnTimeout(new BigDecimal("5.00"), 300, TimeUnit.MILLISECONDS);
        System.out.println("completeOnTimeout: " + withFallback.join());
        // cancel() NÃO interrompe a thread que está executando a tarefa
        var cancelled = CompletableFuture.supplyAsync(() -> { try { Thread.sleep(600); System.out.println("  a tarefa cancelada continuou até o fim"); } catch (InterruptedException e) { System.out.println("  interrompida"); } return 1; }, IO);
        Thread.sleep(100);
        System.out.println("cancel(true) -> " + cancelled.cancel(true) + ", isCancelled=" + cancelled.isCancelled());
        Thread.sleep(700);
    }

    static void pitfall() throws Exception {
        // sem executor explícito: ForkJoinPool.commonPool (tamanho = núcleos - 1); tarefas bloqueantes o esgotam
        System.out.println("commonPool parallelism = " + ForkJoinPool.commonPool().getParallelism() + ", núcleos = " + Runtime.getRuntime().availableProcessors());
        int n = ForkJoinPool.commonPool().getParallelism() * 2;
        long t0 = System.nanoTime();
        List<CompletableFuture<Void>> fs = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) fs.add(CompletableFuture.runAsync(() -> { try { Thread.sleep(500); } catch (InterruptedException e) { } }));   // sem executor
        CompletableFuture.allOf(fs.toArray(CompletableFuture[]::new)).join();
        System.out.printf("%d tarefas bloqueantes de 500 ms no commonPool: %.0f ms%n", n, (System.nanoTime() - t0) / 1e6);
        t0 = System.nanoTime();
        try (ExecutorService big = Executors.newFixedThreadPool(n)) {
            fs.clear();
            for (int i = 0; i < n; i++) fs.add(CompletableFuture.runAsync(() -> { try { Thread.sleep(500); } catch (InterruptedException e) { } }, big));
            CompletableFuture.allOf(fs.toArray(CompletableFuture[]::new)).join();
        }
        System.out.printf("as mesmas com um pool próprio de %d threads: %.0f ms%n", n, (System.nanoTime() - t0) / 1e6);
        // quem roda o thenApply? depende de quem completou
        var cf = CompletableFuture.supplyAsync(() -> { try { Thread.sleep(50); } catch (InterruptedException e) { } return 1; }, IO);
        cf.thenApply(v -> { System.out.println("thenApply rodou em: " + Thread.currentThread().getName().replaceAll("\\d+", "N")); return v; }).join();
        var done = CompletableFuture.completedFuture(1);
        done.thenApply(v -> { System.out.println("sobre futuro já completo, rodou em: " + Thread.currentThread().getName()); return v; });
    }

    // ---- solução da "Pausa para lógica" (P1.06) ----
    static BigDecimal median(List<BigDecimal> xs) {
        var s = xs.stream().sorted().toList();
        int n = s.size();
        return n % 2 == 1 ? s.get(n / 2) : s.get(n / 2 - 1).add(s.get(n / 2)).divide(BigDecimal.TWO, 4, java.math.RoundingMode.HALF_EVEN);
    }

    static BigDecimal convertedBalance(BigDecimal balance, List<java.util.function.Supplier<BigDecimal>> providers) {
        List<CompletableFuture<Optional<BigDecimal>>> fs = providers.stream()
            .map(p -> CompletableFuture.supplyAsync(p, IO)
                     .orTimeout(400, TimeUnit.MILLISECONDS)
                     .handle((v, ex) -> ex == null ? Optional.of(v) : Optional.<BigDecimal>empty()))   // falha ou prazo: sem voto
            .toList();
        CompletableFuture.allOf(fs.toArray(CompletableFuture[]::new)).join();          // nunca lança: handle trata tudo
        List<BigDecimal> ok = fs.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList();
        if (ok.size() < 2) throw new IllegalStateException("poucas cotações: " + ok.size());
        return balance.multiply(median(ok)).setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    static void medianDemo() {
        long t0 = System.nanoTime();
        var v = convertedBalance(new BigDecimal("830.00"), List.of(() -> rate("ecb", 100, false), () -> rate("bcb", 150, false), () -> rate("xe", 200, false)));
        System.out.printf("3 respondem:            %s em %.0f ms%n", v, (System.nanoTime() - t0) / 1e6);
        t0 = System.nanoTime();
        v = convertedBalance(new BigDecimal("830.00"), List.of(() -> rate("ecb", 100, false), () -> rate("bcb", 150, true), () -> rate("xe", 1500, false), () -> rate("abc", 120, false)));
        System.out.printf("1 falha, 1 estoura (2 ok de 4): %s em %.0f ms%n", v, (System.nanoTime() - t0) / 1e6);
        t0 = System.nanoTime();
        try { convertedBalance(new BigDecimal("830.00"), List.of(() -> rate("ecb", 100, false), () -> rate("bcb", 150, true), () -> rate("xe", 1500, false))); }
        catch (IllegalStateException e) { System.out.printf("só 1 ok:               IllegalStateException(%s) em %.0f ms%n", e.getMessage(), (System.nanoTime() - t0) / 1e6); }
    }
}
