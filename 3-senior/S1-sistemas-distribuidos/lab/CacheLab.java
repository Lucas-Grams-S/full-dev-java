import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** S1.04: taxa de acerto (LRU x Zipf), corrida do cache-aside, stampede e write-behind. */
public class CacheLab {
    public static void main(String[] args) throws Exception {
        hitRatio(); raceCacheAside(); stampede(); writeBehind();
    }

    static void hitRatio() {
        System.out.println("== 1. Taxa de acerto de um LRU: 1.000.000 leituras de 100.000 chaves, popularidade Zipf (s=1,0) ==");
        int keys = 100_000; double[] cdf = new double[keys]; double sum = 0;
        for (int i = 0; i < keys; i++) { sum += 1.0 / (i + 1); cdf[i] = sum; }
        for (double pct : new double[]{0.001, 0.01, 0.05, 0.10, 0.30}) {
            int cap = (int) (keys * pct);
            LinkedHashMap<Integer, Boolean> lru = new LinkedHashMap<>(16, 0.75f, true) { protected boolean removeEldestEntry(Map.Entry<Integer, Boolean> e) { return size() > cap; } };
            Random r = new Random(11); int hits = 0, n = 1_000_000;
            for (int i = 0; i < n; i++) {
                double x = r.nextDouble() * sum; int lo = 0, hi = keys - 1; while (lo < hi) { int m = (lo + hi) >>> 1; if (cdf[m] < x) lo = m + 1; else hi = m; }
                if (lru.get(lo) != null) hits++; else lru.put(lo, true);
            }
            System.out.printf("cache com %5.1f%% das chaves (%6d): taxa de acerto = %.1f%%%n", pct * 100, cap, 100.0 * hits / n);
        }
        // distribuicao uniforme: o mesmo cache de 10% acerta ~10%
        int cap = keys / 10; LinkedHashMap<Integer, Boolean> lru = new LinkedHashMap<>(16, 0.75f, true) { protected boolean removeEldestEntry(Map.Entry<Integer, Boolean> e) { return size() > cap; } };
        Random r = new Random(11); int hits = 0;
        for (int i = 0; i < 1_000_000; i++) { int k = r.nextInt(keys); if (lru.get(k) != null) hits++; else lru.put(k, true); }
        System.out.printf("MESMO cache de 10%% com acesso UNIFORME (sem chaves quentes): taxa de acerto = %.1f%%%n", 100.0 * hits / 1_000_000);
    }

    /** Intercalacao deterministica do cache-aside: leitor le o banco velho, escritor atualiza e invalida, leitor preenche com o velho. */
    static void raceCacheAside() {
        System.out.println("\n== 2. Corrida no cache-aside (intercalacao deterministica) ==");
        Map<String, String> db = new HashMap<>(); Map<String, String> cache = new HashMap<>();
        db.put("saldo:1", "100.00");
        String lido = db.get("saldo:1");                    // L1: leitor le o banco (miss no cache)
        db.put("saldo:1", "40.00"); cache.remove("saldo:1"); // E1/E2: escritor grava e invalida
        cache.put("saldo:1", lido);                          // L2: leitor preenche o cache com o valor VELHO
        System.out.println("banco=" + db.get("saldo:1") + " cache=" + cache.get("saldo:1") + "  <- cache velho ate o TTL expirar");
        // mitigacao: preencher com versao; so aceita se versao >= atual
        Map<String, long[]> dbv = new HashMap<>(); Map<String, Object[]> cv = new HashMap<>();
        dbv.put("saldo:1", new long[]{1, 10000});
        long[] snap = dbv.get("saldo:1").clone();             // leitor guarda (versao, valor)
        dbv.put("saldo:1", new long[]{2, 4000}); cv.remove("saldo:1");
        // preenchimento condicional: so se o cache nao tiver nada MAIS NOVO e a versao ainda for a do banco
        boolean ok = dbv.get("saldo:1")[0] == snap[0];
        if (ok) cv.put("saldo:1", new Object[]{snap[0], snap[1]});
        System.out.println("com preenchimento condicional por versao: preencheu? " + ok + " cache=" + (cv.containsKey("saldo:1") ? "presente" : "vazio (a proxima leitura busca o valor novo)"));
    }

    static void stampede() throws Exception {
        System.out.println("\n== 3. Stampede: 200 threads pedem a MESMA chave expirada; o carregamento leva 100 ms ==");
        for (boolean single : new boolean[]{false, true}) {
            AtomicInteger loads = new AtomicInteger(); ConcurrentHashMap<String, CompletableFuture<String>> inflight = new ConcurrentHashMap<>();
            ExecutorService ex = Executors.newFixedThreadPool(200); CountDownLatch go = new CountDownLatch(1);
            List<Future<?>> fs = new ArrayList<>(); long t0 = System.nanoTime();
            for (int i = 0; i < 200; i++) fs.add(ex.submit(() -> { go.await();
                if (!single) { loads.incrementAndGet(); Thread.sleep(100); }
                else inflight.computeIfAbsent("k", k -> CompletableFuture.supplyAsync(() -> { loads.incrementAndGet(); try { Thread.sleep(100); } catch (InterruptedException e) { } return "v"; })).get();
                return null; }));
            go.countDown(); for (Future<?> f : fs) f.get(); ex.shutdown();
            System.out.printf("%-34s consultas ao banco = %3d ; tempo total = %d ms%n", single ? "single-flight (uma carrega)" : "sem protecao (cada uma carrega)", loads.get(), (System.nanoTime() - t0) / 1_000_000);
        }
    }

    static void writeBehind() {
        System.out.println("\n== 4. Write-through x write-behind: 1.000 escritas, o no cai quando 37 ainda nao foram ao banco ==");
        Map<Integer, Integer> dbWT = new HashMap<>(), dbWB = new HashMap<>(); Deque<int[]> pending = new ArrayDeque<>();
        for (int i = 1; i <= 1000; i++) {
            dbWT.put(i, i * 10);                             // write-through: banco e cache juntos
            pending.add(new int[]{i, i * 10});               // write-behind: so a fila em memoria
            if (pending.size() > 37) { int[] p = pending.poll(); dbWB.put(p[0], p[1]); } // flush atrasado
        }
        System.out.printf("apos a queda: write-through tem %d registros no banco; write-behind tem %d (perdeu %d, que estavam so na memoria)%n", dbWT.size(), dbWB.size(), pending.size());
    }
}
