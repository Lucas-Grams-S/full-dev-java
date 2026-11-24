import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * P1.03/P1.04 — carga que imita um serviço: muito lixo de vida curta (objetos de requisição), um cache LRU que sobrevive
 * (dados de vida longa) e, de vez em quando, um buffer grande ("humongous" no G1).
 * Uso: java -Xmx512m -Xlog:gc [-XX:+UseG1GC|UseParallelGC|UseSerialGC|UseZGC] GcLoad.java [segundos] [humongous]
 */
public class GcLoad {

    record Entry(String key, byte[] payload) {}

    static final int CACHE_MAX = 60_000;                          // ~ 60 000 × ~1 KiB ≈ 60 MiB vivos
    static final Map<String, Entry> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Entry> e) { return size() > CACHE_MAX; }
    };

    static long sink;

    public static void main(String[] args) throws Exception {
        int seconds = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        boolean humongous = args.length > 1 && args[1].equals("humongous");
        long end = System.nanoTime() + seconds * 1_000_000_000L;
        long ops = 0, maxLatency = 0;
        var rnd = ThreadLocalRandom.current();
        while (System.nanoTime() < end) {
            long t0 = System.nanoTime();
            // "requisição": cria vários objetos pequenos que morrem logo
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 20; i++) sb.append(rnd.nextInt()).append(',');
            String body = sb.toString();
            byte[] tmp = new byte[2048];
            sink += body.length() + tmp.length;
            if (ops % 3 == 0) {                                   // 1/3 das requisições grava no cache de vida longa
                String key = "acc-" + rnd.nextInt(200_000);
                synchronized (cache) { cache.put(key, new Entry(key, new byte[1024])); }
            }
            if (humongous && ops % 2_000 == 0) sink += new byte[4 * 1024 * 1024].length;   // 4 MiB de uma vez
            long lat = System.nanoTime() - t0;
            if (lat > maxLatency) maxLatency = lat;
            ops++;
        }
        long gcCount = 0, gcMillis = 0;
        StringBuilder names = new StringBuilder();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(0, gc.getCollectionCount()); gcMillis += Math.max(0, gc.getCollectionTime());
            names.append(gc.getName()).append(" | ");
        }
        System.out.printf("RESUMO ops=%d  ops/s=%.0f  pior latência de uma operação=%.2f ms  coletas=%d  tempo em GC (MXBean)=%d ms%n",
                ops, ops / (double) seconds, maxLatency / 1e6, gcCount, gcMillis);
        System.out.println("coletores: " + names);
        System.out.println("sink=" + sink + " cache=" + cache.size());
    }
}
